# Arquitectura

## Diagrama general

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              OpenShift Cluster                                        │
│                                                                                       │
│  ┌────── Factory Edge ──────────────────────────────┐                                │
│  │  industrial-edge-stormshift-*                      │                                │
│  │                                                     │                                │
│  │  ┌───────────────┐    ┌──────────────────────┐     │                                │
│  │  │ Machine        │    │ AMQ Broker           │     │                                │
│  │  │ Sensors (x2)   │───▶│ (MQTT)               │     │                                │
│  │  │ vibration +    │    │ Port 61616           │     │                                │
│  │  │ temperature    │    └──────────┬───────────┘     │                                │
│  │  └───────────────┘               │                  │                                │
│  │                                  ▼                  │                                │
│  │  ┌──────────────────────┐  ┌────────────────────┐  │                                │
│  │  │ IoT Consumer         │  │ Camel K            │  │                                │
│  │  │ (Node.js messaging)  │  │ MQTT→Kafka bridge  │  │                                │
│  │  └──────────┬───────────┘  └────────┬───────────┘  │                                │
│  │             │ (ML inference)         │              │                                │
│  │             ▼                        ▼              │                                │
│  │  ┌──────────────────────┐  ┌────────────────────┐  │                                │
│  │  │ Line Dashboard       │  │ Kafka              │  │                                │
│  │  │ (real-time WebSocket)│  │ factory-cluster    │  │                                │
│  │  └──────────────────────┘  └────────────────────┘  │                                │
│  └─────────────────────────────────────────────────────┘                                │
│                                                                                       │
│  ┌────── Dev/Test ──────────────────────────────────┐                                │
│  │  industrial-edge-tst-all                           │                                │
│  │                                                     │                                │
│  │  ┌───────────────┐  ┌──────────────┐  ┌──────────┐│                                │
│  │  │ Sensors (x2)   │─▶│ AMQ Broker   │─▶│ Camel K  ││                                │
│  │  └───────────────┘  └──────────────┘  │MQTT→Kafka││                                │
│  │                                        └────┬─────┘│                                │
│  │  ┌──────────────┐  ┌──────────────────────┐ │      │                                │
│  │  │ Dashboard     │  │ Kafka dev-cluster    │◀┘      │                                │
│  │  └──────────────┘  └──────────────────────┘        │                                │
│  └─────────────────────────────────────────────────────┘                                │
│                                                                                       │
│  ┌────── Data Lake ─────────────────────────────────┐                                │
│  │  industrial-edge-data-lake                         │                                │
│  │                                                     │                                │
│  │  ┌──────────────────────┐  ┌────────────────────┐  │                                │
│  │  │ Kafka prod-cluster    │  │ Camel K            │  │                                │
│  │  │ (aggregates events)   │─▶│ Kafka→S3 archiver  │  │                                │
│  │  └──────────────────────┘  └────────┬───────────┘  │                                │
│  └──────────────────────────────────────│──────────────┘                                │
│                                         ▼                                              │
│  ┌────── ML Workspace ──────────────────────────────┐                                │
│  │  industrial-edge-ml-workspace + ml-development     │                                │
│  │                                                     │                                │
│  │  ┌──────────────────┐  ┌──────────────────────┐   │                                │
│  │  │ MinIO S3          │  │ OpenShift AI         │   │                                │
│  │  │ (model artifacts) │─▶│ JupyterLab           │   │                                │
│  │  │ (training data)   │  │ DS Pipelines (DSPA)  │   │                                │
│  │  │ (pipeline bucket) │  │ ModelMesh (inference) │   │                                │
│  │  └──────────────────┘  └──────────────────────┘   │                                │
│  └─────────────────────────────────────────────────────┘                                │
│                                                                                       │
│  ┌────── CDC (Change Data Capture) ─────────────────┐                                │
│  │  kafka-cdc                                         │                                │
│  │                                                     │                                │
│  │  ┌──────────────────┐  ┌──────────────────────┐   │                                │
│  │  │ Kafka cdc-cluster │  │ Debezium             │   │                                │
│  │  │                   │◀─│ KafkaConnect          │   │                                │
│  │  │                   │  │ (PostgreSQL → Kafka)  │   │                                │
│  │  └──────────────────┘  └──────────────────────┘   │                                │
│  └─────────────────────────────────────────────────────┘                                │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

## Flujo de datos IoT

1. **Machine Sensors** publican lecturas de vibración y temperatura cada segundo vía **MQTT**
2. **AMQ Broker** recibe los mensajes MQTT en topics `iot-sensor/sw/vibration` y `iot-sensor/sw/temperature`
3. **Camel K (MQTT→Kafka)** bridge consume de MQTT y produce a **Kafka** topics `iot-sensor-sw-vibration` e `iot-sensor-sw-temperature`
4. **Line Dashboard** consume directamente de MQTT vía WebSocket para visualización en tiempo real
5. **IoT Consumer** procesa los datos y opcionalmente invoca **ModelMesh** para detección de anomalías
6. **Camel K (Kafka→S3)** archiva los eventos de Kafka en **MinIO S3** como data lake
7. **OpenShift AI** utiliza los datos del data lake para entrenar modelos de anomaly detection
8. Los modelos entrenados se despliegan en **ModelMesh** para inferencia en tiempo real

## Flujo CDC (Change Data Capture)

1. Aplicaciones (e.g. Neuralbank) escriben en **PostgreSQL**
2. **Debezium** (KafkaConnect) captura cambios de las WAL de PostgreSQL
3. Los eventos CDC se publican en **Kafka cdc-cluster** en topics tipo `<server>.<schema>.<table>`
4. Consumidores (Camel K, FUSE, etc.) procesan los eventos para materialización, notificaciones, etc.

## Namespaces y ArgoCD Apps

| ArgoCD Application | Namespace(s) | Contenido |
|---------------------|-------------|-----------|
| `field-content-industrial-edge-tst` | `industrial-edge-tst-all` | Dev env completo |
| `field-content-industrial-edge-stormshift` | `industrial-edge-stormshift-*` | Factory edge |
| `field-content-industrial-edge-data-lake` | `industrial-edge-data-lake` | Kafka prod + Camel K→S3 |
| `field-content-industrial-edge-minio` | `industrial-edge-ml-workspace` | MinIO S3 storage |
| `field-content-industrial-edge-data-science-project` | `ml-development` | OpenShift AI workloads |
| `field-content-industrial-edge-data-science-cluster` | cluster-scoped | RHODS DataScienceCluster |
| `field-content-industrial-edge-pipelines` | `industrial-edge-pipelines` | Tekton pipelines |
