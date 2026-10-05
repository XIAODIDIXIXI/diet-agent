import unittest

from metrics import summarize


class MetricsTest(unittest.TestCase):
    def test_missing_results_stay_in_denominator(self):
        result = summarize({"intents": [{"id": "a", "expectedIntent": "OTHER"},
                                         {"id": "b", "expectedIntent": "OTHER"}], "tasks": []},
                           [{"kind": "intent", "id": "a", "actualIntent": "OTHER"}])
        self.assertEqual(result["intentAccuracy"], {"passed": 1, "total": 2, "percent": 50.0})

    def test_empty_recommendations_do_not_earn_whitelist_or_replacement_success(self):
        data = {"intents": [], "tasks": [{"id": "a", "category": "adjust", "turns": [{"message": "换一批", "isAdjust": True, "requiresCards": True}]}]}
        rows = [{"kind": "task", "id": "a", "passed": False, "slotPassed": None,
                 "turns": [{"passed": False, "cards": [], "allowedIds": [1], "priorIds": [2],
                            "isAdjust": True, "traceExpected": 4, "traceObserved": 0}]}]
        result = summarize(data, rows)
        self.assertIsNone(result["whitelistCompliance"]["percent"])
        self.assertEqual(result["replacementSuccess"]["percent"], 0.0)
        self.assertEqual(result["traceCoverage"]["percent"], 0.0)

    def test_counts_card_history_overlap_and_noncompliant_response(self):
        data = {"intents": [], "tasks": [{"id": "a", "category": "adjust", "turns": [{"message": "换", "isAdjust": True, "requiresCards": True}]}]}
        rows = [{"kind": "task", "id": "a", "passed": False, "slotPassed": None,
                 "turns": [{"passed": False, "cards": [1, 9], "allowedIds": [1, 2], "priorIds": [1],
                            "isAdjust": True, "traceExpected": 4, "traceObserved": 3}]}]
        result = summarize(data, rows)
        self.assertEqual(result["repeatRate"]["percent"], 50.0)
        self.assertEqual(result["whitelistCompliance"]["percent"], 0.0)
        self.assertEqual(result["traceCoverage"]["percent"], 75.0)

    def test_missing_task_is_failure_and_does_not_disappear(self):
        result = summarize({"intents": [], "tasks": [{"id": "a", "category": "slot", "turns": []}]}, [])
        self.assertEqual(result["taskSuccess"]["percent"], 0.0)
        self.assertEqual(result["slotCompletion"]["percent"], 0.0)

    def test_all_turns_must_pass_not_only_the_last(self):
        data = {"intents": [], "tasks": [{"id": "a", "category": "slot", "turns": [{}, {}]}]}
        rows = [{"kind": "task", "id": "a", "passed": True, "slotPassed": True,
                 "turns": [{"passed": False}, {"passed": True}]}]
        result = summarize(data, rows)
        self.assertEqual(result["taskSuccess"]["percent"], 0.0)

    def test_duplicate_ids_are_rejected(self):
        with self.assertRaises(ValueError):
            summarize({"intents": [], "tasks": []}, [{"kind": "intent", "id": "x"},
                                                     {"kind": "intent", "id": "x"}])

    def test_usage_missing_is_not_counted_as_zero_tokens(self):
        rows = [{"kind": "intent", "id": "a", "trace": {"events": [
            {"eventType": "AGENT_CALL", "totalTokens": 100, "latencyMs": 20},
            {"eventType": "AGENT_CALL", "totalTokens": None, "latencyMs": 40}]}}]
        result = summarize({"intents": [], "tasks": []}, rows)
        self.assertEqual(result["telemetry"]["tokenCompleteness"]["percent"], 50.0)
        self.assertEqual(result["telemetry"]["reportedTokens"], 100)
        self.assertEqual(result["telemetry"]["agentLatencyP95Ms"], 40)

    def test_missing_adjustment_cannot_produce_full_replacement_or_trace_coverage(self):
        data = {"intents": [], "tasks": [{"id": c, "category": "adjust", "turns": [
            {"isAdjust": True, "requiresCards": True}]} for c in ["a", "b"]]}
        rows = [{"kind": "task", "id": "a", "passed": True, "turns": [
            {"passed": True, "cards": [2], "allowedIds": [2], "priorIds": [1], "isAdjust": True,
             "traceObserved": 10, "traceExpected": 10}]}]
        result = summarize(data, rows)
        self.assertEqual(result["replacementSuccess"]["percent"], 50.0)
        self.assertEqual(result["nonemptyRecommendationRate"]["percent"], 50.0)
        self.assertIsNone(result["traceCoverage"]["percent"])
        self.assertTrue(result["traceCoverage"]["incomplete"])


if __name__ == "__main__":
    unittest.main()
