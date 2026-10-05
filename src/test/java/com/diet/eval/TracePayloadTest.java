package com.diet.eval;

import com.diet.enums.SourceMode;
import com.diet.mapper.AgentTraceMapper;
import com.diet.model.*;
import com.diet.service.trace.AgentTraceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TracePayloadTest {
    @Test void fluentDtosRemainStructuredInPersistedTrace() throws Exception {
        AgentTraceMapper mapper = mock(AgentTraceMapper.class);
        List<RequestTraceRow> persisted = new ArrayList<>();
        when(mapper.insert(any())).thenAnswer(i -> { persisted.add(i.getArgument(0)); return 1; });
        ObjectMapper json = new ObjectMapper();
        AgentTraceService service = new AgentTraceService(mapper, json);
        MealItem meal = new MealItem(42L, SourceMode.PUBLIC, null, "餐食", SlotBundle.empty(), 0.5);
        try (var scope = service.openTrace("t", "s", 1L)) {
            service.recordEvent("REQUEST_RECEIVED", "HTTP", null, SessionState.fresh("s", 1L, SourceMode.PUBLIC));
            service.recordEvent("MEAL_RANKED", "RANK", null, Map.of("ranked", List.of(meal)));
            service.recordEvent("RESPONSE_AGENT_RESULT", "RESPONSE", null, ResponseResult.textOnly("有效回复"));
        }
        var events = json.readTree(persisted.get(0).getTraceJson()).path("events");
        var state = assertDoesNotThrow(() -> json.readTree(events.get(0).path("outputPayload").asText()));
        var ranked = assertDoesNotThrow(() -> json.readTree(events.get(1).path("outputPayload").asText()));
        var response = assertDoesNotThrow(() -> json.readTree(events.get(2).path("outputPayload").asText()));
        assertEquals("s", state.path("sessionId").asText());
        assertEquals(42L, ranked.path("ranked").get(0).path("id").asLong());
        assertEquals("有效回复", response.path("speechText").asText());
    }
}
