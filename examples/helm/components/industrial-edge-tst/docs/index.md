# Industrial Edge Platform

IoT manufacturing platform based on [Red Hat Validated Patterns — Industrial Edge](https://validatedpatterns.io/patterns/industrial-edge/). Implements a complete pipeline from factory sensors to event streaming to ML anomaly detection to data lake.

## Quick access

| Resource | URL |
|----------|-----|
| **OpenShift AI — ML Development** | [RHODS Dashboard](https://rhods-dashboard-redhat-ods-applications.<CLUSTER_DOMAIN>/projects/ml-development?section=overview) |
| **Line Dashboard (Dev)** | [Dev Dashboard](https://line-dashboard-industrial-edge-tst-all.<CLUSTER_DOMAIN>) |
| **Line Dashboard (Factory)** | [Factory Dashboard](https://line-dashboard-industrial-edge-stormshift-line-dashboard.<CLUSTER_DOMAIN>) |
| **MinIO Console** | [MinIO](https://minio-console-industrial-edge-ml-workspace.<CLUSTER_DOMAIN>) |

## Namespaces

| Namespace | Purpose |
|-----------|---------|
| `industrial-edge-tst-all` | Dev/test environment: sensors, MQTT broker, Kafka dev-cluster, dashboard, Camel K |
| `industrial-edge-stormshift-messaging` | Factory edge: MQTT broker, Kafka factory-cluster, consumer |
| `industrial-edge-stormshift-machine-sensor` | Simulated factory sensors |
| `industrial-edge-stormshift-line-dashboard` | Production line dashboard (factory) |
| `industrial-edge-data-lake` | Kafka prod-cluster + Camel K -> S3 data lake |
| `industrial-edge-ml-workspace` | MinIO S3 for training data and models |
| `ml-development` | OpenShift AI: JupyterLab, Data Science Pipelines, ModelMesh |

## Credentials

### MinIO (S3)

| Field | Value |
|-------|-------|
| **Endpoint** | `http://minio.industrial-edge-ml-workspace.svc:9000` |
| **Console** | `https://minio-console-industrial-edge-ml-workspace.apps.<domain>` |
| **Username** | `minioadmin` |
| **Password** | `minioadmin` |
| **Buckets** | `anomaly-detection`, `user-bucket`, `pipeline-bucket` |

## Pipeline components

[![Industrial Edge data flow](images/edge-mfg-data-flow.png)](images/edge-mfg-data-flow.png)

*Main data flows: sensors -> event streaming -> data lake -> ML training -> inference at the edge.*

[![Messaging and ML components](images/edge-mfg-messaging-ml.png)](images/edge-mfg-messaging-ml.png)

*Detail of interaction between MQTT sensors, Kafka, Camel K, data lake, and ModelMesh.*

## Technology stack

| Layer | Technology |
|-------|-----------|
| **Sensors** | Node.js MQTT publisher (simulated) |
| **MQTT Broker** | Red Hat AMQ Broker (ActiveMQArtemis) |
| **Event Streaming** | Red Hat AMQ Streams (Kafka / Strimzi) |
| **Integration** | Red Hat Camel K (MQTT->Kafka, Kafka->S3) |
| **Data Lake** | MinIO S3-compatible storage |
| **ML Training** | OpenShift AI (JupyterLab, scikit-learn) |
| **ML Inference** | ModelMesh (MLServer + sklearn runtime) |
| **ML Pipelines** | Data Science Pipelines (Argo-based DSPA) |
| **Dashboard** | Node.js + WebSocket real-time visualization |
| **GitOps** | ArgoCD Applications |
| **Catalog** | Red Hat Developer Hub (Backstage) |

## HA and Sizing documentation

- [Component 1 — CDC for Applications](cdc-sizing.md): Kafka CDC with Debezium, production sizing and HA
- [Component 2 — Industrial Edge IoT](ie-sizing.md): Full IoT stack, sizing and HA
- [Component 3 — Mirror/Replicas for External Queries](mirror-plan.md): Kafka MirrorMaker2 replication plan for external queries
