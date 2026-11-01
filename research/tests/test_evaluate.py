import importlib.util
import math
import unittest
from datetime import date
from pathlib import Path

spec = importlib.util.spec_from_file_location("evaluation", Path(__file__).resolve().parents[1] / "evaluate.py")
evaluation = importlib.util.module_from_spec(spec)
spec.loader.exec_module(evaluation)


class HistoricalEvaluationTests(unittest.TestCase):
    def row(self, played, a="a", b="b"):
        return {"date": played, "a": a, "b": b, "surface": "Hard"}

    def test_overlapping_events_cannot_see_earlier_tournaments_future_results(self):
        rows = [self.row(date(2020,1,1)), self.row(date(2020,1,8)), self.row(date(2020,1,22))]
        result = evaluation.build_features(rows)
        self.assertEqual(result[0]["elo"], .5)
        self.assertEqual(result[1]["elo"], .5)
        self.assertGreater(result[2]["elo"], .5)

    def test_future_results_never_change_past_features(self):
        earlier = [self.row(date(2020,1,1)), self.row(date(2020,2,1))]
        self.assertEqual(evaluation.build_features(earlier), evaluation.build_features(earlier+[self.row(date(2021,1,1),"b","a")])[:2])

    def test_equal_date_batch_is_frozen(self):
        rows = [self.row(date(2020,1,1)), self.row(date(2020,1,1),"a","c")]
        self.assertTrue(all(row["elo"] == .5 for row in evaluation.build_features(rows)))

    def test_logistic_is_symmetric_and_learns_direction(self):
        weight = evaluation.fit_logistic([{"x": .8}, {"x": 1.0}, {"x": -.1}])
        self.assertGreater(weight, 0)
        self.assertAlmostEqual(evaluation.sigmoid(weight*.8)+evaluation.sigmoid(-weight*.8), 1)

    def test_proper_scores_and_tie_accuracy(self):
        result = evaluation.score([.5, .5])
        self.assertAlmostEqual(result["log_loss"], math.log(2))
        self.assertEqual(result["brier"], .25)
        self.assertEqual(result["accuracy"], .5)


if __name__ == "__main__":
    unittest.main()
