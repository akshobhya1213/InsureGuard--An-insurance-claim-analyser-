"""
InsureGuard AI - Gradio UI

Talks ONLY to the Spring Boot backend REST API. It never touches MySQL,
Redis, Kafka, Watson, or Roboflow directly - Spring Boot remains the
single main backend.

Flow: register/login -> get JWT -> submit report (+ optional image)
-> trigger analysis -> render combined text + image results.
"""

import os

import gradio as gr
import requests

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")


def login_or_register(name, email, password, role, mode):
    try:
        if mode == "Register":
            resp = requests.post(
                f"{BACKEND_URL}/api/auth/register",
                json={"name": name, "email": email, "password": password, "role": role},
                timeout=10,
            )
        else:
            resp = requests.post(
                f"{BACKEND_URL}/api/auth/login",
                json={"email": email, "password": password},
                timeout=10,
            )

        if resp.status_code not in (200, 201):
            return None, f"❌ {resp.status_code}: {resp.json().get('message', resp.text)}"

        data = resp.json()
        token = data["token"]
        return token, f"✅ Logged in as {data['email']} ({data['role']})"

    except requests.exceptions.ConnectionError:
        return None, "❌ Could not reach the Spring Boot backend. Is it running on " + BACKEND_URL + "?"
    except Exception as exc:
        return None, f"❌ Unexpected error: {exc}"


def submit_and_analyze(token, description, image_path):
    if not token:
        return "❌ Please log in first.", "", ""

    headers = {"Authorization": f"Bearer {token}"}

    try:
        files = {"report": (None, f'{{"description": "{description}"}}', "application/json")}
        if image_path:
            files["image"] = open(image_path, "rb")

        create_resp = requests.post(
            f"{BACKEND_URL}/api/reports",
            headers=headers,
            files=files,
            timeout=30,
        )
        if create_resp.status_code != 201:
            return f"❌ Failed to create report: {create_resp.text}", "", ""

        report_id = create_resp.json()["id"]

        analyze_resp = requests.post(
            f"{BACKEND_URL}/api/reports/{report_id}/analyze",
            headers=headers,
            timeout=60,
        )
        if analyze_resp.status_code != 200:
            return f"❌ Analysis failed: {analyze_resp.text}", "", ""

        result = analyze_resp.json()
        return format_result(result)

    except requests.exceptions.ConnectionError:
        return "❌ Could not reach the Spring Boot backend.", "", ""
    except Exception as exc:
        return f"❌ Unexpected error: {exc}", "", ""


def format_result(result):
    text_analysis = result.get("textAnalysis") or {}
    detections = result.get("damageDetections") or []

    report_summary = (
        f"### Report Analysis\n\n"
        f"**Suspicion Score:** {result.get('fraudScore', 0)}/100\n\n"
        f"**Status:** {result.get('overallStatus', 'UNKNOWN')}\n\n"
        f"**Suspicious Indicators:**\n"
        + ("\n".join(f"- {i}" for i in text_analysis.get("indicators", [])) or "- None")
    )

    nlp_summary = (
        f"### NLP Information (IBM Watson NLU)\n\n"
        f"**Keywords:** {', '.join(text_analysis.get('keywords', [])) or 'None'}\n\n"
        f"**Entities:** {', '.join(text_analysis.get('entities', [])) or 'None'}\n\n"
        f"**Concepts:** {', '.join(text_analysis.get('concepts', [])) or 'None'}\n\n"
        f"**Sentiment:** {text_analysis.get('sentiment', 'N/A')}"
    )

    if detections:
        damage_summary = "### Vehicle Damage Detected\n\n" + "\n".join(
            f"- **{d['damageType']}** — confidence: {d['confidence'] * 100:.0f}%"
            for d in detections
        )
    else:
        damage_summary = "### Vehicle Damage\n\nNo image analyzed, or no damage detected."

    return report_summary, nlp_summary, damage_summary


with gr.Blocks(title="InsureGuard AI - Vehicle Report Analyzer") as demo:
    gr.Markdown("# 🚗 InsureGuard AI\n### Intelligent Insurance Claim Analyzer")

    token_state = gr.State(value=None)

    with gr.Tab("1. Login / Register"):
        mode = gr.Radio(["Login", "Register"], value="Login", label="Mode")
        name_in = gr.Textbox(label="Name (register only)")
        email_in = gr.Textbox(label="Email")
        password_in = gr.Textbox(label="Password", type="password")
        role_in = gr.Radio(["USER", "ADMIN"], value="USER", label="Role (register only)")
        auth_btn = gr.Button("Submit")
        auth_status = gr.Markdown()

        auth_btn.click(
            login_or_register,
            inputs=[name_in, email_in, password_in, role_in, mode],
            outputs=[token_state, auth_status],
        )

    with gr.Tab("2. Submit & Analyze"):
        description_in = gr.Textbox(
            label="Incident Report",
            placeholder="e.g. My car was damaged yesterday. The front bumper had already been damaged last month.",
            lines=4,
        )
        image_in = gr.Image(type="filepath", label="Vehicle Photo (optional)")
        analyze_btn = gr.Button("Analyze")

        report_out = gr.Markdown()
        nlp_out = gr.Markdown()
        damage_out = gr.Markdown()

        analyze_btn.click(
            submit_and_analyze,
            inputs=[token_state, description_in, image_in],
            outputs=[report_out, nlp_out, damage_out],
        )

if __name__ == "__main__":
    demo.launch(server_name="0.0.0.0", server_port=int(os.getenv("GRADIO_PORT", 7860)))
