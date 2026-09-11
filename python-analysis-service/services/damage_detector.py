"""Combines RoboflowService (inference) with image_utils (OpenCV annotation)."""

import os

from services.roboflow_service import RoboflowService
from utils.image_utils import draw_detections


class DamageDetector:
    def __init__(self):
        self.roboflow_service = RoboflowService()
        self.annotated_dir = os.getenv("ANNOTATED_DIR", "./uploads/annotated")

    def detect(self, image_path: str) -> dict:
        """
        Returns:
            {
              "detections": [...],
              "annotated_image_path": str | None
            }
        Raises RoboflowServiceError if Roboflow isn't configured or fails -
        callers should surface this as a clear setup error, never fabricate results.
        """
        detections = self.roboflow_service.detect_damage(image_path)

        annotated_path = None
        if detections:
            annotated_path = draw_detections(image_path, detections, self.annotated_dir)

        return {
            "detections": detections,
            "annotated_image_path": annotated_path,
        }
