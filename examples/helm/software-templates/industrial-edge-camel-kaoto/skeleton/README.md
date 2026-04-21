# ${{ values.name }} — Industrial Edge Camel Routes (Kaoto)

Visual Camel Quarkus routes that replicate the Industrial Edge Camel K integrations.
Edit routes visually with **Kaoto**, test locally with **JBang**, deploy with **OpenShift Toolkit**.

## Routes

| Route File | What it does | Camel K Equivalent |
|---|---|---|
| `routes/mqtt-to-kafka.camel.yaml` | Bridges MQTT sensor data (temperature + vibration) to Kafka topics | `MQTT2KafkaRoute.java` |
| `routes/kafka-to-s3.camel.yaml` | Aggregates Kafka data and stores batches in MinIO S3 data lake | `Kafka2S3Route.java` |
| `routes/anomaly-to-mailpit.camel.yaml` | Detects vibration anomalies and sends alert emails via Mailpit | *(new)* |

## Quick Start in DevSpaces

1. **Open in DevSpaces** — Click "Open in DevSpaces" from Developer Hub
2. **View routes in Kaoto** — Open any `*.camel.yaml` file, click the Kaoto icon
3. **Run locally** — Use the `run-dev` command (Quarkus dev mode)
4. **Test Mailpit** — Run the `test-mailpit` command to send a test email
5. **Deploy to OpenShift** — Run the `deploy-openshift` command

## Deploy with OpenShift Toolkit

From the DevSpaces terminal:

```bash
# Option 1: Use the devfile deploy command
# In VS Code: Terminal > Run Task > deploy-openshift

# Option 2: Manual deploy
./mvnw package -Popenshift -Dquarkus.kubernetes.namespace=$NAMESPACE -DskipTests
```

Or use the **OpenShift Toolkit** extension sidebar:
1. Click the OpenShift icon in the sidebar
2. Right-click your cluster → "Deploy Component"
3. Select the devfile and the `deploy-openshift` command

## Architecture

```
Sensors ──MQTT──→ [AMQ Broker] ──→ [mqtt-to-kafka route] ──→ [Kafka]
                                                                │
                        ┌───────────────────────────────────────┘
                        │
                        ├──→ [kafka-to-s3 route] ──→ [MinIO S3 Data Lake]
                        │
                        └──→ [anomaly-to-mailpit route] ──→ [Mailpit Emails]
```

## Endpoints

| Service | URL |
|---|---|
| Mailpit | `https://n8n-mailpit-openshift-lightspeed.${{ values.clusterDomain }}` |
| Line Dashboard | `https://line-dashboard-industrial-edge-stormshift-line-dashboard.${{ values.clusterDomain }}` |
| Kafka Console | `https://kafka-console-kafka-cdc.${{ values.clusterDomain }}` |
