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

## Getting Started

### Choose Your Pattern

| Pattern | Use When |
|---------|----------|
| [examples/helm/](examples/helm/) | Deployment can be expressed as Kubernetes manifests with Helm templating |
| [examples/ansible/](examples/ansible/) | You need wait-for-ready, secret generation, API calls, or conditional logic |

### Quick Start

```bash
# Clone this template
git clone https://github.com/rhpds/field-sourced-content-template.git my-content
cd my-content

# Choose an example and start customizing
cd examples/helm      # or examples/ansible
# Edit values.yaml and templates as documented in each example's README
```

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
│   ├── helm/        # Helm-based deployment example
│   └── ansible/     # Ansible-based deployment example
├── roles/
│   └── ocp4_workload_field_content/  # AgnosticD workload role
└── docs/            # Developer guides and diagrams
```
