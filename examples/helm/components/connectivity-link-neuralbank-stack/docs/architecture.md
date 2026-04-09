# Arquitectura

## Diagrama de componentes

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          OpenShift Cluster                               │
│                                                                          │
│  ┌─────────────────────── neuralbank-stack ──────────────────────────┐   │
│  │                                                                    │   │
│  │  ┌──────────────────┐    ┌─────────────────────┐                  │   │
│  │  │ Istio Gateway    │    │  Kuadrant Policies   │                  │   │
│  │  │ (neuralbank-gw)  │───▶│                     │                  │   │
│  │  │                  │    │  OIDCPolicy          │                  │   │
│  │  │ HTTP :8080       │    │  (Keycloak OIDC)     │                  │   │
│  │  │ HTTPS :443       │    │                     │                  │   │
│  │  └────────┬─────────┘    │  RateLimitPolicy     │                  │   │
│  │           │              │  (10 req/min/user)   │                  │   │
│  │           │              └─────────────────────┘                  │   │
│  │           ▼                                                        │   │
│  │  ┌──────────────────┐    ┌──────────────────┐                     │   │
│  │  │ HTTPRoute        │    │ HTTPRoute         │                     │   │
│  │  │ neuralbank-api   │    │ neuralbank-root   │                     │   │
│  │  │ /api/* → backend │    │ /* → frontend     │                     │   │
│  │  │ /q/*  → backend  │    └──────────┬────────┘                     │   │
│  │  └────────┬─────────┘               │                              │   │
│  │           │                          │                              │   │
│  │           ▼                          ▼                              │   │
│  │  ┌──────────────────┐    ┌──────────────────┐                     │   │
│  │  │ neuralbank       │    │ neuralbank       │                     │   │
│  │  │ -backend         │    │ -frontend        │                     │   │
│  │  │ (Quarkus)        │    │ (SPA + PKCE)     │                     │   │
│  │  │ Port: 8080       │    │ Port: 8080       │                     │   │
│  │  └────────┬─────────┘    └──────────────────┘                     │   │
│  │           │                                                        │   │
│  │           ▼                                                        │   │
│  │  ┌──────────────────┐                                              │   │
│  │  │ neuralbank-db    │                                              │   │
│  │  │ (PostgreSQL 15)  │                                              │   │
│  │  │ Port: 5432       │                                              │   │
│  │  └──────────────────┘                                              │   │
│  └────────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────── Keycloak (rhbk) ────────┐  ┌──── Observability ──────────┐   │
│  │  Realm: neuralbank              │  │  Prometheus → Thanos        │   │
│  │  Client: neuralbank-frontend    │  │  Grafana Dashboards         │   │
│  │  Users: user1…user200           │  │  Kiali (Service Mesh)       │   │
│  └─────────────────────────────────┘  └──────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘
```

## Flujo de una request (usuario web)

1. El usuario accede a `https://neuralbank.<domain>` en el navegador
2. El **HTTPRoute** `neuralbank-root-route` enruta `/*` al **frontend** SPA
3. El frontend inicia flujo **OIDC PKCE** con Keycloak (realm `neuralbank`)
4. El usuario se autentica en Keycloak y obtiene un JWT token
5. El frontend envía requests al backend con `Authorization: Bearer <token>`
6. El **HTTPRoute** `neuralbank-api-route` enruta `/api/*` al **backend** Quarkus
7. La **OIDCPolicy** valida el JWT contra Keycloak (issuerURL del realm)
8. La **RateLimitPolicy** verifica el límite (10 req/min por usuario)
9. El backend Quarkus procesa la request y consulta **PostgreSQL**
10. La respuesta viaja de vuelta al frontend

## Flujo de una request (curl/programático)

```
curl + Bearer token      Istio Gateway         OIDCPolicy           RateLimitPolicy     Backend
  │                          │                      │                      │                │
  │  GET /api/v1/customers   │                      │                      │                │
  │  Authorization: Bearer   │                      │                      │                │
  │─────────────────────────▶│  Validate JWT        │                      │                │
  │                          │─────────────────────▶│                      │                │
  │                          │  ✓ Valid token        │                      │                │
  │                          │◀─────────────────────│                      │                │
  │                          │  Check rate limit     │                      │                │
  │                          │─────────────────────────────────────────────▶│                │
  │                          │  ✓ 8/10 used          │                      │                │
  │                          │◀─────────────────────────────────────────────│                │
  │                          │  Forward              │                      │                │
  │                          │─────────────────────────────────────────────────────────────▶│
  │  200 OK (JSON)           │                      │                      │                │
  │◀─────────────────────────│◀────────────────────────────────────────────────────────────│
```

## Service Mesh (Ambient)

El namespace `neuralbank-stack` está configurado con Istio **ambient mode** (`istio.io/dataplane-mode: ambient`):

- **mTLS automático** entre servicios sin sidecars
- **Waypoints** para procesamiento L7 y observabilidad
- Métricas de Envoy recolectadas por Prometheus

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| **API Backend** | Quarkus (Java 17) — Customer Service API |
| **Frontend** | SPA HTML/CSS/JS con OIDC PKCE |
| **Base de datos** | PostgreSQL 15 (RHEL9) |
| **Gateway** | Istio Gateway (Kubernetes Gateway API) |
| **Auth** | Kuadrant OIDCPolicy → Keycloak OIDC (JWT) |
| **Rate Limiting** | Kuadrant RateLimitPolicy (Limitador) |
| **Service Mesh** | Istio Ambient Mode |
| **Observabilidad** | Prometheus + Grafana + Kiali |
| **GitOps** | ArgoCD Application |
| **Catálogo** | Red Hat Developer Hub (Backstage) |

## Despliegue via ArgoCD

```yaml
app: field-content-connectivity-link-neuralbank-stack
path: connectivity-link-neuralbank-stack
destinationNamespace: neuralbank-stack
syncWave: "7"
```

ArgoCD sincroniza automáticamente desde el repositorio Git. Cualquier cambio en `values.yaml` o templates se aplica al clúster tras el push.
