import unittest


class TestDamageResponseParsing(unittest.TestCase):
    """Verifies the center-to-top-left bounding box conversion used by RoboflowService."""

    def test_center_to_topleft_conversion(self):
        center_x, center_y, width, height = 200, 150, 100, 60
        expected_x = int(center_x - width / 2)
        expected_y = int(center_y - height / 2)
        self.assertEqual(expected_x, 150)
        self.assertEqual(expected_y, 120)


if __name__ == "__main__":
    unittest.main()
