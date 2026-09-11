import unittest

from services.fraud_analyzer import analyze_text_for_fraud


class TestFraudAnalyzer(unittest.TestCase):
    def test_clean_report_is_low_risk(self):
        text = "My car was hit by another vehicle in the office parking lot this morning. There is a dent on the rear door."
        result = analyze_text_for_fraud(text)
        self.assertEqual(result["status"], "LOW RISK")
        self.assertEqual(result["indicators"], [])

    def test_previous_damage_raises_score(self):
        text = "The front bumper had already been damaged last month, and it was hit again today."
        result = analyze_text_for_fraud(text)
        self.assertGreaterEqual(result["score"], 40)
        self.assertIn("Previous damage mentioned", result["indicators"])

    def test_timeline_contradiction_detected(self):
        text = "The accident happened yesterday, but I also remember it happened last month in the same spot."
        result = analyze_text_for_fraud(text)
        self.assertIn("Timeline contradiction", result["indicators"])

    def test_score_is_deterministic(self):
        text = "Not sure how it happened, don't remember any details, no witnesses were around."
        result1 = analyze_text_for_fraud(text)
        result2 = analyze_text_for_fraud(text)
        self.assertEqual(result1["score"], result2["score"])

    def test_score_capped_at_100(self):
        text = ("The bumper was already damaged last month. It happened again yesterday, "
                "not sure how, don't remember, no witnesses, and it happened before too, last week.")
        result = analyze_text_for_fraud(text)
        self.assertLessEqual(result["score"], 100)


if __name__ == "__main__":
    unittest.main()
