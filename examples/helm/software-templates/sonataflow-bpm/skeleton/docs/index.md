# ${{ values.name }}

${{ values.description }}

## Overview

This component implements a **SonataFlow BPM workflow** for customer onboarding. It orchestrates the full lifecycle from CDC event reception through validation, classification, approval, and account provisioning.

## Architecture

```
CDC Event ─▶ Validate ─▶ Classify ─▶ Approval? ─▶ Provision ─▶ Metrics
```

## Key Technologies

| Technology | Purpose |
|---|---|
| SonataFlow | Serverless Workflow engine |
| Kogito | Business automation |
| Kafka | Event streaming |
| Quarkus | Runtime platform |
| Data Index | Workflow indexing & GraphQL |

## Quick Start

1. Open in **DevSpaces** using the link in the component overview
2. Use **SonataFlow Editor** to visually modify workflows in `workflows/`
3. Run locally: `mvn quarkus:dev`
4. Build: `mvn package -DskipTests`

## Workflow States

| State | Type | Description |
|---|---|---|
| ReceiveCDCEvent | Event | Listens for CDC Kafka events |
| ValidateCustomerData | Switch | Validates required fields |
| ClassifyCustomer | Operation | Assigns tier (basic/standard/premium) |
| CheckApprovalRequired | Switch | Routes premium to approval |
| RequestManagerApproval | Operation | Prepares approval request |
| ProvisionAccount | Operation | Creates account & welcome email |
| EmitOnboardingMetric | Operation | Publishes onboarding metrics |

## Project Structure

```
├── workflows/
│   └── customer-onboarding.sw.yaml   # Workflow definition
├── src/main/resources/
│   └── application.properties         # Quarkus config
├── pom.xml                            # Maven build
├── devfile.yaml                       # DevSpaces config
└── catalog-info.yaml                  # Backstage entity
```
