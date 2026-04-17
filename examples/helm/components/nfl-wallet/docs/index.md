# NFL Wallet — Customer Service API

API REST de gestión de clientes desplegada en OpenShift con protección de Kuadrant AuthPolicy (API Key) y RateLimitPolicy.

## Acceso rápido

| Recurso | URL |
|---------|-----|
| **Swagger UI** | [nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/swagger-ui](https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/swagger-ui) |
| **API Base** | `https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers` |
| **Grafana** | [grafana-observability.apps.cluster-l9nhj.dynamic.redhatworkshops.io](https://grafana-observability.apps.cluster-l9nhj.dynamic.redhatworkshops.io) |

## Credenciales

### API Keys

| Nombre | Clave | Permisos |
|--------|-------|----------|
| Admin | `nfl-wallet-demo-key-2024` | Lectura y escritura completa |
| Readonly | `nfl-wallet-readonly-key-2024` | Solo lectura |

### Base de datos

| Campo | Valor |
|-------|-------|
| **Host** | `nfl-wallet-db.nfl-wallet-prod.svc.cluster.local` |
| **Puerto** | `5432` |
| **Base de datos** | `nflwallet` |
| **Usuario** | `nflwallet` |
| **Contraseña** | `nflwallet123!` |

### Grafana

| Campo | Valor |
|-------|-------|
| **Usuario** | `admin` |
| **Contraseña** | `openshift` |

## Uso con curl

```bash
# Listar clientes (paginado)
curl -H "X-API-Key: nfl-wallet-demo-key-2024" \
  https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers

# Obtener cliente por ID
curl -H "X-API-Key: nfl-wallet-demo-key-2024" \
  https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers/1

# Crear cliente
curl -X POST -H "X-API-Key: nfl-wallet-demo-key-2024" \
  -H "Content-Type: application/json" \
  -d '{"identificacion":"99-99999999-0","tipoIdentificacion":"CUIT","nombre":"Test","apellido":"User","email":"test@example.com","tipoCliente":"PERSONAL"}' \
  https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers

# Obtener score crediticio
curl -H "X-API-Key: nfl-wallet-demo-key-2024" \
  https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers/1/credit-score

# Resumen del cliente
curl -H "X-API-Key: nfl-wallet-demo-key-2024" \
  https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers/1/summary
```

## Namespaces

| Namespace | Propósito |
|-----------|-----------|
| `nfl-wallet-prod` | Ambiente de producción |
| `nfl-wallet-test` | Ambiente de testing |
