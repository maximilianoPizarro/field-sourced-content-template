# Arquitectura de Observabilidad

## Diagrama de componentes

```
┌──────────────────────────────────────────────────────────────────┐
│                      OpenShift Cluster                            │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │  openshift-cluster-observability-operator                  │   │
│  │                                                            │   │
│  │  ┌──────────┐  ┌────────────┐  ┌──────────────────┐       │   │
│  │  │ Grafana  │  │ Prometheus │  │ Thanos Querier   │       │   │
│  │  │ (v5)     │←─│ (RHOBS)    │←─│ (federation)     │       │   │
│  │  └──────────┘  └─────┬──────┘  └──────────────────┘       │   │
│  │                      │                                     │   │
│  │  ┌──────────┐  ┌─────┴──────┐  ┌─────────────────────┐   │   │
│  │  │ Kiali    │  │Alertmanager│  │ Traffic Generator   │   │   │
│  │  │ (mesh)   │  │            │  │ (curl loop ~10s)    │   │   │
│  │  └──────────┘  └────────────┘  └─────────┬───────────┘   │   │
│  └───────────────────────────────────────────┼───────────────┘   │
│                           ▲                  │ HTTP requests     │
│              scrape metrics│                  ▼                   │
│  ┌────────────┬────────────┼────────────┬──────────────┐        │
│  │            │            │            │              │        │
│  │ nfl-wallet │ neuralbank │  litemaas  │  kuadrant    │        │
│  │ gateways   │ gateway    │  gateway   │  controller  │        │
│  │ + ztunnel  │ + ztunnel  │ + ztunnel  │              │        │
│  └────────────┴────────────┴────────────┴──────────────┘        │
└──────────────────────────────────────────────────────────────────┘
```

## Flujo de datos

1. **Traffic Generator** envía requests HTTP periódicos (~cada 10s con jitter) a los gateways internos del mesh
2. **ztunnel** (L4) y **waypoint proxies** (L7) procesan el tráfico y exponen métricas Istio
3. **Prometheus** (MonitoringStack RHOBS) recolecta métricas via `ServiceMonitor` y `PodMonitor`
4. **Thanos Querier** federa queries entre réplicas de Prometheus
5. **Grafana** consulta Thanos/Prometheus para dashboards (API Gateway Metrics + Service Mesh Overview)
6. **Alertmanager** evalúa reglas de alerta y envía notificaciones
7. **Kiali** consulta Prometheus para visualización de service mesh

## Operadores requeridos

| Operador | Canal | Source |
|----------|-------|--------|
| `cluster-observability-operator` | `stable` | `redhat-operators` |
| `grafana-operator` | `v5` | `community-operators` |
| `servicemeshoperator3` | `stable` | `redhat-operators` |
| `opentelemetry-product` | `stable` | `redhat-operators` |

## Namespaces monitoreados

Los `PodMonitor` y `ServiceMonitor` configurados recolectan métricas de:

- `ztunnel` — L4 TCP metrics del data plane ambient mesh
- `nfl-wallet-prod` — Gateway, waypoints e Istio pods de producción
- `nfl-wallet-test` — Gateway, waypoints e Istio pods de testing
- `neuralbank-stack` — Gateway de la aplicación Neuralbank
- `litemaas` — Gateway de LiteLLM/MaaS
- `istio-system` — Kuadrant controller y Authorino

## Traffic Generator

Un Deployment ligero que envía requests HTTP periódicos a los gateways internos para mantener los dashboards de Grafana siempre con datos:

- **Imagen**: `ubi9/ubi-minimal` (curl)
- **Intervalo**: ~10s + jitter aleatorio (0-5s)
- **Recursos**: 10m CPU / 32Mi RAM (requests), 50m CPU / 64Mi RAM (limits)
- **Targets**: health endpoints + API endpoints de neuralbank, nfl-wallet (prod/test) y litemaas
- **Genera**: mix de 200 (health), 401/403 (API sin auth) y 429 (rate limited) para datos variados en los dashboards

## Retención de datos

- **Prometheus**: 7 días (`retention: 7d`)
- **Alertmanager**: configuración por defecto RHOBS
