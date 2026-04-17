# Arquitectura

## Diagrama de componentes

```
┌──────────────────────────────────────────────────────────────────┐
│                     OpenShift Cluster                            │
│                                                                  │
│  ┌────────────────────── nfl-wallet-prod ──────────────────────┐ │
│  │                                                              │ │
│  │  ┌──────────────────┐    ┌─────────────────┐                │ │
│  │  │ Istio Gateway    │    │  Kuadrant        │                │ │
│  │  │ (nfl-wallet-gw)  │───▶│  AuthPolicy      │                │ │
│  │  │                  │    │  (API Key)       │                │ │
│  │  │ HTTPS :443       │    │  RateLimitPolicy │                │ │
│  │  └────────┬─────────┘    │  (120 req/min)   │                │ │
│  │           │              └─────────────────┘                │ │
│  │           ▼                                                  │ │
│  │  ┌──────────────────┐                                        │ │
│  │  │ HTTPRoute        │                                        │ │
│  │  │ /api/* → :8080   │                                        │ │
│  │  │ /q/swagger-ui    │                                        │ │
│  │  │ /q/openapi       │                                        │ │
│  │  └────────┬─────────┘                                        │ │
│  │           │                                                  │ │
│  │           ▼                                                  │ │
│  │  ┌──────────────────┐    ┌──────────────────┐               │ │
│  │  │ nfl-wallet-api   │    │ nfl-wallet-db    │               │ │
│  │  │ (Quarkus)        │───▶│ (PostgreSQL 15)  │               │ │
│  │  │ Port: 8080       │    │ Port: 5432       │               │ │
│  │  └──────────────────┘    └──────────────────┘               │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌──── openshift-cluster-observability-operator ────────────────┐│
│  │  Prometheus ──▶ Thanos ──▶ Grafana                           ││
│  │  PodMonitor (waypoints, gateways)                            ││
│  │  ServiceMonitor (gateway metrics)                            ││
│  └──────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

## Flujo de una request

1. El cliente envía una request HTTPS al gateway Istio en `nfl-wallet.apps.<domain>`
2. Kuadrant **AuthPolicy** valida el header `X-API-Key` contra los Secrets con label `kuadrant.io/apikey: "true"`
3. Kuadrant **RateLimitPolicy** verifica que no se excedan 120 req/min
4. El **HTTPRoute** enruta la request al servicio `nfl-wallet-api:8080`
5. La aplicación Quarkus procesa la request y responde

## Service Mesh (Ambient)

Los namespaces `nfl-wallet-prod` y `nfl-wallet-test` están configurados con Istio **ambient mode** (`istio.io/dataplane-mode: ambient`), lo que habilita:

- mTLS automático entre servicios sin sidecars
- **Waypoints** para L7 processing y observabilidad
- Métricas de Envoy recolectadas por Prometheus

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| **API** | Quarkus (Java 17) |
| **Base de datos** | PostgreSQL 15 (RHEL9) |
| **Gateway** | Istio Gateway (Kubernetes Gateway API) |
| **Auth** | Kuadrant AuthPolicy (API Key) |
| **Rate Limiting** | Kuadrant RateLimitPolicy (Limitador) |
| **Service Mesh** | Istio Ambient Mode |
| **Observabilidad** | Prometheus + Grafana + Kiali |
| **GitOps** | ArgoCD ApplicationSet |

## Despliegue via ArgoCD

El componente se despliega automáticamente via ArgoCD Application:

```yaml
id: nfl-wallet
path: nfl-wallet
destinationNamespace: nfl-wallet-prod
syncWave: "7"
```
