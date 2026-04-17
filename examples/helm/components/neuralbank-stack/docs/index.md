# Neuralbank Stack

Plataforma financiera de demostración pre-desplegada en el clúster OpenShift. Incluye backend Quarkus, frontend SPA y base de datos PostgreSQL, protegidos con **Connectivity Link** (OIDCPolicy + RateLimitPolicy).

## Acceso rápido

| Recurso | URL |
|---------|-----|
| **Frontend** | [neuralbank.<CLUSTER_DOMAIN>](https://neuralbank.<CLUSTER_DOMAIN>) |
| **Swagger UI** | [neuralbank.<CLUSTER_DOMAIN>/q/swagger-ui](https://neuralbank.<CLUSTER_DOMAIN>/q/swagger-ui) |
| **API Base** | `https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers` |
| **Grafana** | [grafana-observability.<CLUSTER_DOMAIN>](https://grafana-observability.<CLUSTER_DOMAIN>) |

## Credenciales

### Keycloak (acceso al frontend y API)

| Campo | Valor |
|-------|-------|
| **Realm** | `neuralbank` |
| **Client ID** | `neuralbank-frontend` |
| **URL del realm** | `https://rhbk.<CLUSTER_DOMAIN>/realms/neuralbank` |
| **Usuarios** | `user1` … `user200` |
| **Contraseña** | `Welcome123!` |

### Base de datos PostgreSQL

| Campo | Valor |
|-------|-------|
| **Host** | `neuralbank-db.neuralbank-stack.svc.cluster.local` |
| **Puerto** | `5432` |
| **Base de datos** | `postgres` |
| **Usuario** | `postgres` |
| **Contraseña** | `supersecretpassword` |

### Grafana

| Campo | Valor |
|-------|-------|
| **Usuario** | `admin` |
| **Contraseña** | `openshift` |

## Componentes

| Componente | Imagen | Puerto |
|------------|--------|--------|
| **neuralbank-backend** | `quay.io/maximilianopizarro/neuralbank-backend:latest` | 8080 |
| **neuralbank-frontend** | `quay.io/maximilianopizarro/neuralbank-front:pkce` | 8080 |
| **neuralbank-db** | `registry.redhat.io/rhel9/postgresql-15:latest` | 5432 |

## Namespace

Todos los componentes se despliegan en el namespace **`neuralbank-stack`**.

## Uso rápido con curl

```bash
# Obtener token de Keycloak
TOKEN=$(curl -s -X POST \
  "https://rhbk.<CLUSTER_DOMAIN>/realms/neuralbank/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=neuralbank-frontend" \
  -d "username=user1" \
  -d "password=Welcome123!" | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

# Listar clientes
curl -s -H "Authorization: Bearer $TOKEN" \
  https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers | python3 -m json.tool

# Obtener cliente por ID
curl -s -H "Authorization: Bearer $TOKEN" \
  https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers/1 | python3 -m json.tool

# Resumen del cliente
curl -s -H "Authorization: Bearer $TOKEN" \
  https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers/1/summary | python3 -m json.tool
```

## Connectivity Link

Neuralbank utiliza el stack completo de Red Hat Connectivity Link:

| Recurso | Nombre | Función |
|---------|--------|---------|
| **Gateway** (Istio) | `neuralbank-gateway` | Punto de entrada HTTP/HTTPS |
| **HTTPRoute** | `neuralbank-api-route` | Enruta `/api` y `/q` al backend |
| **OIDCPolicy** | `neuralbank-oidc` | Autenticación OIDC con Keycloak |
| **RateLimitPolicy** | `neuralbank-customers-ratelimit` | 10 req/min por usuario en `/api/customers` |

Ver secciones de [Arquitectura](architecture.md) y [Autenticación OIDC](oidc-auth.md) para más detalles.
