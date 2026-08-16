# Bootstrap chart — Validated Patterns Operator + Pattern CR

RHDP Field Content CI must set the GitOps path to **`examples/bootstrap`**. This chart does not deploy workshop workloads. It:

1. Creates a `Subscription` for `patterns-operator` (`community-operators` / channel `fast`) in `openshift-operators`.
2. Applies a hub-only `Pattern` CR that points at this repository. The operator renders `clustergroup` 0.9.* against `values-global.yaml` + `values-hub.yaml` and reuses the **existing** `openshift-gitops` instance (`global.singleArgoCD` + `global.vpArgoNamespace`).

Do not install ACM, Vault, ESO, or a second Argo CD (`vp-gitops`).

## Values RHDP injects

| Key | Source |
|-----|--------|
| `deployer.domain` / `deployer.apiUrl` | AgnosticD role (`ocp4_workload_field_content`) |
| `gitops.repoURL` / `gitops.revision` / `gitops.path` | Same role (fork URL + branch + chart path) |
| `gitops.repoUrl` | Fallback if only the camelCase key is set |
| `litemaas.apiUrl` / `apiKey` / `model` | Injected when the catalog provisions MaaS |

The Pattern CR prefers `gitops.repoURL` over `gitops.repoUrl` so a fork URL from RHDP wins over the chart default.

Lite profile: set `pattern.extraValueFiles: ["/values-lite.yaml"]` in Helm values (Helm replaces lists; that overlay must repeat any `clusterGroup.applications` entries it changes).

## Local render

```bash
helm template bootstrap examples/bootstrap \
  --set gitops.repoUrl=https://github.com/example/fork.git \
  --set gitops.revision=main
```
