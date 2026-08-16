# Validated Patterns Integration — Gap Analysis

Comparative analysis between this repository and the [Red Hat Validated Patterns](https://validatedpatterns.io/) framework.

## Adopted

| VP capability | How this repo uses it |
|---|---|
| Validated Patterns Operator + `Pattern` CR | [`examples/bootstrap`](../examples/bootstrap/) (RHDP GitOps path). Sample CR: [`examples/pattern-cr/hub.yaml`](../examples/pattern-cr/hub.yaml) |
| `clustergroup` chart 0.9.* | Root [`Chart.yaml`](../Chart.yaml) + operator `multiSourceConfig` |
| `values-global.yaml` / `values-hub.yaml` | Real VP schema at the **repository root** (not aliases under `examples/helm/`) |
| Grouped Helm charts | [`charts/all/`](../charts/all/) — about eight Applications instead of ~40 |
| `openshift-gitops` reuse | `global.singleArgoCD: true` and `global.vpArgoNamespace: openshift-gitops` (RHDP already installed GitOps) |
| LLM credentials in GitOps | OLS creates `Secret/llm-credentials`; Continue Secret is in the DevSpaces chart. Coalesce `litemaas` → `maas` → `lightspeed` |

## Explicitly out of scope (unchanged)

| VP capability | Decision |
|---|---|
| HashiCorp Vault + External Secrets Operator | **Not adopted.** `global.secretLoader.disabled: true`. Workshop credentials are Helm values / RHDP injection. |
| ACM multi-cluster / `managedClusterGroups` | **Not adopted.** Hub-only, one cluster. |
| Second GitOps instance (`vp-gitops`) | **Not adopted.** Do not let the operator replace RHDP's `openshift-gitops`. |
| `common` git submodule | **Not adopted.** Monorepo; clustergroup comes from `charts.validatedpatterns.io`. |

## Legacy App-of-Apps

[`examples/helm`](../examples/helm/) used to generate one Argo Application per component (`connectivityLink.apps[]`). That parent chart is **deprecated** as a GitOps entrypoint. Component YAML under `examples/helm/components/` remains for catalog-info, software templates, and as input to `scripts/assemble-vp-charts.py`.

## Lite overlay

Helm **replaces lists** and **merges maps**. `clusterGroup.applications` and `subscriptions` are maps, so [`values-lite.yaml`](../values-lite.yaml) can set `disabled: true` on individual keys. Load it with Pattern `spec.extraValueFiles: ["/values-lite.yaml"]` (see [`examples/pattern-cr/hub-lite.yaml`](../examples/pattern-cr/hub-lite.yaml)). Child charts also honor `global.profile: lite` (DevSpaces and Kogito templates are wrapped).

## Resource comparison

| Profile | Fixed infra | Per user | 30 users |
|---------|------------|----------|----------|
| **Full** | ~66 vCPU / ~122 Gi | 3.5 vCPU / 4.5 Gi | 3 × 64 vCPU / 128 Gi or 4 × m5.8xlarge (32 vCPU) |
| **Lite** | 20–25 vCPU / 30–40 Gi | 1.5 vCPU / 1.5 Gi | 3 × 32 vCPU / 64 Gi |

## Recommendation

Use the operator + Pattern CR on RHDP. Keep ACM and Vault off. Re-run `python3 scripts/assemble-vp-charts.py` only when you change a source chart under `examples/helm/components/` and then re-apply any group-specific wrappers (Continue Secret, lite `if` around DevSpaces/Kogito).
