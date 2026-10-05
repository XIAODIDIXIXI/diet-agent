package com.diet.eval;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class EvalChecksTest {
    @Test void whitelistUsesOnlyTopThreeAndAlsoSupportsPlans() throws Exception {
        var events = EvalChecks.JSON.readTree("""
          [{"eventType":"MEAL_RANKED","outputPayload":"{\\"ranked\\":[{\\"id\\":1},{\\"id\\":2},{\\"id\\":3},{\\"id\\":4}]}"},
           {"eventType":"MEAL_PLAN_SEARCHED","outputPayload":"{\\"plannedMeals\\":[{\\"matched\\":true,\\"mealId\\":9},{\\"matched\\":false,\\"mealId\\":null}]}"}]
          """);
        assertEquals(Set.of(1L, 2L, 3L, 9L), EvalChecks.allowedIds(events));
    }
    @Test void inventedSlotMakesCompletionFail() throws Exception {
        var actual = EvalChecks.JSON.readTree("{\"mealTime\":[\"晚餐\"],\"taste\":[\"辣\"]}");
        var required = EvalChecks.JSON.readTree("{\"mealTime\":[\"晚餐\"]}");
        var allowed = EvalChecks.JSON.readTree("{\"mealTime\":[\"晚餐\"],\"taste\":[]}");
        assertFalse(EvalChecks.slotsMatch(actual, required, allowed));
    }
    @Test void truncatedTracePayloadDoesNotCountAsObserved() throws Exception {
        var events = EvalChecks.JSON.readTree("[{\"eventType\":\"MEAL_RANKED\",\"outputPayload\":\"{bad\"}]");
        assertTrue(EvalChecks.observedNodes(events).isEmpty());
    }
    @Test void absentOrNullPayloadDoesNotProveCoverage() throws Exception {
        var events = EvalChecks.JSON.readTree("""
            [{"eventType":"SLOTS_MERGED"},
             {"eventType":"MEAL_RANKED","outputPayload":null},
             {"eventType":"AGENT_CALL","agentName":"IntentAgent","latencyMs":12}]
            """);
        assertTrue(EvalChecks.observedNodes(events).isEmpty());
    }
}
