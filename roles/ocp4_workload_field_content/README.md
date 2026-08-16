# Field Content Workload (Reference Copy)

> **⚠️ REFERENCE ONLY**: This role is included for documentation purposes so you can understand how your content is deployed. The actual role is maintained in the [agnosticd/core_workloads](https://github.com/agnosticd/core_workloads) collection and executed by RHDP infrastructure.
>
> **Do not modify this role** - changes here have no effect on deployments.

## What This Role Does

When you order "Field Content" from RHDP, this role:

1. Creates an ArgoCD Application pointing to YOUR GitOps repository
2. Verifies the Application was accepted by the cluster
3. Exits immediately (fire-and-forget)

You control everything through your repository. The role just connects ArgoCD to your content.

## Deployment Behavior

This role uses a **fire-and-forget** approach:

- Creates the ArgoCD Application pointing to your repository
- Verifies it was accepted by the cluster (~1 minute max)
- Does **NOT** wait for sync or health status

**Why fire-and-forget?**

- **Developer ownership**: You are responsible for your deployment. Debug issues in ArgoCD and OpenShift.
- **Faster provisioning**: RHDP orders complete quickly.
- **Reduced complexity**: No health check configuration needed.

**What gets validated vs. what doesn't:**

| Error Type | Caught by Role? | Who Fixes It |
|------------|-----------------|--------------|
| Invalid repo URL format | ✅ Yes | Playbook fails |
| ArgoCD not installed | ✅ Yes | Playbook fails |
| Malformed Application YAML | ✅ Yes | Playbook fails |
| Repository doesn't exist | ❌ No | Developer checks ArgoCD |
| Helm chart syntax errors | ❌ No | Developer checks ArgoCD |
| Pods failing to start | ❌ No | Developer checks OpenShift |

## Variables Passed to Your Helm Chart

The role automatically injects these values:

```yaml
deployer:
  domain: "apps.cluster-guid.sandbox.opentlc.com"
  apiUrl: "https://api.cluster-guid.sandbox.opentlc.com:6443"
gitops:
  repoURL: "<catalog git URL>"
  repoUrl: "<catalog git URL>"
  revision: "main"
  path: "examples/bootstrap"
```

Use in your templates:

```yaml
spec:
  host: my-app.{{ .Values.deployer.domain }}
```

## How It Works

See the files in this role for implementation details:

| File | Purpose |
|------|---------|
| [defaults/main.yml](defaults/main.yml) | Available configuration variables |
| [tasks/workload.yml](tasks/workload.yml) | Main deployment logic |
| [templates/application.yaml.j2](templates/application.yaml.j2) | ArgoCD Application template |

## RHDP Integration

### UserInfo ConfigMaps

Create ConfigMaps with the \`demo.redhat.com/userinfo\` label to display information in the RHDP UI:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-demo-info
  labels:
    demo.redhat.com/userinfo: ""
data:
  demo_url: "https://my-demo.{{ .Values.deployer.domain }}"
  demo_title: "My Field Demo"
  access_instructions: "Click the URL to access the demo"
```

## Source

The canonical source for this role is:
- **Collection**: \`agnosticd.core_workloads\`
- **Repository**: https://github.com/agnosticd/core_workloads
- **Role**: \`ocp4_workload_field_content\`
