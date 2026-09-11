"""OpenCV helpers for saving uploads and drawing bounding boxes on detections."""

import os
import uuid

import cv2


def save_upload(file_storage, upload_dir):
    """Persist an uploaded FileStorage to disk and return the file path."""
    os.makedirs(upload_dir, exist_ok=True)
    extension = file_storage.filename.rsplit(".", 1)[1].lower()
    filename = f"{uuid.uuid4()}.{extension}"
    path = os.path.join(upload_dir, filename)
    file_storage.save(path)
    return path


def draw_detections(image_path, detections, annotated_dir):
    """Draw bounding boxes + labels for each detection and save an annotated copy.

    detections: list of dicts with keys class, confidence, x, y, width, height
    (x, y) is the top-left corner of the box.
    Returns the path to the annotated image.
    """
    image = cv2.imread(image_path)
    if image is None:
        raise ValueError(f"Could not read image at {image_path}")

    for det in detections:
        x, y, w, h = det["x"], det["y"], det["width"], det["height"]
        label = f"{det['class']} {det['confidence'] * 100:.0f}%"

        cv2.rectangle(image, (x, y), (x + w, y + h), (0, 0, 255), 2)
        (text_w, text_h), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.6, 2)
        cv2.rectangle(image, (x, y - text_h - 8), (x + text_w + 4, y), (0, 0, 255), -1)
        cv2.putText(image, label, (x + 2, y - 6), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

    os.makedirs(annotated_dir, exist_ok=True)
    annotated_filename = f"annotated_{os.path.basename(image_path)}"
    annotated_path = os.path.join(annotated_dir, annotated_filename)
    cv2.imwrite(annotated_path, image)
    return annotated_path
