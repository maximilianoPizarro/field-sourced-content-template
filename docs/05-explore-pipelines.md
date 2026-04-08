---
layout: default
title: "Explorar pipelines y Topology"
nav_order: 6
---

Tras desplegar **neuralbank-backend**, conviene inspeccionar cómo **Tekton** materializó el CI/CD y cómo **OpenShift** representa la aplicación en la vista de topología. Este módulo guía esa exploración y enlaza con el **Software Catalog** de Developer Hub.

## Abrir la consola de Pipelines

1. Inicia sesión en la **OpenShift Console**.
2. Selecciona el proyecto **`<user_name>-neuralbank`**.
3. En el menú lateral, ve a **Pipelines** (o **CI/CD -> Pipelines** según la versión de la consola).

Aquí verás la definición del **Pipeline** y los **PipelineRuns** recientes generados al crear o actualizar el repositorio.

## Localizar el PipelineRun de neuralbank-backend

1. En la pestaña **PipelineRuns**, ordena por **Started** (más reciente primero).
2. Identifica una ejecución cuyo nombre o etiquetas referencien `neuralbank-backend` o el repositorio en Gitea.

> **Note:** Si no ves ejecuciones, confirma que el trigger o el PipelineRun inicial se creó desde la plantilla; puede ser necesario un push al repo para disparar el pipeline según la configuración del taller.

## Revisar las tareas del pipeline

```mermaid
graph LR
    GC["git-clone<br/>📥 Clonar repo"] --> MB["maven-build<br/>🔨 Compilar"]
    MB --> BI["build-image<br/>🐳 Buildah"]
    BI --> DP["deploy<br/>🚀 Rollout"]

    style GC fill:#0066CC,color:#fff
    style MB fill:#EE0000,color:#fff
    style BI fill:#6a1b9a,color:#fff
    style DP fill:#609926,color:#fff
```

Abre el **PipelineRun** y examina la lista de **Tasks** en orden. Un flujo típico del taller incluye:

| Tarea | Qué observar |
| --- | --- |
| `git-clone` | Repositorio y revisión clonados; revisa los parámetros (URL de Gitea, rama). |
| `maven-build` | Compilación Java; logs de `mvn` y artefacto generado. |
| `build-image` | Construcción de imagen de contenedor (Buildah/Kaniko u otra herramienta según plantilla). |
| `deploy` | Actualización del Deployment en OpenShift; comprueba que la imagen nueva se referencia en el recurso. |

Para cada tarea, abre los **logs** y confirma que terminó en **Succeeded**. Si alguna falla, el mensaje suele indicar credenciales, límites de recursos o errores de compilación.

## Usar la vista Topology

1. En la consola, abre la vista **Topology** del mismo proyecto.
2. Localiza el **Deployment** del backend (icono circular con anillo de estado).
3. Haz clic en el nodo para ver el panel lateral: **Pods**, **Services**, **Routes** y eventos recientes.

La topología muestra relaciones visuales entre componentes: por ejemplo, cómo el **Service** enlaza el tráfico hacia los **Pods** del Deployment.

## Explorar detalles de la aplicación

Desde el panel del componente en Topology o desde los menús dedicados:

- **Pods**: número de réplicas, estado `Running`, reinicios y métricas rápidas.
- **Services**: puertos internos y selectores que apuntan al Deployment.
- **Routes**: URL pública o del cluster para probar la API (coherente con el módulo de despliegue).

> **Note:** Si el taller añade **HorizontalPodAutoscaler** u otros operadores, pueden aparecer recursos adicionales enlazados al mismo Deployment.

## Volver al Developer Hub

1. Abre **Developer Hub**.
2. Localiza el componente **neuralbank-backend** en el catálogo.
3. Desde la ficha, comprueba enlaces a **source** (Gitea), documentación y cualquier anotación de **CI/CD** que exponga el plugin de pipelines o Git.

Así conectas la vista “infraestructura” (OpenShift/Tekton) con la vista “producto” (catálogo y ownership en el Hub).

## Resumen

Has seguido un **PipelineRun** tarea a tarea, validado el estado del despliegue en **Topology** y relacionado el componente con su entrada en **Developer Hub**. Esto completa la visión operativa del backend Neuralbank antes de añadir frontend y MCP.
