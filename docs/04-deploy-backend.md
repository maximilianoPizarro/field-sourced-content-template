---
layout: default
title: "Desplegar el backend con Software Templates"
nav_order: 5
---

En este módulo desplegarás **Neuralbank Backend** usando una Software Template en Red Hat Developer Hub. Al finalizar tendrás un repositorio en Gitea, una aplicación en Argo CD, un pipeline Tekton ejecutado y la API accesible en OpenShift.

## Prerrequisitos

- Acceso a Developer Hub con tu usuario (`<user_name>`) y contraseña `Welcome123!`.
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
- **Propietario (owner)**: tu entidad de catálogo; usa `<user_name>` (el mismo valor que tu usuario del taller).

```bash
name = neuralbank-backend
owner = <user_name>
```

> **Warning:** No uses espacios en el nombre del repositorio si la plantilla no lo permite. Respeta mayúsculas/minúsculas si el pipeline o Argo CD las esperan fijas.

## Paso 3: Crear y esperar el scaffolding

1. Pulsa **Create** (o **Review** y luego **Create** si hay un paso de revisión).
2. Permanece en la pantalla de progreso hasta que todos los pasos del *scaffolder* finalicen (publicación en Git, registro en catálogo, etc.).

> **Note:** Si un paso falla, copia el mensaje de error y revisa permisos en Gitea o cuotas de namespace; en entornos compartidos a veces hay conflictos de nombres si otro usuario ya creó `neuralbank-backend`.

## Paso 4: Verificar el repositorio en Gitea

1. Abre la URL de **Gitea** de tu entorno.
2. Localiza el repositorio `neuralbank-backend` (o el nombre que indicaste).
3. Confirma que existen el código fuente (por ejemplo proyecto Java/Quarkus), carpeta de manifiestos, `tekton` y `catalog-info.yaml` si la plantilla los incluye.

```bash
Gitea -> Repositorios -> neuralbank-backend -> comprobar estructura (src/, manifests/, tekton/, devfile.yaml)
```

## Paso 5: Verificar la aplicación en Argo CD

1. Abre el dashboard de **Argo CD** del clúster.
2. Busca una **Application** asociada al backend (nombre alineado con el componente o el namespace del workshop).
3. Comprueba **Sync Status** y **Health**; si está *OutOfSync*, ejecuta **Sync** si tu rol lo permite.

> **Note:** El primer sync puede tardar mientras se crean namespaces, secretos o imágenes; refresca el árbol de recursos hasta ver Deployments y Services en verde.

## Paso 6: Validar el despliegue en OpenShift

1. Entra en la **OpenShift Console** con las mismas credenciales o las indicadas por el instructor.
2. Cambia al **namespace** donde se desplegó el backend (según la convención del workshop).
3. En **Workloads -> Pods**, verifica que los pods del backend están **Running** y sin reinicios continuos.
4. En **Networking -> Routes** (o **Routes / Ingress** según versión), localiza la ruta HTTP(S) del servicio.

```bash
OpenShift Console -> Project (namespace del taller) -> Pods -> Estado Running
OpenShift Console -> Networking -> Route -> URL pública del backend
```

## Paso 7: Probar los endpoints de la API

Desde tu navegador o con `curl`, prueba los recursos expuestos por la API de demostración. Rutas típicas del taller:

- `/api/customers`
- `/api/credits`

```bash
curl -sk "https://<host-del-route>/api/customers"
curl -sk "https://<host-del-route>/api/credits"
```

Sustituye `<host-del-route>` por el hostname que muestra la Route en OpenShift.

> **Note:** Si recibes redirecciones de autenticación o `401/403`, puede haber una política OIDC o RBAC delante; consulta con el instructor si el taller prevé acceso anónimo en estos endpoints.

## Paso 8: Confirmar el registro en el catálogo

1. Vuelve a **Developer Hub**.
2. Busca el componente **neuralbank-backend** en el **Catalog**.
3. Abre la ficha y revisa enlaces al repositorio, documentación y relaciones (por ejemplo entidad **API**).

## Resumen

Has recorrido el camino completo **Hub → plantilla → Gitea → Argo CD → Tekton → OpenShift**, validando además la API en `/api/customers` y `/api/credits`. Este flujo es la base para los módulos de pipelines, frontend y Dev Spaces.
