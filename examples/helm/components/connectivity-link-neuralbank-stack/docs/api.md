# API REST

Neuralbank expone una API REST de gestión de clientes construida con Quarkus. Todos los endpoints requieren autenticación OIDC (Bearer token de Keycloak).

## Base URL

```
https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io
```

## Autenticación

Todos los endpoints requieren un JWT token válido emitido por Keycloak:

```bash
TOKEN=$(curl -s -X POST \
  "https://rhbk.apps.cluster-l9nhj.dynamic.redhatworkshops.io/realms/neuralbank/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=neuralbank-frontend" \
  -d "username=user1" \
  -d "password=Welcome123!" | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
```

Ver [Autenticación OIDC](oidc-auth.md) para más detalles.

## Endpoints

### Customer Service (CRUD)

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/customers` | Listar clientes (paginado, con filtros) |
| `POST` | `/api/v1/customers` | Crear nuevo cliente |
| `GET` | `/api/v1/customers/{customerId}` | Obtener cliente por ID |
| `PUT` | `/api/v1/customers/{customerId}` | Actualizar cliente completo |
| `PATCH` | `/api/v1/customers/{customerId}` | Actualización parcial |
| `DELETE` | `/api/v1/customers/{customerId}` | Eliminar cliente (soft delete) |

### Búsqueda

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/customers?search=texto` | Buscar por nombre/apellido |
| `GET` | `/api/v1/customers/identification/{id}` | Buscar por documento |

### Credit Scoring

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/customers/{customerId}/credit-score` | Obtener score crediticio |
| `POST` | `/api/v1/customers/{customerId}/credit-score/calculate` | Recalcular score |
| `GET` | `/api/v1/customers/{customerId}/summary` | Resumen completo del cliente |

### Estado del cliente

| Método | Path | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/customers/{customerId}/activate` | Activar cliente |
| `POST` | `/api/v1/customers/{customerId}/deactivate` | Desactivar cliente |
| `POST` | `/api/v1/customers/{customerId}/block` | Bloquear cliente |
| `POST` | `/api/v1/customers/{customerId}/unblock` | Desbloquear cliente |

### Actualización de datos

| Método | Path | Descripción |
|--------|------|-------------|
| `PUT` | `/api/v1/customers/{customerId}/email` | Actualizar email |
| `PUT` | `/api/v1/customers/{customerId}/phone` | Actualizar teléfono |
| `PUT` | `/api/v1/customers/{customerId}/address` | Actualizar dirección |
| `PUT` | `/api/v1/customers/{customerId}/risk-level` | Actualizar nivel de riesgo |
| `PUT` | `/api/v1/customers/{customerId}/executive/{executiveId}` | Asignar ejecutivo |

### Metadata

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/customers/{customerId}/metadata` | Obtener metadata |
| `PUT` | `/api/v1/customers/{customerId}/metadata` | Actualizar metadata completa |
| `PATCH` | `/api/v1/customers/{customerId}/metadata/{key}` | Actualizar campo específico |

## Parámetros de búsqueda (GET /api/v1/customers)

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `search` | string | Texto libre (nombre, apellido) |
| `tipoCliente` | enum | `PERSONAL`, `EMPRESARIAL`, `CORPORATIVO` |
| `activo` | boolean | Filtrar por estado activo/inactivo |
| `ciudad` | string | Filtrar por ciudad |
| `nivelRiesgo` | string | `Bajo`, `Medio`, `Alto`, `Muy Alto` |
| `paisId` | integer | ID del país |
| `sucursalId` | integer | ID de sucursal |
| `ejecutivoId` | integer | ID de ejecutivo |
| `scoreMin` | number | Score crediticio mínimo |
| `scoreMax` | number | Score crediticio máximo |
| `page` | integer | Página (default: 0) |
| `size` | integer | Tamaño de página (default: 20) |

## Ejemplos con curl

### Listar clientes

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers" \
  | python3 -m json.tool
```

### Listar clientes con filtros

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers?tipoCliente=PERSONAL&ciudad=Buenos+Aires&page=0&size=5" \
  | python3 -m json.tool
```

### Crear cliente

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "identificacion": "99-88776655-0",
    "tipoIdentificacion": "CUIT",
    "nombre": "Test",
    "apellido": "Workshop",
    "email": "test.workshop@neuralbank.com",
    "telefono": "+54-11-5555-0001",
    "direccion": "Av. Corrientes 1234",
    "ciudad": "Buenos Aires",
    "estadoProvincia": "Capital Federal",
    "codigoPostal": "C1043",
    "paisId": 4,
    "tipoCliente": "PERSONAL"
  }' \
  "https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers" \
  | python3 -m json.tool
```

### Obtener score crediticio

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers/1/credit-score" \
  | python3 -m json.tool
```

### Resumen del cliente

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers/1/summary" \
  | python3 -m json.tool
```

## Swagger UI

La API expone una interfaz Swagger UI interactiva:

**URL**: [https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/swagger-ui](https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/swagger-ui)

Para autenticarse en Swagger UI, usar el flujo OIDC o inyectar el Bearer token manualmente.

## OpenAPI Spec

El spec OpenAPI completo se puede descargar:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/openapi
```

## Tipos de datos

### CustomerType

| Valor | Descripción |
|-------|-------------|
| `PERSONAL` | Cliente persona física |
| `EMPRESARIAL` | Empresa mediana |
| `CORPORATIVO` | Gran empresa |

### RiskLevel

| Valor | Descripción |
|-------|-------------|
| `Bajo` | Riesgo bajo (score > 700) |
| `Medio` | Riesgo medio (score 500-700) |
| `Alto` | Riesgo alto (score 300-500) |
| `Muy Alto` | Riesgo muy alto (score < 300) |

### IdentificationType

`DNI`, `PASSPORT`, `SSN`, `RFC`, `CURP`, `CPF`, `CUIT`, `RUT`, `RUN`, `NINO`, `CIF`, `EIN`, `TAX_ID`

## Rate Limiting

La API tiene un **RateLimitPolicy** de 10 requests por minuto por usuario autenticado en el path `/api/customers`. Si se excede, responde `429 Too Many Requests`.
