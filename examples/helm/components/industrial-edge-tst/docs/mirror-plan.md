# Componente 3 — Mirror/Réplicas para Consultas Externas (Plan)

## Objetivo

Crear réplicas de lectura de los clusters de Kafka de **CDC** e **Industrial Edge** mediante **KafkaMirrorMaker2** para permitir consultas externas sin impactar los clusters de producción. Esto habilita:

- Consumidores de analytics/BI que leen datos sin afectar la latencia de producción
- Réplica geográfica para equipos en otras regiones
- Cluster de disaster recovery (DR) con datos actualizados
- Acceso externo seguro (TLS + SASL) sin exponer los clusters internos

## Arquitectura propuesta

```
┌─── Clusters Fuente (internos) ────────────────────────────────────────────────────┐
│                                                                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                    │
│  │ cdc-cluster      │  │ factory-cluster  │  │ dev-cluster      │                    │
│  │ (kafka-cdc)      │  │ (stormshift)     │  │ (tst-all)        │                    │
│  │                  │  │                  │  │                  │                    │
│  │ Topics:          │  │ Topics:          │  │ Topics:          │                    │
│  │ dbserver.*       │  │ iot-sensor-sw-*  │  │ iot-sensor-sw-*  │                    │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘                    │
│           │                     │                      │                              │
└───────────│─────────────────────│──────────────────────│──────────────────────────────┘
            │                     │                      │
            ▼                     ▼                      ▼
   ┌────────────────────────────────────────────────────────────────────────┐
   │                     KafkaMirrorMaker2                                  │
   │                     (mirror-cluster)                                   │
   │                                                                        │
   │  sourceCluster: cdc-cluster      → mirrorCluster (cdc.*)              │
   │  sourceCluster: factory-cluster  → mirrorCluster (factory.*)           │
   │  sourceCluster: dev-cluster      → mirrorCluster (dev.*)              │
   │                                                                        │
   │  Pods: 3 réplicas                                                      │
   │  Sync interval: 10s                                                    │
   │  Heartbeat: enabled                                                    │
   │  Checkpoint: enabled                                                   │
   └────────────────────────────────────────────┬───────────────────────────┘
                                                │
                                                ▼
   ┌────────────────────────────────────────────────────────────────────────┐
   │                 Kafka Mirror Cluster (read-only)                       │
   │                 Namespace: kafka-mirror                                │
   │                                                                        │
   │  Brokers: 3 (HA)                                                       │
   │  Storage: 100Gi x 3 = 300Gi                                           │
   │                                                                        │
   │  Topics replicados:                                                    │
   │  ├── cdc.dbserver.public.customers                                     │
   │  ├── cdc.dbserver.public.accounts                                      │
   │  ├── factory.iot-sensor-sw-vibration                                   │
   │  ├── factory.iot-sensor-sw-temperature                                 │
   │  ├── dev.iot-sensor-sw-vibration                                       │
   │  └── dev.iot-sensor-sw-temperature                                     │
   │                                                                        │
   │  Listeners:                                                            │
   │  ├── internal (plain, port 9092) — para consumidores del cluster       │
   │  ├── tls (port 9093) — para consumidores internos con mTLS             │
   │  └── external (route, port 443) — para consumidores externos           │
   │      (TLS + SCRAM-SHA-512)                                             │
   └────────────────────────────────────────────────────────────────────────┘
                    │                              │
                    ▼                              ▼
     ┌─────────────────────────┐    ┌─────────────────────────┐
     │ Analytics / BI          │    │ External Consumers       │
     │ (Grafana, Superset,     │    │ (partner systems,        │
     │  custom dashboards)     │    │  data science teams,     │
     │                         │    │  DR cluster)             │
     └─────────────────────────┘    └─────────────────────────┘
```

## Plan de implementación

### Fase 1: Infraestructura (Semana 1)

1. **Crear namespace `kafka-mirror`**
2. **Desplegar Kafka mirror-cluster** (3 brokers, 3 ZooKeeper)
3. **Configurar listeners**: plain (interno), TLS (interno), external route (TLS + SCRAM-SHA-512)
4. **Crear KafkaUser** para acceso externo con ACLs de solo lectura
5. **Crear ArgoCD Application** `field-content-industrial-edge-mirror`

### Fase 2: Replicación CDC (Semana 2)

1. **Desplegar KafkaMirrorMaker2** con source `cdc-cluster`
2. **Configurar topicsPattern**: `dbserver\..*` (todos los topics CDC)
3. **Validar replicación**: comparar offsets source vs mirror
4. **Configurar monitoring**: lag metrics entre source y mirror

### Fase 3: Replicación Industrial Edge (Semana 2–3)

1. **Agregar sources** `factory-cluster` y `dev-cluster` al MirrorMaker2
2. **Configurar topicsPattern**: `iot-sensor-sw-.*`
3. **Validar datos**: consumir desde mirror y comparar con source
4. **Configurar retention diferente**: mirror con retención extendida (90 días) para analytics

### Fase 4: Acceso externo (Semana 3–4)

1. **Configurar TLS certificates** (Let's Encrypt o cert-manager)
2. **Crear KafkaUsers** con SCRAM-SHA-512 y ACLs restrictivos
3. **Documentar endpoints** para equipos de analytics
4. **Configurar NetworkPolicy** para restringir acceso
5. **Crear dashboard de monitoring** en Grafana

## Sizing HA — Mirror Cluster

### Kafka Mirror Cluster

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **Réplicas broker** | 1 | 3 |
| **CPU request/limit** | 250m / 500m | 1000m / 2000m |
| **Memory request/limit** | 512Mi / 1Gi | 2Gi / 4Gi |
| **Storage (por broker)** | 10Gi | 100Gi (SSD) |
| **JVM Heap** | 256m | 1536m |
| **ZooKeeper réplicas** | 1 | 3 |
| **ZK CPU** | 200m / 400m | 500m / 1000m |
| **ZK Memory** | 256Mi / 512Mi | 1Gi / 2Gi |
| **ZK Storage** | 5Gi | 20Gi |
| **`default.replication.factor`** | 1 | 3 |
| **`min.insync.replicas`** | 1 | 2 |
| **`log.retention.hours`** | 168 (7d) | 2160 (90d) |

### KafkaMirrorMaker2

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **Réplicas** | 1 | 3 |
| **CPU request/limit** | 250m / 500m | 500m / 1000m |
| **Memory request/limit** | 512Mi / 1Gi | 1Gi / 2Gi |
| **`refresh.topics.interval.seconds`** | 600 | 30 |
| **`sync.group.offsets.enabled`** | true | true |
| **`sync.group.offsets.interval.seconds`** | 60 | 10 |
| **`emit.heartbeats.enabled`** | true | true |
| **`emit.checkpoints.enabled`** | true | true |
| **`replication.factor`** | 1 | 3 |
| **`tasks.max`** (por source) | 1 | 4 |

### Resumen de recursos HA (Mirror)

| Componente | Pods | CPU (req/lim) | Memory (req/lim) | Storage |
|-----------|------|---------------|------------------|---------|
| Kafka brokers (mirror) | 3 | 3000m / 6000m | 6Gi / 12Gi | 300Gi |
| ZooKeeper | 3 | 1500m / 3000m | 3Gi / 6Gi | 60Gi |
| KafkaMirrorMaker2 | 3 | 1500m / 3000m | 3Gi / 6Gi | — |
| **TOTAL Mirror** | **9** | **6000m / 12000m** | **12Gi / 24Gi** | **360Gi** |

## Configuración YAML propuesta

### Namespace y ArgoCD

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: kafka-mirror
  labels:
    argocd.argoproj.io/managed-by: openshift-gitops
```

### Kafka Mirror Cluster

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: mirror-cluster
  namespace: kafka-mirror
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
      - name: external
        port: 9094
        type: route
        tls: true
        authentication:
          type: scram-sha-512
    authorization:
      type: simple
    config:
      default.replication.factor: 3
      min.insync.replicas: 2
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      log.retention.hours: 2160
      auto.create.topics.enable: true
    storage:
      type: jbod
      volumes:
        - id: 0
          type: persistent-claim
          size: 100Gi
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
                    strimzi.io/name: mirror-cluster-kafka
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
                    strimzi.io/name: mirror-cluster-zookeeper
                topologyKey: kubernetes.io/hostname
  entityOperator:
    topicOperator: {}
    userOperator: {}
```

### KafkaMirrorMaker2

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaMirrorMaker2
metadata:
  name: mirror-maker
  namespace: kafka-mirror
spec:
  version: 3.7.0
  replicas: 3
  connectCluster: mirror-cluster
  clusters:
    - alias: cdc-source
      bootstrapServers: cdc-cluster-kafka-bootstrap.kafka-cdc.svc:9092
    - alias: factory-source
      bootstrapServers: factory-cluster-kafka-bootstrap.industrial-edge-stormshift-messaging.svc:9092
    - alias: dev-source
      bootstrapServers: dev-cluster-kafka-bootstrap.industrial-edge-tst-all.svc:9092
    - alias: mirror-cluster
      bootstrapServers: mirror-cluster-kafka-bootstrap.kafka-mirror.svc:9092
      config:
        config.storage.replication.factor: 3
        offset.storage.replication.factor: 3
        status.storage.replication.factor: 3
  mirrors:
    - sourceCluster: cdc-source
      targetCluster: mirror-cluster
      sourceConnector:
        tasksMax: 4
        config:
          replication.factor: 3
          offset-syncs.topic.replication.factor: 3
          sync.topic.acls.enabled: false
          refresh.topics.interval.seconds: 30
      topicsPattern: "dbserver\\..*"
      groupsPattern: ".*"
      heartbeatConnector:
        config:
          heartbeats.topic.replication.factor: 3
      checkpointConnector:
        config:
          checkpoints.topic.replication.factor: 3
          sync.group.offsets.enabled: true
          sync.group.offsets.interval.seconds: 10
    - sourceCluster: factory-source
      targetCluster: mirror-cluster
      sourceConnector:
        tasksMax: 4
        config:
          replication.factor: 3
          offset-syncs.topic.replication.factor: 3
          sync.topic.acls.enabled: false
          refresh.topics.interval.seconds: 30
      topicsPattern: "iot-sensor-sw-.*"
      groupsPattern: ".*"
      heartbeatConnector:
        config:
          heartbeats.topic.replication.factor: 3
      checkpointConnector:
        config:
          checkpoints.topic.replication.factor: 3
          sync.group.offsets.enabled: true
          sync.group.offsets.interval.seconds: 10
    - sourceCluster: dev-source
      targetCluster: mirror-cluster
      sourceConnector:
        tasksMax: 2
        config:
          replication.factor: 3
          offset-syncs.topic.replication.factor: 3
          sync.topic.acls.enabled: false
      topicsPattern: "iot-sensor-sw-.*"
      groupsPattern: ".*"
  resources:
    requests:
      cpu: "500m"
      memory: 1Gi
    limits:
      cpu: "1"
      memory: 2Gi
```

### KafkaUser para acceso externo (read-only)

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaUser
metadata:
  name: analytics-reader
  namespace: kafka-mirror
  labels:
    strimzi.io/cluster: mirror-cluster
spec:
  authentication:
    type: scram-sha-512
  authorization:
    type: simple
    acls:
      - resource:
          type: topic
          name: "*"
          patternType: literal
        operations:
          - Describe
          - Read
        host: "*"
      - resource:
          type: group
          name: "analytics-*"
          patternType: prefix
        operations:
          - Read
        host: "*"
      - resource:
          type: cluster
        operations:
          - Describe
        host: "*"
```

## Monitoreo del Mirror

### Métricas clave

| Métrica | Descripción | Alerta si |
|---------|-------------|-----------|
| `kafka_connect_mirror_source_connector_replication_latency_ms` | Latencia de replicación source→mirror | > 30000ms |
| `kafka_connect_mirror_source_connector_record_count` | Registros replicados | Baja a 0 por > 5min |
| `kafka_consumergroup_lag` (en mirror) | Lag de consumidores externos | > 10000 |
| `kafka_server_BrokerTopicMetrics_MessagesInPerSec` | Throughput de mensajes en mirror | Anomalía vs baseline |

### Grafana Dashboard

Se recomienda crear un dashboard con paneles para:

1. **Replication lag** por source cluster y topic
2. **Throughput** (messages/sec) por connector
3. **Consumer group lag** de clientes externos
4. **Disk usage** del mirror cluster
5. **Network traffic** entre namespaces (source → mirror)

## Consideraciones de seguridad

- Los clusters fuente **NO** exponen listeners externos — solo MirrorMaker2 accede internamente
- El mirror cluster expone un **route TLS** con autenticación **SCRAM-SHA-512**
- **ACLs** restringen usuarios externos a operaciones de solo lectura
- **NetworkPolicy** limita tráfico entrante al mirror cluster solo desde namespaces autorizados
- Los **KafkaUser** secrets se almacenan como Kubernetes Secrets y se rotan periódicamente

## Sizing total de la plataforma completa (3 componentes)

| Componente | Pods | vCPU (req) | Memory (req) | Storage |
|-----------|------|-----------|-------------|---------|
| **CDC (Componente 1)** | 8 | 5.5 | 11Gi | 210Gi |
| **Industrial Edge (Componente 2)** | 50 | 29.7 | 54Gi | 1090Gi |
| **Mirror (Componente 3)** | 9 | 6.0 | 12Gi | 360Gi |
| **TOTAL Plataforma** | **67** | **41.2 vCPU** | **77Gi** | **1660Gi** |

### Nodos OpenShift recomendados (plataforma completa)

| Perfil | Workers | Tipo (AWS) | Total vCPU | Total RAM | Notas |
|--------|---------|-----------|-----------|-----------|-------|
| **Demo/PoC** | 3 | m5.2xlarge | 24 | 96Gi | Todo single-replica, sin mirror |
| **HA mínima** | 6 | m5.2xlarge | 48 | 192Gi | HA para todos los componentes |
| **HA producción** | 9 | m5.4xlarge | 144 | 576Gi | Headroom 50%, anti-affinity, taints |
| **HA + GPU** | 9+1 | m5.4xlarge + g4dn.xl | 148 | 592Gi + GPU | Training ML con GPU dedicada |
