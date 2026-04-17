# Validated Patterns Integration — Gap Analysis

Comparative analysis between this repository's architecture and the [Red Hat Validated Patterns](https://validatedpatterns.io/) framework (specifically the [Industrial Edge](https://validatedpatterns.io/patterns/industrial-edge/) pattern).

## What Already Exists (No Changes Needed)

| VP Capability | Equivalent in This Repo | Status |
|---|---|---|
| App-of-Apps pattern | `connectivity-link-applications.yaml` generates N `Application` CRs | Complete |
| ApplicationSets | `connectivity-link-applicationsets` with SCM provider (Gitea) | Complete |
| Operator management via OLM | `connectivity-link-operators/` with 15+ Subscriptions | Complete |
| Sync waves / ordering | Each app has `syncWave` (1-10) in `values.yaml` | Complete |
| Imperative hooks | PostSync Jobs (Gitea token), CronJobs (cleanup, webhooks) | Complete |
| Tekton CI/CD | Pipelines in software-templates + OpenShift Pipelines operator | Complete |
| Per-app enable/disable | `enabled: true/false` on each `connectivityLink.apps[]` | Complete |
| ArgoCD tuning | `connectivity-link-openshift-gitops` patches ArgoCD CR | Complete |

## Gaps Identified

### 1. Values File Structure (Effort: MEDIUM)

**VP requires**: `values-global.yaml` + `values-hub.yaml` + `values-{clustergroup}.yaml`

**Current state**: Single `examples/helm/values.yaml` with all settings mixed.

**Resolution**: Created `values-global.yaml` and `values-hub.yaml` following VP naming conventions. The original `values.yaml` remains the primary source of truth — the VP files are optional aliases for teams adopting the framework.

### 2. ClusterGroup Chart (Effort: HIGH — Not Adopted)

**VP requires**: The `validatedpatterns/clustergroup-chart` (v0.9.x) that generates Applications, Namespaces, and Subscriptions from a standardized schema.

**Current state**: Custom app-of-apps in `examples/helm/templates/` that is functionally equivalent.

**Decision**: **Not migrating**. The current app-of-apps is tested, production-proven, and simpler for single-cluster workshops. The clusterGroup chart adds complexity without benefit for this use case.

### 3. Secret Management with Vault (Effort: HIGH — Not Adopted)

**VP requires**: HashiCorp Vault + External Secrets Operator (ESO).

**Current state**: Secrets in ConfigMaps/Secrets within templates, demo credentials hardcoded for workshop use.

**Decision**: **Not migrating**. For workshops/demos, Vault is overkill. Credentials are injected at deploy time by RHDP or via Ansible. An intermediate step (SealedSecrets) could be adopted if needed.

### 4. Multi-Cluster with ACM (Effort: VERY HIGH — Not Adopted)

**VP requires**: Red Hat ACM, ManagedClusters, Placement, Policies.

**Current state**: Everything points to `https://kubernetes.default.svc` (single cluster).

**Decision**: **Not migrating**. ACM alone consumes ~8 GB RAM + 4 vCPU. For a single-cluster workshop, it adds no value.

### 5. Common Submodule / Multi-Source (Effort: LOW — Not Adopted)

**VP requires**: ArgoCD multi-source with charts in separate repos.

**Current state**: Everything vendored in a single repo under `examples/helm/components/`.

**Decision**: **Keeping monorepo**. Simpler for workshops. VP multi-source targets enterprise patterns with shared chart libraries.

## What Was Adopted

| Change | File | Purpose |
|--------|------|---------|
| Pattern metadata | `pattern.json` | Name, version, profiles, estimated resources |
| Global values | `examples/helm/values-global.yaml` | Cluster-independent settings |
| Hub values | `examples/helm/values-hub.yaml` | Hub cluster config |
| Lite overlay | `examples/helm/values-lite.yaml` | ~60% resource reduction profile |

## Resource Comparison

| Profile | Fixed Infra | Per User | 30 Users | 200 Users |
|---------|------------|----------|----------|-----------|
| **Full** | 50 vCPU / 88 Gi | 3.5 vCPU / 4.5 Gi | 4 × m5.8xlarge | 8–12 × m5.8xlarge |
| **Lite** | 20–25 vCPU / 30–40 Gi | 1.5 vCPU / 1.5 Gi | 2–3 × m5.4xlarge | Not recommended* |

\* The lite profile defaults to 30 users. For 200 users, use the full profile with selective component disabling.

## Recommendation

The most pragmatic path is to **keep the current architecture** (which is already GitOps-first with ArgoCD) and only adopt VP conventions that improve documentation and pattern discoverability — without the overhead of Vault, ACM, or the clusterGroup chart.
