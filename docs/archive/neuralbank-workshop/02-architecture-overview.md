---
layout: default
title: "Arquitectura general del workshop"
nav_order: 3
---

Esta página describe la arquitectura lógica del escenario Neuralbank: cómo encajan Red Hat Developer Hub, GitOps, CI/CD, identidad y exposición segura de APIs frente a un clúster OpenShift.

## Vista de componentes

```mermaid
graph TB
    DEV["👤 Desarrollador"]

    subgraph PLATFORM ["🔴 Plataforma OpenShift"]
        HUB["Red Hat<br/>Developer Hub"]
        KC["Keycloak<br/>SSO / OIDC"]
        GITEA["Gitea<br/>SCM interno"]
        ARGO["Argo CD<br/>GitOps"]
        TEKTON["Tekton<br/>CI/CD Pipelines"]
        DS["Dev Spaces<br/>IDE cloud"]
        LS["Lightspeed<br/>IA Assistant"]
        NOTIF["Notifications<br/>+ Mailpit"]
    end

    subgraph NEURALBANK ["🏦 Neuralbank Stack  (user-neuralbank)"]
        MCP["customer-service-mcp<br/>Quarkus MCP Server"]
        BACK["neuralbank-backend<br/>REST API créditos"]
        FRONT["neuralbank-frontend<br/>SPA visualización"]
    end

    subgraph NETWORKING ["🌐 Gateway API · Istio · Kuadrant"]
        GW["Gateway<br/>Listener TLS"]
        HR["HTTPRoute"]
        OIDC["OIDCPolicy"]
        RL["RateLimitPolicy"]
    end

    DEV --> HUB
    DEV --> DS
    DEV --> LS
    DS --> GITEA
    HUB --> GITEA
    HUB --> KC
    HUB --> NOTIF
    HUB --> LS
    GITEA --> ARGO
    GITEA --> TEKTON
    ARGO --> MCP
    ARGO --> BACK
    ARGO --> FRONT
    TEKTON --> MCP
    TEKTON --> BACK
    TEKTON --> FRONT
    MCP --> BACK
    FRONT --> BACK
    GW --> HR
    HR --> MCP
    HR --> BACK
    GW --> OIDC
    GW --> RL
    KC -.-> OIDC

    style DEV fill:#151515,color:#fff,stroke:#EE0000,stroke-width:2px
    style HUB fill:#EE0000,color:#fff,stroke:#151515
    style KC fill:#4078c0,color:#fff,stroke:#151515
    style GITEA fill:#609926,color:#fff,stroke:#151515
    style ARGO fill:#ef7b4d,color:#fff,stroke:#151515
    style TEKTON fill:#fd495c,color:#fff,stroke:#151515
    style DS fill:#6a1b9a,color:#fff,stroke:#151515
    style LS fill:#0066CC,color:#fff,stroke:#151515
    style NOTIF fill:#ef7b4d,color:#fff,stroke:#151515
    style MCP fill:#6a1b9a,color:#fff,stroke:#151515
    style BACK fill:#EE0000,color:#fff,stroke:#151515
    style FRONT fill:#0066CC,color:#fff,stroke:#151515
    style GW fill:#0066CC,color:#fff,stroke:#151515
    style HR fill:#0066CC,color:#fff,stroke:#151515
    style OIDC fill:#4078c0,color:#fff,stroke:#151515
    style RL fill:#ef7b4d,color:#fff,stroke:#151515
    style PLATFORM fill:#1a1a1a,color:#fff,stroke:#EE0000,stroke-width:2px
    style NEURALBANK fill:#1a1a1a,color:#fff,stroke:#0066CC,stroke-width:2px
    style NETWORKING fill:#1a1a1a,color:#fff,stroke:#0066CC,stroke-width:2px
```

## Flujo principal: de la plantilla al despliegue

```mermaid
sequenceDiagram
    actor Dev as Desarrollador
    participant Hub as Developer Hub
    participant Git as Gitea
    participant Argo as Argo CD
    participant Tek as Tekton
    participant OCP as OpenShift

    Dev->>Hub: 1. Ejecuta Software Template
    Hub->>Git: 2. Crea repo con código + manifiestos
    Hub->>Hub: 3. Registra componente en catálogo (owner-name)
    Hub->>Dev: 4. Envía notificación (in-app + email)
    Git-->>Argo: 5. Detecta nueva Application (owner-name)
    Argo->>OCP: 6. Sincroniza manifiestos
    Git-->>Tek: 7. Dispara PipelineRun
    Tek->>Tek: 8. git-clone → maven → build image
    Tek->>OCP: 9. Deploy a namespace
    OCP-->>Dev: 10. Servicio accesible vía Route
```

Este patrón une **golden path** (plantilla) con **GitOps** (Argo CD) y **CI/CD** (Tekton), manteniendo trazabilidad desde el primer clic en el Hub hasta el pod en ejecución.

## Namespace por usuario y naming convention

Cada usuario recibe su propio namespace basado en su username. Los componentes en el catálogo y las aplicaciones en ArgoCD usan un **nombre único** con prefijo del owner (`owner-name`) para evitar colisiones entre usuarios:

| Recurso | Convención de nombre | Ejemplo (user1) |
| --- | --- | --- |
| Namespace | `owner-neuralbank` | `user1-neuralbank` |
| Componente en catálogo | `owner-name` | `user1-neuralbank-backend` |
| Aplicación ArgoCD | `owner-name` | `user1-neuralbank-backend` |
| Anotación `backstage.io/kubernetes-id` | `owner-name` | `user1-neuralbank-backend` |
| Anotación `janus-idp.io/tekton` | `owner-name` | `user1-neuralbank-backend` |
| ClusterRoleBinding | `owner-name-trigger-clusterbinding` | `user1-neuralbank-backend-trigger-clusterbinding` |

```mermaid
graph TB
    subgraph "user1-neuralbank"
        B1["user1-neuralbank-backend"]
        F1["user1-neuralbank-frontend"]
        M1["user1-customer-service-mcp"]
        G1["Gateway + HTTPRoute"]
        P1["OIDCPolicy + RateLimitPolicy"]
        B1 --- F1
        M1 --> B1
        G1 --> M1
        P1 --> G1
    end

    subgraph "user2-neuralbank"
        B2["user2-neuralbank-backend"]
        F2["user2-neuralbank-frontend"]
        M2["user2-customer-service-mcp"]
    end

    style B1 fill:#EE0000,color:#fff
    style F1 fill:#0066CC,color:#fff
    style M1 fill:#6a1b9a,color:#fff
    style G1 fill:#151515,color:#fff
    style P1 fill:#151515,color:#fff
    style B2 fill:#EE0000,color:#fff
    style F2 fill:#0066CC,color:#fff
    style M2 fill:#6a1b9a,color:#fff
```

## Patrón Connectivity Link (Gateway + rutas + políticas)

```mermaid
graph LR
    CLIENT["🌍 Cliente"] --> GW["Gateway<br/>Listener :8080"]
    GW --> HR["HTTPRoute<br/>Reglas de enrutamiento"]
    HR --> SVC["Service<br/>MCP / Backend"]
    GW --> OIDC["OIDCPolicy<br/>Keycloak auth"]
    GW --> RL["RateLimitPolicy<br/>Límites de tasa"]

    style CLIENT fill:#151515,color:#fff
    style GW fill:#0066CC,color:#fff
    style HR fill:#0066CC,color:#fff
    style SVC fill:#EE0000,color:#fff
    style OIDC fill:#4078c0,color:#fff
    style RL fill:#ef7b4d,color:#fff
```

- **Gateway**: punto de entrada del tráfico (host, listeners, TLS).
- **HTTPRoute**: enlaza hostnames y reglas de enrutamiento con los Services backend.
- **OIDCPolicy**: autenticación OIDC con Keycloak.
- **RateLimitPolicy**: límites de tasa para proteger backends.

## Rol de cada componente

| Componente | Rol |
| --- | --- |
| Developer Hub | Portal del desarrollador: catálogo, plantillas, documentación, notificaciones y plugins hacia GitOps, pipelines y entornos. |
| Gitea | Repositorio Git interno: código fuente, manifiestos y triggers para pipelines. |
| Argo CD | Sincronización continua desde Git al estado del clúster; salud y drift visibles en el dashboard. |
| Tekton | Ejecución de pipelines como recursos de Kubernetes; encadena tareas de CI/CD. Visible en la pestaña **CI** de cada componente en Developer Hub. |
| Keycloak | Identidad y SSO; alimenta políticas OIDC y acceso al Hub. |
| Dev Spaces | Entornos de desarrollo basados en `devfile`, conectados al mismo repo que GitOps y Tekton. |
| Gateway API / Istio / Kuadrant | Entrada norte-sur del tráfico, enrutamiento y políticas (OIDC, rate limit) sobre las APIs expuestas. |
| Lightspeed | Asistente de IA integrado en Developer Hub, con RAG sobre documentación del producto y conexión a LLM vía LiteLLM. |
| Notifications + Mailpit | Sistema de notificaciones in-app y por email; las plantillas notifican automáticamente al crear o eliminar componentes. |

## Lectura para el workshop

Durante los módulos posteriores volverás a este mapa mental: cada vez que crees una plantilla, mira el repo en Gitea; cada vez que sincronice Argo CD, revisa el namespace `YOUR_USER-neuralbank` en OpenShift; cuando el pipeline termine, valida imagen y despliegue; cuando expongas el MCP o APIs, relaciona Gateway, HTTPRoute y políticas con lo que ves en consola y en el catálogo.

Con esta arquitectura, **Developer Hub** actúa como fachada humana sobre un sistema declarativo y automatizado que lleva el software desde el repositorio hasta producción de forma repetible.
