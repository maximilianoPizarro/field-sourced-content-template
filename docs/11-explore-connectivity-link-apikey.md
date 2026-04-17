---
layout: default
title: "Explorar Connectivity Link: API Key Auth (NFL Wallet)"
nav_order: 11
---

En este módulo explorarás cómo **Red Hat Connectivity Link** protege la API de NFL Wallet con **AuthPolicy** basada en **API Key**, un modelo diferente al OIDC de Neuralbank, ideal para integraciones máquina-a-máquina (M2M) y consumo programático de APIs.

## Contexto: OIDC vs API Key

| Aspecto | Neuralbank (OIDC) | NFL Wallet (API Key) |
|---------|-------------------|---------------------|
| **Tipo de auth** | Token JWT (Bearer) | API Key (header) |
| **Flujo** | Redirect a login page | Sin redirect, key estática |
| **Caso de uso** | Usuarios interactivos (web) | Integraciones M2M, scripts |
| **Header** | `Authorization: Bearer <token>` | `X-API-Key: <key>` |
| **Gestión de keys** | Keycloak emite tokens | Secrets de Kubernetes |
| **Rate limit** | Por usuario autenticado | Global (todas las keys) |

## Paso 1: Explorar el APIProduct en Developer Hub

1. Abre **Developer Hub** y navega a **APIs** en el menú lateral.
2. Busca **NFL Wallet API** en la lista.
3. Observa que está vinculado a un **APIProduct** de Kuadrant con estado **Published**.

El APIProduct permite a los desarrolladores:
- Ver la documentación de la API
- Solicitar una API Key para consumir el servicio
- Ver el OpenAPI spec directamente en Developer Hub

## Paso 2: Inspeccionar los recursos en OpenShift

En la terminal, explora los recursos de Connectivity Link en el namespace `nfl-wallet-prod`:

```bash
echo "=== Gateway ===" && \
oc get gateway -n nfl-wallet-prod && echo && \
echo "=== HTTPRoute ===" && \
oc get httproute -n nfl-wallet-prod && echo && \
echo "=== AuthPolicy ===" && \
oc get authpolicy -n nfl-wallet-prod && echo && \
echo "=== RateLimitPolicy ===" && \
oc get ratelimitpolicy -n nfl-wallet-prod && echo && \
echo "=== APIProduct ===" && \
oc get apiproduct -n nfl-wallet-prod
```

## Paso 3: Explorar la AuthPolicy con API Key

A diferencia de la OIDCPolicy de Neuralbank, NFL Wallet usa directamente un `AuthPolicy` con autenticación por API Key:

```bash
oc get authpolicy nfl-wallet-apikey -n nfl-wallet-prod -o yaml
```

Puntos clave:

- **`authentication.api-key-auth.apiKey.selector`**: selecciona Secrets con labels `app: nfl-wallet` y `kuadrant.io/apikey: "true"`
- **`credentials.customHeader.name: X-API-Key`**: la key se envía en el header `X-API-Key`
- **`unauthenticated.code: 401`**: responde 401 si la key es inválida (no redirect)

### Cómo se almacenan las API Keys

Las keys son Kubernetes Secrets con labels especiales que Kuadrant/Authorino detecta automáticamente:

```bash
oc get secrets -n nfl-wallet-prod -l kuadrant.io/apikey=true
```

```bash
oc get secret nfl-wallet-apikey-admin -n nfl-wallet-prod -o jsonpath='{.data.api_key}' | base64 -d ; echo
```

Deberías ver: `nfl-wallet-demo-key-2024`

## Paso 4: Probar la API con curl

### 4.1 — Sin API Key (401 Unauthorized)

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers
```

Resultado esperado: `HTTP Status: 401`

```bash
curl -s https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers
```

Respuesta esperada:
```json
{"error":"Invalid or missing API key. Include header X-API-Key with a valid key."}
```

### 4.2 — Con API Key válida: Listar clientes

```bash
curl -s -H "X-API-Key: nfl-wallet-demo-key-2024" \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

### 4.3 — Consultar un cliente por ID

```bash
curl -s -H "X-API-Key: nfl-wallet-demo-key-2024" \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers/1" \
  | python3 -m json.tool
```

### 4.4 — Consultar el credit score de un cliente

```bash
curl -s -H "X-API-Key: nfl-wallet-demo-key-2024" \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers/1/credit-score" \
  | python3 -m json.tool
```

### 4.5 — Crear un nuevo cliente

```bash
curl -s -X POST \
  -H "X-API-Key: nfl-wallet-demo-key-2024" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "API Key",
    "apellido": "Test",
    "email": "apikey.test@wallet.io",
    "tipoCliente": "EMPRESA",
    "ciudad": "Miami",
    "pais": "USA"
  }' \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

### 4.6 — Con API Key inválida (401)

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -H "X-API-Key: clave-invalida-12345" \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers"
```

Resultado esperado: `HTTP Status: 401`

### 4.7 — Con API Key readonly

```bash
curl -s -H "X-API-Key: nfl-wallet-readonly-key-2024" \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

## Paso 5: Explorar el Rate Limiting

La RateLimitPolicy aplica un límite global de 120 requests por minuto:

```bash
oc get ratelimitpolicy nfl-wallet-ratelimit -n nfl-wallet-prod -o yaml
```

Prueba exceder el límite (en la terminal):

```bash
for i in $(seq 1 130); do
  code=$(curl -s -o /dev/null -w '%{http_code}' \
    -H "X-API-Key: nfl-wallet-demo-key-2024" \
    "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers")
  echo "Request $i: HTTP $code"
done
```

Después de ~120 requests deberías empezar a ver respuestas `429 Too Many Requests`.

## Paso 6: Crear una nueva API Key

Como ejercicio, crea tu propia API Key:

```bash
oc create secret generic my-apikey-user1 \
  --from-literal=api_key=my-custom-key-$(date +%s) \
  -n nfl-wallet-prod

oc label secret my-apikey-user1 \
  app=nfl-wallet \
  kuadrant.io/apikey=true \
  authorino.kuadrant.io/managed-by=authorino \
  -n nfl-wallet-prod
```

Kuadrant detecta automáticamente el nuevo Secret. Prueba tu key:

```bash
MY_KEY=$(oc get secret my-apikey-user1 -n nfl-wallet-prod -o jsonpath='{.data.api_key}' | base64 -d)

curl -s -H "X-API-Key: $MY_KEY" \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

## Paso 7: Explorar el Swagger UI

1. Abre en el navegador:

```
https://nfl-wallet.<CLUSTER_DOMAIN>/q/swagger-ui
```

2. Haz click en **Authorize** (icono de candado).
3. Ingresa la API Key: `nfl-wallet-demo-key-2024`
4. Click en **Authorize** y luego **Close**.
5. Prueba los endpoints directamente desde Swagger UI.

## Paso 8: PlanPolicy — Planes de consumo (free/basic/pro)

Cada API tiene un **PlanPolicy** que define planes de rate limiting por tier. Inspecciona el PlanPolicy de NFL Wallet:

```bash
oc get planpolicy -n nfl-wallet-prod -o yaml
```

Los 3 planes disponibles:

| Plan | Límite diario | Límite por minuto | Caso de uso |
|------|--------------|-------------------|-------------|
| **free** | 100 req/día | 10 req/min | Evaluación y pruebas |
| **basic** | 1,000 req/día | 60 req/min | Aplicaciones en desarrollo |
| **pro** | 10,000 req/día | 300 req/min | Producción y alta demanda |

El plan se asigna según la annotation `secret.kuadrant.io/plan-id` en el Secret de la API Key:

```yaml
metadata:
  labels:
    app: nfl-wallet
    kuadrant.io/apikey: "true"
  annotations:
    secret.kuadrant.io/plan-id: "basic"  # free | basic | pro
```

Puedes ver los planes disponibles en la pestaña **API** de la entidad en Developer Hub:

![API Product con tiers](screenshots/15-nfl-wallet-api-tab.png)

## Paso 9: APIProduct en Developer Hub

El **APIProduct** es el recurso que conecta la API con el Dev Portal de Kuadrant, permitiendo a los developers descubrir y solicitar acceso.

1. En Developer Hub, navega al menú **Kuadrant** en la barra lateral.
2. Verás todos los API Products publicados:

![Kuadrant API Products](screenshots/12-kuadrant-apiproducts.png)

3. Haz click en cualquier API Product para ver:
   - Nombre, descripción y tags
   - HTTPRoute y namespace asociados
   - Planes disponibles (tiers)
   - Botón **Request API Access** para generar una API Key

### Annotations que vinculan API Entity con APIProduct

Para que el plugin Kuadrant pueda vincular un API Product con la entidad API en el catálogo, la entidad necesita estas annotations:

```yaml
apiVersion: backstage.io/v1alpha1
kind: API
metadata:
  name: nfl-wallet-api
  annotations:
    kuadrant.io/namespace: nfl-wallet-prod
    kuadrant.io/apiproduct: nfl-wallet-api
    kuadrant.io/httproute: nfl-wallet-api-route
```

Sin estas annotations, el plugin muestra "No APIProduct linked to this API entity".

## Paso 10: Automatización en el Scaffolder

Cuando un usuario crea una nueva aplicación desde el Software Template, el scaffolder genera automáticamente todos los recursos de Kuadrant. Cada template incluye estos manifiestos:

| Manifiesto | Recurso | Descripción |
|------------|---------|-------------|
| `gateway.yaml` | Gateway | Gateway Istio con annotation `kuadrant.io/namespace` |
| `httproute.yaml` | HTTPRoute | Rutas HTTP para los endpoints de la API |
| `authpolicy.yaml` | AuthPolicy | Autenticación por API Key (`X-API-Key` header) |
| `ratelimitpolicy.yaml` | RateLimitPolicy | Rate limiting global (120 req/min) |
| `apiproduct.yaml` | APIProduct | Published, aprobación automática, tags y docs |
| `planpolicy.yaml` | PlanPolicy | 3 planes: free, basic, pro |

El `catalog-info.yaml` del skeleton incluye las annotations de Kuadrant:

```yaml
apiVersion: backstage.io/v1alpha1
kind: API
metadata:
  name: ${{values.uniqueName}}-api
  annotations:
    kuadrant.io/namespace: ${{values.namespace}}
    kuadrant.io/apiproduct: ${{values.uniqueName}}-api
    kuadrant.io/httproute: ${{values.name}}-route
```

Y el OpenAPI spec incluye el security scheme para Swagger UI:

```yaml
components:
  securitySchemes:
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
security:
  - ApiKeyAuth: []
```

### Flujo completo automatizado

```
Usuario en Developer Hub
  -> Selecciona Software Template (backend o MCP)
    -> fetch:template genera el skeleton con:
        manifests/gateway.yaml         (Istio Gateway)
        manifests/httproute.yaml       (HTTPRoute)
        manifests/authpolicy.yaml      (API Key auth)
        manifests/ratelimitpolicy.yaml (Rate limiting)
        manifests/apiproduct.yaml      (Dev Portal listing)
        manifests/planpolicy.yaml      (free/basic/pro tiers)
        catalog-info.yaml              (con kuadrant.io annotations)
    -> publish:gitea -> pushea a Gitea
    -> catalog:register -> registra en catalogo con annotations
    -> ArgoCD sincroniza -> despliega todos los manifiestos
    -> Kuadrant plugin detecta el APIProduct
    -> El usuario puede solicitar API Keys desde Developer Hub
```

## Paso 11: Comparar con Neuralbank en Developer Hub

En **Developer Hub**, compara las dos APIs:

| Vista | Neuralbank | NFL Wallet |
|-------|-----------|------------|
| **API entity** | `neuralbank-api` | `nfl-wallet-api` |
| **Auth type** | OIDC + API Key | API Key (X-API-Key) |
| **APIProduct** | `neuralbank-api` (Published) | `nfl-wallet-api` (Published) |
| **PlanPolicy** | free/basic/pro | free/basic/pro |
| **Swagger** | Requiere login OIDC | Requiere API Key en header |

![nfl-wallet-api en Developer Hub](screenshots/14-nfl-wallet-api-detail.png)

**Connectivity Link** soporta múltiples modelos de autenticación:
- **OIDCPolicy** para flujos interactivos (usuarios web)
- **AuthPolicy con API Key** para integraciones programáticas (M2M)

Ambos se integran con el mismo stack: Istio Gateway + HTTPRoute + Kuadrant policies.

## Diagrama del flujo API Key con PlanPolicy

```
Cliente/Script           Istio Gateway              Kuadrant/Authorino         Backend API
  |                           |                            |                        |
  |  GET /api/v1/customers    |                            |                        |
  |  X-API-Key: demo-key      |                            |                        |
  |-------------------------->|                            |                        |
  |                           |  Validate API Key          |                        |
  |                           |--------------------------->|                        |
  |                           |                            |  Match Secret labels   |
  |                           |                            |  app=nfl-wallet        |
  |                           |                            |  kuadrant.io/apikey    |
  |                           |  Key valid                 |                        |
  |                           |<---------------------------|                        |
  |                           |                            |                        |
  |                           |  Check PlanPolicy tier     |                        |
  |                           |  (plan-id annotation)      |                        |
  |                           |  Check Rate Limit          |                        |
  |                           |  (free: 10/min, basic:     |                        |
  |                           |   60/min, pro: 300/min)    |                        |
  |                           |  Within limit              |                        |
  |                           |                            |                        |
  |                           |  Forward request           |                        |
  |                           |--------------------------------------------------->|
  |                           |                            |                        |
  |  200 OK (data)            |                            |                        |
  |<--------------------------|<---------------------------------------------------|
```

## Resumen

Has explorado el modelo de **API Key Auth** de Connectivity Link y lo has comparado con el modelo **OIDC** de Neuralbank. Has aprendido:
- Como `AuthPolicy` con `apiKey` usa Secrets de Kubernetes para autenticacion
- Como las API Keys se gestionan con labels de Kuadrant/Authorino
- Como **PlanPolicy** define planes de consumo (free/basic/pro) con rate limits diferenciados
- Como **APIProduct** publica la API en el Dev Portal para que los developers soliciten acceso
- Como las **annotations `kuadrant.io/*`** vinculan la entidad API del catalogo con el APIProduct
- Como el **Scaffolder automatiza** la creacion de todos los recursos de Kuadrant (AuthPolicy, APIProduct, PlanPolicy)
- La diferencia entre autenticacion interactiva (OIDC) y programatica (API Key)
