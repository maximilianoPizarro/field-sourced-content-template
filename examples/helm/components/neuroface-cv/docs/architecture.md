# Architecture

## Overview

The NeuroFace CV Pipeline is an event-driven computer vision system deployed on OpenShift. It provides two independent detection pipelines:

1. **Face Detection** — Intel OpenVINO Model Server with LBPH recognition, Camel pollers, and email notifications
2. **PPE Safety Detection** — Ultralytics YOLOv8n with Granite LLM analysis, Kafka streaming, and Jupyter dashboards

Both pipelines share the same Kafka cluster (`cdc-cluster`) and publish to separate topics.

## Deployment Model

```
Namespace: neuroface                         Namespace: kafka-cdc
┌────────────────────────────────────┐       ┌─────────────────────────────────┐
│ neuroface-frontend  (Angular)      │       │ cdc-cluster (Strimzi Kafka)     │
│ neuroface-backend   (FastAPI)  ────┼──────▶│   ├─ cv.face.detections        │
│   ├─ /api/faces    (OVMS infer)    │       │   ├─ cv.ppe.detections  [NEW]  │
│   ├─ /ppe/detect   (YOLO + Kafka)  │       │   ├─ dlq.cv-errors             │
│   └─ confluent-kafka producer      │       │   └─ dlq.ppe-errors     [NEW]  │
│                                    │       │                                 │
│ neuroface-ovms     (OpenVINO)      │       │ camel-cv-processor              │
│   └─ face-detection-retail-0005    │       │   ├─ cv-ovms-status    (poller) │
│                                    │       │   ├─ cv-labels-poller  (poller) │
│ yolo-ppe-serving   (YOLOv8n) [NEW] │       │   └─ cv-face-notification      │
│   ├─ init: download yolov8n.pt     │       │       (Kafka → Mailpit emails)  │
│   └─ Flask /v1/predict endpoint    │       └─────────────────────────────────┘
│                                    │
│ granite-llm        (KServe)        │
│   └─ Granite 3.1 2B Instruct      │
│                                    │
│ neuroface-workbench (Jupyter)      │
│   ├─ neuroface-cv-pipeline.ipynb   │
│   └─ ppe-yolo-detection.ipynb      │
└────────────────────────────────────┘
```

## ArgoCD Applications

The system is deployed using the App of Apps pattern with two ArgoCD applications:

| Application | Source | Namespace | Sync Wave | What It Deploys |
|-------------|--------|-----------|-----------|-----------------|
| `field-content-helm-neuroface` | Helm chart `neuroface` v1.3.0 from GitHub Pages | `neuroface` | 10 | Backend, Frontend, OVMS, Granite LLM, PVC |
| `field-content-neuroface-cv` | Local chart `neuroface-cv` | `kafka-cdc` + `neuroface` | 11 | Kafka topics, Camel processor, YOLO serving, Jupyter workbench |

### Configuration Flow

```
examples/helm/values.yaml (App of Apps)
  │
  ├─ field-content-helm-neuroface:
  │    targetRevision: "1.3.0"
  │    values:
  │      ovms.enabled: true
  │      chat.enabled: true
  │      ppe.enabled: true           ← enables /ppe/detect endpoint
  │      ppe.endpoint: "http://yolo-ppe-serving.neuroface.svc:8000"
  │      ppe.kafka.enabled: true     ← enables Kafka producer in backend
  │      ppe.kafka.bootstrap: "cdc-cluster-kafka-bootstrap.kafka-cdc.svc:9093"
  │
  └─ neurofaceCv.ppe.enabled: true   ← deploys YOLO serving + Kafka topics
       └─ field-content-neuroface-cv:
            values:
              ppe.enabled: true
              ppe.yolo.image: "registry.access.redhat.com/ubi9/python-312:latest"
              ppe.kafka.topic: "cv.ppe.detections"
```

## PPE Detection — Detailed Flow

### Step-by-step

```
1. User opens NeuroFace Frontend → navigates to /ppe tab
2. Frontend captures webcam frame via WebRTC → encodes as base64
3. Frontend POSTs to Backend /ppe/detect with { image: "base64..." }
4. Backend forwards frame to YOLO PPE Serving POST /v1/predict
5. YOLO runs YOLOv8n inference → returns detections (class, confidence, bbox)
6. Backend classifies compliance:
   - expected PPE: hardhat, safety-vest, goggles (configurable)
   - present_ppe vs missing_ppe → ppe_status: compliant | violation | no_persons
7. If violation detected → Backend calls Granite LLM for safety analysis
8. Backend publishes event to Kafka topic cv.ppe.detections (SASL_SSL)
9. Backend returns full result to Frontend for display
10. Jupyter Workbench consumes events for compliance dashboards
```

### Kafka Producer (in NeuroFace Backend)

The Kafka producer lives inside `app/api/ppe.py` in the NeuroFace backend. It is **not** a separate processor — the same endpoint that serves the UI also publishes events.

| Config Variable | Env Var | Value |
|-----------------|---------|-------|
| `ppe_kafka_enabled` | `NEUROFACE_PPE_KAFKA_ENABLED` | `true` |
| `ppe_kafka_bootstrap` | `NEUROFACE_PPE_KAFKA_BOOTSTRAP` | `cdc-cluster-kafka-bootstrap.kafka-cdc.svc:9093` |
| `ppe_kafka_user` | `NEUROFACE_PPE_KAFKA_USER` | `cdc-user` |
| `ppe_kafka_password` | `NEUROFACE_PPE_KAFKA_PASSWORD` | From Secret `cdc-user` |
| `ppe_kafka_ca_cert` | `NEUROFACE_PPE_KAFKA_CA_CERT` | `/etc/kafka-certs/ca.crt` |
| `ppe_kafka_topic` | `NEUROFACE_PPE_KAFKA_TOPIC` | `cv.ppe.detections` |

### YOLO PPE Serving

| Property | Value |
|----------|-------|
| **Image** | `registry.access.redhat.com/ubi9/python-312:latest` |
| **Model** | YOLOv8n (6.3MB, downloaded at init) |
| **Framework** | Ultralytics + opencv-python-headless |
| **Endpoint** | `POST /v1/predict` (base64 image in body) |
| **Health** | `GET /health` |
| **Port** | 8000 |
| **Resources** | 500m–2 CPU, 1–4Gi memory |

### Granite LLM Integration

The backend calls Granite only when `ppe_status == "violation"`:

```python
POST http://granite-llm-metrics.neuroface.svc:8080/v1/chat/completions
{
  "model": "granite-3.1-2b-instruct",
  "messages": [{"role": "user", "content": "Analyze safety: missing PPE items..."}]
}
```

The LLM response is included in both the API response and the Kafka event as `llm_analysis`.

## Face Detection — Existing Pipeline

### Data Flow

1. User accesses **NeuroFace Frontend** → Face Detection tab
2. Frontend sends camera frames to **Backend** `/api/faces`
3. Backend calls **OVMS** REST API (`/v2/models/face-detection-retail-0005/infer`)
4. OVMS returns bounding boxes with confidence scores
5. Backend applies **LBPH recognizer** to identify known faces
6. Results displayed in the frontend

### Event Publishing (Camel Pollers)

Two Camel timer routes run every 30 seconds:

| Route | Source | Topic | Event Type |
|-------|--------|-------|------------|
| `cv-ovms-status` | OVMS `/v1/config` | `cv.face.detections` | `ovms_model_status` |
| `cv-labels-poller` | NeuroFace `/api/labels` | `cv.face.detections` | `person_registered` |

### Notification Consumer

The `cv-face-notification` route consumes from Kafka and sends emails via Mailpit:

| Event Type | Recipient | Email |
|------------|-----------|-------|
| `person_registered` | Security Admin | `admin@neuralbank.io` |
| `ovms_model_status` | ML Ops | `mlops@neuralbank.io` |

## Security

| Area | Mechanism |
|------|-----------|
| **Kafka** | SASL_SSL with SCRAM-SHA-512 (`cdc-user` credentials) |
| **TLS Certificates** | Mounted from `cdc-cluster-cluster-ca-cert` Secret at `/etc/kafka-certs` |
| **Kafka Password** | Mounted from `cdc-user` Secret as env var `NEUROFACE_PPE_KAFKA_PASSWORD` |
| **Jupyter OAuth** | Injected via `notebooks.opendatahub.io/inject-oauth` annotation |
| **OVMS Model** | Downloaded from Intel Model Zoo (public, no credentials) |
| **YOLO Model** | Downloaded from Ultralytics GitHub releases (public, no credentials) |

## Helm Chart Configuration

### `neuroface` chart (v1.3.0) — Backend + Frontend + OVMS + LLM

| Parameter | Default | Description |
|-----------|---------|-------------|
| `ppe.enabled` | `false` | Enable PPE detection endpoint in backend |
| `ppe.endpoint` | `""` | YOLO serving URL |
| `ppe.classes` | `hardhat,safety-vest,goggles` | Expected PPE items |
| `ppe.confidence` | `0.5` | Minimum detection confidence |
| `ppe.kafka.enabled` | `false` | Enable Kafka producer |
| `ppe.kafka.bootstrap` | `""` | Kafka bootstrap servers |
| `ppe.kafka.user` | `""` | SASL username |
| `ppe.kafka.topic` | `cv.ppe.detections` | Target topic |
| `ppe.kafka.secretName` | `cdc-user` | Secret with Kafka password |
| `ppe.kafka.caCertSecret` | `cdc-cluster-cluster-ca-cert` | Secret with CA cert |

### `neuroface-cv` chart — YOLO Serving + Topics + Camel + Jupyter

| Parameter | Default | Description |
|-----------|---------|-------------|
| `ppe.enabled` | `false` | Deploy YOLO serving + Kafka topics |
| `ppe.yolo.image` | `ubi9/python-312:latest` | Container image |
| `ppe.yolo.modelName` | `yolov8n` | YOLO model variant |
| `ppe.yolo.modelUrl` | ultralytics v8.3.0 | Model weights URL |
| `ppe.kafka.topic` | `cv.ppe.detections` | PPE topic name |
| `ppe.kafka.dlqTopic` | `dlq.ppe-errors` | Dead letter queue |
| `workbench.seedNotebooks` | `true` | Download notebooks on first boot |
