# Grouped Helm charts (Validated Patterns clustergroup)

One Argo CD Application per directory. `values-hub.yaml` → `clusterGroup.applications` points here.

| Group | `argoProject` | syncWave | Contents |
|-------|---------------|----------|----------|
| `platform` | platform | 0 | Per-user namespaces, OAuth users, console links, GitOps tuning |
| `identity-scm` | workshop | 2 | RHBK + Gitea Route/Job (upstream Gitea chart is a separate Application) |
| `developer-experience` | workshop | 3 | Developer Hub, ApplicationSets, Showroom, registration, DevSpaces + Continue Secret |
| `mesh-observability` | mesh | 3 | Service Mesh CRs, observability, Kiali, OpenTelemetry, Kuadrant CR |
| `cdc-pipeline` | data | 4 | Kafka, console, Apicurio, CDC demo, Camel, Mailpit, Kogito, cleanup |
| `ai-ml` | ai | 5 | OpenShift AI, OLS (`llm-credentials`), MCP gateway, NeuroFace CV |
| `industrial-edge` | edge | 5 | MinIO, DSC/DSP, data lake, TST, StormShift, pipelines |
| `litemaas` | ai | 7 | Optional; `disabled: true` by default |

Regenerate from `examples/helm/components` with `python3 scripts/assemble-vp-charts.py` after editing a source component (then re-apply the LLM/Continue and lite-wrap fixes if the script overwrote them).
