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

## Paso 4: Probar la API con API Key

### Sin API Key (401 Unauthorized)

```bash
curl -s https://nfl-wallet.YOUR_CLUSTER_DOMAIN/api/customers
```

Respuesta esperada:
```json
{"error":"Invalid or missing API key. Include header X-API-Key with a valid key."}
```

### Con API Key válida

```bash
curl -s -H "X-API-Key: nfl-wallet-demo-key-2024" \
  https://nfl-wallet.YOUR_CLUSTER_DOMAIN/api/customers | python3 -m json.tool
```

### Con API Key readonly

```bash
curl -s -H "X-API-Key: nfl-wallet-readonly-key-2024" \
  https://nfl-wallet.YOUR_CLUSTER_DOMAIN/api/customers | python3 -m json.tool
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
    https://nfl-wallet.YOUR_CLUSTER_DOMAIN/api/customers)
  echo "Request $i: HTTP $code"
done
```

Después de ~120 requests deberías empezar a ver respuestas `429 Too Many Requests`.

## Paso 6: Crear una nueva API Key

Como ejercicio, crea tu propia API Key:

```bash
oc create secret generic my-apikey-YOUR_USER \
  --from-literal=api_key=my-custom-key-$(date +%s) \
  -n nfl-wallet-prod

oc label secret my-apikey-YOUR_USER \
  app=nfl-wallet \
  kuadrant.io/apikey=true \
  authorino.kuadrant.io/managed-by=authorino \
  -n nfl-wallet-prod
```

Kuadrant detecta automáticamente el nuevo Secret. Prueba tu key:

```bash
MY_KEY=$(oc get secret my-apikey-YOUR_USER -n nfl-wallet-prod -o jsonpath='{.data.api_key}' | base64 -d)

curl -s -H "X-API-Key: $MY_KEY" \
  https://nfl-wallet.YOUR_CLUSTER_DOMAIN/api/customers | python3 -m json.tool
```

## Paso 7: Explorar el Swagger UI

1. Abre en el navegador:

```
https://nfl-wallet.YOUR_CLUSTER_DOMAIN/q/swagger-ui
```

2. Haz click en **Authorize** (icono de candado).
3. Ingresa la API Key: `nfl-wallet-demo-key-2024`
4. Click en **Authorize** y luego **Close**.
5. Prueba los endpoints directamente desde Swagger UI.

## Paso 8: Comparar con Neuralbank en Developer Hub

En **Developer Hub**, compara las dos APIs:

| Vista | Neuralbank | NFL Wallet |
|-------|-----------|------------|
| **API entity** | `neuralbank-api` | `nfl-wallet-api` |
| **Auth type** | OIDC (Keycloak) | API Key (X-API-Key) |
| **APIProduct** | No tiene | `nfl-wallet-api` (Published) |
| **Swagger** | Requiere login OIDC | Requiere API Key en header |
| **Grafana** | Dashboards compartidos | Dashboards compartidos |

La clave es que **Connectivity Link** soporta múltiples modelos de autenticación:
- **OIDCPolicy** para flujos interactivos (usuarios web)
- **AuthPolicy con API Key** para integraciones programáticas (M2M)

Ambos se integran con el mismo stack: Istio Gateway + HTTPRoute + Kuadrant policies.

## Diagrama del flujo API Key

```
Cliente/Script           Istio Gateway              Kuadrant/Authorino         nfl-wallet-api
  │                           │                            │                        │
  │  GET /api/customers       │                            │                        │
  │  X-API-Key: demo-key      │                            │                        │
  │──────────────────────────▶│                            │                        │
  │                           │  Validate API Key          │                        │
  │                           │───────────────────────────▶│                        │
  │                           │                            │  Match Secret labels   │
  │                           │                            │  app=nfl-wallet        │
  │                           │                            │  kuadrant.io/apikey    │
  │                           │  ✓ Key valid               │                        │
  │                           │◀───────────────────────────│                        │
  │                           │                            │                        │
  │                           │  Check Rate Limit          │                        │
  │                           │  (Limitador: 120/min)      │                        │
  │                           │  ✓ Within limit            │                        │
  │                           │                            │                        │
  │                           │  Forward request           │                        │
  │                           │───────────────────────────────────────────────────▶│
  │                           │                            │                        │
  │  200 OK (data)            │                            │                        │
  │◀──────────────────────────│◀───────────────────────────────────────────────────│
```

## Resumen

Has explorado el modelo de **API Key Auth** de Connectivity Link en NFL Wallet y lo has comparado con el modelo **OIDC** de Neuralbank. Has aprendido:
- Cómo `AuthPolicy` con `apiKey` usa Secrets de Kubernetes para autenticación
- Cómo las API Keys se gestionan con labels de Kuadrant/Authorino
- La diferencia entre autenticación interactiva (OIDC) y programática (API Key)
- Cómo crear y gestionar API Keys dinámicamente
- El rol del **APIProduct** en Developer Hub para publicar APIs consumibles
