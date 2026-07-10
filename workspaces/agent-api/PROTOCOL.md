# Agent API — short protocol

## 1. Authenticate

`POST /auth/login` on CSS (`:9000` or `https://delena.buzz/auth/login`)

```json
{ "username": "admin", "password": "admin123", "clientId": "agent-portal" }
```

Use `accessToken` as `Authorization: Bearer …`.

## 2. Verify

`GET /api/health` on portal (`:8080` or `https://delena.buzz/api/health`)

## 3. Session

`POST /api/sessions` with `workspacePath: "agent-api"`.

## 4. Work

`POST /api/sessions/{id}/prompt` and subscribe to STOMP `/topic/sessions/{id}`.

## 5. Review

Use Changes APIs / human Changes tab before any promote.

Full detail: [docs/platform/AGENT-API.md](../../docs/platform/AGENT-API.md)
