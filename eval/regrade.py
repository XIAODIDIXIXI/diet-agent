"""Re-score saved responses with versioned acceptance criteria; never call the model."""
import argparse
import copy
import hashlib
import json
from pathlib import Path

SLOT_NAMES = ("mealTime", "mood", "scene", "healthGoal", "cuisine", "taste", "convenience")


def payload(event):
    value = event.get("outputPayload")
    if not isinstance(value, str) or not value.strip():
        return None
    try:
        parsed = json.loads(value)
        return parsed if isinstance(parsed, (dict, list)) else None
    except (ValueError, TypeError):
        return None


def output(events, event_type):
    found = {}
    for event in events:
        if event.get("eventType") == event_type:
            found = payload(event) or {}
    return found


def observed_nodes(events):
    observed = set()
    for event in events:
        kind = event.get("eventType")
        error = bool(event.get("errorMessage"))
        if kind == "AGENT_CALL":
            if isinstance(event.get("latencyMs"), (int, float)) and (error or isinstance(event.get("outputPayload"), str)):
                observed.add(f"AGENT_CALL:{event.get('agentName')}")
        elif error or payload(event) is not None:
            observed.add(kind)
    return observed


def expected_nodes(route, response_type, has_candidates):
    nodes = ["REQUEST_RECEIVED", "USER_MESSAGE_RECORDED", "AGENT_CALL:IntentAgent", "INTENT_RECOGNIZED",
             "INTENT_REVISED", "ROUTE_SELECTED", "RESPONSE_READY", "REQUEST_FINISHED"]
    recommend = False
    if route in ("MEAL_RECOMMENDATION", "CLARIFY_NEEDED"):
        nodes += ["SLOTS_MERGED", "CLARIFY_DECISION"]
        if response_type == "CLARIFY":
            nodes += ["AGENT_CALL:ClarifyAgent"]
        else:
            recommend = True
    elif route == "MEAL_ADJUST":
        nodes += ["ADJUST_CONTEXT_RESOLVED"]
        recommend = True
    elif route == "MEAL_PLAN":
        nodes += ["PLAN_CONTEXT_RESOLVED", "MEAL_PLAN_SEARCHED"]
        nodes += ["AGENT_CALL:PlanResponseAgent", "PLAN_RESULT_BUILT", "PLAN_RESPONSE_AGENT_RESULT", "NUTRITION_GUARD_CHECKED"] if has_candidates else ["NO_MEAL_PLAN_MATCHED"]
    if recommend:
        nodes += ["MEAL_SEARCHED", "MEAL_RANKED"]
        nodes += ["AGENT_CALL:RecommendResponseAgent", "RECOMMEND_RESULT_BUILT", "RESPONSE_AGENT_RESULT", "NUTRITION_GUARD_CHECKED"] if has_candidates else ["NO_MEAL_MATCHED"]
    return nodes


def grade_turn(expected, original):
    turn = copy.deepcopy(original)
    events = turn.get("trace", {}).get("events", [])
    response = turn.get("response", {})
    route = output(events, "ROUTE_SELECTED").get("route", "")
    ranked = output(events, "MEAL_RANKED").get("ranked", [])
    plans = output(events, "MEAL_PLAN_SEARCHED").get("plannedMeals", [])
    allowed = {r["id"] for r in ranked[:3] if isinstance(r.get("id"), int)}
    allowed.update(p["mealId"] for p in plans if p.get("matched") and isinstance(p.get("mealId"), int))
    blocks = response.get("displayBlocks", []) or []
    cards = [c.get("id") for c in blocks]
    failures = []
    if not 200 <= turn.get("httpStatus", 0) < 300:
        failures.append("HTTP failure")
    if route not in expected.get("routes", []):
        failures.append(f"Unexpected route: {route}")
    if response.get("responseType") != expected.get("responseType"):
        failures.append("Unexpected responseType")
    if not response.get("speechText", "").strip():
        failures.append("Empty response")
    if expected.get("requiresCards") and len(cards) < expected.get("minCards", 1):
        failures.append("Insufficient recommendations")
    if not set(cards).issubset(allowed):
        failures.append("Card outside candidate whitelist or candidate evidence missing")
    if len(set(cards)) != len(cards):
        failures.append("Duplicate cards in same response")
    if expected.get("isAdjust") and set(cards).intersection(turn.get("priorIds", [])):
        failures.append("Repeated historical recommendation")
    meal_times = {t for card in blocks for t in card.get("mealTime", [])}
    if not set(expected.get("requiredMealTimes", [])).issubset(meal_times):
        failures.append("Missing planned meal time")
    actual_slots = turn.get("actualSlots", {})
    slot_ok = True
    for slot in SLOT_NAMES:
        values = set(actual_slots.get(slot, []))
        if not values.issuperset(expected.get("requiredSlots", {}).get(slot, [])):
            slot_ok = False
        if slot in expected.get("allowedSlots", {}) and not values.issubset(expected["allowedSlots"][slot]):
            slot_ok = False
    if not slot_ok:
        failures.append("Slot mismatch")
    nodes = expected_nodes(route or expected.get("routes", [""])[0], response.get("responseType"), bool(allowed))
    observed = observed_nodes(events)
    turn.update(passed=not failures, failures=failures, slotPassed=slot_ok, cards=cards, allowedIds=sorted(allowed), route=route,
                isAdjust=bool(expected.get("isAdjust")), requiresCards=bool(expected.get("requiresCards")),
                traceExpected=len(nodes), traceObserved=sum(n in observed for n in nodes), requiredTraceNodes=nodes,
                missingTraceNodes=[n for n in nodes if n not in observed])
    return turn


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--corpus", type=Path, default=Path(__file__).with_name("corpus.json"))
    args = parser.parse_args()
    manifest = json.loads((args.source / "manifest.json").read_text(encoding="utf-8"))
    current = json.loads(args.corpus.read_text(encoding="utf-8"))
    rows = [json.loads(line) for line in (args.source / "observations.jsonl").read_text(encoding="utf-8").splitlines() if line.strip()]
    if not (args.source / "completed.txt").exists():
        raise ValueError("Only regrade completed real-model runs; preserve incomplete-run semantics.")
    by_id = {t["id"]: t for t in current["tasks"]}
    for old in manifest["tasks"]:
        new = by_id[old["id"]]
        if [t["message"] for t in old["turns"]] != [t["message"] for t in new["turns"]]:
            raise ValueError("User inputs changed; a new model run is required")
    manifest["tasks"] = [by_id[t["id"]] for t in manifest["tasks"]]
    manifest["metadata"].update(version=current["metadata"]["version"], sourceRun=str(args.source),
                                 regradedFromSavedResponses=True, changesFrom1_0=current["metadata"].get("changesFrom1.0"),
                                 corpusSha256=hashlib.sha256(args.corpus.read_bytes()).hexdigest())
    for row in rows:
        if row["kind"] != "task":
            continue
        expected = by_id[row["id"]]
        if len(row["turns"]) != len(expected["turns"]):
            raise ValueError(f"Incomplete task {row['id']}")
        row["turns"] = [grade_turn(e, t) for e, t in zip(expected["turns"], row["turns"])]
        row["passed"] = all(t["passed"] for t in row["turns"])
        slot_turns = [t for e, t in zip(expected["turns"], row["turns"]) if "allowedSlots" in e]
        row["slotPassed"] = bool(slot_turns) and all(t["slotPassed"] for t in slot_turns)
        row.pop("reason", None)
        if not row["passed"]:
            row["reason"] = [t["failures"] for t in row["turns"] if not t["passed"]]
    args.destination.mkdir(parents=True, exist_ok=False)
    (args.destination / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    (args.destination / "observations.jsonl").write_text("".join(json.dumps(r, ensure_ascii=False) + "\n" for r in rows), encoding="utf-8")
    (args.destination / "completed.txt").write_text("Rescored saved responses; no new model calls.\n", encoding="utf-8")
    print(f"Regraded {len(rows)} saved observations into {args.destination}")


if __name__ == "__main__":
    main()
