---
layout: default
title: "Red Hat Developer Hub: De Cero a Producción"
nav_order: 1
has_children: false
---

## Bienvenido al Workshop

En este workshop aprenderás a utilizar **Red Hat Developer Hub** como portal de desarrollo self-service para construir, desplegar y gestionar aplicaciones en OpenShift.

### Qué vas a aprender

- Comprender la propuesta de valor de Red Hat Developer Hub
- Explorar la arquitectura: Developer Hub, ArgoCD, Tekton, DevSpaces, Gitea, Keycloak
- Crear servicios backend y frontend usando **Software Templates**
- Explorar pipelines automatizados, topology view y detalles de la aplicación
- Desplegar y configurar API Gateways con seguridad (OIDCPolicy, RateLimitPolicy)
- Actualizar código fuente usando **Red Hat OpenShift Dev Spaces** con CI/CD automatizado

### El caso de negocio: Neuralbank

Neuralbank es una entidad financiera que necesita modernizar su stack tecnológico. Como desarrollador, vas a construir tres componentes:

1. **Customer Service MCP** - Un servidor MCP (Model Context Protocol) para atención al cliente
2. **Neuralbank Backend** - API REST para gestión de créditos
3. **Neuralbank Frontend** - Interfaz web para visualización de créditos

### Acceso al entorno

Tu usuario es `<user_name>`. La contraseña es `Welcome123!`.

> **Note:** El atributo `<user_name>` se rellena automáticamente según tu inicio de sesión en OpenShift.

- **Developer Hub**: https://backstage-developer-hub-developer-hub.<cluster_domain>
- **Gitea**: https://gitea.<cluster_domain>
- **ArgoCD**: https://openshift-gitops-server-openshift-gitops.<cluster_domain>
- **DevSpaces**: https://devspaces.<cluster_domain>
