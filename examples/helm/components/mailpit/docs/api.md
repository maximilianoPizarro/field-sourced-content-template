# Mailpit API Reference

## Base URL

**Internal (cluster):**
```
http://n8n-mailpit.openshift-lightspeed.svc:8025
```

**Via Backstage proxy:**
```
/proxy/mailpit
```

## Endpoints

### List Messages

```bash
GET /api/v1/messages?start=0&limit=50
```

Response:
```json
{
  "total": 42,
  "messages": [
    {
      "ID": "abc123",
      "Subject": "Component Created: my-service",
      "From": { "Name": "Developer Hub", "Address": "devhub@neuralbank.demo" },
      "To": [{ "Name": "user1", "Address": "user1@workshop.demo" }],
      "Created": "2026-04-09T10:30:00Z"
    }
  ]
}
```

### Send Email

```bash
POST /api/v1/send
Content-Type: application/json

{
  "From": { "Email": "test@demo.local", "Name": "Test Sender" },
  "To": [{ "Email": "user@demo.local", "Name": "Recipient" }],
  "Subject": "Test Email",
  "HTML": "<h1>Hello</h1><p>This is a test email.</p>",
  "Text": "Hello, this is a test email."
}
```

### Get Message

```bash
GET /api/v1/messages/{id}
```

### Delete Message

```bash
DELETE /api/v1/messages/{id}
```

### Search

```bash
GET /api/v1/search?query=neuralbank
```

## Testing from CLI

```bash
# List recent messages
curl -s https://n8n-mailpit-openshift-lightspeed.<domain>/api/v1/messages | jq '.messages[:3]'

# Send a test email
curl -X POST https://n8n-mailpit-openshift-lightspeed.<domain>/api/v1/send \
  -H "Content-Type: application/json" \
  -d '{
    "From": {"Email": "test@demo.local", "Name": "CLI Test"},
    "To": [{"Email": "admin@demo.local", "Name": "Admin"}],
    "Subject": "CLI Test Email",
    "Text": "Sent from curl"
  }'
```
