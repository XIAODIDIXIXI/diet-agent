package com.diet.eval;

import com.diet.agent.factory.AgentFactory;
import com.diet.enums.SourceMode;
import com.diet.mapper.AgentTraceMapper;
import com.diet.model.*;
import com.diet.service.recommend.RecommendResponseAgentService;
import com.diet.service.trace.AgentTraceService;
import com.diet.util.LlmJsonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WhitelistConstraintTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"recommendations\":[{\"mealId\":999,\"reason\":\"候选外餐食\"}]}",
            "{\"recommendations\":[{\"mealId\":-1,\"reason\":\"非法ID\"}]}",
            "{\"recommendations\":[{\"mealId\":null,\"reason\":\"空ID\"}]}",
            "{\"recommendations\":[{\"mealId\":\"invented\",\"reason\":\"非数字\"}]}",
            "{\"recommendations\":[{\"mealId\":1,\"reason\":\"有效\"},{\"mealId\":999,\"reason\":\"无效\"}]}",
            "{\"recommendations\":[{\"mealId\":1,\"reason\":\"有效\"},{\"mealId\":1,\"reason\":\"重复\"}]}",
            "{\"recommendations\":[{\"mealId\":4,\"reason\":\"排序Top10但不在发送给模型的Top3中\"}]}",
            "{\"recommendations\":[]}", "{\"recommendations\":null}", "{}", "", "{broken"
    })
    void modelCannotInventOrDuplicateFinalCardEntities(String content) {
        ObjectMapper json = new ObjectMapper();
        AgentFactory factory = mock(AgentFactory.class);
        ReActAgent agent = mock(ReActAgent.class);
        when(agent.getMemory()).thenReturn(new InMemoryMemory());
        when(agent.call(any(Msg.class))).thenReturn(Mono.just(Msg.builder().role(MsgRole.ASSISTANT).textContent(content).build()));
        when(factory.get("s")).thenReturn(new AgentFactory.AgentSet(agent, agent, agent, agent));
        AgentTraceService trace = new AgentTraceService(mock(AgentTraceMapper.class), json);
        RecommendResponseAgentService service = new RecommendResponseAgentService(factory, new LlmJsonService(json), trace, "stub");
        List<MealItem> candidates = List.of(meal(1), meal(2), meal(3), meal(4));
        var result = service.recommendAndRespond("s", "推荐午餐", SourceMode.PUBLIC, SlotBundle.empty(), candidates);
        assertEquals(List.of(1L, 2L, 3L), result.response().displayBlocks().stream().map(MealResponse::id).toList());
        assertEquals(List.of("候选1", "候选2", "候选3"), result.response().displayBlocks().stream().map(MealResponse::name).toList());
        assertFalse(result.response().speechText().isBlank());
    }
    private static MealItem meal(long id) { return new MealItem(id, SourceMode.PUBLIC, null, "候选" + id, SlotBundle.empty(), .5); }
}
