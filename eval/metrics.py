"""Summarize saved evaluation observations without invoking models or changing labels."""
import argparse
import json
import math
from collections import Counter
from pathlib import Path


def rate(passed, total):
    return {"passed": passed, "total": total, "percent": round(100 * passed / total, 2) if total else None}


def summarize(corpus, observations):
    by_key = {}
    for row in observations:
        key = (row["kind"], row["id"])
        if key in by_key:
            raise ValueError(f"Duplicate observation: {key}")
        by_key[key] = row
    intents = corpus.get("intents", [])
    tasks = corpus.get("tasks", [])
    correct = raw_correct = 0
    confusion = Counter()
    class_counts = Counter()
    class_correct = Counter()
    for case in intents:
        row = by_key.get(("intent", case["id"]), {})
        actual = row.get("actualIntent", "MISSING")
        expected = case["expectedIntent"]
        correct += actual == expected
        raw_correct += row.get("rawIntent") == expected
        confusion[(expected, actual)] += 1
        class_counts[expected] += 1
        class_correct[expected] += actual == expected
    task_ok = slot_ok = slot_total = 0
    turns = []
    planned_turns = []
    missing_turns = 0
    adjustment_total = 0
    recommendation_total = 0
    recommendation_nonempty = 0
    for case in tasks:
        row = by_key.get(("task", case["id"]), {})
        actual_turns = row.get("turns", [])
        complete = len(actual_turns) == len(case["turns"])
        missing_turns += max(0, len(case["turns"]) - len(actual_turns))
        for index, expected in enumerate(case["turns"]):
            planned_turns.append(expected)
            adjustment_total += bool(expected.get("isAdjust"))
            if expected.get("requiresCards"):
                recommendation_total += 1
                recommendation_nonempty += index < len(actual_turns) and bool(actual_turns[index].get("cards"))
        task_ok += bool(row.get("passed")) and complete and all(t.get("passed") for t in actual_turns)
        if case["category"] == "slot":
            slot_total += 1
            slot_ok += bool(row.get("slotPassed")) and complete
        turns.extend(actual_turns)
    nonempty = [t for t in turns if t.get("cards")]
    adjustments = [t for t in turns if t.get("isAdjust")]
    repeat = sum(sum(card in t.get("priorIds", []) for card in t.get("cards", [])) for t in adjustments)
    repeat_total = sum(len(t.get("cards", [])) for t in adjustments)
    whitelist_ok = sum(set(t["cards"]).issubset(t.get("allowedIds", [])) for t in nonempty)
    replacement_ok = sum(bool(t.get("cards")) and not set(t["cards"]).intersection(t.get("priorIds", []))
                         and bool(t.get("passed")) for t in adjustments)
    fault_cases = corpus.get("faults", [])
    fault_ok = sum(bool(by_key.get(("fault", c["id"]), {}).get("passed")) for c in fault_cases)
    fault_by_type = {}
    for fault in sorted({c["fault"] for c in fault_cases}):
        subset = [c for c in fault_cases if c["fault"] == fault]
        fault_by_type[fault] = rate(sum(bool(by_key.get(("fault", c["id"]), {}).get("passed")) for c in subset), len(subset))
    trace_coverage = rate(sum(t.get("traceObserved", 0) for t in turns), sum(t.get("traceExpected", 0) for t in turns))
    if missing_turns:
        trace_coverage.update(percent=None, incomplete=True)
    # Token absence is unknown, never zero cost. Includes standalone intent, task and fault traces.
    trace_records = [r.get("trace", {}) for r in observations]
    trace_records.extend(t.get("trace", {}) for t in turns)
    calls = [e for trace in trace_records for e in trace.get("events", []) if e.get("eventType") == "AGENT_CALL"]
    tokens = [e["totalTokens"] for e in calls if isinstance(e.get("totalTokens"), (int, float))]
    latencies = sorted(e["latencyMs"] for e in calls if isinstance(e.get("latencyMs"), (int, float)))
    telemetry = {"agentCalls": len(calls), "tokenCompleteness": rate(len(tokens), len(calls)),
                 "reportedTokens": sum(tokens),
                 "agentLatencyP95Ms": latencies[max(0, math.ceil(len(latencies) * .95) - 1)] if latencies else None,
                 "agentLatencyMeanMs": round(sum(latencies) / len(latencies), 2) if latencies else None}
    return {
        "intentAccuracy": rate(correct, len(intents)),
        "rawIntentAccuracy": rate(raw_correct, len(intents)),
        "intentPerClass": {name: rate(class_correct[name], n) for name, n in sorted(class_counts.items())},
        "confusionMatrix": [{"expected": a, "actual": b, "count": n} for (a, b), n in sorted(confusion.items())],
        "taskSuccess": rate(task_ok, len(tasks)),
        "slotCompletion": rate(slot_ok, slot_total),
        "repeatRate": rate(repeat, repeat_total),
        "replacementSuccess": rate(replacement_ok, adjustment_total),
        "nonemptyRecommendationRate": rate(recommendation_nonempty, recommendation_total),
        "whitelistCompliance": rate(whitelist_ok, len(nonempty)),
        "emptyRecommendationTurns": sum(not t.get("cards") and t.get("requiresCards", False) for t in turns),
        "traceCoverage": trace_coverage,
        "turnCompleteness": rate(len(turns), len(planned_turns)),
        "missingTurns": missing_turns,
        "telemetry": telemetry,
        "fallbackAvailability": rate(fault_ok, len(fault_cases)),
        "fallbackByType": fault_by_type,
        "recordedIntents": sum(("intent", c["id"]) in by_key for c in intents),
        "recordedTasks": sum(("task", c["id"]) in by_key for c in tasks),
        "recordedFaults": sum(("fault", c["id"]) in by_key for c in fault_cases),
    }


NAMES = {
    "intentAccuracy": "规则修正后意图路由准确率", "rawIntentAccuracy": "规则修正前意图准确率（含服务解析与兜底）",
    "taskSuccess": "端到端任务成功率", "slotCompletion": "多轮槽位补全成功率",
    "repeatRate": "换一批餐食重复率（越低越好）", "replacementSuccess": "换新任务成功率",
    "nonemptyRecommendationRate": "要求推荐时的非空响应率", "turnCompleteness": "计划轮次记录完整率",
    "whitelistCompliance": "最终非空推荐卡片白名单合规率", "traceCoverage": "预定义关键节点 Trace 覆盖率",
    "fallbackAvailability": "故障注入降级可用率",
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("run", type=Path)
    args = parser.parse_args()
    corpus = json.loads((args.run / "manifest.json").read_text(encoding="utf-8"))
    observations = [json.loads(line) for line in (args.run / "observations.jsonl").read_text(encoding="utf-8").splitlines() if line.strip()]
    result = summarize(corpus, observations)
    (args.run / "metrics.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    meta = corpus.get("metadata", {})
    lines = ["# 饮食 Agent 评测报告", "", f"- 运行模式：{meta.get('mode', 'unknown')}",
             f"- 数据来源：{meta.get('provenance', 'AI 辅助构造与标注；未经过人工独立复核')}",
             f"- 数据库：{meta.get('database', 'none')}", f"- 运行时间：{meta.get('startedAt', '')}",
             f"- 模型：{meta.get('models', '')}", "", "| 指标 | 成功/总数 | 实测比例 |", "|---|---:|---:|"]
    for key, name in NAMES.items():
        value = result[key]
        number = "未测 / 不适用" if value["percent"] is None else f"{value['percent']:.2f}%"
        lines.append(f"| {name} | {value['passed']}/{value['total']} | {number} |")
    lines += ["", f"要求推荐但返回空卡片的已记录轮次：{result['emptyRecommendationTurns']}；缺失轮次：{result['missingTurns']}。",
              "", "## 调用观测", "", f"- Agent调用数：{result['telemetry']['agentCalls']}。",
              f"- 可获得usage的调用：{result['telemetry']['tokenCompleteness']['passed']}/{result['telemetry']['tokenCompleteness']['total']}；已报告Token总数：{result['telemetry']['reportedTokens']}。",
              f"- Agent耗时均值：{result['telemetry']['agentLatencyMeanMs']} ms；P95：{result['telemetry']['agentLatencyP95Ms']} ms。",
              "", "## 解释与边界", "", "- 未完成或缺失的意图/任务样本保留在分母；零分母报告未测。",
              "- 空推荐不计为白名单成功；同时报告换新成功率，避免空结果掩盖无法推荐。",
              "- 所有任务轮次均须满足标注验收条件，最后一轮成功不能覆盖前面失败。",
              "- Trace 覆盖率基于评测前定义的场景节点清单；不是 Java 行覆盖率，也不代表所有 HTTP 入口。",
              "- 若计划轮次未执行完，Trace覆盖率标为未测，不能用局部记录宣称完整覆盖。",
              "- 故障注入使用受控 Agent 响应；不计入真实模型语义准确率。",
              "- 合成样本包含模板变体，不能当作同等数量的独立真实用户样本或人工金标准。",
              "- 不得将本报告等同于线上成功率或医学安全性验证。", "", "## 失败样本", ""]
    failures = [r for r in observations if r.get("passed") is False]
    for row in failures:
        lines.append(f"- `{row['kind']}/{row['id']}`：{row.get('reason', '参见 observations.jsonl 对应记录')}")
    if not failures:
        lines.append("已记录样本中没有失败；仍须检查实际完成数量与样本总数是否一致。")
    (args.run / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in result.items() if k in NAMES}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
