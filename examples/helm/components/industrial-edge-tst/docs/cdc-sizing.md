# Componente 1 — CDC para Aplicaciones (Sizing HA)

## Descripción

Change Data Capture (CDC) con Debezium permite capturar cambios en bases de datos relacionales (PostgreSQL, MySQL, SQL Server) y publicarlos como eventos en Kafka. Esto habilita patrones como event sourcing, materialización de vistas, sincronización entre microservicios y auditoría en tiempo real.

## Arquitectura CDC

```
┌─────────────────┐     ┌──────────────────────┐     ┌──────────────────────┐
│  PostgreSQL     │     │  Debezium            │     │  Kafka CDC Cluster   │
│  (WAL replication)│───▶│  KafkaConnect        │────▶│                      │
│                 │     │  (connector per DB)   │     │  Topics:             │
│  neuralbank-db  │     │                      │     │  dbserver.public.*   │
│  other-app-db   │     │  max.tasks: 1/conn   │     │                      │
└─────────────────┘     └──────────────────────┘     └───────────┬──────────┘
                                                                  │
                                           ┌──────────────────────┤
                                           ▼                      ▼
                                  ┌────────────────┐    ┌─────────────────────┐
                                  │ Camel K         │    │ Custom Consumer     │
                                  │ (enrich + route)│    │ (notifications,     │
                                  │                │    │  search index, etc.) │
                                  └────────────────┘    └─────────────────────┘
```

## Sizing HA — Producción mínima (3 nodos)

### Kafka Cluster (CDC)

| Parámetro | Dev/Demo | HA Producción (mín. 3 nodos) | HA Producción (alto volumen) |
|-----------|----------|------------------------------|------------------------------|
| **Réplicas broker** | 1 | 3 | 5 |
| **CPU request/limit** | 250m / 500m | 1000m / 2000m | 2000m / 4000m |
| **Memory request/limit** | 512Mi / 1Gi | 2Gi / 4Gi | 4Gi / 8Gi |
| **Storage (por broker)** | 5Gi | 50Gi (SSD/gp3) | 200Gi (SSD/gp3) |
| **StorageClass** | gp3-csi | gp3-csi (io1 para IOPS alto) | io1/io2 |
| **JVM Heap (-Xms/-Xmx)** | 256m | 1536m | 3072m |
| **Réplicas ZooKeeper** | 1 | 3 | 3 |
| **ZK CPU** | 200m / 400m | 500m / 1000m | 500m / 1000m |
| **ZK Memory** | 256Mi / 512Mi | 1Gi / 2Gi | 1Gi / 2Gi |
| **ZK Storage** | 5Gi | 20Gi | 50Gi |
| **`min.insync.replicas`** | 1 | 2 | 2 |
| **`default.replication.factor`** | 1 | 3 | 3 |
| **`num.partitions`** | 1 | 3 | 12 |
| **`log.retention.hours`** | 168 (7d) | 720 (30d) | 2160 (90d) |
| **Nodos totales (cluster)** | 1 worker | 3+ workers (anti-affinity) | 5+ workers |

### Debezium KafkaConnect

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **Réplicas** | 1 | 2–3 |
| **CPU request/limit** | 250m / 500m | 500m / 2000m |
| **Memory request/limit** | 512Mi / 1Gi | 1Gi / 2Gi |
| **`max.tasks`** | 1 | 1 por conector (Debezium requiere exactamente 1 task por source) |
| **Heartbeat interval** | — | 10000ms (detección rápida de failover) |
| **`snapshot.mode`** | initial | initial (primera vez), luego schema_only |

### Resumen de recursos HA (CDC)

| Componente | Pods | CPU Total (req/lim) | Memory Total (req/lim) | Storage |
|-----------|------|---------------------|----------------------|---------|
| Kafka brokers | 3 | 3000m / 6000m | 6Gi / 12Gi | 150Gi |
| ZooKeeper | 3 | 1500m / 3000m | 3Gi / 6Gi | 60Gi |
| KafkaConnect (Debezium) | 2 | 1000m / 4000m | 2Gi / 4Gi | — |
| **TOTAL CDC** | **8** | **5500m / 13000m** | **11Gi / 22Gi** | **210Gi** |

## Configuración HA recomendada

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: cdc-cluster
spec:
  kafka:
    version: 3.7.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
      num.partitions: 3
      log.retention.hours: 720
      log.segment.bytes: 1073741824
      auto.create.topics.enable: false
    storage:
      type: jbod
      volumes:
        - id: 0
          type: persistent-claim
          size: 50Gi
          class: gp3-csi
          deleteClaim: false
    resources:
      requests:
        cpu: "1"
        memory: 2Gi
      limits:
        cpu: "2"
        memory: 4Gi
    jvmOptions:
      -Xms: 1536m
      -Xmx: 1536m
    template:
      pod:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchLabels:
                    strimzi.io/name: cdc-cluster-kafka
                topologyKey: kubernetes.io/hostname
    metricsConfig:
      type: jmxPrometheusExporter
      valueFrom:
        configMapKeyRef:
          name: kafka-metrics
          key: kafka-metrics-config.yml
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 20Gi
      class: gp3-csi
      deleteClaim: false
    resources:
      requests:
        cpu: "500m"
        memory: 1Gi
      limits:
        cpu: "1"
        memory: 2Gi
    template:
      pod:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchLabels:
                    strimzi.io/name: cdc-cluster-zookeeper
                topologyKey: kubernetes.io/hostname
  entityOperator:
    topicOperator:
      resources:
        requests:
          cpu: 100m
          memory: 256Mi
    userOperator:
      resources:
        requests:
          cpu: 100m
          memory: 256Mi
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnect
metadata:
  name: debezium-connect
  annotations:
    strimzi.io/use-connector-resources: "true"
spec:
  version: 3.7.0
  replicas: 2
  bootstrapServers: cdc-cluster-kafka-bootstrap:9092
  resources:
    requests:
      cpu: "500m"
      memory: 1Gi
    limits:
      cpu: "2"
      memory: 2Gi
  config:
    group.id: debezium-connect
    offset.storage.topic: debezium-connect-offsets
    offset.storage.replication.factor: 3
    config.storage.topic: debezium-connect-configs
    config.storage.replication.factor: 3
    status.storage.topic: debezium-connect-status
    status.storage.replication.factor: 3
    key.converter: org.apache.kafka.connect.json.JsonConverter
    value.converter: org.apache.kafka.connect.json.JsonConverter
  build:
    output:
      type: imagestream
      image: debezium-connect:latest
    plugins:
      - name: debezium-postgresql
        artifacts:
          - type: maven
            group: io.debezium
            artifact: debezium-connector-postgres
            version: 2.5.4.Final
```

## Consideraciones operativas

### Monitoreo

- **Kafka Lag**: monitorear `kafka_consumergroup_lag` para detectar retrasos en procesamiento CDC
- **Debezium Metrics**: `debezium_metrics_MilliSecondsSinceLastEvent` — si supera 60s, investigar
- **Disk usage**: alertar al 70% de capacidad de storage Kafka

### Backup y DR

- Los offsets de Debezium se almacenan en Kafka → se replican automáticamente con `replication.factor: 3`
- En caso de pérdida total, Debezium puede hacer re-snapshot de la base de datos (`snapshot.mode: initial`)
- Se recomienda **KafkaMirrorMaker2** para replicar topics CDC a un cluster de DR (ver [Componente 3 — Mirror](mirror-plan.md))

### Escalamiento

- Kafka brokers: agregar brokers y redistribuir particiones con `kafka-reassign-partitions.sh`
- Debezium: NO escalar tasks (Debezium PostgreSQL requiere exactamente 1 task por conector), pero sí se pueden agregar más conectores para más bases de datos
- Para alto throughput (>10K eventos/seg), considerar particiones dedicadas y compaction
