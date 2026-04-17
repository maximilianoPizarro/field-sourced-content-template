# Componente 2 — Industrial Edge IoT (Sizing HA)

## Descripción

El stack Industrial Edge IoT comprende el pipeline completo de manufactura: sensores de máquina simulados, broker MQTT, event streaming Kafka, integración Camel K, dashboards en tiempo real y detección de anomalías con ML. Este componente opera en múltiples namespaces replicando la arquitectura edge→datacenter.

## Arquitectura por capas

```
┌─── Capa 1: Edge (Factory) ───┐  ┌─── Capa 2: Datacenter ───────────┐
│                                │  │                                    │
│  Sensors → AMQ Broker → Kafka  │  │  Kafka (data-lake) → Camel K → S3 │
│        → Camel K (bridge)      │  │                                    │
│        → Dashboard             │  │  OpenShift AI → ModelMesh          │
│        → IoT Consumer          │  │  (anomaly detection)               │
│                                │  │                                    │
└────────────────────────────────┘  └────────────────────────────────────┘
```

## Sizing HA — Producción mínima (3 nodos)

### AMQ Broker (MQTT)

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **Réplicas** | 1 | 2 (active/passive con shared storage) |
| **CPU request/limit** | 250m / 500m | 500m / 1000m |
| **Memory request/limit** | 256Mi / 512Mi | 1Gi / 2Gi |
| **Storage** | 2Gi | 10Gi (persistent, RWO) |
| **acceptors** | all:61616 | MQTT:1883, AMQP:5672, CORE:61616 |
| **Clustering** | No | HA pair con `ha-policy: shared-store` |
| **Max connections** | 100 | 10000 |
| **Journal type** | NIO | AIO (si el SO lo soporta) |

### Kafka Clusters (IoT)

Se requieren hasta **3 clusters Kafka** separados (dev, factory, data-lake):

| Parámetro | Dev/Demo (c/u) | HA Producción (c/u) |
|-----------|----------------|---------------------|
| **Réplicas broker** | 1 | 3 |
| **CPU request/limit** | 250m / 500m | 1000m / 2000m |
| **Memory request/limit** | 512Mi / 1Gi | 2Gi / 4Gi |
| **Storage (por broker)** | 5Gi | 50Gi (SSD) |
| **JVM Heap** | 256m | 1536m |
| **ZooKeeper réplicas** | 1 | 3 |
| **ZK CPU** | 200m / 400m | 500m / 1000m |
| **ZK Memory** | 256Mi / 512Mi | 1Gi / 2Gi |
| **ZK Storage** | 5Gi | 20Gi |
| **`min.insync.replicas`** | 1 | 2 |
| **`default.replication.factor`** | 1 | 3 |
| **Topics** | 2 (vibration, temperature) | 2+ por línea de producción |

### Camel K Integrations

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **MQTT→Kafka réplicas** | 1 | 2 |
| **Kafka→S3 réplicas** | 1 | 2 |
| **CPU request/limit** | 250m / 500m | 500m / 1000m |
| **Memory request/limit** | 256Mi / 512Mi | 512Mi / 1Gi |
| **IntegrationPlatform maxRunningBuilds** | 1 | 3 |
| **Base image** | ubi9/openjdk-17-runtime | ubi9/openjdk-17-runtime |

### Machine Sensors

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **Réplicas por sensor** | 1 | 1 (no requiere HA, son generadores) |
| **Cantidad de sensores** | 2 | N (según líneas de producción) |
| **CPU** | 50m / 100m | 100m / 200m |
| **Memory** | 64Mi / 128Mi | 128Mi / 256Mi |
| **Publish interval** | 1000ms | Configurable (100ms–5000ms) |

### Line Dashboard

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **Réplicas** | 1 | 2 |
| **CPU** | 100m / 200m | 200m / 500m |
| **Memory** | 128Mi / 256Mi | 256Mi / 512Mi |
| **WebSocket connections** | 10 | 500+ (con HPA) |

### OpenShift AI (ML)

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **JupyterLab** | 1 notebook (Small) | Medium/Large (4+ CPU, 8+ GB RAM) |
| **ModelMesh réplicas** | 1 | 2–3 |
| **ModelMesh CPU** | 250m / 500m | 500m / 2000m |
| **ModelMesh Memory** | 512Mi / 1Gi | 1Gi / 4Gi |
| **DSPA réplicas** | 1 | 2 |
| **MinIO storage** | 10Gi | 100Gi+ (según volumen de datos) |
| **GPU** | — | 1x NVIDIA T4/A10G (opcional para deep learning) |

### MinIO S3

| Parámetro | Dev/Demo | HA Producción |
|-----------|----------|---------------|
| **Modo** | Standalone (1 pod) | Distributed (4+ pods, erasure coding) |
| **Réplicas** | 1 | 4 (mínimo para erasure coding) |
| **CPU** | 250m / 500m | 1000m / 2000m |
| **Memory** | 256Mi / 512Mi | 2Gi / 4Gi |
| **Storage por nodo** | 10Gi | 100Gi–500Gi (SSD) |
| **Erasure coding** | — | EC:2 (tolera 2 discos/pods caídos) |

## Resumen total de recursos HA

### Por entorno (factory o dev)

| Componente | Pods | CPU (req/lim) | Memory (req/lim) | Storage |
|-----------|------|---------------|------------------|---------|
| AMQ Broker | 2 | 1000m / 2000m | 2Gi / 4Gi | 20Gi |
| Kafka (3 brokers) | 3 | 3000m / 6000m | 6Gi / 12Gi | 150Gi |
| ZooKeeper | 3 | 1500m / 3000m | 3Gi / 6Gi | 60Gi |
| Camel K (2 integrations) | 4 | 2000m / 4000m | 2Gi / 4Gi | — |
| Sensors | 2 | 200m / 400m | 256Mi / 512Mi | — |
| Dashboard | 2 | 400m / 1000m | 512Mi / 1Gi | — |
| **Subtotal edge env** | **16** | **8100m / 16400m** | **14Gi / 28Gi** | **230Gi** |

### Componentes centrales (compartidos)

| Componente | Pods | CPU (req/lim) | Memory (req/lim) | Storage |
|-----------|------|---------------|------------------|---------|
| Kafka data-lake | 3+3 ZK | 4500m / 9000m | 9Gi / 18Gi | 210Gi |
| Camel K → S3 | 2 | 1000m / 2000m | 1Gi / 2Gi | — |
| MinIO (distributed) | 4 | 4000m / 8000m | 8Gi / 16Gi | 400Gi |
| ModelMesh | 3 | 1500m / 6000m | 3Gi / 12Gi | — |
| DSPA | 2 | 500m / 1000m | 1Gi / 2Gi | — |
| JupyterLab | 1 | 2000m / 4000m | 4Gi / 8Gi | 20Gi |
| **Subtotal central** | **18** | **13500m / 30000m** | **26Gi / 58Gi** | **630Gi** |

### Total HA Industrial Edge (2 entornos edge + central)

| | Pods | vCPU (req) | Memory (req) | Storage |
|--|------|-----------|-------------|---------|
| **Factory edge** | 16 | 8.1 | 14Gi | 230Gi |
| **Dev/test edge** | 16 | 8.1 | 14Gi | 230Gi |
| **Central** | 18 | 13.5 | 26Gi | 630Gi |
| **TOTAL** | **50** | **29.7 vCPU** | **54Gi** | **1090Gi** |

## Nodos OpenShift recomendados

| Perfil | Workers | Tipo instancia (AWS) | vCPU | RAM | Notas |
|--------|---------|---------------------|------|-----|-------|
| **Demo/PoC** | 3 | m5.xlarge | 4 | 16Gi | Todo en modo single replica |
| **HA mínima** | 5 | m5.2xlarge | 8 | 32Gi | Anti-affinity para Kafka/ZK |
| **HA producción** | 7+ | m5.4xlarge | 16 | 64Gi | Separar edge/central con taints |
| **Con GPU (ML)** | +1 | g4dn.xlarge | 4 | 16Gi + T4 GPU | Nodo dedicado para training |

## Configuración HA clave

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: factory-cluster
spec:
  kafka:
    replicas: 3
    config:
      default.replication.factor: 3
      min.insync.replicas: 2
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
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
    template:
      pod:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchLabels:
                    strimzi.io/name: factory-cluster-kafka
                topologyKey: kubernetes.io/hostname
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
    template:
      pod:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchLabels:
                    strimzi.io/name: factory-cluster-zookeeper
                topologyKey: kubernetes.io/hostname
```

## Escalamiento horizontal

| Componente | Método de escalamiento |
|-----------|----------------------|
| **Kafka** | Agregar brokers + redistribuir particiones |
| **Sensors** | Agregar deployments (1 por línea de producción) |
| **Camel K** | Aumentar réplicas en la Integration CR |
| **Dashboard** | HPA basado en WebSocket connections |
| **ModelMesh** | Aumentar réplicas en InferenceService |
| **MinIO** | Agregar nodos al pool de erasure coding |
