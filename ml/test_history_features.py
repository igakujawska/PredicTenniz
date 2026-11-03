"""Methodology checks for the next experiment; this pipeline is not a deployed model."""
from datetime import date, timedelta
import unittest
import numpy as np
try:
    from .history_features import Event, History, build_examples, flip_features, serving_snapshot
except ImportError:
    from history_features import Event, History, build_examples, flip_features, serving_snapshot

class HistoricalFeaturesTest(unittest.TestCase):
    def test_result_is_invisible_until_the_declared_availability_day(self):
        day = date(2025, 1, 1)
        state = History()
        state.schedule(Event(day, 1, 2, 1, 2))
        self.assertEqual(1500, state.profile(1, day + timedelta(days=20))["elo"])
        self.assertEqual(1516, state.profile(1, day + timedelta(days=21))["elo"])
        self.assertEqual(1, state.profile(1, day + timedelta(days=21))["matches90"])

    def test_same_tournament_rows_never_enter_their_own_training_features(self):
        day = date(2025, 1, 1)
        events = [Event(day, 1, 2, 1, 2), Event(day, 1, 3, 1, 3)]
        for _, features in build_examples(events):
            np.testing.assert_array_equal(features[1:8], 0)

    def test_release_batch_ratings_do_not_depend_on_source_round_order(self):
        day = date(2025, 1, 1)
        events = [Event(day, 1, 2, 1, 2), Event(day, 1, 3, 1, 3), Event(day, 2, 3, 2, 3)]
        forward, _ = serving_snapshot(events, day + timedelta(days=21))
        reversed_, _ = serving_snapshot(list(reversed(events)), day + timedelta(days=21))
        self.assertEqual(forward, reversed_)

    def test_swapping_players_negates_directed_features_only(self):
        day = date(2025, 1, 1)
        state = History()
        state.schedule(Event(day, 1, 2, 4, 8, 'CLAY', 5))
        later = day + timedelta(days=21)
        a = state.features(1, 2, 4, 8, 'CLAY', 5, later)
        b = state.features(2, 1, 8, 4, 'CLAY', 5, later)
        np.testing.assert_allclose(flip_features(a), b)
        np.testing.assert_allclose(flip_features(b), a)

    def test_future_events_do_not_change_an_earlier_snapshot(self):
        day = date(2025, 1, 1)
        known = Event(day, 1, 2, 1, 2)
        future = Event(day + timedelta(days=40), 2, 1, 2, 1)
        self.assertEqual(serving_snapshot([known], day + timedelta(days=21)),
                         serving_snapshot([future, known], day + timedelta(days=21)))
        state = History()
        state.advance(day)
        with self.assertRaises(ValueError): state.advance(day - timedelta(days=1))

if __name__ == '__main__': unittest.main()
