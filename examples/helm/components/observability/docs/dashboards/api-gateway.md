# API Gateway Metrics

Dashboard de métricas de los gateways Istio para las aplicaciones protegidas por Kuadrant.

<iframe src="https://grafana-observability.apps.cluster-l9nhj.dynamic.redhatworkshops.io/d/api-gateway-metrics?orgId=1&kiosk&theme=light" width="100%" height="900" frameborder="0" style="border:1px solid #ddd; border-radius:8px;"></iframe>

[Abrir en Grafana :material-open-in-new:](https://grafana-observability.apps.cluster-l9nhj.dynamic.redhatworkshops.io/d/api-gateway-metrics){ .md-button }

## Paneles incluidos

| Panel | Descripción |
|-------|-------------|
| **Request Rate by Gateway** | Solicitudes por segundo agrupadas por namespace destino |
| **Error Rate (4xx/5xx)** | Tasa de errores HTTP por namespace |
| **P95 Latency** | Percentil 95 de latencia por namespace |
| **Rate Limit Rejections** | Solicitudes rechazadas por rate limiting (Kuadrant) |

## Datasource

Usa **Prometheus (RHOBS)** por defecto. Puedes cambiar a **Thanos Querier** desde el dropdown `DS_PROMETHEUS` en la parte superior del dashboard.

## Credenciales

- **Usuario**: `admin`
- **Password**: `openshift`
- Acceso anónimo como **Viewer** habilitado (no requiere login para visualizar)
