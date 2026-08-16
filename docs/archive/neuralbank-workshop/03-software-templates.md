---
layout: default
title: "Entender Software Templates"
nav_order: 4
---

Las **Software Templates** son uno de los pilares de Backstage y de Red Hat Developer Hub. Permiten que la organización empaquete opiniones, estándares y automatización en flujos que el desarrollador ejecuta con un formulario seguro.

## ¿Qué son las Software Templates?

Una Software Template es una definición declarativa que describe:

- **Qué** se va a generar (código, manifiestos, pipelines, catalog-info, etc.).
- **Qué parámetros** debe introducir el usuario (nombre del componente, propietario, visibilidad).
- **Qué pasos** ejecuta el motor de plantillas en secuencia (sustituir archivos, publicar en Git, registrar en el catálogo).

En la práctica, convierten un procedimiento interno de varias horas en un flujo de minutos con trazabilidad y cumplimiento.

## Estructura típica: `template.yaml` y `skeleton/`

En un repositorio de plantillas encontrarás habitualmente:

- **`template.yaml`** (o equivalente): metadatos de la plantilla (`apiVersion`, `kind: Template`), secciones `parameters` y `steps`. Aquí se declara el nombre visible en el Hub, los campos del formulario y las acciones.
- **Directorio `skeleton/`**: árbol de archivos plantilla con **marcadores** que el motor reemplaza con los valores de los parámetros (por ejemplo nombre del proyecto, grupo, rutas de imagen).

El directorio `skeleton` puede contener:

- Código fuente (Java, Node, etc.).
- Manifiestos de Kubernetes/OpenShift (`deployment`, `service`, `route`, recursos de Gateway API).
- `devfile.yaml` para **Dev Spaces**.
- `tekton/pipeline.yaml` u otros recursos de CI/CD.
- `catalog-info.yaml` para el **Backstage Catalog**.

## Parámetros y pasos comunes

Los **parámetros** suelen incluir:

- Nombre del componente o repositorio.
- Propietario (`owner`) o equipo, alineado con el modelo de entidades del catálogo.
- A veces URL del clúster, visibilidad o etiquetas—siempre acotadas por quien mantenga la plantilla.

Los **pasos** frecuentes en ecosistemas GitOps son:

- **`fetch:template`** (o equivalente): copia el `skeleton` y aplica sustituciones desde los parámetros. En este workshop se genera un **nombre único** (`uniqueName`) con formato `owner-name` (e.g. `user1-neuralbank-backend`) para evitar colisiones entre usuarios.
- **`publish:gitea`** / **`publish:github`** / similar: crea o actualiza el repositorio remoto con el contenido generado.
- **`catalog:register`**: registra el componente (y a veces la API) en el Software Catalog para que aparezca en Developer Hub con enlaces y metadatos.
- **Creación de ArgoCD Application** (`http:backstage:request`): crea la Application como un CRD de Kubernetes a través del proxy `/k8s-api` — el ServiceAccount de Developer Hub ya tiene permisos RBAC sobre `argoproj.io/applications`.
- **Creación de Gitea Webhook** (`http:backstage:request`): configura un webhook para disparar pipelines Tekton ante cada push.
- **Envío de notificación** (`http:backstage:request` a `/proxy/mailpit/api/v1/send`): envía un email de confirmación al owner vía Mailpit.

La nomenclatura exacta depende de los **scaffolder actions** instalados en tu instancia; lo importante es reconocer el patrón: *generar → publicar en Git → crear ArgoCD App → registrar en catálogo → notificar*.

## Naming convention multi-usuario

En un entorno compartido con múltiples participantes, es fundamental evitar colisiones de nombres. Las plantillas de este workshop aplican un **prefijo con el username** a todos los recursos que requieren unicidad global:

| Recurso | Nombre base | Nombre único |
| --- | --- | --- |
| Componente en catálogo | `neuralbank-backend` | `user1-neuralbank-backend` |
| Aplicación ArgoCD | `neuralbank-backend` | `user1-neuralbank-backend` |
| Entidad API | `neuralbank-backend-api` | `user1-neuralbank-backend-api` |
| System | `neuralbank` | `user1-neuralbank` |
| ClusterRoleBinding | `neuralbank-backend-trigger-clusterbinding` | `user1-neuralbank-backend-trigger-clusterbinding` |

Los recursos **namespace-scoped** (Deployment, Service, Pipeline, etc.) mantienen el nombre base (`neuralbank-backend`) porque el namespace ya es único por usuario (`user1-neuralbank`).

## Plantillas disponibles en el workshop

### Neuralbank (caso de negocio)

| Plantilla | Propósito |
| --- | --- |
| `customer-service-mcp` | Servidor **MCP** (Model Context Protocol) para atención al cliente; incluye piezas de exposición y políticas asociadas al patrón de conectividad. |
| `neuralbank-backend` | API REST (por ejemplo Quarkus) para **créditos** y dominio bancario; incluye pipeline de build/despliegue y registro de **API** en el catálogo. |
| `neuralbank-frontend` | Interfaz web para **visualización** de créditos, alineada con el backend y el despliegue en OpenShift. |

### Industrial Edge (IoT / Scale-to-N)

| Plantilla | Propósito |
| --- | --- |
| `industrial-edge` | Despliega un **stack IoT completo** y aislado: sensores MQTT, AMQ Broker, Kafka KRaft, Camel K (MQTT→Kafka), y Line Dashboard. Cada instancia genera su propio namespace, cluster Kafka y ArgoCD Application. Diseñada para **escalar la arquitectura validada a N instancias** de forma self-service. |
| `remove-component` | **Cleanup template** que elimina instancias creadas por cualquier template: borra el repo en Gitea, cascade-delete del ArgoCD Application (namespace incluido), limpieza de entidades del catálogo, y notificación. |

### Otras plantillas

| Plantilla | Propósito |
| --- | --- |
| `kafka-external-consumer` | Consumer Kafka externo con configuración de tópicos y consumer groups. |
| `kafka-cdc-mcp` | MCP Server para CDC (Change Data Capture) sobre Kafka. |
| `k8s-ops-mcp` | MCP Server para operaciones Kubernetes. |
| `sonataflow-bpm` | Workflow BPM con SonataFlow/Kogito. |
| `camel-kaoto` | Integración Camel con editor visual Kaoto en Dev Spaces. |

Cada una implementa un **golden path** distinto. La plantilla `industrial-edge` es especialmente relevante como ejemplo de *platform-as-a-service*: una arquitectura validada empaquetada como template para escalar bajo demanda. Ver [Scale to N: Industrial Edge Self-Service]({{ site.baseurl }}/13-industrial-edge-scale) para la documentación completa.

## Flujo de scaffolding

```mermaid
graph LR
    DEV["👤 Desarrollador"] -->|"Rellena formulario"| TPL["📋 Software Template"]
    TPL -->|"fetch:template"| SKEL["📁 Skeleton<br/>código + manifests"]
    SKEL -->|"publish:gitea"| REPO["📦 Repo en Gitea"]
    REPO -->|"catalog:register"| CAT["📒 Catálogo<br/>Developer Hub"]
    REPO -->|"ArgoCD App"| ARGO["🔄 Argo CD"]
    REPO -->|"Gitea webhook"| PIPE["⚙️ Tekton Pipeline"]
    ARGO -->|"sync"| OCP["☸️ OpenShift"]
    CAT -->|"notificación"| NOTIF["🔔 Notification<br/>in-app + email"]

    style DEV fill:#151515,color:#fff,stroke:#EE0000
    style TPL fill:#EE0000,color:#fff
    style SKEL fill:#609926,color:#fff
    style REPO fill:#609926,color:#fff
    style CAT fill:#0066CC,color:#fff
    style PIPE fill:#fd495c,color:#fff
    style ARGO fill:#ef7b4d,color:#fff
    style OCP fill:#EE0000,color:#fff
    style NOTIF fill:#4078c0,color:#fff
```

## ¿Qué genera cada plantilla?

En líneas generales, al ejecutar una plantilla Neuralbank obtendrás:

- **Código** listo para compilar y extender (fuentes, `pom.xml` o equivalente, Dockerfile si aplica).
- **Manifiestos** de despliegue en OpenShift (Deployment, Service, Route u objetos de enrutamiento según el caso).
- **Definición de pipeline Tekton** alineada con el repo (para build de imagen y despliegue), con la anotación `janus-idp.io/tekton` para visibilidad en la pestaña **CI** de Developer Hub.
- **`devfile.yaml`** para abrir el proyecto en **Red Hat OpenShift Dev Spaces** con herramientas y comandos preconfigurados.
- **Objetos de connectivity link** donde corresponda—en especial en el escenario **MCP**—: recursos como **Gateway**, **HTTPRoute**, **OIDCPolicy** y **RateLimitPolicy**, para exponer el servicio con autenticación OIDC y límites de tasa.
- **Aplicación ArgoCD** creada automáticamente con nombre único (`owner-name`) y sincronización automática.
- **Webhook en Gitea** configurado para disparar pipelines ante cada push.
- **Notificación** al owner confirmando la creación exitosa del componente.

Además, el **`catalog-info.yaml`** permite que Developer Hub muestre el componente con **owner**, **system** y relaciones hacia APIs y dependencias, usando el nombre único con prefijo de usuario.

## Buenas prácticas al usar plantillas

- Revisa los **valores por defecto** y la **descripción** de cada parámetro antes de crear el componente.
- Tras la creación, abre el **repositorio en Gitea** y confirma que la estructura coincide con lo esperado.
- Localiza en el catálogo la **entidad Component** (y **API** si aplica) para enlazar documentación y pipelines.

Dominar las Software Templates es dominar el **contrato** entre desarrollo y plataforma: tú aportas el contexto de negocio en el formulario; la plataforma aplica el resto de forma repetible.
