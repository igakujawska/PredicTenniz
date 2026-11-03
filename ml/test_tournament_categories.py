import unittest
try:
    from .tournament_categories import category_for
except ImportError:
    from tournament_categories import category_for

class TournamentCategoriesTest(unittest.TestCase):
    def test_promotions_are_dated_not_applied_backwards(self):
        for identifier in (424,451,308):
            self.assertEqual('ATP_250', category_for('A',f'2024-{identifier:04}',2024))
            self.assertEqual('ATP_500', category_for('A',f'2025-{identifier:04}',2025))
    def test_astana_exception_and_non_standard_competitions(self):
        self.assertEqual('ATP_500',category_for('A','2022-9410',2022))
        self.assertEqual('ATP_250',category_for('A','2023-9410',2023))
        for identifier in (96,7696,8888,9210,9900): self.assertIsNone(category_for('A',f'2025-{identifier}',2025))
    def test_legacy_archived_identifier_is_mapped(self):
        self.assertEqual('ATP_500',category_for('A','2018-M004',2018))

    def test_known_major_categories(self):
        self.assertEqual('GRAND_SLAM', category_for('G','2025-580',2025))
        self.assertEqual('MASTERS_1000', category_for('M','2025-416',2025))
        self.assertEqual('ATP_500', category_for('A','2025-0407',2025))
