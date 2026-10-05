package com.diet.eval;

import com.diet.model.SlotBundle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.*;

/** Independent acceptance rules over persisted trace JSON and public response data. */
final class EvalChecks {
    static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    static final List<String> SLOT_NAMES = List.of("mealTime", "mood", "scene", "healthGoal", "cuisine", "taste", "convenience");

    static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) node.forEach(n -> values.add(n.asText()));
        return values;
    }
    static List<Long> longs(JsonNode node) {
        List<Long> values = new ArrayList<>();
        if (node.isArray()) node.forEach(n -> values.add(n.asLong()));
        return values;
    }
    static SlotBundle slots(JsonNode n) {
        return new SlotBundle(strings(n.path("mealTime")), strings(n.path("mood")), strings(n.path("scene")),
                strings(n.path("healthGoal")), strings(n.path("cuisine")), strings(n.path("taste")), strings(n.path("convenience")));
    }
    static JsonNode payload(JsonNode event) {
        JsonNode raw = event.path("outputPayload");
        if (!raw.isTextual() || raw.asText().isBlank()) return MissingNode.getInstance();
        try {
            JsonNode parsed = JSON.readTree(raw.asText());
            return parsed != null && parsed.isContainerNode() ? parsed : MissingNode.getInstance();
        }
        catch (Exception error) { return MissingNode.getInstance(); }
    }
    static JsonNode output(JsonNode events, String type) {
        JsonNode result = MissingNode.getInstance();
        for (JsonNode event : events) if (type.equals(event.path("eventType").asText())) result = payload(event);
        return result;
    }
    static Set<Long> allowedIds(JsonNode events) {
        Set<Long> ids = new LinkedHashSet<>();
        JsonNode ranked = output(events, "MEAL_RANKED").path("ranked");
        for (int i = 0; i < Math.min(3, ranked.size()); i++) if (ranked.get(i).path("id").isNumber()) ids.add(ranked.get(i).path("id").asLong());
        for (JsonNode planned : output(events, "MEAL_PLAN_SEARCHED").path("plannedMeals")) {
            if (planned.path("matched").asBoolean() && planned.path("mealId").isNumber()) ids.add(planned.path("mealId").asLong());
        }
        return ids;
    }
    static boolean slotsMatch(JsonNode actual, JsonNode required, JsonNode allowed) {
        for (String key : SLOT_NAMES) {
            Set<String> got = new HashSet<>(strings(actual.path(key)));
            if (!got.containsAll(strings(required.path(key)))) return false;
            if (allowed.has(key) && !new HashSet<>(strings(allowed.path(key))).containsAll(got)) return false;
        }
        return true;
    }
    static List<String> expectedNodes(String route, String responseType, boolean hasCandidates) {
        List<String> types = new ArrayList<>(List.of("REQUEST_RECEIVED", "USER_MESSAGE_RECORDED", "AGENT_CALL:IntentAgent",
                "INTENT_RECOGNIZED", "INTENT_REVISED", "ROUTE_SELECTED", "RESPONSE_READY", "REQUEST_FINISHED"));
        if (List.of("MEAL_RECOMMENDATION", "CLARIFY_NEEDED").contains(route)) {
            types.addAll(List.of("SLOTS_MERGED", "CLARIFY_DECISION"));
            if ("CLARIFY".equals(responseType)) types.add("AGENT_CALL:ClarifyAgent");
            else addRecommendation(types, hasCandidates);
        } else if ("MEAL_ADJUST".equals(route)) {
            types.add("ADJUST_CONTEXT_RESOLVED");
            addRecommendation(types, hasCandidates);
        } else if ("MEAL_PLAN".equals(route)) {
            types.addAll(List.of("PLAN_CONTEXT_RESOLVED", "MEAL_PLAN_SEARCHED"));
            if (hasCandidates) types.addAll(List.of("AGENT_CALL:PlanResponseAgent", "PLAN_RESULT_BUILT", "PLAN_RESPONSE_AGENT_RESULT", "NUTRITION_GUARD_CHECKED"));
            else types.add("NO_MEAL_PLAN_MATCHED");
        }
        return types;
    }
    private static void addRecommendation(List<String> types, boolean hasCandidates) {
        types.addAll(List.of("MEAL_SEARCHED", "MEAL_RANKED"));
        if (hasCandidates) types.addAll(List.of("AGENT_CALL:RecommendResponseAgent", "RECOMMEND_RESULT_BUILT", "RESPONSE_AGENT_RESULT", "NUTRITION_GUARD_CHECKED"));
        else types.add("NO_MEAL_MATCHED");
    }
    static Set<String> observedNodes(JsonNode events) {
        Set<String> types = new HashSet<>();
        for (JsonNode event : events) {
            String type = event.path("eventType").asText();
            // A named event with an unparseable/missing payload does not prove coverage.
            boolean error = !event.path("errorMessage").isNull() && !event.path("errorMessage").asText("").isBlank();
            if ("AGENT_CALL".equals(type)) {
                if (event.path("latencyMs").isNumber() && (error || event.path("outputPayload").isTextual()))
                    types.add(type + ":" + event.path("agentName").asText());
            } else if (error || !payload(event).isMissingNode()) types.add(type);
        }
        return types;
    }
}
