# ${{ values.name }}

${{ values.description }}

## Overview

MCP (Model Context Protocol) server for Kafka CDC operations. Provides AI-assisted tools to interact with Kafka topics, consumer groups, and Debezium connectors.

## Key Technologies

| Technology | Purpose |
|---|---|
| Kafka | Event streaming platform |
| Debezium | Change Data Capture |
| MCP | AI model integration protocol |

## Kafka Consumer Groups

- `camel-k-enricher` — Camel route processing CDC events
- `connect-mailpit-http-sink` — HTTP Sink connector for email notifications

## Kafka Topics

- `cdc.public.customers` — CDC events from customers table
- `bpm.approval.requests` — Approval workflow events
- `bpm.onboarding.metrics` — Onboarding metrics events
