# NeuroFace CV Pipeline

Computer Vision pipeline built on [NeuroFace](https://github.com/maximilianoPizarro/neuroface) — facial recognition with OpenVINO, PPE safety detection with YOLO, Kafka event streaming, Granite LLM analysis, and Jupyter workbench.

## Quick Access

| Resource | URL |
|----------|-----|
| **NeuroFace Web App** | [Frontend](https://neuroface-neuroface.\<CLUSTER_DOMAIN\>) |
| **PPE Safety Tab** | [Frontend → PPE](https://neuroface-neuroface.\<CLUSTER_DOMAIN\>/ppe) |
| **YOLO Health** | [/health](https://yolo-ppe-serving-neuroface.\<CLUSTER_DOMAIN\>/health) |
| **OVMS Config** | [/v1/config](https://neuroface-ovms-neuroface.\<CLUSTER_DOMAIN\>/v1/config) |
| **Jupyter Workbench** | [OpenShift AI](https://data-science-gateway.\<CLUSTER_DOMAIN\>/projects/neuroface) |
| **Kafka Console** | [Kafka UI](https://kafka-console-kafka-cdc.\<CLUSTER_DOMAIN\>) |
| **Mailpit Inbox** | [CV Notifications](https://n8n-mailpit-openshift-lightspeed.\<CLUSTER_DOMAIN\>) |

## Namespaces

| Namespace | Purpose |
|-----------|---------|
| `neuroface` | NeuroFace app (backend + frontend), OVMS, YOLO PPE Serving, Granite LLM, Jupyter workbench |
| `kafka-cdc` | Kafka cluster (`cdc-cluster`), topics, Camel CV processor, DLQ |

## Internal Endpoints

| Service | Address | Protocol |
|---------|---------|----------|
| NeuroFace Backend | `http://neuroface-backend.neuroface.svc:8080` | REST |
| OVMS REST | `http://neuroface-ovms.neuroface.svc:8000` | REST (KServe v2) |
| OVMS gRPC | `neuroface-ovms.neuroface.svc:9000` | gRPC |
| YOLO PPE Serving | `http://yolo-ppe-serving.neuroface.svc:8000` | REST |
| Granite LLM | `http://granite-llm-metrics.neuroface.svc:8080` | REST (OpenAI-compatible) |
| Kafka Bootstrap | `cdc-cluster-kafka-bootstrap.kafka-cdc.svc:9093` | SASL_SSL |

## How Everything Connects

```
                        ┌──────────────────────────────────────────────────┐
                        │                  NEUROFACE NAMESPACE              │
                        │                                                  │
 ┌────────┐   WebRTC    │  ┌──────────────┐    REST     ┌──────────────┐  │
 │ User   │────────────▶│  │  Angular     │───────────▶│   FastAPI     │  │
 │ Webcam │             │  │  Frontend    │            │   Backend     │  │
 └────────┘             │  │  /ppe tab    │◀───────────│              │  │
                        │  └──────────────┘   results   │  ┌──────────┤  │
                        │                               │  │ /ppe/    │  │
                        │         ┌─────────────────────│  │ detect   │  │
                        │         │ POST /v1/predict     │  └──────────┤  │
                        │         ▼                      │             │  │
                        │  ┌──────────────┐              │  ┌──────────┤  │
                        │  │ YOLO PPE     │──detections─▶│  │ Kafka   │  │
                        │  │ Serving      │              │  │ Producer│──┼──┐
                        │  │ (YOLOv8n)    │              │  └──────────┤  │  │
                        │  └──────────────┘              │             │  │  │
                        │                                │  ┌──────────┤  │  │
                        │  ┌──────────────┐   analyze   │  │ Granite │  │  │
                        │  │ Granite LLM  │◀────────────│  │ LLM call│  │  │
                        │  │ 3.1 2B       │─────────────▶│  └──────────┤  │  │
                        │  └──────────────┘  llm_analysis│             │  │  │
                        │                               └──────────────┘  │  │
                        │  ┌──────────────┐                               │  │
                        │  │ Jupyter      │  consumes from Kafka          │  │
                        │  │ Workbench    │◀──────────────────────────────┼──┤
                        │  └──────────────┘                               │  │
                        └──────────────────────────────────────────────────┘  │
                                                                             │
                        ┌──────────────────────────────────────────────────┐  │
                        │               KAFKA-CDC NAMESPACE                │  │
                        │                                                  │  │
                        │  ┌──────────────────────────────────────┐        │  │
                        │  │  cdc-cluster (Strimzi Kafka)         │        │  │
                        │  │                                      │◀───────┼──┘
                        │  │  Topics:                             │        │
                        │  │   • cv.face.detections               │        │
                        │  │   • cv.ppe.detections  ◀── PPE events│        │
                        │  │   • dlq.cv-errors                    │        │
                        │  │   • dlq.ppe-errors                   │        │
                        │  └──────────────────────────────────────┘        │
                        │                     │                            │
                        │                     ▼                            │
                        │  ┌──────────────────────────────────────┐        │
                        │  │  Camel CV Processor                  │        │
                        │  │   • cv-ovms-status (OVMS → Kafka)   │        │
                        │  │   • cv-labels-poller (Labels → Kafka)│        │
                        │  │   • cv-face-notification (→ Mailpit) │        │
                        │  └──────────────────────────────────────┘        │
                        └──────────────────────────────────────────────────┘
```

## Two Detection Pipelines

### Pipeline 1: Face Detection (OpenVINO)

| Step | Component | Action |
|------|-----------|--------|
| 1 | Angular Frontend | Captures webcam frame, sends to backend |
| 2 | FastAPI Backend | Forwards to OVMS REST API |
| 3 | OVMS | Runs `face-detection-retail-0005` model, returns bounding boxes |
| 4 | FastAPI Backend | Applies LBPH recognizer for face identification |
| 5 | Camel Pollers | Poll OVMS status + labels registry every 30s |
| 6 | Kafka | Events published to `cv.face.detections` |
| 7 | Camel Notifier | Consumes events, sends emails via Mailpit |

### Pipeline 2: PPE Safety Detection (YOLO)

| Step | Component | Action |
|------|-----------|--------|
| 1 | Angular Frontend `/ppe` | Captures webcam frame, sends base64 to backend |
| 2 | FastAPI Backend `/ppe/detect` | Forwards frame to YOLO PPE Serving |
| 3 | YOLO PPE Serving | Runs YOLOv8n inference, returns detections |
| 4 | FastAPI Backend | Classifies compliance (hardhat, safety-vest, goggles) |
| 5 | FastAPI Backend | Calls Granite LLM for natural language safety analysis |
| 6 | FastAPI Backend (Kafka Producer) | Publishes `ppe_compliance_check` event to `cv.ppe.detections` |
| 7 | Jupyter Workbench | Consumes events for dashboards, compliance trends |

## Technology Stack

| Layer | Technology |
|-------|-----------|
| **Face Detection** | OpenVINO Model Server (`face-detection-retail-0005` FP16) |
| **Face Recognition** | LBPH via OpenCV (NeuroFace backend) |
| **PPE Detection** | Ultralytics YOLOv8n (served via Flask) |
| **Safety Analysis** | Granite 3.1 2B Instruct (llama.cpp via KServe) |
| **Backend** | FastAPI (Python 3.12) with `confluent-kafka` |
| **Frontend** | Angular with WebRTC camera capture |
| **Event Streaming** | Apache Kafka (Strimzi `cdc-cluster`, SASL_SSL) |
| **Integration** | Apache Camel (timer pollers + Kafka consumer) |
| **Notifications** | Mailpit HTTP API |
| **ML Workbench** | Jupyter Data Science Notebook (OpenShift AI) |
| **GitOps** | ArgoCD (App of Apps pattern) |
| **Catalog** | Red Hat Developer Hub (Backstage) |

## Documentation

- [Architecture](architecture.md) — Deployment model, ArgoCD apps, security
- [OVMS Inference](ovms.md) — OpenVINO Model Server setup and REST/gRPC API
- [Kafka Events](kafka-events.md) — Event types, topic config, PPE events, DLQ
