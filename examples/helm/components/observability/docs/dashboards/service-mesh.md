# Service Mesh Overview

Dashboard en tiempo real (auto-refresh 30s) del service mesh Istio en modo ambient.

<iframe src="https://grafana-observability.apps.cluster-l9nhj.dynamic.redhatworkshops.io/d/service-mesh-overview?orgId=1&kiosk&theme=light" width="100%" height="900" frameborder="0" style="border:1px solid #ddd; border-radius:8px;"></iframe>

[Abrir en Grafana :material-open-in-new:](https://grafana-observability.apps.cluster-l9nhj.dynamic.redhatworkshops.io/d/service-mesh-overview){ .md-button }

## Paneles incluidos

| Fila | Paneles |
|------|---------|
| **Traffic** | Request rate source-to-destination, Request rate por namespace (L7+L4) |
| **Success** | Success rate 2xx (timeseries + gauge instantáneo) |
| **Latency** | P50 / P95 / P99 por servicio destino |
| **TCP** | Bytes enviados, Bytes recibidos (desde ztunnel) |
| **Connections** | Conexiones activas (opened vs closed rate), TCP opened rate |
| **Waypoints** | Request rate y P95 latency via waypoint proxies |
| **Errors** | 4xx breakdown por código/destino, 5xx breakdown por código/destino |

## Fuentes de métricas

- **ztunnel** (L4): `istio_tcp_sent_bytes_total`, `istio_tcp_connections_opened_total`, etc.
- **Waypoint proxies** (L7): `istio_requests_total`, `istio_request_duration_milliseconds_bucket`
- **Gateway pods** (L7): mismas métricas Istio desde Envoy

## Traffic Generator

Un pod ligero envía requests periódicos (~10s) a los gateways internos para mantener los dashboards siempre con datos.
