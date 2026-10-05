package com.diet.eval;

import com.diet.agent.factory.AgentFactory;
import com.diet.enums.*;
import com.diet.model.*;
import com.diet.service.intent.*;
import com.diet.service.session.*;
import com.diet.service.trace.AgentTraceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.diet.eval.EvalChecks.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "diet.eval", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "logging.level.com.diet=WARN", "logging.level.io.agentscope=WARN", "spring.main.banner-mode=off"})
class QuantitativeEvaluationTest {
    static final long USER_ID = 909090L;
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EvalDatabase.URL);
        registry.add("spring.datasource.username", () -> EvalDatabase.USER);
        registry.add("spring.datasource.password", () -> EvalDatabase.PASSWORD);
        if ("fault".equals(System.getProperty("diet.eval.mode"))) registry.add("agentscope.dashscope.api-key", () -> "offline-evaluation-placeholder");
    }
    @Autowired IntentAgentService intentService;
    @Autowired IntentReviseService reviseService;
    @Autowired SessionStateService states;
    @Autowired AgentTraceService traces;
    @Autowired TestRestTemplate http;
    @Autowired Environment environment;
    @SpyBean AgentFactory agents;
    Path runDirectory;

    @Test void measureFrozenCorpus() throws Exception {
        String mode = System.getProperty("diet.eval.mode", "smoke");
        ObjectNode corpus = (ObjectNode) JSON.readTree(Files.readString(Path.of("eval/corpus.json"), StandardCharsets.UTF_8));
        if ("fault".equals(mode)) {
            corpus.set("intents", JSON.createArrayNode());
            corpus.set("tasks", JSON.createArrayNode());
        } else {
            corpus.set("faults", JSON.createArrayNode());
            if ("smoke".equals(mode)) {
                ArrayNode selected = JSON.createArrayNode();
                for (int i = 0; i < 600; i += 100) selected.add(corpus.path("intents").get(i));
                corpus.set("intents", selected);
                ArrayNode tasks = JSON.createArrayNode();
                for (int i : List.of(0, 60, 90)) tasks.add(corpus.path("tasks").get(i));
                corpus.set("tasks", tasks);
            }
        }
        runDirectory = Path.of(System.getProperty("diet.eval.output", "eval/runs/" + mode + "-" + EvalDatabase.SCHEMA));
        Files.createDirectories(runDirectory);
        if (Files.exists(runDirectory.resolve("observations.jsonl"))) throw new IllegalStateException("Use a new output directory; existing observations must not be overwritten");
        ObjectNode meta = (ObjectNode) corpus.path("metadata");
        meta.put("mode", mode);
        meta.put("database", EvalDatabase.SCHEMA);
        meta.put("startedAt", ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        meta.put("models", environment.getProperty("diet.llm.light-model") + " / " + environment.getProperty("diet.llm.main-model"));
        Files.writeString(runDirectory.resolve("manifest.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(corpus), StandardCharsets.UTF_8);
        Files.createFile(runDirectory.resolve("observations.jsonl"));
        System.out.println("EVAL_OUTPUT=" + runDirectory.toAbsolutePath());
        if ("fault".equals(mode)) {
            FaultEvaluation faultEvaluation = new FaultEvaluation(agents, traces, states, http, this::append);
            faultEvaluation.run(corpus.path("faults"));
            return;
        }
        if (environment.getProperty("agentscope.dashscope.api-key", "").isBlank()) throw new IllegalStateException("DASHSCOPE_API_KEY is missing; no real-model result was produced");
        List<JsonNode> intentCases = new ArrayList<>();
        corpus.path("intents").forEach(intentCases::add);
        AtomicInteger completed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(Integer.getInteger("diet.eval.workers", 3));
        try {
            List<Callable<Void>> jobs = intentCases.stream().<Callable<Void>>map(c -> () -> {
                append(measureIntent(c));
                int n = completed.incrementAndGet();
                if (n % 10 == 0 || n == intentCases.size()) System.out.println("EVAL_INTENTS=" + n + "/" + intentCases.size());
                return null;
            }).toList();
            for (Future<Void> future : pool.invokeAll(jobs)) future.get();
        } finally { pool.shutdownNow(); }
        int taskCount = 0;
        for (JsonNode task : corpus.path("tasks")) {
            append(measureTask(task));
            System.out.println("EVAL_TASKS=" + (++taskCount) + "/" + corpus.path("tasks").size());
        }
        Files.writeString(runDirectory.resolve("completed.txt"), ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
    }

    Map<String, Object> measureIntent(JsonNode c) throws Exception {
        String id = c.path("id").asText();
        String session = "eval_" + UUID.randomUUID().toString().replace("-", "");
        String traceId = "eval_" + UUID.randomUUID().toString().replace("-", "");
        JsonNode context = c.path("context");
        SessionState state = states.loadOrCreate(session, USER_ID, SourceMode.PUBLIC)
                .withSlots(slots(context.path("slots"))).withLastRecommendations(longs(context.path("historyIds")));
        List<ConversationTurn> history = new ArrayList<>();
        for (JsonNode h : context.path("history")) history.add(new ConversationTurn(h.path("role").asText(),
                h.has("intent") ? Intent.valueOf(h.path("intent").asText()) : null, h.path("summary").asText(), 0L));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", "intent"); result.put("id", id); result.put("message", c.path("message").asText());
        long start = System.nanoTime();
        try (var scope = traces.openTrace(traceId, session, USER_ID)) {
            IntentResult raw = intentService.recognize(session, USER_ID, c.path("message").asText(), state.slots(), history);
            IntentResult revised = reviseService.revise(state, raw, c.path("message").asText());
            traces.recordEvent("INTENT_RECOGNIZED", "INTENT", c.path("message").asText(), raw);
            traces.recordEvent("INTENT_REVISED", "INTENT", raw, revised);
            result.put("rawIntent", raw.intent().name()); result.put("actualIntent", revised.intent().name());
            result.put("actualSlots", JSON.valueToTree(revised.slots())); result.put("confidence", raw.confidence());
            boolean pass = c.path("expectedIntent").asText().equals(revised.intent().name());
            result.put("passed", pass);
            if (!pass) result.put("reason", "Expected " + c.path("expectedIntent").asText() + ", received " + revised.intent());
        } catch (Exception error) {
            result.put("passed", false); result.put("reason", error.getClass().getSimpleName() + ": " + error.getMessage());
        } finally { agents.remove(session); }
        result.put("durationMs", (System.nanoTime() - start) / 1_000_000);
        RequestTraceRow row = traces.findByTraceId(USER_ID, traceId);
        if (row != null) {
            result.put("trace", JSON.readTree(row.getTraceJson()));
            result.put("modelCallFailed", "FAILED".equals(row.getStatus()));
        } else result.put("traceMissing", true);
        return result;
    }

    Map<String, Object> measureTask(JsonNode task) throws Exception {
        SessionState state = states.create(USER_ID, SourceMode.PUBLIC);
        List<Map<String, Object>> turns = new ArrayList<>();
        Set<Long> displayedHistory = new LinkedHashSet<>();
        boolean allPassed = true;
        boolean slotPassed = false;
        for (JsonNode expected : task.path("turns")) {
            Map<String, Object> turn = measureTurn(state.sessionId(), expected, displayedHistory);
            turns.add(turn);
            allPassed &= Boolean.TRUE.equals(turn.get("passed"));
            if (expected.has("requiredSlots") && expected.has("allowedSlots")) slotPassed = Boolean.TRUE.equals(turn.get("slotPassed"));
            displayedHistory.addAll((List<Long>) turn.getOrDefault("cards", List.of()));
        }
        agents.remove(state.sessionId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", "task"); result.put("id", task.path("id").asText()); result.put("category", task.path("category").asText());
        result.put("sessionId", state.sessionId()); result.put("turns", turns); result.put("passed", allPassed);
        result.put("slotPassed", slotPassed);
        if (!allPassed) result.put("reason", turns.stream().filter(t -> !Boolean.TRUE.equals(t.get("passed"))).map(t -> t.get("failures")).toList());
        return result;
    }

    Map<String, Object> measureTurn(String session, JsonNode expected, Set<Long> displayedHistory) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", expected.path("message").asText()); result.put("requiresCards", expected.path("requiresCards").asBoolean());
        result.put("isAdjust", expected.path("isAdjust").asBoolean()); result.put("priorIds", List.copyOf(displayedHistory));
        List<String> failures = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders(); headers.set("X-User-Id", Long.toString(USER_ID));
        ChatRequest request = new ChatRequest(session, expected.path("message").asText(), SourceMode.PUBLIC, Map.of());
        ResponseEntity<String> httpResponse = http.postForEntity("/api/v1/diet/chat", new HttpEntity<>(request, headers), String.class);
        JsonNode response = JSON.readTree(httpResponse.getBody());
        result.put("httpStatus", httpResponse.getStatusCode().value()); result.put("response", response);
        if (!httpResponse.getStatusCode().is2xxSuccessful()) failures.add("HTTP failure");
        List<Long> cards = new ArrayList<>();
        Set<String> mealTimes = new HashSet<>();
        for (JsonNode card : response.path("displayBlocks")) { cards.add(card.path("id").asLong()); mealTimes.addAll(strings(card.path("mealTime"))); }
        result.put("cards", cards);
        RequestTraceRow row = traces.findByTraceId(USER_ID, response.path("traceId").asText());
        JsonNode trace = row == null ? JSON.createObjectNode() : JSON.readTree(row.getTraceJson());
        JsonNode events = trace.path("events");
        result.put("trace", trace);
        String route = output(events, "ROUTE_SELECTED").path("route").asText();
        Set<Long> allowed = allowedIds(events);
        result.put("allowedIds", allowed); result.put("route", route);
        if (!strings(expected.path("routes")).contains(route)) failures.add("Unexpected route: " + route);
        if (!expected.path("responseType").asText().equals(response.path("responseType").asText())) failures.add("Unexpected responseType");
        if (response.path("speechText").asText("").isBlank()) failures.add("Empty response");
        if (expected.path("requiresCards").asBoolean() && cards.size() < expected.path("minCards").asInt(1)) failures.add("Insufficient recommendations");
        if (!allowed.containsAll(cards)) failures.add("Card outside candidate whitelist");
        if (new HashSet<>(cards).size() != cards.size()) failures.add("Duplicate cards in same response");
        if (expected.path("isAdjust").asBoolean() && cards.stream().anyMatch(displayedHistory::contains)) failures.add("Repeated historical recommendation");
        if (!mealTimes.containsAll(strings(expected.path("requiredMealTimes")))) failures.add("Missing planned meal time");
        SessionState after = states.loadOrCreate(session, USER_ID, SourceMode.PUBLIC);
        JsonNode actualSlots = JSON.valueToTree(after.slots());
        boolean slotOk = slotsMatch(actualSlots, expected.path("requiredSlots"), expected.path("allowedSlots"));
        if (!slotOk) failures.add("Slot mismatch");
        result.put("actualSlots", actualSlots); result.put("slotPassed", slotOk);
        List<String> nodes = expectedNodes(route.isBlank() ? expected.path("routes").path(0).asText() : route,
                response.path("responseType").asText(), !allowed.isEmpty());
        Set<String> observed = observedNodes(events);
        result.put("traceExpected", nodes.size()); result.put("traceObserved", nodes.stream().filter(observed::contains).count());
        result.put("missingTraceNodes", nodes.stream().filter(n -> !observed.contains(n)).toList());
        result.put("passed", failures.isEmpty()); result.put("failures", failures);
        return result;
    }

    synchronized void append(Map<String, Object> record) {
        try {
            String json = JSON.writeValueAsString(record);
            String secret = environment.getProperty("agentscope.dashscope.api-key", "");
            if (!secret.isBlank()) json = json.replace(secret, "[REDACTED]");
            Files.writeString(runDirectory.resolve("observations.jsonl"), json + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (Exception error) { throw new IllegalStateException("Failed to persist evaluation evidence", error); }
    }
}
