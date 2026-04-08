---
layout: default
title: "Explorar Argo CD y APIs"
nav_order: 8
---

Este módulo conecta la vista **GitOps** en Argo CD con el **Software Catalog** de Developer Hub y las **políticas** aplicadas en el clúster, para que entiendas el estado de cada componente Neuralbank de punta a punta.

## Abrir el panel de Argo CD

1. Navega a la URL de **Argo CD** de tu entorno e inicia sesión (credenciales del taller o SSO según configuración).
2. En la lista de **Applications**, filtra por proyecto o por prefijo de nombre usado en el workshop.

Deberías ver aplicaciones correspondientes a **neuralbank-backend**, **neuralbank-frontend** y **customer-service-mcp** (los nombres exactos dependen de cómo la plantilla registró la Application).

## Explorar el árbol de recursos

Para cada aplicación:

1. Haz clic en el nombre para abrir la vista de **Tree** o **Network**.
2. Observa **Deployments**, **Services**, **Routes**, **ConfigMaps**, **Secrets** (referenciados, no valores), y en el caso MCP los recursos de **Gateway API** y políticas.

Comprueba el **Sync Status**:

- *Synced* indica que el clúster coincide con el manifiesto en Git.
- *OutOfSync* señala cambios pendientes o drift; el botón **Sync** aplica el estado deseado si tienes permisos.

Revisa también **Health**:

- *Healthy*, *Progressing* y *Degraded* explican si los pods y rutas están en el estado esperado.

> **Note:** Si un recurso aparece como desconocido para Argo CD, puede faltar la anotación `argocd.argoproj.io` o el recurso puede ser gestionado por otro Application.

## Sincronización y eventos

En la pestaña de **Events** o en la vista detallada, revisa mensajes recientes de despliegue. Útil cuando una imagen nueva no arranca o un hook de sync falla.

## Developer Hub: dependencias del componente

1. Abre **Developer Hub** y entra en la ficha de **neuralbank-backend**.
2. Busca la sección de **dependencies** o diagrama de relaciones (según plugins instalados).
3. Identifica vínculos hacia **APIs**, **systems** u otros **components** (frontend, MCP).

Esta vista complementa Argo CD: Argo CD muestra *recursos*; el Hub muestra *intención de producto* y ownership.

## Entidad API y OpenAPI

Muchas plantillas registran una entidad **API** junto al **Component**:

1. En el catálogo, abre la entidad **API** asociada al backend (nombre acorde al dominio Neuralbank).
2. Revisa la definición **OpenAPI** incrustada o enlazada (según cómo se publicó en `catalog-info.yaml`).

La definición OpenAPI documenta paths, esquemas y a veces ejemplos; es la referencia para consumidores del servicio y para el frontend.

```bash
Developer Hub -> Catalog -> API -> ver documentación OpenAPI / Swagger
```

## Políticas Kuadrant en OpenShift

Para el servicio MCP (y otros expuestos con el mismo patrón), inspecciona en la consola los objetos:

- **OIDCPolicy** — requisitos de autenticación OIDC (integración con Keycloak u otro IdP).
- **RateLimitPolicy** — límites de tasa aplicados al tráfico que coincide con la HTTPRoute.

La ubicación en la UI depende de los CRD instalados; usa **Search** en la consola con el tipo de recurso si no aparece en el menú lateral.

> **Note:** Si no ves políticas, el entorno puede usar solo parte del stack o un namespace distinto; pide al instructor el namespace de referencia.

## Resumen

Has correlacionado el **estado GitOps** en Argo CD con las **fichas de catálogo** en Developer Hub, has localizado la **API** y su **OpenAPI**, y has situado las **políticas OIDC y de rate limit** en el contexto de Kuadrant. Esto cierra el circuito entre despliegue declarativo, documentación viva y seguridad perimetral.
