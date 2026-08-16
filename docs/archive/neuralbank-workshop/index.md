---
layout: default
title: "From Zero To Hero with Red Hat Developer Hub"
nav_order: 1
has_children: false
---

---

## Bienvenido al Workshop

En este workshop aprenderás a utilizar **Red Hat Developer Hub** como portal de desarrollo self-service para construir, desplegar y gestionar aplicaciones en OpenShift.

### Qué vas a aprender

- Comprender la propuesta de valor de Red Hat Developer Hub
- Explorar la arquitectura: Developer Hub, ArgoCD, Tekton, DevSpaces, Gitea, Keycloak, Lightspeed
- Crear servicios backend y frontend usando **Software Templates** con naming convention multi-usuario
- Explorar pipelines automatizados (pestaña **CI** en Developer Hub), topology view y detalles de la aplicación
- Desplegar y configurar API Gateways con seguridad (OIDCPolicy, RateLimitPolicy)
- **Explorar Connectivity Link** con dos modelos de autenticación:
  - **OIDCPolicy** (Neuralbank) — autenticación interactiva con Keycloak
  - **AuthPolicy con API Key** (NFL Wallet) — autenticación M2M programática
- **Hands-on:** explorar las aplicaciones pre-desplegadas (Neuralbank overview + NFL Wallet API Key test desde la terminal)
- Actualizar código fuente usando **Red Hat OpenShift Dev Spaces** con CI/CD automatizado
- Utilizar **Red Hat Developer Lightspeed** como asistente de IA integrado en el portal
- Recibir **notificaciones** en tiempo real y por email sobre el estado de tus componentes

### El caso de negocio: Neuralbank

Neuralbank es una entidad financiera que necesita modernizar su stack tecnológico. Como desarrollador, vas a construir tres componentes:

1. **Customer Service MCP** - Un servidor MCP (Model Context Protocol) para atención al cliente
2. **Neuralbank Backend** - API REST para gestión de créditos
3. **Neuralbank Frontend** - Interfaz web para visualización de créditos

### Acceso al entorno

Tu usuario es `YOUR_USER`. La contraseña es `Welcome123!`.

Tu namespace de trabajo es **`YOUR_USER-neuralbank`**. Todos los servicios que crees con las Software Templates se desplegarán ahí. Los componentes en el catálogo usan un **nombre único** con prefijo de tu usuario (por ejemplo `YOUR_USER-neuralbank-backend`) para evitar conflictos entre participantes.

> **Note:** El atributo `YOUR_USER` se rellena automáticamente según tu inicio de sesión en OpenShift.

- **Developer Hub**: `https://backstage-developer-hub-developer-hub.YOUR_CLUSTER_DOMAIN`
- **Gitea**: `https://gitea-gitea.YOUR_CLUSTER_DOMAIN`
- **ArgoCD**: `https://openshift-gitops-server-openshift-gitops.YOUR_CLUSTER_DOMAIN`
- **DevSpaces**: `https://devspaces.YOUR_CLUSTER_DOMAIN`
- **Mailpit**: `https://n8n-mailpit-openshift-lightspeed.YOUR_CLUSTER_DOMAIN`
- **Grafana**: `https://grafana-observability.YOUR_CLUSTER_DOMAIN`
- **Kiali**: `https://kiali-openshift-cluster-observability-operator.YOUR_CLUSTER_DOMAIN`
- **Thanos Querier**: `https://thanos-querier.YOUR_CLUSTER_DOMAIN`
- **Lightspeed**: disponible en el menú lateral de Developer Hub (icono de chat IA)
- **Terminal Web**: disponible en el panel derecho del showroom (tab "Terminal") para ejecutar comandos `oc`, `curl`, etc.

### Interfaces del entorno

<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px;margin:16px 0;">
  <div class="screenshot-wrapper">
    <img src="{{ site.baseurl }}/assets/screenshots/05-hub-login.png" alt="Developer Hub Login">
    <div class="screenshot-caption">Developer Hub — Login OIDC</div>
  </div>
  <div class="screenshot-wrapper">
    <img src="{{ site.baseurl }}/assets/screenshots/02-gitea-dashboard.png" alt="Gitea Dashboard">
    <div class="screenshot-caption">Gitea — Dashboard</div>
  </div>
  <div class="screenshot-wrapper">
    <img src="{{ site.baseurl }}/assets/screenshots/03-argocd-apps.png" alt="ArgoCD Login">
    <div class="screenshot-caption">Argo CD — Login</div>
  </div>
  <div class="screenshot-wrapper">
    <img src="{{ site.baseurl }}/assets/screenshots/04-devspaces-login.png" alt="DevSpaces Login">
    <div class="screenshot-caption">Dev Spaces — Login OpenShift</div>
  </div>
</div>

> Hacé click en cualquier imagen para agrandarla.
