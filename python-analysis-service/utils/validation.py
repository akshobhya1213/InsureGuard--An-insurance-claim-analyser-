"""Input validation helpers for the analysis service."""

ALLOWED_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "webp"}
MAX_TEXT_LENGTH = 5000
MIN_TEXT_LENGTH = 5


def validate_text_payload(data):
    """Validate the JSON body of POST /analyze-text.

    Returns (is_valid, error_message).
    """
    if not data or "text" not in data:
        return False, "Request body must include a 'text' field"

    text = data.get("text", "")
    if not isinstance(text, str):
        return False, "'text' must be a string"

    text = text.strip()
    if len(text) < MIN_TEXT_LENGTH:
        return False, f"'text' must be at least {MIN_TEXT_LENGTH} characters"
    if len(text) > MAX_TEXT_LENGTH:
        return False, f"'text' must not exceed {MAX_TEXT_LENGTH} characters"

    return True, None


def validate_image_file(file_storage):
    """Validate an uploaded image (a werkzeug FileStorage object)."""
    if file_storage is None or file_storage.filename == "":
        return False, "No image file was uploaded"

    filename = file_storage.filename.lower()
    if "." not in filename:
        return False, "Uploaded file has no extension"

    extension = filename.rsplit(".", 1)[1]
    if extension not in ALLOWED_IMAGE_EXTENSIONS:
        return False, f"Unsupported image type '.{extension}'. Allowed: {', '.join(ALLOWED_IMAGE_EXTENSIONS)}"

    return True, None
