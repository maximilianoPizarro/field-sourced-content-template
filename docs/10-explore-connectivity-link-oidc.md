---
layout: default
title: "Explorar Connectivity Link: OIDC Policy (Neuralbank)"
nav_order: 10
---

En este módulo explorarás cómo **Red Hat Connectivity Link** protege la API de Neuralbank con **OIDCPolicy** integrada con Keycloak, junto con **RateLimitPolicy** para control de tráfico.

## Contexto

Neuralbank utiliza el patrón completo de Connectivity Link para exponer su API REST de forma segura:

| Recurso | Función |
|---------|---------|
| **Gateway** (Istio) | Punto de entrada HTTPS al servicio |
| **HTTPRoute** | Enruta `/api` y `/q` al backend Quarkus |
| **OIDCPolicy** | Autenticación OIDC con Keycloak (Bearer token) |
| **RateLimitPolicy** | Límite de 60 req/min por usuario autenticado |

## Paso 1: Inspeccionar el Gateway en OpenShift

1. Abre la **OpenShift Console** y selecciona el proyecto **`neuralbank-stack`**.
2. En la terminal web (o con `oc`), lista el Gateway:

```bash
oc get gateway -n neuralbank-stack
```

Resultado esperado:

```
NAME                 CLASS   ADDRESS                                          PROGRAMMED
neuralbank-gateway   istio   neuralbank-gateway-istio.neuralbank-stack.svc    True
```

3. Inspecciona los listeners:

```bash
oc get gateway neuralbank-gateway -n neuralbank-stack -o jsonpath='{.spec.listeners[*].name}' ; echo
```

El Gateway tiene listeners `http` (8080) y `https` (443) con `allowedRoutes: from: All`.

## Paso 2: Explorar la HTTPRoute

```bash
oc get httproute -n neuralbank-stack
```

La HTTPRoute `neuralbank-api-route` enruta las requests al servicio `neuralbank-backend-svc:8080` para los paths `/api` y `/q`:

```bash
oc get httproute neuralbank-api-route -n neuralbank-stack -o yaml | grep -A5 "matches:" | head -15
```

## Paso 3: Explorar la OIDCPolicy

La **OIDCPolicy** es el recurso clave de Connectivity Link que protege la HTTPRoute con autenticación OIDC:

```bash
oc get oidcpolicy -n neuralbank-stack
```

Inspecciona la configuración:

```bash
oc get oidcpolicy neuralbank-oidc -n neuralbank-stack -o yaml
```

Puntos clave de la configuración:

- **`provider.issuerURL`**: apunta al realm de Keycloak (`https://rhbk.<CLUSTER_DOMAIN>/realms/neuralbank`)
- **`provider.clientID`**: el cliente OIDC configurado en Keycloak (`backstage`)
- **`auth.tokenSource`**: extrae el Bearer token del header `Authorization`
- **`targetRef`**: apunta al HTTPRoute `neuralbank-api-route`

### Cómo funciona internamente

La OIDCPolicy genera automáticamente un `AuthPolicy` de Kuadrant bajo el capó:

```bash
oc get authpolicy -n neuralbank-stack
```

Verás dos AuthPolicies generadas:

| AuthPolicy | Función |
|-----------|---------|
| `neuralbank-oidc` | Valida el JWT token contra Keycloak |
| `neuralbank-oidc-callback` | Maneja el flujo de redirect para el callback OIDC |

La AuthPolicy `neuralbank-oidc` configura:
- Validación JWT contra el issuer de Keycloak
- Redirect automático a la página de login si no hay token válido (código 302)
- Cookie `target` para recordar la URL original

## Paso 4: Explorar la RateLimitPolicy

```bash
oc get ratelimitpolicy -n neuralbank-stack
```

Inspecciona los límites:

```bash
oc get ratelimitpolicy neuralbank-customers-ratelimit -n neuralbank-stack -o yaml
```

Configuración:
- **60 requests por minuto** por usuario autenticado
- El counter usa `auth.identity.username` para aislar el rate limit por usuario OIDC

## Paso 5: Probar el flujo OIDC desde el navegador

1. Abre la URL de Neuralbank en el navegador:

```
https://neuralbank.<CLUSTER_DOMAIN>
```

2. Serás redirigido automáticamente a la pantalla de login de Keycloak (realm `neuralbank`).
3. Ingresa tus credenciales de workshop (`user1` / `Welcome123!`).
4. Tras la autenticación, serás redirigido de vuelta a la API.

## Paso 6: Probar con curl — Obtener Bearer Token y consumir la API

### 6.1 — Request sin token (redirect 302)

Primero verificamos que la API rechaza requests sin autenticación:

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers
```

Resultado esperado: `HTTP Status: 302` (redirect a Keycloak login).

### 6.2 — Obtener un Bearer Token de Keycloak

Usa el flujo **Resource Owner Password Credentials** para obtener un JWT token:

```bash
TOKEN=$(curl -s -X POST \
  "https://rhbk.<CLUSTER_DOMAIN>/realms/neuralbank/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=neuralbank-frontend" \
  -d "username=user1" \
  -d "password=Welcome123!" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

echo "Token obtenido (primeros 50 chars): ${TOKEN:0:50}..."
```

### 6.3 — Listar todos los clientes

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

### 6.4 — Consultar un cliente por ID

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers/1" \
  | python3 -m json.tool
```

### 6.5 — Consultar el resumen de un cliente

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers/1/summary" \
  | python3 -m json.tool
```

### 6.6 — Consultar el credit score

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers/1/credit-score" \
  | python3 -m json.tool
```

### 6.7 — Crear un nuevo cliente

```bash
curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Workshop",
    "apellido": "Demo",
    "email": "workshop.demo@neuralbank.io",
    "tipoCliente": "PERSONAL",
    "ciudad": "Buenos Aires",
    "pais": "Argentina"
  }' \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

### 6.8 — Verificar que un token inválido es rechazado

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer token-invalido-12345" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers"
```

Resultado esperado: `HTTP Status: 302` o `401` (token inválido rechazado).

## Paso 7: Ver en Developer Hub

1. Abre **Developer Hub** y navega al componente **neuralbank-stack** en el catálogo.
2. En la pestaña **Topology**, observa los pods del backend conectados al Gateway.
3. En la pestaña **API**, verifica el spec OpenAPI de la API de Neuralbank.
4. En la ficha del **System** `neuralbank-system`, ve las relaciones entre backend, frontend y database.

## Diagrama del flujo

```
Usuario                Keycloak               Istio Gateway           Kuadrant              Backend
  │                      │                       │                      │                     │
  │  GET /api/customers  │                       │                      │                     │
  │─────────────────────────────────────────────▶│                      │                     │
  │                      │                       │  Validate JWT        │                     │
  │                      │                       │─────────────────────▶│                     │
  │                      │                       │                      │  No token → 302     │
  │  302 → Login page    │                       │◀─────────────────────│                     │
  │◀─────────────────────────────────────────────│                      │                     │
  │                      │                       │                      │                     │
  │  Login + credentials │                       │                      │                     │
  │─────────────────────▶│                       │                      │                     │
  │  Token               │                       │                      │                     │
  │◀─────────────────────│                       │                      │                     │
  │                      │                       │                      │                     │
  │  GET /api + Bearer   │                       │                      │                     │
  │─────────────────────────────────────────────▶│  Validate JWT ✓      │                     │
  │                      │                       │─────────────────────▶│                     │
  │                      │                       │  Check rate limit    │                     │
  │                      │                       │  (60/min per user)   │                     │
  │                      │                       │  ✓ Within limit      │                     │
  │                      │                       │                      │                     │
  │  200 OK (customers)  │                       │  Forward             │                     │
  │◀─────────────────────────────────────────────│──────────────────────────────────────────▶│
```

## Resumen

Has explorado el stack completo de **Connectivity Link con OIDCPolicy** en Neuralbank:
- **Gateway** Istio como punto de entrada
- **HTTPRoute** para enrutamiento de paths
- **OIDCPolicy** que integra autenticación OIDC con Keycloak
- **RateLimitPolicy** con límite por usuario autenticado
- Flujo de redirect automático y validación JWT
