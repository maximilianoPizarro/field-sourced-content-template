# Architecture

## Overview

The NeuroFace CV Pipeline is an event-driven computer vision system deployed across two OpenShift namespaces. It detects faces using Intel's OpenVINO, tracks trained persons via LBPH recognition, and publishes all events to a shared Kafka topic for downstream processing and email notifications.

## Data Flow

### 1. Face Detection (User-driven)

1. User accesses the **NeuroFace Frontend** (Angular)
2. Frontend sends camera frames to **NeuroFace Backend** (FastAPI)
3. Backend calls **OVMS** REST API (`/v2/models/face-detection-retail-0005/infer`)
4. OVMS returns bounding boxes with confidence scores
5. Backend applies **LBPH recognizer** to identify known faces
6. Results displayed in the frontend

### 2. Event Publishing (Automated pollers)

Two Camel timer routes run every 30 seconds:

| Route | Source | Event Type |
|-------|--------|------------|
| `cv-ovms-status` | OVMS `/v1/config` | `ovms_model_status` |
| `cv-labels-poller` | NeuroFace `/api/labels` | `person_registered` |

Both publish to the `cv.face.detections` Kafka topic on `cdc-cluster`.

### 3. Notification (Consumer)

The `cv-face-notification` Camel route consumes from Kafka and dispatches emails:

| Event Type | Recipient | Subject |
|------------|-----------|---------|
| `person_registered` | Security Admin (`admin@neuralbank.io`) | Person Registered: {name} |
| `ovms_model_status` | MLOps (`mlops@neuralbank.io`) | OVMS Model Status: {model} |

Failed messages are routed to the `dlq.cv-errors` dead letter queue.

## Deployment Model

```
Namespace: neuroface                    Namespace: kafka-cdc
┌──────────────────────┐                ┌──────────────────────────┐
│ neuroface-backend    │                │ cdc-cluster (Kafka)      │
│ neuroface-frontend   │                │   └─ cv.face.detections  │
│ neuroface-ovms       │                │   └─ dlq.cv-errors       │
│   └─ init: download  │                │                          │
│   └─ ovms container  │                │ camel-cv-processor       │
│ neuroface-workbench  │                │   └─ cv-ovms-status      │
│   └─ Jupyter DS      │                │   └─ cv-labels-poller    │
└──────────────────────┘                │   └─ cv-face-notification│
                                        └──────────────────────────┘
```

## ArgoCD Applications

| Application | Chart/Path | Namespace | Sync Wave |
|-------------|------------|-----------|-----------|
| `field-content-helm-neuroface` | `neuroface` Helm chart v1.3.0 | `neuroface` | 10 |
| `field-content-neuroface-cv` | `neuroface-cv` local component | `kafka-cdc` | 11 |

## PPE Safety Detection (YOLO) — Optional Module

When `ppe.enabled=true`, the chart deploys a parallel YOLO-based PPE detection pipeline that does **not** interfere with the existing OpenVINO face detection:

### PPE Data Flow

1. **YOLO Serving** (`yolo-ppe-serving` in `neuroface`) loads a pre-trained YOLOv8n model
2. **PPE Processor** (`ppe-yolo-processor` in `kafka-cdc`) polls the YOLO endpoint every N seconds
3. Processor classifies detected objects against expected PPE (hardhat, safety-vest, goggles)
4. On **violation**, calls **Granite LLM** for a natural language safety analysis
5. Publishes `ppe_compliance_check` events to `cv.ppe.detections` Kafka topic
6. Consumer sends HTML email alerts to Safety Officer via Mailpit

### PPE Deployment Model

```
Namespace: neuroface                    Namespace: kafka-cdc
┌──────────────────────────────┐        ┌────────────────────────────────┐
│ (existing — untouched)       │        │ (existing — untouched)         │
│ neuroface-backend            │        │ cdc-cluster (Kafka)            │
│ neuroface-frontend           │        │   └─ cv.face.detections        │
│ neuroface-ovms               │        │   └─ dlq.cv-errors             │
│ granite-llm (existing)  ◄────┼────────┤ camel-cv-processor             │
│                              │        │                                │
│ (NEW — ppe.enabled=true)     │        │ (NEW — ppe.enabled=true)       │
│ yolo-ppe-serving         ◄───┼────────┤ cv.ppe.detections (NEW topic)  │
│   └─ init: download model    │        │ dlq.ppe-errors (NEW topic)     │
│   └─ ultralytics yolov8n     │        │                                │
│                              │        │ Kafka producer lives in the    │
│ neuroface-workbench          │        │ NeuroFace backend (ppe.py)     │
│   └─ seeded notebooks        │        │   └─ webcam → YOLO → Kafka    │
└──────────────────────────────┘        └────────────────────────────────┘
```

### PPE Configuration

All PPE parameters are in `values.yaml` under the `ppe` key and validated by `values.schema.json`. Key settings:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `ppe.enabled` | `false` | Master toggle for the entire PPE module |
| `ppe.yolo.modelName` | `yolov8n` | YOLO model variant |
| `ppe.yolo.modelUrl` | ultralytics/v8.3.0 | Model weights download URL |
| `ppe.kafka.topic` | `cv.ppe.detections` | Kafka topic name |

## Security

- Kafka connections use **SASL_SSL** with SCRAM-SHA-512 (`cdc-user` credentials)
- TLS certificates mounted from `cdc-cluster-cluster-ca-cert` secret
- OpenShift OAuth injected into Jupyter workbench via `notebooks.opendatahub.io/inject-oauth`
- OVMS model downloaded from Intel's public model zoo (no credentials required)
- YOLO model downloaded from Ultralytics GitHub releases (public, no credentials)
