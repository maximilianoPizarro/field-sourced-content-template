# Helm Example - App of Apps

Deploy workloads using an Argo CD App of Apps pattern. The root chart renders Argo CD `Application` resources that point at standalone Helm charts under `components/`.

## Quick Start

1. Fork or clone this repository and point `gitops.repoUrl` in `values.yaml` at your Git remote.
2. Enable or disable pieces under `components:` (hello-world, showroom) and `connectivityLink.apps:` (connectivity-link stack).
3. Order **Field Content CI** from RHDP with your repository URL and GitOps path `examples/helm` (or the path you use for this chart).

RHDP injects `deployer.domain` and `deployer.apiUrl`. Optional **LiteMaaS / MaaS** ordering injects `litemaas.apiUrl`, `litemaas.apiKey`, and `litemaas.model` into this chart’s values when enabled.

## Architecture

```
examples/helm/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── applications.yaml              # hello-world, showroom, optional single-operator
│   └── applications.yaml   # connectivity-link Argo CD apps
└── components/
    ├── operator/                      # Optional single OLM subscription (template)
    ├── hello-world/
    ├── showroom/                      # Lab guide (showroom-from-zero-to-hero by default)
    ├── operators/   # OLM operators (RHCL, mesh, Dev Spaces, RHBK, …)
    ├── namespaces/
    ├── rhcl-operator/
    ├── developer-hub/
    ├── observability/
    └── …                              # see values.yaml → connectivityLink.apps
```

Connectivity-link manifests are **vendored** from [connectivity-link](https://gitlab.com/maximilianoPizarro/connectivity-link): plain YAML directories were rendered with `kubectl kustomize` into `templates/all.yaml` where applicable; the `operators` upstream Helm charts were copied as subcharts.

## Configuration

| Area | Purpose |
|------|---------|
| `gitops.repoUrl`, `gitops.revision`, `gitops.basePath` | Git source Argo CD uses for every child `Application` |
| `connectivityLink.apps[]` | Toggle each connectivity-link app, destination namespace, prune, sync-wave |
| `connectivityLink.operators` | `channel`, `version`, `subscriptions` passed to `operators` |
| `litemaas.*` | Optional RHDP injection for LLM model serving |
| `components.showroom` | Showroom content repo, nookbag, terminal (default: showroom-from-zero-to-hero) |

**Note:** LiteMaaS-related YAML in `litemaas` still contains cluster-specific URLs from the upstream snapshot. For a new cluster, adjust `cluster-config` / domain handling in that chart or maintain a fork.

## Testing Locally

```bash
helm lint .
helm template my-release . --set deployer.domain=apps.cluster.example.com
```

## Adding a Component

1. Add a Helm chart under `components/<name>/`.
2. Append an entry to `connectivityLink.apps` in `values.yaml` (or add a dedicated block in `templates/applications.yaml` if it needs special `valuesObject` handling).
