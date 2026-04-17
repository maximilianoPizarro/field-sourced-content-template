# Platform Observability

Stack de observabilidad pre-desplegado en el clúster para monitoreo de APIs, gateways e Istio service mesh.

## Componentes

| Componente | Descripción | Namespace |
|-----------|------------|-----------|
| **Grafana** | Dashboards de visualización de métricas | `openshift-cluster-observability-operator` |
| **Prometheus (RHOBS)** | Recolección y almacenamiento de métricas | `openshift-cluster-observability-operator` |
| **Thanos Querier** | Query federation para métricas multi-replica | `openshift-cluster-observability-operator` |
| **Alertmanager** | Gestión de alertas y notificaciones | `openshift-cluster-observability-operator` |
| **Kiali** | Consola de Service Mesh con visualización de tráfico | `openshift-cluster-observability-operator` |
| **OpenTelemetry** | Distributed tracing para aplicaciones | `openshift-opentelemetry` |

## Acceso rápido

- **Grafana**: [grafana-observability.<CLUSTER_DOMAIN>](https://grafana-observability.<CLUSTER_DOMAIN>)
- **Thanos Querier**: [thanos-querier.<CLUSTER_DOMAIN>](https://thanos-querier.<CLUSTER_DOMAIN>)

## Credenciales

| Servicio | Usuario | Contraseña |
|---------|---------|------------|
| Grafana | `admin` | `openshift` |
| Thanos Querier | (OAuth - usar credenciales de OpenShift) | - |

## Métricas disponibles

El stack recolecta métricas de:

- **Gateways Istio**: latencia, tráfico, errores y saturación (RED metrics)
- **Kuadrant**: rate limiting, auth policies, estado de policies
- **Waypoints**: métricas de ambient mesh
- **Aplicaciones**: neuralbank-stack, nfl-wallet, litemaas
