# ${{ values.name }}

SonataFlow serverless workflow for business process automation — customer onboarding triggered by CDC events with classification logic.

## Open in DevSpaces

1. From **Red Hat Developer Hub**, click **Open in DevSpaces** in the component page.
   Alternatively, open this URL directly:

   ```
   https://devspaces.<cluster-domain>/#https://gitea-gitea.<cluster-domain>/ws-<user>/${{ values.name }}
   ```

2. Wait for the workspace to start (~2 min on first launch). The `devfile.yaml` automatically:
   - Provisions a container with Maven, JDK, and workflow tooling
   - Downloads and installs **SonataFlow Editor** and **Kaoto** VS Code extensions

3. If the extensions don't appear immediately, open the **Extensions** panel (`Ctrl+Shift+X`) and search for `SonataFlow` — it should be listed under *Installed*.

## Edit the workflow visually

1. Open `workflows/customer-onboarding.sw.yaml`
2. The **SonataFlow Editor** automatically renders the workflow diagram in a visual canvas
3. Click any state to edit its properties (transitions, actions, event filters, expressions)
4. The editor supports:
   - **Event states** — trigger workflows from Kafka/CloudEvents
   - **Operation states** — execute functions (expressions, REST calls, custom)
   - **Switch states** — conditional branching based on data
   - **Parallel states** — concurrent execution branches
   - **Callback states** — async wait for external events (e.g., approvals)
5. Changes are synced back to the YAML file

## Workflow overview

| State | Type | Description |
|-------|------|-------------|
| `ReceiveCDCEvent` | Event | Listens for CDC events from `cdc.public.customers` via CloudEvents |
| `ClassifyCustomer` | Operation | Classifies customer tier: basic (<10k), standard (10k–50k), premium (>50k) |
| `LogResult` | Operation | Logs the classification result |

### Events

| Event | Type | Source |
|-------|------|--------|
| `cdcCustomerEvent` | `cdc.public.customers` | `debezium/cdc` |

### Functions

| Function | Type | Description |
|----------|------|-------------|
| `classifyCustomer` | expression | JQ expression that assigns tier based on amount |
| `logEvent` | custom (sysout) | Console logging |

## Build & run

From the DevSpaces terminal (or the devfile command palette):

```bash
# Build
mvn package -DskipTests

# Run in dev mode (hot reload)
mvn quarkus:dev
```

The workflow engine starts on `http://localhost:8080`. Dev mode provides:
- Auto-reload on YAML changes
- Dev UI at `http://localhost:8080/q/dev-ui`
- Swagger UI at `http://localhost:8080/q/swagger-ui`

## Project structure

```
.
├── devfile.yaml          # DevSpaces workspace definition
├── catalog-info.yaml     # Backstage catalog entity
├── workflows/
│   └── customer-onboarding.sw.yaml   # SonataFlow workflow (visual editor)
└── README.md
```

## Extending the workflow

To add new states (e.g., approval, notification, provisioning):

1. Open the workflow in the visual editor
2. Click **+** on a transition arrow to insert a new state
3. Choose the state type:
   - **Callback** for async approvals (produces a Kafka event, waits for response)
   - **Parallel** for concurrent tasks (e.g., create account + send email)
   - **Operation** for REST calls or expressions
4. Wire events and functions in the state properties panel
5. Save — the YAML updates automatically

## Useful links

- [Serverless Workflow Specification](https://serverlessworkflow.io/)
- [SonataFlow documentation](https://sonataflow.org/serverlessworkflow/latest/)
- [SonataFlow on OpenShift](https://docs.redhat.com/en/documentation/red_hat_build_of_apache_serverless_logic/)
- [Quarkus Dev Services](https://quarkus.io/guides/dev-services)
