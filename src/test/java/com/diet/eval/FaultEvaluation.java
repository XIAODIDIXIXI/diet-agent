package com.diet.eval;

import com.diet.agent.factory.AgentFactory;
import com.diet.enums.SourceMode;
import com.diet.model.*;
import com.diet.service.session.SessionStateService;
import com.diet.service.trace.AgentTraceService;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import static com.diet.eval.EvalChecks.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Replace only the external Agent response; HTTP, parsing, rules, MySQL and trace persistence stay real. */
final class FaultEvaluation {
    private final AgentFactory factory;
    private final AgentTraceService traces;
    private final SessionStateService states;
    private final TestRestTemplate http;
    private final Consumer<Map<String, Object>> save;
    FaultEvaluation(AgentFactory factory, AgentTraceService traces, SessionStateService states,
                    TestRestTemplate http, Consumer<Map<String, Object>> save) {
        this.factory = factory; this.traces = traces; this.states = states; this.http = http; this.save = save;
    }
    void run(JsonNode cases) throws Exception {
        int completed = 0;
        for (JsonNode c : cases) {
            String target = c.path("agent").asText();
            String fault = c.path("fault").asText();
            String session = states.create(QuantitativeEvaluationTest.USER_ID, SourceMode.PUBLIC).sessionId();
            String intent = target.equals("PlanResponseAgent") ? "MEAL_PLAN" : "MEAL_RECOMMENDATION";
            String slotJson = target.equals("ClarifyAgent") ? "{}" : target.equals("PlanResponseAgent")
                    ? "{\"mealTime\":[\"三餐\"]}" : "{\"mealTime\":[\"午餐\"],\"healthGoal\":[\"清淡\"]}";
            Map<String, ReActAgent> stubs = new LinkedHashMap<>();
            stubs.put("IntentAgent", stub("{\"intent\":\"" + intent + "\",\"slots\":" + slotJson + ",\"confidence\":0.9}"));
            stubs.put("ClarifyAgent", stub("你想吃早餐、午餐还是晚餐？"));
            stubs.put("RecommendResponseAgent", stub("{\"recommendations\":[],\"speechText\":\"根据你的偏好推荐以下餐食。\"}"));
            stubs.put("PlanResponseAgent", stub("{\"mealPlans\":[],\"speechText\":\"下面是按餐次安排的方案。\"}"));
            ReActAgent injected = stubs.get(target);
            when(injected.call(any(Msg.class))).thenAnswer(invocation -> switch (fault) {
                case "TIMEOUT_EXCEPTION" -> Mono.error(new IllegalStateException("Injected provider timeout", new TimeoutException("evaluation")));
                case "EMPTY" -> Mono.just(message(""));
                case "NULL_RESPONSE" -> Mono.empty();
                case "MALFORMED_JSON" -> Mono.just(message(malformed(c.path("fixtureIndex").asInt())));
                default -> throw new IllegalArgumentException(fault);
            });
            AgentFactory.AgentSet set = new AgentFactory.AgentSet(stubs.get("IntentAgent"), stubs.get("ClarifyAgent"),
                    stubs.get("RecommendResponseAgent"), stubs.get("PlanResponseAgent"));
            doReturn(set).when(factory).get(session);
            String text = target.equals("PlanResponseAgent") ? "安排今天三餐" : target.equals("ClarifyAgent") ? "想吃点东西" : "午餐想吃清淡的";
            HttpHeaders headers = new HttpHeaders(); headers.set("X-User-Id", Long.toString(QuantitativeEvaluationTest.USER_ID));
            long start = System.nanoTime();
            ResponseEntity<String> response = http.postForEntity("/api/v1/diet/chat",
                    new HttpEntity<>(new ChatRequest(session, text, SourceMode.PUBLIC, Map.of()), headers), String.class);
            long latency = (System.nanoTime() - start) / 1_000_000;
            JsonNode body = JSON.readTree(response.getBody());
            RequestTraceRow row = traces.findByTraceId(QuantitativeEvaluationTest.USER_ID, body.path("traceId").asText());
            JsonNode trace = row == null ? JSON.createObjectNode() : JSON.readTree(row.getTraceJson());
            List<Long> cards = new ArrayList<>();
            body.path("displayBlocks").forEach(card -> cards.add(card.path("id").asLong()));
            boolean clarifyExpected = target.equals("IntentAgent") || target.equals("ClarifyAgent");
            boolean called = mockingDetails(injected).getInvocations().stream().anyMatch(i -> i.getMethod().getName().equals("call"));
            boolean structural = clarifyExpected ? "CLARIFY".equals(body.path("responseType").asText()) && cards.isEmpty()
                    : "ANSWER".equals(body.path("responseType").asText()) && !cards.isEmpty();
            boolean passed = called && response.getStatusCode().is2xxSuccessful() && latency < 5000 && structural
                    && !body.path("speechText").asText("").isBlank();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("kind", "fault"); result.put("id", c.path("id").asText()); result.put("agent", target); result.put("fault", fault);
            result.put("passed", passed); result.put("injectionReached", called); result.put("durationMs", latency);
            result.put("response", body); result.put("trace", trace); result.put("cards", cards);
            result.put("timeoutScope", "Provider has already emitted a timeout exception; not a hanging-network deadline test");
            if (!passed) result.put("reason", "http=" + response.getStatusCode().value() + ", injection=" + called + ", structure=" + structural + ", ms=" + latency);
            save.accept(result);
            factory.remove(session);
            if (++completed % 20 == 0) System.out.println("EVAL_FAULTS=" + completed + "/" + cases.size());
        }
    }
    private static ReActAgent stub(String text) {
        ReActAgent agent = mock(ReActAgent.class);
        when(agent.getMemory()).thenReturn(new InMemoryMemory());
        when(agent.call(any(Msg.class))).thenReturn(Mono.just(message(text)));
        return agent;
    }
    private static Msg message(String text) { return Msg.builder().role(MsgRole.ASSISTANT).textContent(text).build(); }
    private static String malformed(int index) {
        return List.of("{broken", "not-json", "```json\n{\"intent\":\n```", "{\"a\":}", "[1,2", "{\"x\": true,}",
                "\"json string\"", "null", "[]", "{\"x\":undefined}").get(index);
    }
}
