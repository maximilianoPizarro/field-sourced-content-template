# Acceso a Grafana

## URL de acceso

```
https://grafana-observability.apps.cluster-l9nhj.dynamic.redhatworkshops.io
```

## Credenciales

| Campo | Valor |
|-------|-------|
| **Usuario** | `admin` |
| **Contraseña** | `openshift` |

Todos los usuarios del workshop (`user1` a `user200`) pueden acceder a Grafana como **Viewer** utilizando las mismas credenciales de administrador o mediante SSO si está configurado.

## Dashboards disponibles

### API Gateway Metrics
Métricas de los gateways Istio para las aplicaciones protegidas por Kuadrant:

- **Request Rate**: solicitudes por segundo por gateway/namespace
- **Error Rate**: porcentaje de errores 4xx/5xx
- **Latency P50/P95/P99**: distribución de latencia
- **Active Connections**: conexiones activas al gateway

### Kuadrant Policy Metrics
Estado y métricas de las policies de Kuadrant:

- **Rate Limit Hits**: solicitudes rechazadas por rate limiting
- **Auth Policy Status**: estado de autenticación/autorización
- **Policy Enforcement**: métricas de enforcement por namespace

### Service Mesh Overview
Vista general del mesh Istio en tiempo real (auto-refresh 30s):

- **Traffic Flow**: request rate entre servicios (source → destination)
- **Success Rate**: porcentaje de respuestas 2xx por servicio (timeseries + gauge)
- **Latency Distribution**: percentiles P50/P95/P99 por servicio destino
- **TCP Traffic**: bytes enviados y recibidos por servicio (bar chart)
- **Active Connections**: conexiones activas y rate de nuevas conexiones TCP
- **Waypoint Metrics**: request rate y P95 latency a través de ambient mesh waypoints
- **Error Breakdown**: errores 4xx y 5xx desglosados por código de respuesta y servicio destino (stacked)

## Datasources

| Datasource | Tipo | URL interna |
|-----------|------|-------------|
| Prometheus (RHOBS) | `prometheus` | `http://stack-prometheus.openshift-cluster-observability-operator.svc.cluster.local:9090` |
| Thanos Querier | `prometheus` | `http://thanos-querier-querier.openshift-cluster-observability-operator.svc.cluster.local:9090` |

## Agregar nuevos dashboards

Los dashboards se gestionan via `GrafanaDashboard` CRs del `grafana-operator`:

```yaml
apiVersion: grafana.integreatly.org/v1beta1
kind: GrafanaDashboard
metadata:
  name: my-dashboard
  namespace: openshift-cluster-observability-operator
spec:
  instanceSelector:
    matchLabels:
      app: grafana-observability
  json: |
    { ... dashboard JSON ... }
```
