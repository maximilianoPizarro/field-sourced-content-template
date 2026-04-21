---
layout: default
title: "Hands-On: Deploy and Test the Neuralbank Stack"
nav_order: 12
---

In this activity you will deploy the **complete Neuralbank stack** using the three Software Templates available in Developer Hub, then verify each component is working using `curl` from the terminal.

## Overview

You will create three components in sequence:

| Template | Description | What it deploys |
|----------|-------------|-----------------|
| **Neuralbank: Backend API** | Quarkus REST API with customer management | Deployment, Service, Pipeline, Gateway, HTTPRoute |
| **Neuralbank: Frontend** | Web application for credit visualization | Deployment, Service, Route |
| **Customer Service MCP** | MCP server exposing backend as AI-callable tools | Deployment, Service, Pipeline, EventListener |

All three deploy to namespace `YOUR_USER-neuralbank` and are connected.

## Step 1: Deploy the Backend

1. Log in to **Developer Hub**: `https://backstage-developer-hub-developer-hub.<CLUSTER_DOMAIN>`
2. Click **Create** in the left sidebar.
3. Select **"Neuralbank: Backend API"**.
4. Fill: **Name** = `neuralbank-backend`, **Owner** = `YOUR_USER`
5. Click **Create** and wait for all steps to complete.

## Step 2: Deploy the Frontend

1. Go to **Create** > Select **"Neuralbank: Frontend"**.
2. Fill: **Name** = `neuralbank-frontend`, **Owner** = `YOUR_USER`
3. Click **Create**.

## Step 3: Deploy the MCP Server

1. Go to **Create** > Select **"Customer Service MCP"**.
2. Fill: **Name** = `customer-service-mcp`, **Owner** = `YOUR_USER`
3. Click **Create**.

## Step 4: Verify in ArgoCD

Open ArgoCD and confirm three apps are **Synced** and **Healthy**:

```
https://openshift-gitops-server-openshift-gitops.<CLUSTER_DOMAIN>
```

## Step 5: Wait for Pipelines

```bash
oc get pipelinerun -n YOUR_USER-neuralbank
```

Wait until status shows `Succeeded` (3-5 minutes).

## Step 6: Test the Backend API with curl (OIDC)

### 6.1 — Get a Bearer Token from Keycloak

```bash
TOKEN=$(curl -s -X POST \
  "https://rhbk.<CLUSTER_DOMAIN>/realms/neuralbank/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=neuralbank-frontend" \
  -d "username=user1" \
  -d "password=Welcome123!" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

echo "Token: ${TOKEN:0:50}..."
```

### 6.2 — List all customers

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool | head -30
```

### 6.3 — Get a customer by ID

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers/1" \
  | python3 -m json.tool
```

### 6.4 — Get credit score

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers/1/credit-score" \
  | python3 -m json.tool
```

### 6.5 — Create a new customer

```bash
curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Workshop",
    "apellido": "Demo",
    "email": "workshop@neuralbank.io",
    "tipoCliente": "PERSONAL",
    "ciudad": "Buenos Aires",
    "pais": "Argentina"
  }' \
  "https://neuralbank.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

## Step 7: Test the NFL Wallet API (API Key)

API Key is simpler — no token exchange needed:

### 7.1 — Without API Key (expect 401)

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers
```

### 7.2 — With valid API Key (expect 200)

```bash
curl -s -H "X-API-Key: nfl-wallet-demo-key-2024" \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool | head -20
```

### 7.3 — Create a customer

```bash
curl -s -X POST \
  -H "X-API-Key: nfl-wallet-demo-key-2024" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "API Key",
    "apellido": "Test",
    "email": "apikey@wallet.io",
    "tipoCliente": "EMPRESA",
    "ciudad": "Miami",
    "pais": "USA"
  }' \
  "https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers" \
  | python3 -m json.tool
```

### 7.4 — With invalid API Key (expect 401)

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -H "X-API-Key: invalid-key-12345" \
  https://nfl-wallet.<CLUSTER_DOMAIN>/api/v1/customers
```

## Step 8: Access the Frontend

Open in your browser:

```
https://neuralbank.<CLUSTER_DOMAIN>
```

Log in with `YOUR_USER` / `Welcome123!` to see the credit visualization dashboard.

## Step 9: Explore in Developer Hub

Go back to Developer Hub and explore:

- **Catalog** — Search for your components
- **Topology** — Kubernetes resources as a visual map
- **CI** — Tekton pipeline execution status
- **API** — OpenAPI spec for the backend
- **TechDocs** — Auto-generated documentation

## Authentication Models Comparison

| | OIDC (Neuralbank) | API Key (NFL Wallet) |
|---|---|---|
| **How to authenticate** | Get JWT from Keycloak, pass as `Authorization: Bearer <token>` | Include key as `X-API-Key: <key>` |
| **Steps to test** | 2 steps: get token + call API | 1 step: call API with key |
| **Best for** | Web apps with user login | Scripts, CI/CD, M2M |
| **Token expiry** | Short-lived (minutes) | No expiry (revoke by deleting Secret) |

> **Tip:** For quick terminal testing, **API Key** is faster. For production web apps, **OIDC** provides better security.

### Screenshots

![Developer Hub — Template Creation]({{ site.baseurl }}/assets/images/neuroface.png)

![Developer Hub — Catalog Overview]({{ site.baseurl }}/assets/images/neuroface2.png)

![Developer Hub — CI Pipeline]({{ site.baseurl }}/assets/images/neuroface3.png)

![Developer Hub — Frontend and Notifications]({{ site.baseurl }}/assets/images/neuroface4.png)
