"""
Wraps a Roboflow-hosted YOLO model for vehicle damage detection.

Uses Roboflow's hosted inference API (via inference-sdk) so no local
GPU/model weights are required. Requires a trained damage-detection
project on Roboflow (workspace + project slug + version).
"""

import os

from inference_sdk import InferenceHTTPClient


class RoboflowServiceError(Exception):
    """Raised when Roboflow is not configured or the API call fails."""


class RoboflowService:
    def __init__(self):
        self.api_key = os.getenv("ROBOFLOW_API_KEY")
        self.workspace = os.getenv("ROBOFLOW_WORKSPACE")
        self.project = os.getenv("ROBOFLOW_PROJECT")
        self.version = os.getenv("ROBOFLOW_VERSION")
        self._client = None

    def _get_client(self):
        if not all([self.api_key, self.workspace, self.project, self.version]):
            raise RoboflowServiceError(
                "Roboflow is not configured. Set ROBOFLOW_API_KEY, ROBOFLOW_WORKSPACE, "
                "ROBOFLOW_PROJECT and ROBOFLOW_VERSION in .env"
            )
        if self._client is None:
            self._client = InferenceHTTPClient(
                api_url="https://detect.roboflow.com",
                api_key=self.api_key,
            )
        return self._client

    def detect_damage(self, image_path: str) -> list:
        """
        Run inference on the given image and return a list of detections:
        [{"class": str, "confidence": float, "x": int, "y": int, "width": int, "height": int}, ...]

        Coordinates are converted from Roboflow's center-based format
        to top-left (x, y) + width/height, which is what OpenCV expects
        for drawing rectangles.
        """
        client = self._get_client()
        model_id = f"{self.project}/{self.version}"

        try:
            result = client.infer(image_path, model_id=model_id)
        except Exception as exc:
            raise RoboflowServiceError(f"Roboflow inference failed: {exc}") from exc

        detections = []
        for pred in result.get("predictions", []):
            center_x = pred["x"]
            center_y = pred["y"]
            width = pred["width"]
            height = pred["height"]
            detections.append({
                "class": pred.get("class", "unknown"),
                "confidence": round(float(pred.get("confidence", 0.0)), 4),
                "x": int(center_x - width / 2),
                "y": int(center_y - height / 2),
                "width": int(width),
                "height": int(height),
            })

        return detections
