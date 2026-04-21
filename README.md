# Field Content

Self-service platform for developing RHDP Catalog Items using GitOps patterns.

## Overview

Create demos and labs for Red Hat Demo Platform without deep AgnosticD knowledge:

1. Clone this template repository
2. Choose an example (`helm/` or `ansible/`) as your starting point
3. Customize the deployment for your use case
4. Push to your Git repository
5. Order the **Field Content CI** from RHDP with your repository URL

ArgoCD deploys your content, and the platform handles health monitoring and data flow back to AgnosticD.

## Architecture

This deployment provisions a full Neuralbank developer workshop environment on OpenShift, including:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        OpenShift Cluster                                │
│                                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────┐  ┌─────────────────┐  │
│  │  Developer   │  │   ArgoCD     │  │  Tekton  │  │   DevSpaces     │  │
│  │    Hub       │  │  (GitOps)    │  │ Pipelines│  │  (Workspaces)   │  │
│  └──────┬──────┘  └──────┬───────┘  └────┬─────┘  └────────┬────────┘  │
│         │                │               │                  │           │
│         ▼                ▼               ▼                  ▼           │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────┐  ┌─────────────────┐  │
│  │   Gitea     │  │  Keycloak    │  │  Istio   │  │   Kuadrant      │  │
│  │  (SCM)      │  │  (Auth)      │  │ Gateway  │  │ (API Mgmt)      │  │
│  └─────────────┘  └──────────────┘  └──────────┘  └─────────────────┘  │
│                                                                         │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                    Per-User Namespaces (×200)                       │  │
│  │  ┌─────────────────┐ ┌──────────────┐ ┌──────────────────────┐    │  │
│  │  │ customer-service │ │  neuralbank  │ │  neuralbank-frontend │    │  │
│  │  │    -mcp (MCP)    │ │   -backend   │ │     (SPA)            │    │  │
│  │  └────────┬─────────┘ └──────┬───────┘ └──────────┬───────────┘    │  │
│  │           │                  │                    │                │  │
│  │           ▼                  ▼                    ▼                │  │
│  │     Gateway + HTTPRoute + OIDCPolicy + RateLimitPolicy            │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │
│  │  Showroom    │  │     OLS      │  │   LiteMaaS   │                  │
│  │ (Lab Guide)  │  │ (Lightspeed) │  │  (LLM Proxy) │                  │
│  └──────────────┘  └──────────────┘  └──────────────┘                  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Components

| Component | Purpose |
|-----------|---------|
| **Developer Hub** | Self-service developer portal (Backstage) with 3 Neuralbank software templates |
| **ArgoCD** | GitOps continuous delivery, auto-syncs scaffolded apps from Gitea |
| **Tekton Pipelines** | CI/CD pipelines: git-clone → maven-build → buildah → deploy |
| **DevSpaces** | Cloud-based developer workspaces with pre-configured devfiles |
| **Gitea** | In-cluster Git server for scaffolded application repos (200 users) |
| **Keycloak** | Identity provider for backstage and neuralbank realms (200 users) |
| **Istio / Gateway API** | Service mesh with Gateway, HTTPRoute per scaffolded service |
| **Kuadrant** | API management: OIDCPolicy (auth) + RateLimitPolicy per service |
| **Streams for Apache Kafka** | Kafka cluster (KRaft mode) with topics, bridge, and Kafka exporter |
| **Streams Console** | StreamsHub web console for monitoring Kafka clusters and topics |
| **Apicurio Registry** | Schema Registry (Avro/JSON/Protobuf) for CDC event validation |
| **CDC Demo (Debezium)** | Change Data Capture pipeline: PostgreSQL → Debezium → Kafka → Mailpit |
| **Kafka Bridge** | HTTP REST proxy for producing/consuming Kafka messages via curl |
| **Showroom** | Antora-based workshop lab guide (English) |
| **OLS (Lightspeed)** | AI assistant with MCP Gateway integration |
| **LiteMaaS** | LLM proxy for model access |

### Industrial Edge / Industrial Edge Stack

Integrated from the [Red Hat Validated Patterns Industrial Edge](https://github.com/validatedpatterns/industrial-edge) pattern, adapted for single-cluster deployment without Vault, ACM, or ODF.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Industrial Edge IoT Manufacturing Demo                    │
│                                                                     │
│  ┌──────────────────────────┐  ┌──────────────────────────────┐    │
│  │  Factory (Stormshift)     │  │  Datacenter                  │    │
│  │  ┌────────────────────┐  │  │  ┌────────────────────────┐  │    │
│  │  │ Machine Sensors ×2 │──┼──┼─▶│ Kafka Data Lake        │  │    │
│  │  │ (temp + vibration) │  │  │  │ (MirrorMaker2)         │  │    │
│  │  └────────┬───────────┘  │  │  └────────────┬───────────┘  │    │
│  │           │              │  │               │              │    │
│  │  ┌────────▼───────────┐  │  │  ┌────────────▼───────────┐  │    │
│  │  │ AMQ Broker (MQTT)  │  │  │  │ Camel K (S3 store)     │  │    │
│  │  │ + Kafka + Camel K  │  │  │  └────────────┬───────────┘  │    │
│  │  └────────┬───────────┘  │  │               │              │    │
│  │           │              │  │  ┌────────────▼───────────┐  │    │
│  │  ┌────────▼───────────┐  │  │  │ MinIO (S3 storage)     │  │    │
│  │  │ Line Dashboard     │  │  │  └────────────┬───────────┘  │    │
│  │  │ (IoT visualization)│  │  │               │              │    │
│  │  └────────────────────┘  │  │  ┌────────────▼───────────┐  │    │
│  │                          │  │  │ OpenShift AI (RHODS)    │  │    │
│  │  ┌────────────────────┐  │  │  │ + ML Pipelines         │  │    │
│  │  │ Anomaly Detection  │  │  │  │ + Anomaly Detection    │  │    │
│  │  │ (ModelMesh)        │◀─┼──┼──│ + Model Serving        │  │    │
│  │  └────────────────────┘  │  │  └────────────────────────┘  │    │
│  └──────────────────────────┘  └──────────────────────────────┘    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  CI/CD Pipelines (Tekton)                                    │    │
│  │  build sensor → build frontend → build consumer → deploy     │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

| Component | Chart | Purpose |
|-----------|-------|---------|
| **Machine Sensors** | `industrial-edge-stormshift` | Simulated IoT sensors (temperature + vibration) publishing via MQTT |
| **AMQ Broker + Kafka** | `industrial-edge-stormshift` | MQTT broker + Kafka cluster for factory-side event streaming |
| **Line Dashboard** | `industrial-edge-stormshift` | Real-time IoT data visualization web app |
| **Kafka MirrorMaker2** | `industrial-edge-stormshift` | Replicates factory Kafka topics to datacenter data lake |
| **Kafka Data Lake** | `industrial-edge-data-lake` | Central Kafka cluster + Camel K S3 integration |
| **MinIO** | `industrial-edge-minio` | S3-compatible object storage (replaces ODF) for ML models and data |
| **OpenShift AI** | `industrial-edge-data-science-cluster` | DataScienceCluster CR, custom notebooks, serving runtimes |
| **ML Workspace** | `industrial-edge-data-science-project` | ML pipelines, model serving, S3 data connections |
| **Anomaly Detection** | `industrial-edge-tst` / `industrial-edge-stormshift` | ModelMesh inference service for vibration anomaly detection |
| **CI/CD Pipelines** | `industrial-edge-pipelines` | Tekton pipelines for building IoT components |

**Additional resources**: ~16 vCPU, ~34 Gi RAM, ~102 GB disk (see [Deployment Profiles](#deployment-profiles)).

### Software Templates (Neuralbank)

Each template generates a full application with CI/CD pipeline, connectivity-link manifests (Gateway, HTTPRoute, OIDCPolicy, RateLimitPolicy), DevSpaces devfile, and catalog registration.

| Template | Type | Description |
|----------|------|-------------|
| **customer-service-mcp** | Quarkus MCP Server | MCP server with `@Tool`/`@ToolArg` annotations, REST client to backend, SSE transport. Includes MCP Inspector in DevSpaces. |
| **neuralbank-backend** | Quarkus REST API | Credit management API (`/api/customers`, `/api/credits`, `/api/credits/{id}/update`) |
| **neuralbank-frontend** | Static HTML/CSS/JS | Credit visualization SPA with Neuralbank theme (Red Hat palette) |

### Scaffolding Flow (End-to-End CI/CD)

All scaffolder steps use only actions registered in this RHDH instance:

| Step | Scaffolder Action | Description |
|------|-------------------|-------------|
| 1 | `fetch:template` | Generates skeleton from template, injects user values (name, owner, namespace, clusterDomain). Creates unique name `owner-name` to avoid multi-user conflicts |
| 2 | `publish:gitea` | Pushes generated code to Gitea `ws-userN` organization (plugin: `backstage-plugin-scaffolder-backend-module-gitea`) |
| 3 | `catalog:register` | Registers Component + API + System entities in the Backstage catalog with owner-prefixed unique names |
| 4 | `http:backstage:request` | Creates ArgoCD Application via K8s API proxy (`/api/proxy/k8s-api/`) with unique name `owner-name` |
| 5 | `http:backstage:request` | Creates Gitea webhook via Gitea API proxy (`/api/proxy/gitea/`) |
| 6 | `http:backstage:request` | Sends notification to owner via `/api/notifications` (in-app + email via Mailpit) |

```
User in Developer Hub
  → Selects Software Template (neuralbank-backend / frontend / customer-service-mcp)
    → Step 1: fetch:template → generates skeleton with user values (uniqueName = owner-name)
    → Step 2: publish:gitea → pushes to Gitea ws-userN org
    → Step 3: catalog:register → registers Component + API + System in catalog (owner-prefixed)
    → Step 4: http:backstage:request → POST K8s API → creates ArgoCD Application (owner-name)
    → Step 5: http:backstage:request → POST Gitea API → creates push webhook
    → Step 6: http:backstage:request → POST /api/notifications → notifies owner (in-app + email)
    → ArgoCD auto-syncs manifests/ → Deploys to userN-neuralbank namespace:
        Deployment + Service
        Gateway (Istio/Gateway API)
        HTTPRoute
        OIDCPolicy (Keycloak backstage realm)
        RateLimitPolicy (60 req/min per user)
        Pipeline + TriggerTemplate + TriggerBinding + EventListener
        Initial PipelineRun (first build)
    → On git push → Gitea webhook → EventListener → New PipelineRun
```

### Dynamic Plugins Enabled

| Plugin | Source | Purpose |
|--------|--------|---------|
| `backstage-community-plugin-rbac` | Built-in | Role-based access control |
| `backstage-community-plugin-catalog-backend-module-keycloak-dynamic` | Built-in | Keycloak user/group sync to catalog |
| `backstage-plugin-kubernetes-backend` | OCI overlay | Kubernetes resource viewer |
| `backstage-plugin-scaffolder-backend-module-gitea` | OCI overlay | `publish:gitea` scaffolder action |
| `backstage-community-plugin-tekton` | OCI overlay | Tekton CI tab on entity pages |
| `backstage-community-plugin-topology` | Built-in | Kubernetes topology view |
| `roadiehq-scaffolder-backend-module-http-request-dynamic` | Built-in | `http:backstage:request` scaffolder action |
| `roadiehq-backstage-plugin-argo-cd-backend-dynamic` | Built-in | ArgoCD status on entity pages |
| `@kuadrant/kuadrant-backstage-plugin-backend-dynamic` | External | Kuadrant API Product provider |
| `@kuadrant/kuadrant-backstage-plugin-frontend` | External | Kuadrant UI (API Products, API Keys) |
| `backstage-plugin-notifications` | Built-in | In-app notifications system |
| `backstage-plugin-notifications-backend-module-email-dynamic` | Built-in | Email notifications processor (SMTP/Mailpit) |
| `backstage-community-plugin-kafka` | Built-in | Kafka consumer group offsets on entity pages |
| `backstage-community-plugin-kafka-backend-dynamic` | Built-in | Kafka cluster connectivity for the Kafka plugin |
| `red-hat-developer-hub-backstage-plugin-lightspeed` | OCI overlay | Red Hat Developer Lightspeed AI assistant (frontend) |
| `red-hat-developer-hub-backstage-plugin-lightspeed-backend` | OCI overlay | Red Hat Developer Lightspeed AI assistant (backend) |

### Backstage Proxy Endpoints

| Proxy Path | Target | Auth | Used By |
|------------|--------|------|---------|
| `/api/proxy/gitea/*` | `https://gitea-gitea.<domain>/api/v1` | Basic (gitea_admin) | Webhook creation in scaffolder |
| `/api/proxy/k8s-api/*` | `https://kubernetes.default.svc` | Bearer (SA token) | ArgoCD Application creation in scaffolder |

**User sees in Developer Hub:**
- Topology view (Deployments, Pods, Routes, Gateways)
- Tekton CI tab (PipelineRuns, task logs) — via `janus-idp.io/tekton` annotation
- ArgoCD CD tab (sync status, health)
- Kubernetes tab (pods, events)
- API documentation (OpenAPI)
- Kuadrant API Product info (OIDCPolicy, RateLimitPolicy, API keys)
- Component relationships (System graph: frontend → backend → MCP)
- Kafka consumer group offsets and topic lag — via `kafka.apache.org/consumer-groups` annotation
- Notifications (in-app bell + email via Mailpit)
- Lightspeed AI assistant (contextual help with RAG)

## User Scaling

User count is controlled by a single parameter in `values.yaml`:

```yaml
userCount: 200  # Default: 200. Adjust as needed (30, 50, 100, 200).
```

This parameter drives all user provisioning via Helm `range` loops:

| Resource | Template | Per-User Objects |
|----------|----------|-----------------|
| Keycloak users (`user1`…`userN`) | `rhbk` | 1 user in backstage realm |
| DevSpaces namespaces (`userN-devspaces`) | `namespaces` | Namespace + 3 RoleBindings |
| Neuralbank namespaces (`userN-neuralbank`) | `namespaces` | Namespace + 3 RoleBindings |
| Gitea users + organizations (`ws-userN`) | `gitea` | 1 user + 1 org |
| ArgoCD ApplicationSets | `applicationsets` | 1 ApplicationSet (SCM Provider) |
| Backstage RBAC assignments | `developer-hub` | 1 policy line (`role:default/authenticated`) |
| Workshop registration seats | `workshop-registration` | 1 seat (up to `maxUsers`) |

### Pre-deployed Components (Neuralbank Stack)

The `neuralbank-stack` namespace contains a pre-deployed demo application (backend + frontend + PostgreSQL) visible to all users via the Developer Hub catalog. Components are registered with `backstage.io/kubernetes-id` annotations for topology visualization.

### Access Model: Developer Hub as Single Pane of Glass

Users interact exclusively through **Developer Hub** — no OpenShift Console access required:

| Capability | Where | How |
|------------|-------|-----|
| Deploy apps | Developer Hub → Create | Software Templates |
| View topology | Developer Hub → Component → Topology tab | `backstage-community-plugin-topology` |
| View pipelines | Developer Hub → Component → CI tab | `backstage-community-plugin-tekton` + `janus-idp.io/tekton` annotation |
| View GitOps status | Developer Hub → Component → CD tab | `roadiehq-backstage-plugin-argo-cd-backend-dynamic` |
| View pods/events | Developer Hub → Component → Kubernetes tab | `backstage-plugin-kubernetes-backend` |
| Edit code | Developer Hub → Component → Open in Dev Spaces | DevSpaces with Keycloak OIDC auth |
| AI assistance | Developer Hub → Lightspeed | `red-hat-developer-hub-backstage-plugin-lightspeed` |
| API documentation | Developer Hub → API entity | OpenAPI definition |
| Notifications | Developer Hub → Bell icon | In-app + email via Mailpit |

### DevSpaces Authentication via Keycloak OIDC

DevSpaces is configured to authenticate users via the same **Keycloak OIDC** provider used by Developer Hub, eliminating the need for OpenShift user accounts:

```yaml
# CheCluster spec.networking.auth
auth:
  identityProviderURL: "https://rhbk.<cluster-domain>/realms/backstage"
  oAuthClientName: devspaces
  oAuthSecret: devspaces-oidc-secret
```

A `devspaces` OIDC client is registered in the Keycloak `backstage` realm. DevSpaces auto-provisions `<username>-devspaces` namespaces using its operator ServiceAccount.

**Result**: Users only need a Keycloak account (`user1`…`userN`) to access Developer Hub AND DevSpaces. No OpenShift User objects or manual RBAC required.

### Cluster Sizing (measured April 2026)

Based on a real deployment running on RHDP with the full profile (all components enabled, no users scaffolded yet).

#### Measured Baseline (0 users, full profile)

| Metric | Value |
|--------|-------|
| **Cluster** | 3 control-plane (16 vCPU, 64 Gi) + 3 workers (64 vCPU, 128 Gi) |
| **Total running pods** | 474 |
| **Namespaces** | 522 (includes operator + system namespaces) |
| **Worker CPU usage** | 15.2 vCPU of 192 vCPU (7.9%) |
| **Worker memory usage** | 87 Gi of 384 Gi (22.6%) |
| **Control-plane CPU** | 18.8 vCPU of 48 vCPU (39%) |
| **Control-plane memory** | 93 Gi of 192 Gi (48.5%) |

#### Pods by Component

| Component | Pods | Namespaces |
|-----------|------|------------|
| Kafka CDC (Strimzi + Debezium + Bridge + Console) | 16 | `kafka-cdc` |
| Industrial Edge (sensors, Kafka ×3, dashboards, ML) | 34 | 8 namespaces (`industrial-edge-*`) |
| NeuroFace CV (frontend, backend, OVMS, YOLO, processor) | 8 | `neuroface` |
| OpenShift AI / ML (Jupyter, S3 Browser, KServe) | 9 | `ml-development` |
| ArgoCD | 8 | `openshift-gitops` |
| Developer Hub | 2 | `developer-hub` |
| Gitea | 5 | `gitea` |
| Keycloak (RHBK) | 3 | `rhbk-operator` |
| DevSpaces Operator | 3 | `devspaces` |
| Lightspeed + MCP Gateway | 5 | `openshift-lightspeed` |
| Showroom + Registration | 2 | `showroom` |
| **Platform (OCP operators, monitoring, ingress)** | **~380** | various |

#### Per-User Resource Footprint

| Component | CPU (limit) | RAM (limit) |
|-----------|------------|------------|
| DevSpaces workspace (UDI + Maven cache) | 2 vCPU | 3 Gi |
| customer-service-mcp (Quarkus) | 500m | 512 Mi |
| neuralbank-backend (Quarkus) | 500m | 512 Mi |
| neuralbank-frontend (httpd) | 200m | 128 Mi |
| Istio sidecar gateways (×3) | 300m | 384 Mi |
| **Total per user (all 3 apps + DevSpaces)** | **3.5 vCPU** | **4.5 Gi** |
| **Total per user (all 3 apps, no DevSpaces)** | **1.5 vCPU** | **1.5 Gi** |

#### Scaling Profiles

| Users | Worker CPU needed | Worker MEM needed | Recommended Workers | Instance Type |
|-------|-------------------|-------------------|---------------------|---------------|
| **Demo (0 users)** | 15 vCPU / 87 Gi | — | **3 nodes** | **64 vCPU, 128 Gi** |
| **30** | 60 vCPU / 135 Gi | 120 vCPU / 222 Gi total | 3 nodes | 64 vCPU, 128 Gi |
| **50** | 90 vCPU / 175 Gi | 150 vCPU / 262 Gi total | 3 nodes | 64 vCPU, 128 Gi |
| **100** (30% DevSpaces) | 120 vCPU / 222 Gi | 180 vCPU / 309 Gi total | 3-4 nodes | 64 vCPU, 128 Gi |
| **200** (30% DevSpaces) | 225 vCPU / 357 Gi | 285 vCPU / 444 Gi total | **5-6 nodes** | **64 vCPU, 128 Gi** |

> **Key finding**: The original theoretical estimates (12 workers for 200 users) were significantly oversized. Real measurements show 3 workers with 64 vCPU / 128 Gi each handle the full platform at 7.9% CPU and 22.6% memory. Even at 200 users with 30% DevSpaces concurrency, 5-6 workers suffice.

#### Known Blockers from Real Deployments

| Blocker | Impact | Fix |
|---------|--------|-----|
| Jupyter Notebook image not found | RHOAI notebooks fail to start | Use `s2i-generic-data-science-notebook:2025.1` instead of `jupyter-datascience-cpu-py312` |
| DevSpaces extensions silently fail | `libnode.so.127` not found | Add `LD_LIBRARY_PATH=/checode/checode-linux-libc/ubi9/ld_libs` |
| RHOAI Gateway port mismatch | 500 errors on Jupyter routes | Create direct Routes bypassing Gateway API (targetPort 8888) |
| Quarkus build-strategy=docker | Build fails without Dockerfile | Remove property; let Quarkus default to S2I binary build |
| Kafka CSV messages vs JSON unmarshal | JsonParseException in Camel routes | Use Simple language OGNL for CSV parsing |
| KServe vs ModelMesh confusion | ServingRuntime format mismatch | Remove `modelmesh-enabled` labels; use KServe format |
| Operator OOMKilled loops | COO, Kuadrant, Kiali crash | Apply CSV memory patches (512 Mi → 2 Gi) |

Control plane: 3 masters with **16 vCPU, 64 Gi RAM** each. Standard 8 vCPU / 32 Gi is sufficient up to 100 users; for 200 users, upgrade to 16 vCPU / 64 Gi due to etcd load from 500+ namespaces.

## Deployment Profiles

Two deployment profiles are available to match different resource budgets:

### Full Profile (default)

All components enabled — AI/ML, Service Mesh, Observability, DevSpaces, BPM workflows.

```bash
helm install field-content examples/helm -f examples/helm/values.yaml
```

| Metric | Value |
|--------|-------|
| Fixed infra (platform + all components) | ~66 vCPU, ~122 Gi RAM, ~570 GB disk |
| Industrial Edge stack alone | ~16 vCPU, ~34 Gi RAM, ~102 GB disk |
| Per user (with DevSpaces) | ~3.5 vCPU, ~4.5 Gi RAM |
| Per user (no DevSpaces) | ~1.5 vCPU, ~1.5 Gi RAM |
| Recommended for 200 users | 10–14 workers m5.8xlarge |

### Lite Profile (~60% fewer resources)

Core EDA workshop only: Kafka CDC pipeline, Developer Hub, Keycloak, Gitea, Neuralbank demo, Showroom. Disables 12 optional components (OpenShift AI, DevSpaces, Service Mesh, Observability, LiteMaaS, Kuadrant, OLS, MCP Gateway, BPM, NFL Wallet).

```bash
helm install field-content examples/helm -f examples/helm/values.yaml -f examples/helm/values-lite.yaml
```

Or in ArgoCD:
```yaml
source:
  helm:
    valueFiles:
      - values.yaml
      - values-lite.yaml
```

| Metric | Value |
|--------|-------|
| Fixed infra | ~20–25 vCPU, ~30–40 Gi RAM, ~150 GB disk |
| Per user | ~1.5 vCPU, ~1.5 Gi RAM |
| Default users | 30 |
| Recommended workers | 2–3 nodes m5.4xlarge |

### Validated Patterns Compatibility

The repository also ships VP-convention files for teams adopting the [Red Hat Validated Patterns](https://validatedpatterns.io/) framework:

| File | Purpose |
|------|---------|
| `pattern.json` | Pattern metadata (name, version, profiles, estimated resources) |
| `examples/helm/values-global.yaml` | Cluster-independent settings (repo URL, ArgoCD config, userCount) |
| `examples/helm/values-hub.yaml` | Hub cluster config (operators, apps, secrets, components) |
| `examples/helm/values-lite.yaml` | Lite overlay that disables heavy components |

The existing `values.yaml` remains the primary source of truth. The VP-convention files are optional and provided for compatibility — the app-of-apps templates consume the same schema regardless of which file supplies the values.

> **Note**: This repo intentionally does not adopt the VP `clusterGroup` chart, HashiCorp Vault, or ACM multi-cluster. These layers add significant complexity and resource overhead without benefit for single-cluster workshops. See the [gap analysis plan](docs/validated-patterns-gap-analysis.md) for details.

## Getting Started

### Choose Your Pattern

| Pattern | Use When |
|---------|----------|
| [examples/helm/](examples/helm/) | Deployment can be expressed as Kubernetes manifests with Helm templating |
| [examples/ansible/](examples/ansible/) | You need wait-for-ready, secret generation, API calls, or conditional logic |

### Quick Start

```bash
# Clone this template
git clone https://github.com/maximilianoPizarro/field-sourced-content-template.git my-content
cd my-content

# Choose an example and start customizing
cd examples/helm      # or examples/ansible
# Edit values.yaml and templates as documented in each example's README
```

### Setting the Cluster Domain

The cluster domain is injected by RHDP via `deployer.domain`. For manual deployments, update it with the provided script:

```bash
# Replace with your cluster's domain
./update-cluster-domain.sh apps.cluster-xxxxx.dynamic.redhatworkshops.io
git add -A && git commit -m "update cluster domain" && git push
```

### Platform Engineer Access

Two admin users with full Platform Engineer permissions in Developer Hub:

| Username | Auth Method | Roles | Notes |
|----------|-------------|-------|-------|
| `maximilianopizarro` | Keycloak SSO (email) | platformengineer, api-admin, api-owner | Primary admin |
| `platformadmin` | Keycloak username/password | platformengineer, api-admin, api-owner | Must be created in Keycloak manually |

**Creating `platformadmin` in Keycloak:**

```bash
KEYCLOAK_URL="https://rhbk.apps.<cluster-domain>"

# Get admin token
TOKEN=$(curl -sk "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" -d "grant_type=password" \
  -d "username=admin" -d "password=<KEYCLOAK_ADMIN_PASSWORD>" | jq -r .access_token)

# Create platformadmin user with password Welcome123!
curl -sk "$KEYCLOAK_URL/admin/realms/backstage/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"platformadmin","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"Welcome123!","temporary":false}]}'
```

Platform Engineer permissions include: full catalog CRUD, scaffolder execution, RBAC administration, Lightspeed chat, Kuadrant API product management (create/update/delete/approve), and Adoption Insights.

### Manual Credentials (not stored in Git)

After deploying to a new cluster, the following secrets must be updated **manually** via `oc` commands. These credentials are intentionally excluded from Git to avoid exposing sensitive data.

#### LiteLLM Virtual Key

The LiteLLM Virtual Key authenticates clients (OLS, LiteMaaS backend) against the LiteLLM proxy. Obtain it from the LiteLLM admin UI or API, then update:

```bash
# 1. OLS → LiteLLM (OpenShift Lightspeed uses this to call the LLM)
oc create secret generic llm-credentials \
  --from-literal=apitoken='<LITELLM_VIRTUAL_KEY>' \
  -n openshift-lightspeed \
  --dry-run=client -o yaml | oc apply -f -

# 2. LiteMaaS backend → LiteLLM
oc patch secret backend-secret -n litemaas \
  --type merge -p '{"stringData":{"litellm-api-key":"<LITELLM_VIRTUAL_KEY>"}}'

# 3. Restart affected pods to pick up the new key
oc rollout restart deployment/lightspeed-app-server -n openshift-lightspeed
```

| Secret | Namespace | Key | Used by |
|--------|-----------|-----|---------|
| `llm-credentials` | `openshift-lightspeed` | `apitoken` | OLS (Lightspeed) → LiteLLM |
| `backend-secret` | `litemaas` | `litellm-api-key` | LiteMaaS backend → LiteLLM |

> **Note**: The `litellm-secret` in `litemaas` (master-key, ui-password) and `postgres-secret` (db password) ship with default values in Git. Change them in production clusters via the same `oc patch secret` approach.

#### Continue AI — LLM for DevSpaces Workspaces

All software templates include [Continue AI](https://continue.dev/) pre-configured with an external LLM via LiteLLM proxy. The API key is injected at workspace startup from a Kubernetes Secret that auto-mounts into every DevSpaces workspace.

A template file is provided at `examples/helm/components/devspaces/secret-continue-ai-template.yaml`. To configure:

```bash
# Create the secret with your LLM credentials (auto-mounts to all DevSpaces workspaces)
oc apply -f - <<'EOF'
apiVersion: v1
kind: Secret
metadata:
  name: continue-ai-config
  namespace: devspaces
  labels:
    controller.devfile.io/mount-to-devworkspace: "true"
    controller.devfile.io/watch-secret: "true"
  annotations:
    controller.devfile.io/mount-as: env
type: Opaque
stringData:
  CONTINUE_API_KEY: "<YOUR_LITELLM_API_KEY>"
  CONTINUE_API_BASE: "<YOUR_LITELLM_URL>/v1"
  CONTINUE_MODEL: "<YOUR_MODEL_NAME>"
EOF
```

The `controller.devfile.io/mount-to-devworkspace: "true"` label causes DevSpaces to auto-inject these environment variables into every new workspace. The devfile `postStart` event reads them to patch `~/.continue/config.json` with the real credentials.

| Secret | Namespace | Keys | Used by |
|--------|-----------|------|---------|
| `continue-ai-config` | `devspaces` | `CONTINUE_API_KEY`, `CONTINUE_API_BASE`, `CONTINUE_MODEL` | All DevSpaces workspaces → Continue AI extension |

> **Important**: Do NOT commit this secret with real credentials to Git. The template file in the repo contains only placeholder values.

### Service Access URLs

All services use the cluster domain pattern `apps.<cluster-domain>`:

| Service | URL Pattern |
|---------|-------------|
| **Developer Hub** | `https://backstage-developer-hub-developer-hub.apps.<domain>` |
| **Gitea** | `https://gitea-gitea.apps.<domain>` |
| **ArgoCD** | `https://openshift-gitops-server-openshift-gitops.apps.<domain>` |
| **DevSpaces** | `https://devspaces.apps.<domain>` |
| **Showroom** | `https://showroom.apps.<domain>` |
| **Registration Portal** | `https://workshop-registration.apps.<domain>` |
| **Keycloak** | `https://rhbk.apps.<domain>` |
| **Mailpit** | `https://n8n-mailpit-openshift-lightspeed.apps.<domain>` |
| **Grafana** | `https://grafana-observability.apps.<domain>` |
| **Kiali** | `https://kiali-openshift-cluster-observability-operator.apps.<domain>` |
| **Thanos Querier** | `https://thanos-querier.apps.<domain>` |
| **Kafka Console** | `https://kafka-console-kafka-cdc.apps.<domain>` |
| **Apicurio Registry** | `https://apicurio-registry-kafka-cdc.apps.<domain>` |
| **Kafka Bridge (REST)** | `https://kafka-bridge-kafka-cdc.apps.<domain>` |
| **Lightspeed** | Available from OpenShift Console |

## How It Works

```
Your Git Repo                    OpenShift Cluster
┌─────────────┐                 ┌─────────────────────────────┐
│ Helm Chart  │──── ArgoCD ────▶│ Your Workload               │
│ (templates, │                 │ (operators, apps, showroom) │
│  values)    │                 └─────────────────────────────┘
└─────────────┘                           │
                                          ▼
                                ConfigMap with demo.redhat.com/userinfo
                                          │
                                          ▼
                                    AgnosticD picks up user info
```

## RHDP Integration

Label resources for platform integration:

```yaml
# Health monitoring
metadata:
  labels:
    demo.redhat.com/application: "my-demo"

# Pass data back to AgnosticD (URLs, credentials, etc.)
metadata:
  labels:
    demo.redhat.com/userinfo: ""
```

## Kafka / CDC Troubleshooting with Lightspeed

| Situation | Lightspeed Prompt |
|-----------|-------------------|
| Kafka cluster not ready | _"Get the Kafka resource in namespace kafka-cdc and show its status"_ |
| KafkaConnect build failing | _"Get the logs from the KafkaConnect pod in namespace kafka-cdc"_ |
| Debezium connector not capturing changes | _"Get the KafkaConnectors in namespace kafka-cdc and show their status"_ |
| CDC events not arriving to topics | _"List KafkaTopics in namespace kafka-cdc and show message counts"_ |
| Kafka Bridge not responding | _"Get pods in namespace kafka-cdc with label strimzi.io/kind=KafkaBridge"_ |
| Apicurio Registry not available | _"Get pods in namespace kafka-cdc with label app=apicurio-registry"_ |
| Consumer group lag growing | _"Get the Kafka exporter metrics for consumer group lag in kafka-cdc"_ |
| Camel CDC processor errors | _"Get the logs from deployment camel-cdc-processor in namespace kafka-cdc"_ |
| Streams Console not loading | _"Get the Console resource in namespace kafka-cdc and show its status"_ |

## Documentation

- [Workshop (GitHub Pages)](https://maximilianopizarro.github.io/field-sourced-content-template/) - Full workshop guide
- [examples/helm/README.md](examples/helm/README.md) - Helm deployment guide
- [examples/ansible/README.md](examples/ansible/README.md) - Ansible deployment guide
- [docs/ansible-developer-guide.md](docs/ansible-developer-guide.md) - In-depth Ansible patterns
- [docs/SHOWROOM-UPDATE-SPEC.md](docs/SHOWROOM-UPDATE-SPEC.md) - Showroom maintenance guide
- [docs/validated-patterns-gap-analysis.md](docs/validated-patterns-gap-analysis.md) - Validated Patterns integration analysis

## Repository Structure

```
field-content/
├── pattern.json                           # Validated Patterns metadata
├── examples/
│   ├── helm/
│   │   ├── values.yaml                    # Parent chart values (full profile)
│   │   ├── values-lite.yaml              # Lite overlay (~60% fewer resources)
│   │   ├── values-global.yaml            # VP convention: cluster-independent settings
│   │   ├── values-hub.yaml               # VP convention: hub cluster config
│   │   ├── templates/                     # ArgoCD Application definitions
│   │   ├── components/                    # Per-component Helm sub-charts
│   │   │   ├── */       # Infrastructure components
│   │   │   ├── workshop-registration/  # Self-service registration portal
│   │   │   ├── industrial-edge-tst/              # IoT test env (sensors, messaging, dashboard)
│   │   │   ├── industrial-edge-data-lake/        # Central Kafka + Camel K S3 integration
│   │   │   ├── industrial-edge-stormshift/       # Factory edge (sensors, MirrorMaker2, dashboard)
│   │   │   ├── industrial-edge-data-science-cluster/  # RHODS DataScienceCluster
│   │   │   ├── industrial-edge-data-science-project/  # ML workspace + pipelines
│   │   │   ├── industrial-edge-pipelines/        # Tekton CI/CD for IoT components
│   │   │   ├── industrial-edge-minio/            # MinIO S3 storage (replaces ODF)
│   │   │   ├── showroom/                  # Workshop lab guide
│   │   │   └── ...
│   │   └── software-templates/            # Backstage scaffolder templates
│   │       ├── templates-catalog.yaml     # Auto-import catalog
│   │       ├── customer-service-mcp/      # Quarkus MCP server template
│   │       ├── neuralbank-backend/        # REST API template
│   │       └── neuralbank-frontend/       # SPA frontend template
│   └── ansible/                           # Ansible-based deployment example
├── roles/
│   └── ocp4_workload_field_content/       # AgnosticD workload role
└── docs/                                  # Developer guides and diagrams
```
