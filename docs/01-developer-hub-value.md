---
layout: default
title: "Propuesta de valor de Red Hat Developer Hub"
nav_order: 2
---

Red Hat Developer Hub es la oferta empresarial basada en **Backstage**, el portal de desarrollo open source de CNCF. Su objetivo es ofrecer un único punto de entrada donde los equipos descubren servicios, crean nuevos componentes siguiendo caminos probados y colaboran sin fricción con la plataforma subyacente.

## ¿Qué es Developer Hub?

Developer Hub centraliza la experiencia del desarrollador frente a un ecosistema que suele estar fragmentado: múltiples consolas, repositorios, pipelines y clústeres. En lugar de pedir a cada desarrollador que memorice URLs, permisos y convenciones, el portal expone **documentación viva**, **plantillas** y **integraciones** que reflejan cómo tu organización construye software de verdad.

## Portal self-service para desarrolladores

El enfoque *self-service* significa que un desarrollador puede:

- **Registrar o descubrir** componentes existentes en el catálogo sin abrir tickets manuales.
- **Crear nuevos proyectos** a partir de plantillas aprobadas por la plataforma (golden paths).
- **Abrir entornos de desarrollo** integrados (por ejemplo Dev Spaces) desde la ficha del componente.
- **Consultar APIs, dependencias y ownership** en un solo lugar.

Esto reduce la dependencia de equipos de plataforma para tareas repetitivas y acelera el onboarding de nuevos miembros.

## Software Catalog: descubrimiento y gobernanza

El **Software Catalog** modela entidades como *Component*, *API*, *System*, *Domain* y *Resource*. Cada ficha enlaza código, documentación, pipelines y despliegues, de modo que el catálogo no es solo un listado, sino un **mapa operativo** del landscape de aplicaciones.

Beneficios típicos:

- Visibilidad de qué servicios existen, quién es responsable y cómo se relacionan.
- Cumplimiento de estándares al obligar a registrar componentes creados desde plantillas.
- Base para plugins que enriquecen la vista (por ejemplo GitOps, métricas o políticas).

## Software Templates: golden paths reproducibles

Las **Software Templates** codifican el camino recomendado: estructura de repositorio, manifiestos de Kubernetes/OpenShift, pipelines Tekton, `devfile` para Dev Spaces y registro en el catálogo. El desarrollador rellena un formulario con parámetros seguros (nombre, propietario, entorno) y el sistema ejecuta los pasos definidos en la plantilla.

Así se evita el “copiar y pegar” de repositorios zombi y se garantiza que todo lo generado cumple políticas de seguridad, nomenclatura y despliegue.

## Plugins y extensibilidad

Backstage se basa en un modelo de **plugins** que añaden tarjetas, páginas y acciones al catálogo: integración con Git (Gitea, GitHub, GitLab), visualización de pipelines, enlaces a clusters, escaneo de API, etc. Red Hat Developer Hub selecciona y soporta un conjunto de plugins alineado con productos Red Hat y patrones GitOps, de modo que las organizaciones pueden **extender** el portal sin bifurcar por completo el upstream.

Para equipos de plataforma, esto significa que las políticas y la automatización pueden exponerse de forma uniforme: un mismo plugin de **Kubernetes** o **Tekton** muestra información homogénea para todos los desarrolladores, en lugar de depender de wikis estáticas.

## Internal Developer Portal (IDP)

En muchas empresas, Developer Hub es el núcleo del **Internal Developer Portal**: un sitio donde conviven estándares arquitectónicos, plantillas aprobadas, métricas de madurez y enlaces a herramientas internas. El IDP reduce la pregunta “¿cómo hago X aquí?” a “¿qué plantilla o componente del catálogo uso?”.

El valor no es solo documentación: es **operacional**. Las fichas del catálogo pueden enlazar al repositorio real, al pipeline activo y al estado de despliegue, de modo que la información está tan actualizada como el propio clúster.

## Integración con el stack de plataforma

Developer Hub no sustituye Argo CD, Tekton, Kubernetes ni Dev Spaces: **los orquesta desde la experiencia del desarrollador**.

- **Argo CD**: sincroniza el estado deseado del repositorio Git con el clúster; el desarrollador ve el resultado en el catálogo y en GitOps.
- **Tekton**: ejecuta pipelines de build, imagen y despliegue disparados por cambios en el repositorio.
- **Kubernetes / OpenShift**: es el runtime donde viven cargas de trabajo, rutas, gateways y políticas.
- **Dev Spaces**: proporciona IDEs en el navegador alineados con el `devfile` del componente.

Esta integración hace que el portal sea la capa de **abstracción con contexto**: menos saltos entre herramientas, más trazabilidad.

## RBAC e integración con Keycloak

En entornos empresariales, el acceso al portal y a acciones sensibles (crear plantillas, ver secretos, aprobar despliegues) se gobierna con **RBAC** y proveedores de identidad como **Keycloak**. Los grupos y roles del IdP se mapean a permisos en Backstage/Developer Hub, de forma que las golden paths estén disponibles solo para quien corresponda y la información sensible quede acotada.

## Reducción de carga cognitiva y tiempo hasta producción

Al combinar catálogo, plantillas e integraciones:

- **Menos decisiones ad hoc**: el desarrollador elige un camino ya validado.
- **Menos errores de configuración**: manifiestos y pipelines salen de la plantilla, no de ediciones manuales dispersas.
- **Ciclos más cortos**: de la idea al repositorio, al pipeline y al despliegue en OpenShift con pasos guiados y visibles en el Hub.

En el workshop de Neuralbank aplicarás estos conceptos creando backend, frontend y un servicio MCP mediante plantillas, y observando cómo el portal conecta cada pieza con GitOps y CI/CD.

## Métricas de adopción (visión práctica)

Las organizaciones suelen medir el éxito del portal con indicadores como: tiempo medio desde la solicitud hasta el primer commit en un repo golden path, número de componentes registrados frente a silos no inventariados, o reducción de tickets repetitivos a plataforma. Aunque este taller no implementa dashboards de negocio, conviene tener presente que Developer Hub es también una palanca de **governanza suave**: al canalizar la creación de software por plantillas, se facilita auditar qué se despliega y cómo.

## Resumen

Red Hat Developer Hub transforma OpenShift y el ecosistema GitOps en una **experiencia de producto** para desarrolladores: descubrimiento en el catálogo, creación disciplinada con plantillas e integración con Argo CD, Tekton, Dev Spaces y Keycloak para llevar software a producción con menos fricción y más consistencia.

Con esta base conceptual, el siguiente módulo describe la arquitectura concreta del taller y el recorrido de datos desde la plantilla hasta el clúster.
