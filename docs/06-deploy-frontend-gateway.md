---
layout: default
title: "Desplegar frontend y gateway"
nav_order: 7
---

En este módulo desplegarás el **frontend** Neuralbank y el servicio **Customer Service MCP** mediante Software Templates, y comprobarás cómo el escenario MCP incorpora objetos de **Gateway API** y políticas de conectividad.

## Contexto

Ya deberías tener **neuralbank-backend** en ejecución en el proyecto **`YOUR_USER-neuralbank`**. Ahora añadirás:

- **neuralbank-frontend**: interfaz web para visualizar datos de créditos expuestos por el backend.
- **customer-service-mcp**: servidor MCP con recursos adicionales para el patrón de **connectivity link** (Gateway, HTTPRoute, OIDCPolicy, RateLimitPolicy).

## Paso 1: Desplegar Neuralbank Frontend

1. En **Developer Hub**, ve a **Create** y elige la plantilla **Neuralbank: Frontend** (nombre puede variar ligeramente).
2. Completa el formulario con un nombre coherente, por ejemplo `neuralbank-frontend`.
3. Asigna el **owner** a tu usuario de taller (`YOUR_USER`, mismo criterio que en el backend).
4. Ejecuta **Create** y espera a que finalicen los pasos del scaffolder.

```bash
Developer Hub -> Create -> "Neuralbank: Frontend"
nombre: neuralbank-frontend
owner: YOUR_USER
→ namespace: YOUR_USER-neuralbank
→ componente en catálogo: YOUR_USER-neuralbank-frontend
→ aplicación ArgoCD: YOUR_USER-neuralbank-frontend
```

Al finalizar recibirás una **notificación** confirmando la creación del componente.

> **Note:** Si el frontend espera una URL de API configurable, comprueba en el repo generado (Gitea) variables de entorno o `ConfigMap` que apunten al backend; ajústalas solo si el taller lo indica.

## Paso 2: Desplegar Customer Service MCP

1. Repite el flujo **Create** con la plantilla **Neuralbank: Customer Service MCP** (o título equivalente).
2. Usa un nombre claro, por ejemplo `customer-service-mcp`.
3. Mantén el **owner** alineado con tu usuario para facilitar el filtrado en el catálogo.

```bash
Developer Hub -> Create -> "Neuralbank: Customer Service MCP"
nombre: customer-service-mcp
owner: YOUR_USER
→ namespace: YOUR_USER-neuralbank
→ componente en catálogo: YOUR_USER-customer-service-mcp
→ aplicación ArgoCD: YOUR_USER-customer-service-mcp
```

Esta plantilla no solo genera código: materializa el patrón de **connectivity link** con objetos como **Gateway**, **HTTPRoute**, **OIDCPolicy** y **RateLimitPolicy**, según la convención del repositorio de plantillas del workshop. También recibirás una notificación al finalizar.

## Paso 3: Verificar repositorios en Gitea

En **Gitea**, confirma que existen dos repositorios nuevos (o los nombres que elegiste):

- `neuralbank-frontend` — Dockerfile o assets estáticos, manifiestos, `devfile` si aplica.
- `customer-service-mcp` — fuentes Java u otras, manifiestos y recursos de gateway/políticas.

Revisa que los **PipelineRuns** se hayan disparado o estén programados según los triggers del taller.

## Paso 4: Comprobar aplicaciones en Argo CD

1. Abre **Argo CD**.
2. Localiza las **Applications** `YOUR_USER-neuralbank-frontend` y `YOUR_USER-customer-service-mcp` (los nombres incluyen tu usuario como prefijo).
3. Verifica **sync** y **health**; sincroniza manualmente si tu rol lo permite y el estado lo requiere.

> **Note:** Los recursos de Gateway API y políticas pueden aparecer como objetos adicionales en el grafo de la aplicación MCP; si alguno está en estado degradado, revisa eventos en OpenShift y la configuración del operador (Kuadrant / Istio) del entorno.

## Paso 5: Validar los tres componentes en el catálogo

1. Entra en **Developer Hub -> Catalog**.
2. Filtra por tu **owner** o busca por nombre.
3. Deberías ver **tres** componentes: `YOUR_USER-neuralbank-backend`, `YOUR_USER-neuralbank-frontend` y `YOUR_USER-customer-service-mcp`.

Abre cada ficha y comprueba:
- Enlaces al código en Gitea.
- Pestaña **CI** con los PipelineRuns de Tekton.
- Relaciones entre componentes (frontend consume API del backend, MCP depende del backend).
- El **System** compartido `YOUR_USER-neuralbank` que agrupa los tres componentes.

## Paso 6: Inspeccionar Gateway y HTTPRoute en OpenShift

1. En la **OpenShift Console**, selecciona el proyecto **`YOUR_USER-neuralbank`** (donde se despliegan frontend y MCP) o el indicado por el instructor.
2. Navega a **Networking** y busca recursos de **Gateway** y **HTTPRoute** (la ubicación exacta depende de la versión y de los CRD instalados).
3. Anota el **hostname** o estado de los listeners y las reglas que enrutan hacia el Service del MCP.

```bash
OpenShift Console -> Project: YOUR_USER-neuralbank -> Gateway / HTTPRoute -> host y backends
```

Opcionalmente, revisa **OIDCPolicy** y **RateLimitPolicy** asociados para entender cómo se protege el endpoint expuesto.

## Paso 7: Probar el frontend

Abre la **Route** o URL del frontend en el navegador. Comprueba que la interfaz carga y, si el taller lo configura, que consume datos del backend (página de créditos, estado de API, etc.).

> **Warning:** Errores CORS o de URL de API suelen deberse a configuración del frontend o a rutas internas; revisa variables en el Deployment antes de cambiar políticas de red.

## Resumen

Has añadido **frontend** y **MCP** con plantillas, has verificado **GitOps** y **catálogo** (cada componente con nombre único `YOUR_USER-name`), has localizado **Gateway** y **HTTPRoute** que formalizan la conectividad segura del MCP, y has recibido notificaciones por cada componente creado. El siguiente módulo profundiza en **Argo CD**, entidades de **API** y políticas **Kuadrant**.

### Capturas de Pantalla

![Developer Hub — Frontend y Gateway]({{ site.baseurl }}/assets/images/neuroface4.png)
