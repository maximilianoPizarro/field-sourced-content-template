---
layout: default
title: "Arquitectura general del workshop"
nav_order: 3
---

Esta página describe la arquitectura lógica del escenario Neuralbank: cómo encajan Red Hat Developer Hub, GitOps, CI/CD, identidad y exposición segura de APIs frente a un clúster OpenShift.

## Vista de componentes (descripción textual)

Imagina el siguiente flujo de capas:

```text
[Desarrollador]
      |
      v
Red Hat Developer Hub (Backstage) -- Software Catalog, Templates, plugins
      |
      +--> Gitea (Git: código, manifiestos, definiciones de pipeline)
      |
      +--> Keycloak (identidad / SSO para Hub y servicios)
      |
      v
Argo CD (OpenShift GitOps) -- sincroniza el estado deseado desde Git al clúster
      |
      v
OpenShift (Kubernetes)
      |
      +--> Tekton (Pipelines / PipelineRuns: build, imagen, despliegue)
      +--> Cargas de trabajo (Deployments, Services, Routes)
      +--> Gateway API + Istio / Kuadrant (Gateway, HTTPRoute, políticas)
      +--> Red Hat OpenShift Dev Spaces (IDE en cluster, devfile)
```

Cada bloque tiene un rol claro: el Hub orquesta la experiencia; Git es la fuente de verdad; Argo CD aplica el modelo declarativo; Tekton automatiza la cadena de suministro de software; la capa de gateway y políticas expone y protege el tráfico; Dev Spaces acerca el desarrollo al entorno real del clúster.

## Flujo principal: de la plantilla al despliegue

El recorrido típico que seguirás en el workshop es:

1. El desarrollador abre **Developer Hub** y lanza una **Software Template** (por ejemplo Neuralbank Backend).
2. La plantilla **genera o publica** un repositorio en **Gitea** con código, manifiestos, `devfile` y definición de pipeline.
3. **Argo CD** detecta la aplicación (Application) asociada y **sincroniza** manifiestos hacia el namespace de OpenShift.
4. **Tekton** ejecuta un **PipelineRun** (clonado, build Maven, build de imagen, despliegue) según los recursos creados en el repo.
5. OpenShift **programa los pods**, expone **Services** y, según el caso, recursos de **Gateway API** y rutas HTTP.
6. El componente queda **registrado en el catálogo** de Developer Hub para descubrimiento, documentación y relaciones (por ejemplo APIs).

Este patrón une **golden path** (plantilla) con **GitOps** (Argo CD) y **CI/CD** (Tekton), manteniendo trazabilidad desde el primer clic en el Hub hasta el pod en ejecución.

## Patrón Connectivity Link (Gateway + rutas + políticas)

Para exponer servicios hacia el exterior o entre equipos con controles uniformes, el escenario utiliza un patrón alineado con **Gateway API** e integración con **Istio** y **Kuadrant** para políticas:

- **Gateway**: define el punto de entrada del tráfico (host, listeners, TLS según configuración).
- **HTTPRoute**: enlaza hostnames y reglas de enrutamiento con los Services backend.
- **OIDCPolicy**: exige autenticación OIDC coherente con tu IdP (p. ej. Keycloak), protegiendo el tráfico de la API expuesta.
- **RateLimitPolicy**: aplica límites de tasa para proteger backends y cumplir políticas de uso justo o defensa ante abuso.

En conjunto, estas piezas forman un **connectivity link** declarativo: el mismo enfoque GitOps que el resto del sistema, revisable en Git y aplicado por operadores en el clúster.

## Rol de cada componente

| Componente | Rol |
| --- | --- |
| Developer Hub | Portal del desarrollador: catálogo, plantillas, documentación y plugins hacia GitOps, pipelines y entornos. |
| Gitea | Repositorio Git interno: código fuente, manifiestos y triggers para pipelines. |
| Argo CD | Sincronización continua desde Git al estado del clúster; salud y drift visibles en el dashboard de Argo CD. |
| Tekton | Ejecución de pipelines como recursos de Kubernetes; encadena tareas de CI/CD. |
| Keycloak | Identidad y SSO; alimenta políticas OIDC y acceso al Hub. |
| Dev Spaces | Entornos de desarrollo basados en `devfile`, conectados al mismo repo que GitOps y Tekton. |
| Gateway API / Istio / Kuadrant | Entrada norte-sur del tráfico, enrutamiento y políticas (OIDC, rate limit) sobre las APIs expuestas. |

## Lectura para el workshop

Durante los módulos posteriores volverás a este mapa mental: cada vez que crees una plantilla, mira el repo en Gitea; cada vez que sincronice Argo CD, revisa el namespace en OpenShift; cuando el pipeline termine, valida imagen y despliegue; cuando expongas el MCP o APIs, relaciona Gateway, HTTPRoute y políticas con lo que ves en consola y en el catálogo.

Con esta arquitectura, **Developer Hub** actúa como fachada humana sobre un sistema declarativo y automatizado que lleva el software desde el repositorio hasta producción de forma repetible.
