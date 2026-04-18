# NeuroFace CV Pipeline

Computer Vision pipeline built on [NeuroFace](https://github.com/maximilianoPizarro/neuroface) — facial recognition with OpenVINO Model Server, Kafka event streaming, Camel-based notifications, and an interactive Jupyter workbench.

## Quick access

| Resource | URL |
|----------|-----|
| **NeuroFace Web App** | [Frontend](https://neuroface-neuroface.<CLUSTER_DOMAIN>) |
| **OVMS Config** | [/v1/config](https://neuroface-ovms-neuroface.<CLUSTER_DOMAIN>/v1/config) |
| **Jupyter Workbench** | [OpenShift AI](https://rhods-dashboard-redhat-ods-applications.<CLUSTER_DOMAIN>/projects/neuroface) |
| **Mailpit Inbox** | [CV Notifications](https://n8n-mailpit-openshift-lightspeed.<CLUSTER_DOMAIN>) |
| **Kafka Console** | [Kafka UI](https://console-openshift-console.<CLUSTER_DOMAIN>/k8s/ns/kafka-cdc/kafka.strimzi.io~v1beta2~KafkaTopic/cv.face.detections) |

## Namespaces

| Namespace | Purpose |
|-----------|---------|
| `neuroface` | NeuroFace app (backend + frontend), OVMS, Jupyter workbench |
| `kafka-cdc` | Kafka topics, Camel CV processor, DLQ |

## Credentials

### NeuroFace App

| Field | Value |
|-------|-------|
| **Backend API** | `http://neuroface-backend.neuroface.svc:8080` |
| **OVMS REST** | `http://neuroface-ovms.neuroface.svc:8000` |
| **OVMS gRPC** | `neuroface-ovms.neuroface.svc:9000` |
| **Detection Model** | `face-detection-retail-0005` (FP16) |
| **Recognition Model** | LBPH (OpenCV local binary patterns) |

## Pipeline components

```
┌─────────────┐     ┌──────────────┐     ┌───────────────────┐     ┌─────────────┐
│  NeuroFace  │     │    OVMS      │     │   Camel Pollers   │     │   Kafka     │
│  Frontend   │────▶│  face-det-   │     │                   │     │  cv.face.   │
│  (Angular)  │     │  retail-0005 │◀────│  cv-ovms-status   │────▶│  detections │
└─────────────┘     └──────────────┘     │  (30s timer)      │     └──────┬──────┘
      │                                  │                   │            │
      ▼                                  │  cv-labels-poller │            ▼
┌─────────────┐                          │  (30s timer)      │     ┌─────────────┐
│  NeuroFace  │◀─────────────────────────│                   │     │   Camel     │
│  Backend    │                          └───────────────────┘     │  Notifier   │
│  (FastAPI)  │                                                   │             │
│  /api/labels│                                                   └──────┬──────┘
└─────────────┘                                                          │
                                                                         ▼
                                                                  ┌─────────────┐
                                                                  │   Mailpit   │
                                                                  │  (email)    │
                                                                  └─────────────┘
```

## Technology stack

| Layer | Technology |
|-------|-----------|
| **Face Detection** | OpenVINO Model Server (face-detection-retail-0005 FP16) |
| **Face Recognition** | LBPH via OpenCV (NeuroFace backend) |
| **Backend** | FastAPI (Python) |
| **Frontend** | Angular |
| **Event Streaming** | Apache Kafka (Strimzi `cdc-cluster`) |
| **Integration** | Apache Camel (timer pollers + Kafka consumer) |
| **Notifications** | Mailpit HTTP API |
| **ML Workbench** | Jupyter Data Science Notebook (OpenShift AI) |
| **GitOps** | ArgoCD Applications |
| **Catalog** | Red Hat Developer Hub (Backstage) |

## Documentation

- [Architecture](architecture.md): System design, data flows, and event schemas
- [OVMS Inference](ovms.md): OpenVINO Model Server setup and inference API
- [Kafka Events](kafka-events.md): Event types, topic configuration, and DLQ
