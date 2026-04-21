---
layout: default
title: "Desplegar el backend con Software Templates"
nav_order: 5
---

En este módulo desplegarás **Neuralbank Backend** usando una Software Template en Red Hat Developer Hub. Al finalizar tendrás un repositorio en Gitea, una aplicación en Argo CD, un pipeline Tekton ejecutado y la API accesible en OpenShift.

## Prerrequisitos

- Acceso a Developer Hub con tu usuario (`YOUR_USER`) y contraseña `Welcome123!`.
- Permisos para crear componentes desde plantillas en el catálogo del workshop.

## Paso 1: Abrir la creación desde plantilla

1. Inicia sesión en Developer Hub.
2. En el menú principal, selecciona **Create** (o **Crear** según localización).
3. Busca la plantilla **Neuralbank: Backend API** (el nombre exacto puede incluir el prefijo `Neuralbank`).

```bash
Navegación: Developer Hub -> Create -> Software Template -> "Neuralbank: Backend API"
```

## Paso 2: Completar el formulario

Rellena los campos solicitados por la plantilla. Valores orientativos:

- **Nombre del componente / repositorio**: `neuralbank-backend`
- **Propietario (owner)**: tu entidad de catálogo; usa `YOUR_USER` (el mismo valor que tu usuario del taller). El **owner** define el namespace de despliegue: **`YOUR_USER-neuralbank`**.

```bash
name = neuralbank-backend
owner = YOUR_USER
→ namespace resultante: YOUR_USER-neuralbank
→ nombre único en catálogo: YOUR_USER-neuralbank-backend
→ aplicación ArgoCD: YOUR_USER-neuralbank-backend
```

El nombre del repositorio en Gitea sigue siendo `neuralbank-backend` (en la organización `ws-YOUR_USER`), pero el componente en el catálogo y la aplicación en ArgoCD usan el **nombre único** `YOUR_USER-neuralbank-backend` para evitar conflictos con otros participantes.

> **Warning:** No uses espacios en el nombre del repositorio si la plantilla no lo permite. Respeta mayúsculas/minúsculas si el pipeline o Argo CD las esperan fijas.

## Paso 3: Crear y esperar el scaffolding

1. Pulsa **Create** (o **Review** y luego **Create** si hay un paso de revisión).
2. Permanece en la pantalla de progreso hasta que todos los pasos del *scaffolder* finalicen:
   - **Generate Skeleton**: genera el código con los valores del formulario.
   - **Publish to Gitea**: crea el repositorio en la organización `ws-YOUR_USER`.
   - **Register in Catalog**: registra el componente con nombre `YOUR_USER-neuralbank-backend`.
   - **Create ArgoCD Application**: crea la aplicación `YOUR_USER-neuralbank-backend` en ArgoCD.
   - **Create Gitea Webhook**: configura el trigger para pipelines Tekton.
   - **Send Notification**: envía una notificación al usuario confirmando el despliegue.

3. Al finalizar, recibirás una **notificación in-app** (campana de notificaciones en Developer Hub) y un **email** en tu casilla de Mailpit confirmando la creación exitosa.

> **Note:** Gracias a la naming convention con prefijo de usuario, ya no hay conflictos de nombres si otro participante también crea `neuralbank-backend`; cada uno tiene su propio componente `YOUR_USER-neuralbank-backend`.

## Paso 4: Verificar el repositorio en Gitea

1. Abre la URL de **Gitea** de tu entorno.
2. Localiza el repositorio `neuralbank-backend` (o el nombre que indicaste).
3. Confirma que existen el código fuente (por ejemplo proyecto Java/Quarkus), carpeta de manifiestos, `tekton` y `catalog-info.yaml` si la plantilla los incluye.

```bash
Gitea -> Repositorios -> neuralbank-backend -> comprobar estructura (src/, manifests/, tekton/, devfile.yaml)
```

## Paso 5: Verificar la aplicación en Argo CD

1. Abre el dashboard de **Argo CD** del clúster.
2. Busca la **Application** `YOUR_USER-neuralbank-backend` (el nombre incluye tu usuario como prefijo).
3. Comprueba **Sync Status** y **Health**; si está *OutOfSync*, ejecuta **Sync** si tu rol lo permite.

> **Note:** El primer sync puede tardar mientras se crean namespaces, secretos o imágenes; refresca el árbol de recursos hasta ver Deployments y Services en verde.

## Paso 6: Validar el despliegue en OpenShift

1. Entra en la **OpenShift Console** con las mismas credenciales o las indicadas por el instructor.
2. Cambia al proyecto **`YOUR_USER-neuralbank`** donde se desplegó el backend.
3. En **Workloads -> Pods**, verifica que los pods del backend están **Running** y sin reinicios continuos.
4. En **Networking -> Routes** (o **Routes / Ingress** según versión), localiza la ruta HTTP(S) del servicio.

```bash
OpenShift Console -> Project: YOUR_USER-neuralbank -> Pods -> Estado Running
OpenShift Console -> Networking -> Route -> URL pública del backend
```

## Paso 7: Probar los endpoints de la API

Desde tu navegador o con `curl`, prueba los recursos expuestos por la API de demostración. Rutas típicas del taller:

- `/api/customers`
- `/api/credits`

```bash
curl -sk "https://YOUR_ROUTE_HOST/api/customers"
curl -sk "https://YOUR_ROUTE_HOST/api/credits"
```

Sustituye `YOUR_ROUTE_HOST` por el hostname que muestra la Route en OpenShift.

> **Note:** Si recibes redirecciones de autenticación o `401/403`, puede haber una política OIDC o RBAC delante; consulta con el instructor si el taller prevé acceso anónimo en estos endpoints.

## Paso 8: Confirmar el registro en el catálogo

1. Vuelve a **Developer Hub**.
2. Busca el componente **YOUR_USER-neuralbank-backend** en el **Catalog** (usa el filtro por owner o busca por nombre).
3. Abre la ficha y revisa:
   - Enlaces al repositorio en Gitea.
   - Pestaña **CI**: PipelineRuns de Tekton asociados al componente (gracias a la anotación `janus-idp.io/tekton`).
   - Pestaña **Topology**: vista gráfica de los recursos desplegados.
   - Pestaña **Kubernetes**: pods, events y estado del despliegue.
   - Pestaña **CD**: estado de sincronización en ArgoCD.
   - Relaciones con entidades **API** y **System** (`YOUR_USER-neuralbank`).

## Paso 9: Revisar la notificación recibida

1. Haz clic en el icono de **campana** (notificaciones) en la barra superior de Developer Hub.
2. Deberías ver una notificación con el título "Neuralbank Backend deployed successfully" indicando el componente creado y el namespace de despliegue.

## Resumen

Has recorrido el camino completo **Hub → plantilla → Gitea → Argo CD → Tekton → OpenShift → Notificación**, validando además la API en `/api/customers` y `/api/credits`. Cada componente usa un nombre único con prefijo de usuario para evitar colisiones en el entorno compartido. Este flujo es la base para los módulos de pipelines, frontend y Dev Spaces.

### Capturas de Pantalla

![Developer Hub — Creación de componente]({{ site.baseurl }}/assets/images/neuroface.png)

![Developer Hub — Catálogo y componentes]({{ site.baseurl }}/assets/images/neuroface2.png)
