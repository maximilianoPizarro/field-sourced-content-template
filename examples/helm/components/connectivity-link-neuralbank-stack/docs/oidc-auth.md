# Autenticación OIDC

Neuralbank utiliza **Kuadrant OIDCPolicy** integrada con **Keycloak** para proteger la API REST con autenticación OpenID Connect.

## Cómo funciona

### OIDCPolicy

La OIDCPolicy está configurada en el namespace `neuralbank-stack` y apunta al `HTTPRoute`:

```yaml
apiVersion: extensions.kuadrant.io/v1alpha1
kind: OIDCPolicy
metadata:
  name: neuralbank-oidc
  namespace: neuralbank-stack
spec:
  auth:
    tokenSource:
      authorizationHeader:
        prefix: Bearer
      cookie:
        name: jwt
  provider:
    issuerURL: https://rhbk.<domain>/realms/neuralbank
    clientID: neuralbank-frontend
    authorizationEndpoint: https://rhbk.<domain>/realms/neuralbank/protocol/openid-connect/auth
    redirectURI: https://neuralbank.<domain>/auth/callback
    tokenEndpoint: https://rhbk.<domain>/realms/neuralbank/protocol/openid-connect/token
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: neuralbank-api-route
```

### Flujo de autenticación

La OIDCPolicy genera automáticamente AuthPolicies de Kuadrant que implementan:

1. **Validación JWT**: Verifica el token Bearer contra el issuer de Keycloak
2. **Redirect automático**: Si no hay token válido, redirige (302) a la página de login de Keycloak
3. **Cookie de sesión**: Almacena el JWT en una cookie `jwt` tras la autenticación exitosa
4. **Callback**: Maneja el redirect de vuelta desde Keycloak en `/auth/callback`

```
AuthPolicies generadas automáticamente:
├── neuralbank-oidc          → Valida JWT, redirige si falta
└── neuralbank-oidc-callback → Maneja /auth/callback de Keycloak
```

## Keycloak: Realm y Client

### Realm: neuralbank

| Campo | Valor |
|-------|-------|
| **Realm URL** | `https://rhbk.apps.cluster-l9nhj.dynamic.redhatworkshops.io/realms/neuralbank` |
| **Well-known** | `https://rhbk.apps.cluster-l9nhj.dynamic.redhatworkshops.io/realms/neuralbank/.well-known/openid-configuration` |

### Client: neuralbank-frontend

| Campo | Valor |
|-------|-------|
| **Client ID** | `neuralbank-frontend` |
| **Tipo** | Public (PKCE) |
| **Redirect URI** | `https://neuralbank.<domain>/auth/callback` |
| **Grant types** | Authorization Code (PKCE), Password |

### Usuarios

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `user1` … `user200` | `Welcome123!` | Usuario del workshop |

## Comparación: OIDC vs API Key

Neuralbank usa OIDC (interactivo), mientras que NFL Wallet usa API Key (programático):

| Aspecto | Neuralbank (OIDC) | NFL Wallet (API Key) |
|---------|-------------------|---------------------|
| **Policy** | OIDCPolicy | AuthPolicy |
| **Header** | `Authorization: Bearer <JWT>` | `X-API-Key: <key>` |
| **Sin auth** | Redirect 302 a login | Error 401 JSON |
| **Token source** | Keycloak emite JWT | Kubernetes Secrets |
| **Caso de uso** | Usuarios interactivos (web) | Integraciones M2M |
| **Rate limit** | Por `auth.identity.username` | Global |

## Acceso desde el navegador

1. Navegar a `https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io`
2. La OIDCPolicy redirige automáticamente a Keycloak
3. Ingresar credenciales (`user1` / `Welcome123!`)
4. Tras la autenticación, se redirige de vuelta a la app con un JWT en cookie

## Acceso programático (curl)

### Obtener token

```bash
TOKEN=$(curl -s -X POST \
  "https://rhbk.apps.cluster-l9nhj.dynamic.redhatworkshops.io/realms/neuralbank/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=neuralbank-frontend" \
  -d "username=user1" \
  -d "password=Welcome123!" | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

echo $TOKEN
```

### Usar el token

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers | python3 -m json.tool
```

### Sin token (redirect 302)

```bash
curl -v https://neuralbank.apps.cluster-l9nhj.dynamic.redhatworkshops.io/api/v1/customers 2>&1 | grep "< HTTP\|< location"
```

Resultado esperado: `302` redirect a la URL de login de Keycloak.

## RateLimitPolicy

Además de la autenticación, Neuralbank aplica un rate limit por usuario:

```yaml
apiVersion: kuadrant.io/v1
kind: RateLimitPolicy
metadata:
  name: neuralbank-customers-ratelimit
spec:
  targetRef:
    kind: HTTPRoute
    name: neuralbank-api-route
  limits:
    customers-api-per-user:
      counters:
        - expression: auth.identity.username
      rates:
        - limit: 10
          window: 1m
      when:
        - predicate: request.path.matches('/api/customers')
```

- **10 requests por minuto** por usuario autenticado
- El counter usa `auth.identity.username` del JWT
- Solo aplica al path `/api/customers`

## Inspeccionar en OpenShift

```bash
# Ver OIDCPolicy
oc get oidcpolicy -n neuralbank-stack

# Ver AuthPolicies generadas
oc get authpolicy -n neuralbank-stack

# Ver RateLimitPolicy
oc get ratelimitpolicy -n neuralbank-stack

# Ver Gateway y HTTPRoutes
oc get gateway,httproute -n neuralbank-stack
```

## Errores comunes

| Código | Causa | Solución |
|--------|-------|----------|
| `302` | Sin token / token expirado | Obtener nuevo token de Keycloak |
| `401` | Token JWT inválido | Verificar issuer y client_id |
| `429` | Rate limit excedido | Esperar 1 minuto o usar otro usuario |
| `403` | Permisos insuficientes | Verificar roles del usuario en Keycloak |
