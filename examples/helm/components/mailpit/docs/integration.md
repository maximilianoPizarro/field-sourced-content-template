# Mailpit Integration with Developer Hub

## Backstage Proxy Configuration

Mailpit is exposed to Backstage templates through a proxy endpoint configured in the Developer Hub app-config:

```yaml
proxy:
  endpoints:
    '/mailpit':
      target: "http://n8n-mailpit.openshift-lightspeed.svc:8025"
      changeOrigin: true
      allowedMethods: ['GET', 'POST']
      credentials: dangerously-allow-unauthenticated
```

## Software Template Integration

Templates use the `http:backstage:request` scaffolder action to send emails. This is the standard notification pattern used across all templates in this workshop:

### Templates Using Mailpit

| Template | Notification Trigger |
|----------|---------------------|
| `neuralbank-backend` | Component scaffolded successfully |
| `neuralbank-frontend` | Component scaffolded successfully |
| `customer-service-mcp` | MCP service scaffolded successfully |
| `remove-component` | Component removed from catalog |

### Email Format

All notification emails use the Neuralbank enterprise branding:

- **From**: `devhub@neuralbank.demo` / "Red Hat Developer Hub"
- **To**: `<username>@workshop.demo`
- **Subject**: Includes component name and action
- **Body**: HTML with Neuralbank styling, links to Developer Hub catalog and ArgoCD

## n8n Workflow Integration

Mailpit is also used by n8n workflows in the `openshift-lightspeed` namespace for automated notifications triggered by OpenShift events (health checks, alerts, etc.).
