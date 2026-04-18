# ${{ values.name }}

Quarkus-based Kafka consumer connected to the external CDC cluster.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `${{ values.kafkaBootstrap }}` | Kafka bootstrap address |
| `KAFKA_TOPIC` | `${{ values.kafkaTopic }}` | Source topic to consume |
| `KAFKA_GROUP_ID` | `${{ values.consumerGroup }}` | Consumer group ID |

## Architecture

```
┌─────────────┐    ┌──────────────┐    ┌──────────────────┐
│  PostgreSQL  │───▶│  Debezium    │───▶│  cdc-cluster     │
│  (source DB) │    │  Connector   │    │  (Kafka broker)  │
└─────────────┘    └──────────────┘    └────────┬─────────┘
                                                │
                                    ┌───────────▼──────────┐
                                    │  This Consumer       │
                                    │  (${{ values.name }}) │
                                    └──────────────────────┘
```

## Endpoints

- `GET /api/status` — Consumer metrics (processed/error counts)
- `GET /q/health` — Health checks
- `GET /q/metrics` — Prometheus metrics

## Development

Open in DevSpaces for live coding with `quarkus:dev` mode.
