# Swagger UI

## Acceso

La API de NFL Wallet expone una interfaz Swagger UI para explorar y probar los endpoints.

**URL**: [https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/swagger-ui](https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/swagger-ui)

## Configuración de autenticación en Swagger

1. Acceder a la URL de Swagger UI
2. En la esquina superior derecha, hacer click en **Authorize**
3. En el campo **ApiKeyAuth (apiKey)**, ingresar:
   ```
   nfl-wallet-demo-key-2024
   ```
4. Click en **Authorize**, luego **Close**
5. Los endpoints ahora enviarán automáticamente el header `X-API-Key`

## Endpoints disponibles

### Customer Service (CRUD)

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/customers` | Listar clientes (paginado, con filtros) |
| `POST` | `/api/v1/customers` | Crear nuevo cliente |
| `GET` | `/api/v1/customers/{customerId}` | Obtener cliente por ID |
| `PUT` | `/api/v1/customers/{customerId}` | Actualizar cliente |
| `DELETE` | `/api/v1/customers/{customerId}` | Eliminar cliente (soft delete) |

### Credit Scoring

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/customers/{customerId}/credit-score` | Obtener score crediticio |
| `POST` | `/api/v1/customers/{customerId}/credit-score/calculate` | Recalcular score |
| `GET` | `/api/v1/customers/{customerId}/summary` | Resumen completo del cliente |

### Customer Status

| Método | Path | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/customers/{customerId}/activate` | Activar cliente |
| `POST` | `/api/v1/customers/{customerId}/deactivate` | Desactivar cliente |
| `POST` | `/api/v1/customers/{customerId}/block` | Bloquear cliente |
| `POST` | `/api/v1/customers/{customerId}/unblock` | Desbloquear cliente |

## OpenAPI Spec

El spec OpenAPI se puede descargar en formato JSON/YAML:

```bash
curl -H "X-API-Key: nfl-wallet-demo-key-2024" \
  https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/openapi
```

## Kuadrant Auth Flow

```
Cliente                    Gateway Istio              Kuadrant/Authorino         nfl-wallet-api
  │                           │                            │                        │
  │  GET /api/v1/customers    │                            │                        │
  │  X-API-Key: demo-key      │                            │                        │
  │──────────────────────────▶│                            │                        │
  │                           │  Validate API Key          │                        │
  │                           │───────────────────────────▶│                        │
  │                           │                            │  Check Secret labels   │
  │                           │                            │  kuadrant.io/apikey     │
  │                           │  ✓ Authorized              │                        │
  │                           │◀───────────────────────────│                        │
  │                           │  Check Rate Limit          │                        │
  │                           │  (Limitador: 120/min)      │                        │
  │                           │  ✓ Within limit            │                        │
  │                           │                            │                        │
  │                           │  Forward request           │                        │
  │                           │───────────────────────────────────────────────────▶│
  │                           │                            │                        │
  │  200 OK (customers list)  │                            │                        │
  │◀──────────────────────────│◀───────────────────────────────────────────────────│
```
