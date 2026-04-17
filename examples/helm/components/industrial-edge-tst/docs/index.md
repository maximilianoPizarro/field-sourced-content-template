# Industrial Edge Platform

Plataforma IoT de manufactura basada en [Red Hat Validated Patterns — Industrial Edge](https://validatedpatterns.io/patterns/industrial-edge/). Implementa un pipeline completo de sensores de fábrica → event streaming → detección de anomalías ML → data lake.

## Acceso rápido

| Recurso | URL |
|---------|-----|
| **OpenShift AI — ML Development** | [RHODS Dashboard](https://rhods-dashboard-redhat-ods-applications.apps.cluster-l9nhj.dynamic.redhatworkshops.io/projects/ml-development?section=overview) |
| **Line Dashboard (Dev)** | [Dev Dashboard](https://line-dashboard-industrial-edge-tst-all.apps.cluster-l9nhj.dynamic.redhatworkshops.io) |
| **Line Dashboard (Factory)** | [Factory Dashboard](https://line-dashboard-industrial-edge-stormshift-line-dashboard.apps.cluster-l9nhj.dynamic.redhatworkshops.io) |
| **MinIO Console** | [MinIO](https://minio-console-industrial-edge-ml-workspace.apps.cluster-l9nhj.dynamic.redhatworkshops.io) |

## Namespaces

| Namespace | Función |
|-----------|---------|
| `industrial-edge-tst-all` | Entorno dev/test: sensores, broker MQTT, Kafka dev-cluster, dashboard, Camel K |
| `industrial-edge-stormshift-messaging` | Edge factory: broker MQTT, Kafka factory-cluster, consumer |
| `industrial-edge-stormshift-machine-sensor` | Sensores de fábrica simulados |
| `industrial-edge-stormshift-line-dashboard` | Dashboard de línea de producción (factory) |
| `industrial-edge-data-lake` | Kafka prod-cluster + Camel K → S3 data lake |
| `industrial-edge-ml-workspace` | MinIO S3 para datos de entrenamiento y modelos |
| `ml-development` | OpenShift AI: JupyterLab, Data Science Pipelines, ModelMesh |

## Credenciales

### MinIO (S3)

| Campo | Valor |
|-------|-------|
| **Endpoint** | `http://minio.industrial-edge-ml-workspace.svc:9000` |
| **Console** | `https://minio-console-industrial-edge-ml-workspace.apps.<domain>` |
| **Usuario** | `minioadmin` |
| **Contraseña** | `minioadmin` |
| **Buckets** | `anomaly-detection`, `user-bucket`, `pipeline-bucket` |

## Componentes del pipeline

```
Machine Sensors (MQTT) → AMQ Broker → Camel K (MQTT→Kafka) → Kafka Cluster
                                                                     │
                                              ┌──────────────────────┤
                                              ▼                      ▼
                                     Line Dashboard           Camel K (Kafka→S3)
                                     (real-time viz)                  │
                                              │                      ▼
                                              ▼               MinIO Data Lake
                                     ML Inference                    │
                                     (ModelMesh)                     ▼
                                              ▲          OpenShift AI Pipelines
                                              │          (train + deploy model)
                                              └──────────────────────┘
```

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| **Sensores** | Node.js MQTT publisher (simulados) |
| **Broker MQTT** | Red Hat AMQ Broker (ActiveMQArtemis) |
| **Event Streaming** | Red Hat AMQ Streams (Kafka / Strimzi) |
| **Integración** | Red Hat Camel K (MQTT→Kafka, Kafka→S3) |
| **Data Lake** | MinIO S3-compatible storage |
| **ML Training** | OpenShift AI (JupyterLab, scikit-learn) |
| **ML Inference** | ModelMesh (MLServer + sklearn runtime) |
| **ML Pipelines** | Data Science Pipelines (Argo-based DSPA) |
| **Dashboard** | Node.js + WebSocket real-time visualization |
| **GitOps** | ArgoCD Applications |
| **Catálogo** | Red Hat Developer Hub (Backstage) |

## Documentación de HA y Sizing

- [Componente 1 — CDC para Aplicaciones](cdc-sizing.md): Kafka CDC con Debezium, sizing y HA en producción
- [Componente 2 — Industrial Edge IoT](ie-sizing.md): Stack IoT completo, sizing y HA
- [Componente 3 — Mirror/Réplicas para Consultas Externas](mirror-plan.md): Plan de réplicas Kafka MirrorMaker2 para consultas externas
