import unittest

from utils.validation import validate_text_payload, validate_image_file


class FakeFileStorage:
    def __init__(self, filename):
        self.filename = filename


class TestTextValidation(unittest.TestCase):
    def test_missing_text_field(self):
        valid, error = validate_text_payload({})
        self.assertFalse(valid)
        self.assertIn("text", error)

    def test_text_too_short(self):
        valid, error = validate_text_payload({"text": "hi"})
        self.assertFalse(valid)

    def test_valid_text(self):
        valid, error = validate_text_payload({"text": "My car was damaged in the parking lot yesterday."})
        self.assertTrue(valid)
        self.assertIsNone(error)


class TestImageValidation(unittest.TestCase):
    def test_no_file(self):
        valid, error = validate_image_file(None)
        self.assertFalse(valid)

    def test_unsupported_extension(self):
        valid, error = validate_image_file(FakeFileStorage("report.txt"))
        self.assertFalse(valid)

    def test_supported_extension(self):
        valid, error = validate_image_file(FakeFileStorage("damage.jpg"))
        self.assertTrue(valid)


if __name__ == "__main__":
    unittest.main()
