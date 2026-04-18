# Kafka Events

## Topic: `cv.face.detections`

Main event topic for the CV pipeline on `cdc-cluster` in namespace `kafka-cdc`.

| Property | Value |
|----------|-------|
| **Partitions** | 3 |
| **Replicas** | 3 |
| **Retention** | 7 days (604800000 ms) |
| **Cleanup** | delete |
| **Consumer Group** | `camel-cv-consumer` |

## Event Types

### `person_registered`

Emitted every 30s by the **Labels Poller** (`cv-labels-poller`) when trained persons are found in the NeuroFace LBPH registry.

```json
{
  "event_type": "person_registered",
  "source": "neuroface-lbph",
  "person_name": "John Doe",
  "face_count": 5,
  "confidence": 1.0,
  "camera_id": "label-registry",
  "timestamp": "2026-04-18T14:30:00.000+0000",
  "detection_method": "lbph"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `person_name` | string | Name/label of the trained person |
| `face_count` | integer | Number of face samples used for training |
| `detection_method` | string | Always `lbph` |

### `ovms_model_status`

Emitted every 30s by the **OVMS Poller** (`cv-ovms-status`) with the current model server health.

```json
{
  "event_type": "ovms_model_status",
  "source": "openvino-model-server",
  "model_name": "face-detection-retail-0005",
  "model_server": "ovms",
  "ovms_response": { "face-detection-retail-0005": { "model_version_status": [...] } },
  "camera_id": "ovms-monitor",
  "timestamp": "2026-04-18T14:30:00.000+0000",
  "person_name": "system",
  "confidence": 1.0
}
```

| Field | Type | Description |
|-------|------|-------------|
| `model_name` | string | Name of the served model |
| `model_server` | string | Always `ovms` |
| `ovms_response` | object | Raw response from OVMS `/v1/config` |

## Dead Letter Queue: `dlq.cv-errors`

Failed messages from the notification consumer are routed here.

| Property | Value |
|----------|-------|
| **Partitions** | 3 |
| **Replicas** | 3 |
| **Retention** | 30 days (2592000000 ms) |

## Kafka Connection

| Parameter | Value |
|-----------|-------|
| **Bootstrap** | `cdc-cluster-kafka-bootstrap.kafka-cdc.svc:9093` |
| **Security** | SASL_SSL |
| **SASL Mechanism** | SCRAM-SHA-512 |
| **User** | `cdc-user` |
| **CA Cert** | `/etc/kafka-certs/ca.crt` (from `cdc-cluster-cluster-ca-cert`) |

## Email Notifications

The `cv-face-notification` Camel consumer routes events to Mailpit:

| Event Type | Recipient | Email |
|------------|-----------|-------|
| `person_registered` | Security Admin | `admin@neuralbank.io` |
| `ovms_model_status` | ML Ops | `mlops@neuralbank.io` |

Mailpit endpoint: `http://n8n-mailpit.openshift-lightspeed.svc:8025/api/v1/send`
