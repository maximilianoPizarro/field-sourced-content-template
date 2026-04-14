# Architecture

## Customer Onboarding BPM Flow

The SonataFlow workflow implements the following business process:

1. **ReceiveCDCEvent** — Consumes CloudEvents from Kafka CDC topic
2. **ValidateCustomerData** — Checks required fields (email, name)
3. **ClassifyCustomer** — Assigns tier based on amount:
   - `basic`: < 10,000
   - `standard`: 10,000 – 49,999
   - `premium`: ≥ 50,000
4. **CheckApprovalRequired** — Premium customers require manager approval
5. **RequestManagerApproval** — Builds approval payload for premium tier
6. **ProvisionAccount** — Creates account and sends welcome email
7. **EmitOnboardingMetric** — Publishes onboarding completion metric

## Integration Points

| Service | Protocol | Purpose |
|---|---|---|
| Kafka CDC topic | CloudEvents | Event source |
| Data Index | GraphQL | Workflow indexing |
| Mailpit | HTTP | Email notifications |
| Management Console | Web UI | Workflow monitoring |

## Monitoring

- **SonataFlow Management Console**: View workflow instances, status, and variables
- **Data Index GraphQL**: Query process definitions and instances
- **Grafana**: Metrics dashboards for workflow execution
