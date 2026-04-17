# Autenticación API Key

NFL Wallet utiliza **Kuadrant AuthPolicy** para proteger los endpoints de la API con autenticación basada en API Key.

## Cómo funciona

### AuthPolicy

La AuthPolicy está configurada en el namespace `nfl-wallet-prod` y apunta al `HTTPRoute`:

```yaml
apiVersion: kuadrant.io/v1
kind: AuthPolicy
metadata:
  name: nfl-wallet-apikey
  namespace: nfl-wallet-prod
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: nfl-wallet-route
  defaults:
    rules:
      authentication:
        api-key-auth:
          apiKey:
            selector:
              matchLabels:
                app: nfl-wallet
                kuadrant.io/apikey: "true"
          credentials:
            customHeader:
              name: X-API-Key
```

### Almacenamiento de API Keys

Las API keys se almacenan como Kubernetes `Secrets` con labels específicos:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: nfl-wallet-apikey-admin
  namespace: nfl-wallet-prod
  labels:
    app: nfl-wallet
    kuadrant.io/apikey: "true"
    authorino.kuadrant.io/managed-by: authorino
type: Opaque
stringData:
  api_key: nfl-wallet-demo-key-2024
```

Kuadrant/Authorino detecta automáticamente los Secrets con el label `kuadrant.io/apikey: "true"` y los usa para validar las requests.

## API Keys disponibles

| Key | Valor | Uso |
|-----|-------|-----|
| **Admin** | `nfl-wallet-demo-key-2024` | Acceso completo (CRUD) |
| **Readonly** | `nfl-wallet-readonly-key-2024` | Solo lectura (GET) |

## Cómo usar la API Key

### Header HTTP

Enviar la API key en el header `X-API-Key`:

```bash
curl -H "X-API-Key: nfl-wallet-demo-key-2024" \
  https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers
```

### En Swagger UI

1. Abrir [Swagger UI](https://nfl-wallet.apps.cluster-l9nhj.dynamic.redhatworkshops.io/q/swagger-ui)
2. Hacer click en **Authorize** (icono de candado)
3. Ingresar la API key: `nfl-wallet-demo-key-2024`
4. Click en **Authorize** y luego **Close**
5. Todas las requests incluirán automáticamente el header `X-API-Key`

## Rate Limiting

Además de la autenticación, la API tiene un **RateLimitPolicy** global:

- **Límite**: 120 requests por minuto
- **Scope**: Global (todas las API keys comparten el límite)

Si se excede el límite, el gateway responde con `429 Too Many Requests`.

## Crear nuevas API Keys

Para crear una nueva API key:

```bash
oc create secret generic my-new-apikey \
  --from-literal=api_key=my-custom-key-value \
  -n nfl-wallet-prod

oc label secret my-new-apikey \
  app=nfl-wallet \
  kuadrant.io/apikey=true \
  authorino.kuadrant.io/managed-by=authorino \
  -n nfl-wallet-prod
```

Kuadrant detectará automáticamente el nuevo Secret y habilitará la key.

## Errores comunes

| Código | Mensaje | Causa |
|--------|---------|-------|
| `401` | `Invalid or missing API key` | Header `X-API-Key` faltante o key inválida |
| `429` | `Too Many Requests` | Se excedió el rate limit de 120 req/min |
| `403` | `Forbidden` | Key válida pero sin permisos para la operación |
