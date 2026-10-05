import unittest
from regrade import grade_turn, observed_nodes


class RegradeTest(unittest.TestCase):
    def test_absent_null_blank_or_unparseable_outputs_do_not_prove_coverage(self):
        events = [{"eventType": "SLOTS_MERGED"}, {"eventType": "MEAL_RANKED", "outputPayload": None},
                  {"eventType": "CLARIFY_DECISION", "outputPayload": " "},
                  {"eventType": "REQUEST_RECEIVED", "outputPayload": "not json"},
                  {"eventType": "AGENT_CALL", "agentName": "IntentAgent", "latencyMs": 20}]
        self.assertEqual(observed_nodes(events), set())

    def test_quick_preference_is_checked_and_original_observation_is_preserved(self):
        expected = {"routes": ["MEAL_ADJUST"], "responseType": "ANSWER", "requiresCards": True,
                    "requiredSlots": {"convenience": ["快速"]}, "isAdjust": True}
        observation = {"passed": True, "httpStatus": 200, "actualSlots": {"convenience": []}, "priorIds": [1],
                       "response": {"speechText": "推荐", "responseType": "ANSWER", "displayBlocks": [{"id": 2}]},
                       "trace": {"events": [
                           {"eventType": "ROUTE_SELECTED", "outputPayload": '{"route":"MEAL_ADJUST"}'},
                           {"eventType": "MEAL_RANKED", "outputPayload": '{"ranked":[{"id":2}]}'}]}}
        result = grade_turn(expected, observation)
        self.assertFalse(result["passed"])
        self.assertIn("Slot mismatch", result["failures"])
        self.assertTrue(observation["passed"])

    def test_full_trace_can_be_valid_for_failed_task(self):
        result = grade_turn({"responseType": "ANSWER", "routes": ["OTHER"], "requiresCards": False},
                            {"httpStatus": 200, "response": {"responseType": "ANSWER", "speechText": ""}, "trace": {"events": []}})
        self.assertFalse(result["passed"])
        self.assertEqual(result["traceObserved"], 0)


if __name__ == "__main__":
    unittest.main()
