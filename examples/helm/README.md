# Helm charts under examples/helm — deprecated as GitOps entrypoint

The App-of-Apps parent that used to live here (`templates/applications.yaml` + `connectivityLink.apps[]`) is **not** the install path anymore.

## What to use instead

| Role | Path |
|------|------|
| RHDP GitOps Application | [`examples/bootstrap`](../bootstrap/) — Validated Patterns Operator + Pattern CR |
| Pattern values | [`values-global.yaml`](../../values-global.yaml) + [`values-hub.yaml`](../../values-hub.yaml) at the repo root |
| Grouped workloads | [`charts/all/`](../../charts/all/) |

`components/` remains the vendor tree for Backstage `catalog-info.yaml`, software templates, and as the source `scripts/assemble-vp-charts.py` copies into `charts/all/`.

Do not set the RHDP GitOps path to `examples/helm`.
