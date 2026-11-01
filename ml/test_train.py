"""Small methodological checks, separate from evaluation on real held-out matches."""
from datetime import date
import math
import unittest
import numpy as np
from train import Match, baseline, calibration, evaluate, features, fit_model, split_matches


class TrainingTests(unittest.TestCase):
    def test_player_swap_negates_features(self):
        for surface in ("HARD", "CLAY", "GRASS"):
            self.assertTrue(np.allclose(features(3, 16, surface, 5), -np.asarray(features(16, 3, surface, 5))))

    def test_temporal_partitions_embargo_event_start_dates(self):
        dates = [date(2023, 11, 1), date(2023, 12, 25), date(2024, 6, 1), date(2024, 12, 25), date(2025, 6, 1)]
        train, validation, final_train, test = split_matches([Match(day, 1, 10, "HARD", 3) for day in dates])
        self.assertEqual([match.event_date for match in train], [dates[0]])
        self.assertEqual([match.event_date for match in validation], [dates[2]])
        self.assertNotIn(dates[3], [match.event_date for match in final_train])
        self.assertEqual([match.event_date for match in test], [dates[4]])

    def test_calibration_counts_every_observation_once_at_bin_boundaries(self):
        probabilities = np.asarray([0.1, 0.2, 0.3, 0.5, 0.9, 1.0])
        self.assertEqual(sum(bin_["observations"] for bin_ in calibration(probabilities)), 12)

    def test_neutral_forecast_has_known_scores(self):
        scores = evaluate([0.5, 0.5])
        self.assertAlmostEqual(scores["logLoss"], math.log(2))
        self.assertEqual(scores["brierScore"], 0.25)
        self.assertEqual(scores["accuracy"], 0.5)

    def test_fitted_model_export_uses_same_feature_order(self):
        matches = [Match(date(2023, 1, 1), 1, 16, surface, best_of)
                   for surface in ("HARD", "CLAY", "GRASS") for best_of in (3, 5)] * 10
        model = fit_model(matches, 1.0)
        x = features(2, 20, "GRASS", 5)
        exported_probability = 1 / (1 + math.exp(-float(np.dot(model.coef_[0], x))))
        self.assertAlmostEqual(exported_probability, model.predict_proba([x])[0, 1], places=12)
        self.assertAlmostEqual(baseline([matches[0]])[0], 0.8)


if __name__ == "__main__":
    unittest.main()
