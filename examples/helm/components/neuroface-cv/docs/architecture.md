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
| `field-content-helm-neuroface` | `neuroface` Helm chart v1.2.1 | `neuroface` | 10 |
| `field-content-neuroface-cv` | `neuroface-cv` local component | `kafka-cdc` | 11 |

## Security

- Kafka connections use **SASL_SSL** with SCRAM-SHA-512 (`cdc-user` credentials)
- TLS certificates mounted from `cdc-cluster-cluster-ca-cert` secret
- OpenShift OAuth injected into Jupyter workbench via `notebooks.opendatahub.io/inject-oauth`
- OVMS model downloaded from Intel's public model zoo (no credentials required)
