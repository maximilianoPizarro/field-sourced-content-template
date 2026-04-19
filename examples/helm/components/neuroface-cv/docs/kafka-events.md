# Kafka Events

All events are published to the `cdc-cluster` Strimzi Kafka cluster in namespace `kafka-cdc`.

## Kafka Connection

| Parameter | Value |
|-----------|-------|
| **Bootstrap** | `cdc-cluster-kafka-bootstrap.kafka-cdc.svc:9093` |
| **Security** | SASL_SSL |
| **SASL Mechanism** | SCRAM-SHA-512 |
| **User** | `cdc-user` |
| **Password Secret** | `cdc-user` (key: `password`) |
| **CA Cert Secret** | `cdc-cluster-cluster-ca-cert` (key: `ca.crt`) |
| **CA Cert Mount** | `/etc/kafka-certs/ca.crt` |

## Topics Overview

| Topic | Publisher | Consumer | Events |
|-------|----------|----------|--------|
| `cv.face.detections` | Camel pollers | Camel notifier | `person_registered`, `ovms_model_status` |
| `cv.ppe.detections` | NeuroFace backend | Jupyter workbench | `ppe_compliance_check` |
| `dlq.cv-errors` | Camel notifier | — | Failed face events |
| `dlq.ppe-errors` | — | — | Reserved for failed PPE events |

## Topic: `cv.face.detections`

Face detection pipeline events.

| Property | Value |
|----------|-------|
| **Partitions** | 3 |
| **Replicas** | 3 |
| **Retention** | 7 days (604800000 ms) |
| **Consumer Group** | `camel-cv-consumer` |

### Event: `person_registered`

Emitted every 30s by the **Labels Poller** when trained persons are found in the LBPH registry.

```json
{
  "event_type": "person_registered",
  "source": "neuroface-lbph",
  "person_name": "John Doe",
  "face_count": 5,
  "confidence": 1.0,
  "camera_id": "label-registry",
  "timestamp": "2026-04-19T14:30:00.000+0000",
  "detection_method": "lbph"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `person_name` | string | Name/label of the trained person |
| `face_count` | integer | Number of face samples used for training |
| `detection_method` | string | Always `lbph` |

### Event: `ovms_model_status`

Emitted every 30s by the **OVMS Poller** with the current model server health.

```json
{
  "event_type": "ovms_model_status",
  "source": "openvino-model-server",
  "model_name": "face-detection-retail-0005",
  "model_server": "ovms",
  "ovms_response": { "face-detection-retail-0005": { "model_version_status": ["..."] } },
  "camera_id": "ovms-monitor",
  "timestamp": "2026-04-19T14:30:00.000+0000",
  "person_name": "system",
  "confidence": 1.0
}
```

## Topic: `cv.ppe.detections`

PPE safety detection events published by the **NeuroFace backend** Kafka producer.

| Property | Value |
|----------|-------|
| **Partitions** | 3 |
| **Replicas** | 3 |
| **Retention** | 7 days (604800000 ms) |
| **Publisher** | NeuroFace backend (`app/api/ppe.py`) |
| **Consumer** | Jupyter workbench (`ppe-yolo-detection.ipynb`) |

### Event: `ppe_compliance_check`

Published each time a user triggers PPE detection from the frontend `/ppe` tab.

```json
{
  "event_type": "ppe_compliance_check",
  "source": "neuroface-ppe-backend",
  "model": "yolov8n",
  "timestamp": "2026-04-19T14:35:12.456Z",
  "ppe_status": "violation",
  "person_count": 1,
  "detections_count": 2,
  "detections": [
    {
      "class_id": 0,
      "class_name": "hardhat",
      "confidence": 0.87,
      "bbox": { "x1": 120.5, "y1": 45.2, "x2": 210.8, "y2": 130.1 }
    },
    {
      "class_id": 7,
      "class_name": "person",
      "confidence": 0.92,
      "bbox": { "x1": 80.0, "y1": 30.0, "x2": 350.0, "y2": 480.0 }
    }
  ],
  "present_ppe": ["hardhat"],
  "missing_ppe": ["safety-vest", "goggles"],
  "expected_ppe": ["hardhat", "safety-vest", "goggles"],
  "confidence": "0.5",
  "image_source": "webcam-live",
  "llm_analysis": "SAFETY ALERT: Worker detected without safety vest and protective goggles. Hardhat is present. Immediate corrective action required for PPE compliance."
}
```

| Field | Type | Description |
|-------|------|-------------|
| `ppe_status` | string | `compliant`, `violation`, or `no_persons` |
| `person_count` | integer | Number of persons detected in the frame |
| `detections` | array | All YOLO detections with class, confidence, bbox |
| `present_ppe` | array | PPE items detected in the frame |
| `missing_ppe` | array | Expected PPE items NOT detected |
| `expected_ppe` | array | Configurable list of required PPE classes |
| `llm_analysis` | string | Granite LLM safety analysis (only on violations) |
| `image_source` | string | `webcam-live` for real-time detections |

### PPE Status Values

| Status | Meaning | LLM Called? |
|--------|---------|-------------|
| `compliant` | All expected PPE items detected | No |
| `violation` | One or more PPE items missing | Yes |
| `no_persons` | No persons detected in frame | No |

## Dead Letter Queues

### `dlq.cv-errors`

Failed messages from the face detection notification consumer.

| Property | Value |
|----------|-------|
| **Partitions** | 3 |
| **Replicas** | 3 |
| **Retention** | 30 days |

### `dlq.ppe-errors`

Reserved for failed PPE event processing.

| Property | Value |
|----------|-------|
| **Partitions** | 3 |
| **Replicas** | 3 |
| **Retention** | 7 days |

## Email Notifications (Face Detection Only)

The `cv-face-notification` Camel consumer routes face detection events to Mailpit:

| Event Type | Recipient | Email |
|------------|-----------|-------|
| `person_registered` | Security Admin | `admin@neuralbank.io` |
| `ovms_model_status` | ML Ops | `mlops@neuralbank.io` |

Mailpit endpoint: `http://n8n-mailpit.openshift-lightspeed.svc:8025/api/v1/send`

## Consuming PPE Events from Jupyter

The `ppe-yolo-detection.ipynb` notebook connects to Kafka and consumes PPE events:

```python
from confluent_kafka import Consumer

conf = {
    'bootstrap.servers': 'cdc-cluster-kafka-bootstrap.kafka-cdc.svc:9093',
    'group.id': 'jupyter-ppe-consumer',
    'auto.offset.reset': 'earliest',
    'security.protocol': 'SASL_SSL',
    'sasl.mechanism': 'SCRAM-SHA-512',
    'sasl.username': 'cdc-user',
    'sasl.password': open('/etc/kafka-certs/user.password').read().strip(),
    'ssl.ca.location': '/etc/kafka-certs/ca.crt'
}

consumer = Consumer(conf)
consumer.subscribe(['cv.ppe.detections'])

while True:
    msg = consumer.poll(1.0)
    if msg and not msg.error():
        event = json.loads(msg.value())
        # Process event: event['ppe_status'], event['missing_ppe'], etc.
```

## Verifying Events from CLI

```bash
# List PPE topic
oc exec -n kafka-cdc cdc-cluster-kafka-0 -- bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe --topic cv.ppe.detections

# Consume latest PPE events
oc exec -n kafka-cdc cdc-cluster-kafka-0 -- bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic cv.ppe.detections \
  --from-beginning --max-messages 5
```
