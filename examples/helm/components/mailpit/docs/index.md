# Mailpit — Email & SMTP Testing

Mailpit is the email testing tool used by Developer Hub software templates to send notification emails when scaffolding events occur (component creation, deletion, etc.).

## Access

| Resource | URL |
|----------|-----|
| **Web UI** | `https://n8n-mailpit-openshift-lightspeed.<cluster-domain>/` |
| **API** | `https://n8n-mailpit-openshift-lightspeed.<cluster-domain>/api/v1/messages` |
| **SMTP** | `n8n-mailpit.openshift-lightspeed.svc:1025` (internal) |

## Architecture

```
Developer Hub (Backstage)
    │
    │  POST /proxy/mailpit/api/v1/send
    │  (http:backstage:request action)
    ▼
Backstage Proxy (/mailpit)
    │
    │  http://n8n-mailpit.openshift-lightspeed.svc:8025
    ▼
Mailpit (Pod in openshift-lightspeed)
    ├── Port 8025: HTTP API + Web UI
    └── Port 1025: SMTP server
```

## How Templates Use Mailpit

All software templates include a `notify` step that sends an email via the Backstage proxy:

```yaml
- id: notify
  name: Send Email Notification
  action: http:backstage:request
  input:
    method: POST
    path: /proxy/mailpit/api/v1/send
    body:
      From:
        Email: devhub@neuralbank.demo
        Name: Red Hat Developer Hub
      To:
        - Email: "${user}@workshop.demo"
          Name: "${user}"
      Subject: "Component Created: ${componentId}"
      HTML: "<h2>Your component is ready!</h2>"
```

## Viewing Emails

1. Open the **Mailpit Web UI** from the link above
2. All emails sent by Developer Hub templates appear in the inbox
3. Use the search bar to filter by subject, sender, or recipient
4. Click any email to view its full HTML content

## API Quick Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/messages` | List all messages (paginated) |
| `POST` | `/api/v1/send` | Send a test email |
| `GET` | `/api/v1/messages/{id}` | Get message by ID |
| `DELETE` | `/api/v1/messages/{id}` | Delete a message |
| `GET` | `/api/v1/search?query=...` | Search messages |

## Namespace & Deployment

- **Namespace**: `openshift-lightspeed`
- **Service**: `n8n-mailpit` (ports 8025/HTTP, 1025/SMTP)
- **Backstage proxy**: configured at `/mailpit` → `http://n8n-mailpit.openshift-lightspeed.svc:8025`
