<div align="center">

# 🚗 InsureGuard AI
### Intelligent Insurance Claim Analyzer

*Automated first-pass analysis of vehicle insurance claims — suspicious-text scoring and visual damage detection, wired through a real microservice architecture.*

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-Event--Driven-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)

![IBM Watson](https://img.shields.io/badge/IBM%20Watson-NLU-052FAD?style=flat-square&logo=ibm&logoColor=white)
![Roboflow](https://img.shields.io/badge/Roboflow-YOLO-6706CE?style=flat-square)
![OpenCV](https://img.shields.io/badge/OpenCV-Image%20Annotation-5C3EE8?style=flat-square&logo=opencv&logoColor=white)
![Gradio](https://img.shields.io/badge/Gradio-UI-FF7C00?style=flat-square&logo=gradio&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

</div>

---

## 📋 Table of Contents

- [Problem Statement](#-problem-statement)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Folder Structure](#-folder-structure)
- [End-to-End Flow](#-end-to-end-flow)
- [Database Schema](#-database-schema)
- [API Reference](#-api-reference)
- [Fraud/Suspicion Scoring Engine](#-fraudsuspicion-scoring-engine)
- [Redis Caching](#-redis-caching)
- [Kafka Flow](#-kafka-flow)
- [Watson NLU & Roboflow/YOLO Integration](#-watson-nlu--roboflowyolo-integration)
- [Environment Variables](#-environment-variables)
- [Setup](#-setup)
- [Testing](#-testing)
- [Sample cURL Commands](#-sample-curl-commands)
- [Troubleshooting](#-troubleshooting)
- [Limitations & Future Work](#-limitations--future-work)
- [Resume Summary](#-resume-summary)

---

## 🎯 Problem Statement

Manually verifying incident reports and damage photos is slow and inconsistent. InsureGuard AI automates a **first-pass read**: it flags textual inconsistencies for a human adjuster to review, and detects/annotates visible vehicle damage from a photo — without pretending to replace a human decision.

1. **Text analysis** — the incident description is run through IBM Watson NLU, and a separate, explainable rule-based engine scores it for suspicious indicators.
2. **Image analysis** — an uploaded vehicle photo is run through a Roboflow-hosted YOLO model to detect visible damage, annotated with OpenCV.

> Java/Spring Boot is the main backend and system of record. Python is used **only** for the Watson and Roboflow/YOLO calls.

---

## 🏗️ Architecture

```
                         USER
                           │
                           ▼
                    ┌─────────────┐
                    │   Gradio    │
                    │     UI      │
                    └──────┬──────┘
                           │ REST
                           ▼
                 ┌─────────────────────┐
                 │    Spring Boot      │
                 │   Main Backend      │
                 └──────────┬──────────┘
                            │
             ┌──────────────┼──────────────┐
             │              │              │
             ▼              ▼              ▼
          MySQL           Redis          Kafka
             │                             │
             │                             ▼
             │                   Python Analysis Service
             │                         ╱         ╲
             ▼                        ▼            ▼
       Persistent Data        IBM Watson NLU   Roboflow + YOLO
                                     │             │
                                     ▼             ▼
                              Text Analysis    Damage Detection
                                     ╲             ╱
                                      ▼           ▼
                                Spring Boot stores results
                                            │
                                            ▼
                                         Gradio
```

Spring Boot owns auth, persistence, caching and messaging. The Python service is a pure analysis worker with **no database and no auth** — it only knows how to call Watson/Roboflow and return JSON.

---

## 🧰 Technology Stack

| Layer | Tech |
|---|---|
| 🟢 Main backend | Java 17 · Spring Boot 3.3 · Spring Web · Spring Security · JWT · Spring Data JPA · Hibernate · Maven |
| 🗄️ Database | MySQL 8 |
| ⚡ Caching | Redis 7 |
| 📨 Messaging | Apache Kafka (+ Zookeeper) |
| 🧠 Text analysis | IBM Watson NLU |
| 🖼️ Image analysis | Python · Roboflow (hosted YOLO inference) · OpenCV |
| 🎛️ UI | Gradio |
| 📦 Deployment | Docker · Docker Compose |

---

## 📁 Folder Structure

```
InsureGuard-AI/
├── backend/                      # Spring Boot main backend
│   ├── src/main/java/com/insureguard/
│   │   ├── controller/  service/  repository/  entity/  dto/
│   │   ├── security/  config/  exception/  kafka/  redis/  client/
│   │   └── InsureGuardApplication.java
│   ├── src/test/java/...         # JUnit + Mockito tests
│   ├── pom.xml
│   └── Dockerfile
├── python-analysis-service/      # Watson + Roboflow/YOLO worker
│   ├── app.py
│   ├── services/  utils/  tests/
│   ├── requirements.txt  .env.example
│   └── Dockerfile
├── gradio-ui/
│   ├── app.py  requirements.txt  Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🔄 End-to-End Flow

| Step | What happens |
|---|---|
| 1 | User registers / logs in → Spring Boot returns a JWT |
| 2 | User submits a report (text + optional image) → Spring Boot stores it (`SUBMITTED`) and publishes a `REPORT_SUBMITTED` Kafka event |
| 3 | A Kafka consumer (or the synchronous `/analyze` endpoint) triggers analysis: Spring Boot calls the Python service |
| 4 | Python calls Watson NLU for keywords/entities/concepts/sentiment, and runs the local `fraud_analyzer` rule engine on the raw text for a suspicion score |
| 5 | If an image was uploaded, Python calls Roboflow's hosted YOLO model, gets back bounding boxes, and uses OpenCV to draw an annotated copy |
| 6 | Spring Boot persists `TextAnalysis`, `DamageDetection`, and a combined `AnalysisResult`; report status becomes `COMPLETED` (or `FAILED` on error) |
| 7 | The result is cached in Redis (TTL-based) so repeated lookups skip MySQL |
| 8 | Gradio calls the backend's REST APIs to display the score, indicators, NLP details, and annotated image |

---

## 🗃️ Database Schema

```
User (1) ──< Report (1) ──── TextAnalysis
                 │
                 ├──< DamageDetection (many, one per detected damage area)
                 │
                 └──── AnalysisResult (1, combined verdict)
```

<details>
<summary><b>Field-level detail (click to expand)</b></summary>

- **User**: id, name, email (unique), password (BCrypt hash), role, createdAt
- **Report**: id, user_id (FK), description, image_path, status, createdAt, updatedAt
- **TextAnalysis**: id, report_id (FK, unique), keywords/entities/concepts/sentiment (JSON text), suspicion_score, suspicion_indicators, status
- **DamageDetection**: id, report_id (FK), damage_type, confidence, bbox_x/y/width/height, annotated_image_path
- **AnalysisResult**: id, report_id (FK, unique), overall_status, fraud_score, createdAt

</details>

---

## 🔌 API Reference

<details open>
<summary><b>Auth</b></summary>

| Method | Path | Auth |
|---|---|---|
| `POST` | `/api/auth/register` | public |
| `POST` | `/api/auth/login` | public |

</details>

<details open>
<summary><b>Reports</b></summary>

| Method | Path | Auth |
|---|---|---|
| `POST` | `/api/reports` (multipart: `report` JSON part + optional `image`) | USER |
| `GET` | `/api/reports/{id}` | authenticated |
| `GET` | `/api/reports/user/{userId}` | owner or ADMIN |

</details>

<details open>
<summary><b>Analysis</b></summary>

| Method | Path | Auth |
|---|---|---|
| `POST` | `/api/reports/{id}/analyze` | authenticated |
| `GET` | `/api/reports/{id}/result` | authenticated |

</details>

<details open>
<summary><b>Admin</b></summary>

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/admin/reports` | ADMIN |
| `GET` | `/api/admin/users` | ADMIN |
| `GET` | `/api/admin/statistics` | ADMIN |

</details>

### Sample request/response

```http
POST /api/auth/login
{ "email": "matrixx@example.com", "password": "password123" }
```
```json
200 OK
{ "token": "eyJhbGciOi...", "tokenType": "Bearer", "userId": 1, "name": "Matrixx", "email": "matrixx@example.com", "role": "USER" }
```

```http
POST /api/reports/5/analyze
Authorization: Bearer <token>
```
```json
200 OK
{
  "reportId": 5,
  "overallStatus": "SUSPICIOUS",
  "fraudScore": 75,
  "textAnalysis": {
    "keywords": ["damaged", "bumper", "accident"],
    "indicators": ["Previous damage mentioned", "Timeline contradiction"],
    "status": "SUSPICIOUS",
    "suspicionScore": 75
  },
  "damageDetections": [
    { "damageType": "front_bumper", "confidence": 0.92, "x": 120, "y": 80, "width": 300, "height": 150 }
  ],
  "annotatedImagePath": "./uploads/annotated/annotated_xyz.jpg"
}
```

---

## ⚖️ Fraud/Suspicion Scoring Engine

> **Watson NLU is not a fraud detector** — it only returns NLP structure. The scoring itself is a separate, explainable rule engine (`python-analysis-service/services/fraud_analyzer.py`) that scans the raw report text with regex-based indicators.

| Indicator | Points |
|---|:---:|
| Previous damage mentioned | `+25` |
| Previous incident mentioned | `+20` |
| Timeline contradiction | `+25` |
| Repeated incident mentioned | `+15` |
| Suspicious wording | `+15` |

**Bands:** 🟢 `0–39` LOW RISK · 🟡 `40–69` REVIEW REQUIRED · 🔴 `70–100` SUSPICIOUS

The score is deterministic (same text → same score) and dynamic (different text → different score) — covered by unit tests in `tests/test_fraud_analyzer.py`.

---

## ⚡ Redis Caching

`AnalysisResult` lookups (`GET /api/reports/{id}/result`) go through a **cache-aside** pattern (`ReportCacheService`): check Redis first, and on a miss fall back to MySQL and populate the cache with a TTL (`RESULT_CACHE_TTL_SECONDS`, default 600s). Redis never computes or influences the fraud score — it only caches already-computed results.

## 📨 Kafka Flow

On report creation, `ReportEventProducer` publishes a `REPORT_SUBMITTED` event (`{reportId, userId, eventType}`) to the topic named by `app.kafka.topic.report-submitted`. `ReportEventConsumer` picks it up and calls `AnalysisService.processReportAsync`, so the client isn't blocked while Watson/Roboflow run. The synchronous `POST /api/reports/{id}/analyze` endpoint is also available for manual re-analysis or the Gradio UI.

## 🧠 Watson NLU & Roboflow/YOLO Integration

- **Watson** (`services/watson_service.py`) uses the official `ibm-watson` SDK with an `IAMAuthenticator`, requesting keywords, entities, concepts and document sentiment. Credentials come from `WATSON_API_KEY` / `WATSON_URL` — if unset, the service raises a clear `WatsonServiceError` instead of returning fake data.
- **Roboflow** (`services/roboflow_service.py`) uses `inference-sdk` to call a hosted YOLO model (`ROBOFLOW_WORKSPACE/ROBOFLOW_PROJECT/ROBOFLOW_VERSION`). Roboflow returns center-based bounding boxes, converted to top-left `(x, y, width, height)` for OpenCV. `utils/image_utils.py` draws the boxes and labels and saves an annotated copy.

> ⚠️ If Roboflow isn't configured, the service returns a clear setup error — it never fabricates detections.

---

## 🔑 Environment Variables

<details>
<summary><b>backend</b> (root <code>.env</code> + docker-compose)</summary>

`DB_NAME` · `DB_USERNAME` · `DB_PASSWORD` · `JWT_SECRET` · `JWT_EXPIRATION_MS` · `REDIS_HOST` · `REDIS_PORT` · `KAFKA_BOOTSTRAP_SERVERS` · `PYTHON_SERVICE_URL` · `RESULT_CACHE_TTL_SECONDS` · `IMAGE_UPLOAD_DIR`

</details>

<details>
<summary><b>python-analysis-service/.env</b></summary>

`WATSON_API_KEY` · `WATSON_URL` · `ROBOFLOW_API_KEY` · `ROBOFLOW_WORKSPACE` · `ROBOFLOW_PROJECT` · `ROBOFLOW_VERSION` · `PORT` · `UPLOAD_DIR` · `ANNOTATED_DIR`

</details>

---

## 🚀 Setup

<details>
<summary><b>Local (without Docker)</b></summary>

```bash
# 1. MySQL, Redis, Kafka+Zookeeper — via docker-compose for just these three:
docker compose up -d mysql redis zookeeper kafka

# 2. Python analysis service
cd python-analysis-service
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env      # then fill in WATSON_* and ROBOFLOW_* keys
python app.py             # runs on :5000

# 3. Spring Boot backend (new terminal)
cd backend
cp ../.env.example .env   # or export the vars directly
mvn clean install
mvn spring-boot:run       # runs on :8080

# 4. Gradio UI (new terminal)
cd gradio-ui
pip install -r requirements.txt
python app.py             # runs on :7860
```

</details>

<details>
<summary><b>Docker (full stack)</b></summary>

```bash
cp .env.example .env
cp python-analysis-service/.env.example python-analysis-service/.env
# edit both .env files with real secrets/keys

docker compose up --build
```

| Service | Port |
|---|:---:|
| MySQL | `3306` |
| Redis | `6379` |
| Kafka | `9092` |
| Python analysis service | `5000` |
| Spring Boot backend | `8080` |
| Gradio UI | `7860` |

</details>

---

## 🧪 Testing

```bash
# Java (JUnit 5 + Mockito)
cd backend && mvn test

# Python (unittest)
cd python-analysis-service && python -m unittest discover -s tests
```

`test_fraud_analyzer.py` verifies the scoring engine is deterministic, dynamic across different inputs, and capped at 100. `AuthServiceTest` / `JwtUtilTest` cover registration, login, and token generation/validation.

---

## 💻 Sample cURL Commands

<details>
<summary>Click to expand</summary>

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Matrixx","email":"matrixx@example.com","password":"password123","role":"USER"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"matrixx@example.com","password":"password123"}'

# Create report (with image)
curl -X POST http://localhost:8080/api/reports \
  -H "Authorization: Bearer <token>" \
  -F 'report={"description":"My car was damaged yesterday. The bumper had already been damaged last month."};type=application/json' \
  -F "image=@car_damage.jpg"

# Trigger analysis
curl -X POST http://localhost:8080/api/reports/1/analyze -H "Authorization: Bearer <token>"

# Get result
curl http://localhost:8080/api/reports/1/result -H "Authorization: Bearer <token>"
```

</details>

---

## 🛠️ Troubleshooting

| Symptom | Fix |
|---|---|
| `"Watson NLU is not configured"` | Set `WATSON_API_KEY` / `WATSON_URL` in `python-analysis-service/.env` |
| `"Roboflow is not configured"` | Set the four `ROBOFLOW_*` vars — expected until you train/attach a real damage-detection project |
| `503` from `/api/reports/{id}/analyze` | Python service isn't running or isn't reachable at `PYTHON_SERVICE_URL` |
| Kafka consumer not processing events | Confirm backend and Kafka share `KAFKA_BOOTSTRAP_SERVERS`/network, check the `insureguard-group` consumer is up |

---

## ⚠️ Limitations & Future Work

**Limitations**
- The fraud scoring engine is regex/keyword-based, not a trained ML classifier — intentionally simple and explainable, not state-of-the-art.
- Damage detection quality depends entirely on the Roboflow model you train and attach; no bundled/pretrained model ships with this repo.
- No refresh-token flow — JWTs simply expire after `JWT_EXPIRATION_MS`.
- Single annotated image path per report today; multi-image reports would need a small schema change.

**Future improvements**
- Refresh-token flow + rate limiting on `/api/auth/login`
- Trained text classifier alongside the current explainable rule engine
- Pagination/filtering on admin endpoints
- Dead-letter topic for Kafka consumer failures

---

## 📄 Resume Summary

**InsureGuard AI – Intelligent Insurance Claim Analyzer** · *Java, Spring Boot, Python, IBM Watson NLU, Roboflow, YOLO, Kafka, Redis, MySQL, Gradio*

- Built a microservice-based claim analyzer with a Spring Boot/JWT-secured backend and a dedicated Python service for IBM Watson NLU text analysis and Roboflow/YOLO vehicle damage detection.
- Designed an explainable, rule-based fraud/suspicion scoring engine that flags textual inconsistencies (prior damage, repeated incidents, timeline contradictions) independent of the NLP layer.
- Implemented asynchronous claim processing with Kafka producer/consumer pipelines and a Redis cache-aside layer for analysis-result lookups.
- Delivered a Gradio front end consuming the Spring Boot REST API end-to-end, containerized the full stack with Docker Compose, and covered core logic with JUnit/Mockito and Python unit tests. Selected for the final round of a hackathon.

> *No accuracy, user-count, or performance numbers are claimed, since no production traffic or benchmark dataset backs them yet.*

<div align="center">

---
Built by [Matrixx](https://github.com/akshobhya1213)

</div>
