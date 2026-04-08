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
│  │                    Per-User Namespaces (×30)                        │  │
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
| **Gitea** | In-cluster Git server for scaffolded application repos (30 users) |
| **Keycloak** | Identity provider for backstage and neuralbank realms (30 users) |
| **Istio / Gateway API** | Service mesh with Gateway, HTTPRoute per scaffolded service |
| **Kuadrant** | API management: OIDCPolicy (auth) + RateLimitPolicy per service |
| **Showroom** | Antora-based workshop lab guide in Spanish |
| **OLS (Lightspeed)** | AI assistant with MCP Gateway integration |
| **LiteMaaS** | LLM proxy for model access |

### Software Templates (Neuralbank)

Each template generates a full application with CI/CD pipeline, connectivity-link manifests (Gateway, HTTPRoute, OIDCPolicy, RateLimitPolicy), DevSpaces devfile, and catalog registration.

| Template | Type | Description |
|----------|------|-------------|
| **customer-service-mcp** | Quarkus MCP Server | MCP server with `@Tool`/`@ToolArg` annotations, REST client to backend, SSE transport. Includes MCP Inspector in DevSpaces. |
| **neuralbank-backend** | Quarkus REST API | Credit management API (`/api/customers`, `/api/credits`, `/api/credits/{id}/update`) |
| **neuralbank-frontend** | Static HTML/CSS/JS | Credit visualization SPA with OpenShift Commons theme (Red Hat palette) |

### Scaffolding Flow

```
User in Developer Hub
  → Selects Software Template
    → 1. fetch:template (generates skeleton with user values)
    → 2. publish:gitea (pushes to Gitea org/developer-hub)
    → 3. catalog:register (registers Component in catalog)
    → ArgoCD syncs manifests/ → Deploys to userN-devspaces namespace
      → Deployment + Service
      → Gateway (Istio, ClusterIP)
      → HTTPRoute
      → OIDCPolicy (Keycloak backstage realm)
      → RateLimitPolicy (60 req/min per user)
      → OpenShift Route (edge TLS → gateway-istio service)
```

## Cluster Sizing (30 users)

### Resource Summary

| Layer | CPU (limits) | RAM (limits) | Disk |
|-------|-------------|-------------|------|
| OpenShift Platform | 14 vCPU | 34 Gi | 220 GB |
| Infrastructure Services | 36 vCPU | 54 Gi | 135 GB |
| 30 Users (apps + DevSpaces) | 105 vCPU | 135 Gi | 60 GB |
| Container Images | — | — | 113 GB |
| **Total** | **155 vCPU** | **223 Gi** | **528 GB** |

### Per-User Breakdown

| Component | CPU (limit) | RAM (limit) |
|-----------|------------|------------|
| DevSpaces workspace (UDI + Maven cache) | 2 vCPU | 3 Gi |
| customer-service-mcp (Quarkus) | 500m | 512 Mi |
| neuralbank-backend (Quarkus) | 500m | 512 Mi |
| neuralbank-frontend (httpd) | 200m | 128 Mi |
| Istio sidecar gateways (×3) | 300m | 384 Mi |
| **Total per user** | **3.5 vCPU** | **4.5 Gi** |

### Recommended Configurations

| Profile | Workers | vCPU / Worker | RAM / Worker | Disk / Worker | Total Workers | Notes |
|---------|---------|--------------|-------------|--------------|---------------|-------|
| **Minimum** (15 active users) | 2 | 32 vCPU | 64 Gi | 500 GB | 64 vCPU / 128 Gi | ~50% DevSpaces concurrency |
| **Recommended** (30 users) | 3 | 32 vCPU | 64 Gi | 300 GB | 96 vCPU / 192 Gi | All apps deployed, ~20 active DevSpaces |
| **Full concurrency** (30 users) | 4 | 32 vCPU | 64 Gi | 500 GB | 128 vCPU / 256 Gi | All 30 DevSpaces + builds simultaneously |

Control plane: 3 masters with 8 vCPU, 32 Gi RAM, 120 GB disk each (standard).

> **Warning**: Single-node (SNO) deployments with ≤32 GB RAM and ≤120 GB disk will experience persistent DiskPressure and pod evictions under this workload.

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

## Documentation

- [examples/helm/README.md](examples/helm/README.md) - Helm deployment guide
- [examples/ansible/README.md](examples/ansible/README.md) - Ansible deployment guide
- [docs/ansible-developer-guide.md](docs/ansible-developer-guide.md) - In-depth Ansible patterns
- [docs/SHOWROOM-UPDATE-SPEC.md](docs/SHOWROOM-UPDATE-SPEC.md) - Showroom maintenance guide

## Repository Structure

```
field-content/
├── examples/
│   ├── helm/
│   │   ├── values.yaml                    # Parent chart values
│   │   ├── templates/                     # ArgoCD Application definitions
│   │   ├── components/                    # Per-component Helm sub-charts
│   │   │   ├── connectivity-link-*/       # Infrastructure components
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
