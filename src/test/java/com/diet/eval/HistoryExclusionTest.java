package com.diet.eval;

import com.diet.enums.SourceMode;
import com.diet.model.*;
import com.diet.service.meal.MealRankService;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HistoryExclusionTest {
    @Test void repeatedBatchesExcludeAllHistoricallyDisplayedMeals() throws Exception {
        MealRankService ranking = new MealRankService();
        int returned = 0;
        int duplicates = 0;
        int nonempty = 0;
        List<Map<String, Object>> observations = new ArrayList<>();
        // Different pre-existing histories and candidate scores; literal fixture IDs 1..24.
        for (int scenario = 0; scenario < 30; scenario++) {
            SessionState state = SessionState.fresh("controlled-" + scenario, 1L, SourceMode.PUBLIC);
            List<Long> initial = switch (scenario % 3) {
                case 0 -> List.of(2L, 4L, 6L);
                case 1 -> List.of(1L, 3L, 5L);
                default -> List.of(7L, 8L, 9L);
            };
            state = state.withLastRecommendations(initial);
            SlotBundle query = new SlotBundle(List.of("晚餐"), List.of(), List.of(), List.of("均衡"), List.of(), List.of(), List.of());
            List<MealItem> candidates = new ArrayList<>();
            for (long id = 1; id <= 24; id++) {
                SlotBundle tags = id % (scenario % 5 + 2) == 0 ? query : SlotBundle.empty();
                candidates.add(new MealItem(id, SourceMode.PUBLIC, null, "受控候选" + id, tags, 0));
            }
            Set<Long> independentHistory = new HashSet<>(initial);
            for (int turn = 0; turn < 3; turn++) {
                List<Long> batch = ranking.rank(new MealRankRequest(candidates, query, state.lastRecommendations()))
                        .stream().limit(3).map(MealItem::id).toList();
                long repeats = batch.stream().filter(independentHistory::contains).count();
                returned += batch.size(); duplicates += (int) repeats;
                if (!batch.isEmpty()) nonempty++;
                observations.add(Map.of("scenario", scenario, "turn", turn, "priorIds", List.copyOf(independentHistory), "cards", batch, "repeats", repeats));
                assertEquals(3, batch.size(), "Enough fresh candidates exist in this controlled fixture");
                assertEquals(0, repeats, "Any historical ID escaping the rank filter must fail");
                independentHistory.addAll(batch);
                state = state.appendLastRecommendations(batch);
                assertTrue(new HashSet<>(state.lastRecommendations()).containsAll(independentHistory), "Session must retain every previously shown ID");
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scope", "CONTROLLED_JAVA_RANK_AND_SESSION_RULES; no LLM, HTTP or MySQL calls");
        report.put("scenarios", 30); report.put("adjustmentTurns", 90); report.put("returnedItems", returned);
        report.put("repeatedItems", duplicates); report.put("nonemptyTurns", nonempty); report.put("observations", observations);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/history-exclusion-metrics.json"), EvalChecks.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report), StandardCharsets.UTF_8);
    }
}
