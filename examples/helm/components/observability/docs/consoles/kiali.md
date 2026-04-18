# Kiali — Service Mesh Console

Consola de observabilidad de Istio Service Mesh con visualización de tráfico en tiempo real, estado de configuración y health de servicios.

<iframe src="https://kiali-openshift-cluster-observability-operator.<CLUSTER_DOMAIN>/kiali/" width="100%" height="900" frameborder="0" style="border:1px solid #ddd; border-radius:8px;"></iframe>

[Abrir Kiali :material-open-in-new:](https://kiali-openshift-cluster-observability-operator.<CLUSTER_DOMAIN>){ .md-button }

## Funcionalidades principales

| Feature | Descripción |
|---------|-------------|
| **Graph** | Visualización del tráfico entre servicios en el mesh |
| **Applications** | Estado de health de las aplicaciones |
| **Workloads** | Detalle de pods, deployments y sus métricas |
| **Services** | Listado de servicios con indicadores de health |
| **Istio Config** | Validación de configuración de Istio (VirtualServices, DestinationRules, etc.) |
| **Mesh** | Vista general del estado del mesh |

## Namespaces en el mesh

- `litemaas` — LiteLLM/MaaS gateway
- `istio-system` — Control plane, Kuadrant, Authorino
