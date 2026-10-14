"""No-emulator unit tests for DZlog filename counter scopes."""
import unittest
from android16_capture_media_smoke import verify_scoped_counter_sequence


class CounterScopeTests(unittest.TestCase):
    def test_one_day_is_sequential(self):
        names = [f"DZlog_20261013_{i}.jpg" for i in range(1, 6)]
        self.assertEqual(verify_scoped_counter_sequence(names), [1, 2, 3, 4, 5])

    def test_midnight_resets_each_date(self):
        names = [f"DZlog_20261013_{i}.jpg" for i in range(1, 6)]
        names += ["DZlog_20261014_1.jpg"]
        self.assertEqual(verify_scoped_counter_sequence(names), [1, 1, 2, 3, 4, 5])

    def test_duplicate_within_same_scope_fails(self):
        with self.assertRaises(AssertionError):
            verify_scoped_counter_sequence(["DZlog_20261013_1.jpg", "DZlog_20261013_1.jpg"])

    def test_missing_counter_within_same_scope_fails(self):
        with self.assertRaises(AssertionError):
            verify_scoped_counter_sequence(["DZlog_20261013_1.jpg", "DZlog_20261013_3.jpg"])

    def test_new_date_must_start_at_one(self):
        with self.assertRaises(AssertionError):
            verify_scoped_counter_sequence(["DZlog_20261013_1.jpg", "DZlog_20261014_2.jpg"])


if __name__ == "__main__":
    unittest.main()
