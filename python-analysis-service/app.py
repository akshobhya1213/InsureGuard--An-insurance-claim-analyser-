"""
InsureGuard AI - Python Analysis Service

Exposes three REST endpoints consumed by the Spring Boot backend:
  POST /analyze-text   -> Watson NLU + explainable fraud scoring
  POST /analyze-image  -> Roboflow/YOLO damage detection + OpenCV annotation
  GET  /health         -> liveness check

This service does ONLY analysis. It has no database and no auth -
Spring Boot is the system of record and the only thing Gradio talks to
for anything other than this analysis.
"""

import logging
import os

from dotenv import load_dotenv
from flask import Flask, jsonify, request

from services.watson_service import WatsonService, WatsonServiceError
from services.damage_detector import DamageDetector
from services.roboflow_service import RoboflowServiceError
from services.fraud_analyzer import analyze_text_for_fraud
from utils.image_utils import save_upload
from utils.validation import validate_text_payload, validate_image_file

load_dotenv()

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
logger = logging.getLogger("insureguard.analysis-service")

app = Flask(__name__)

watson_service = WatsonService()
damage_detector = DamageDetector()

UPLOAD_DIR = os.getenv("UPLOAD_DIR", "./uploads")


@app.route("/health", methods=["GET", "POST"])
def health():
    return jsonify({"status": "UP", "service": "insureguard-analysis-service"}), 200


@app.route("/analyze-text", methods=["POST"])
def analyze_text():
    data = request.get_json(silent=True)
    is_valid, error = validate_text_payload(data)
    if not is_valid:
        return jsonify({"error": error}), 400

    text = data["text"].strip()
    logger.info("Analyzing report text (%d chars)", len(text))

    try:
        watson_result = watson_service.analyze(text)
    except WatsonServiceError as exc:
        logger.error("Watson NLU error: %s", exc)
        return jsonify({"error": str(exc)}), 502

    fraud_result = analyze_text_for_fraud(text)

    return jsonify({
        "watson": watson_result,
        "fraud_analysis": fraud_result,
    }), 200


@app.route("/analyze-image", methods=["POST"])
def analyze_image():
    if "image" not in request.files:
        return jsonify({"error": "No image file provided under form field 'image'"}), 400

    image_file = request.files["image"]
    is_valid, error = validate_image_file(image_file)
    if not is_valid:
        return jsonify({"error": error}), 400

    saved_path = save_upload(image_file, UPLOAD_DIR)
    logger.info("Saved uploaded image to %s", saved_path)

    try:
        result = damage_detector.detect(saved_path)
    except RoboflowServiceError as exc:
        logger.error("Roboflow configuration/inference error: %s", exc)
        return jsonify({"error": str(exc)}), 502

    return jsonify(result), 200


if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)
