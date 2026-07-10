# Agent API — how any AI talks to our application

**Status:** Contract for Phase 0. Use with workspace [`workspaces/agent-api/`](../../workspaces/agent-api/).

External AIs (Cursor, ChatGPT tools, scripts, other agents) should treat Agent Portal + CSS as the **HTTP/WS control plane**. Do not SSH-scrape the UI.

## Base URLs

| Env | UI | API | CSS |
|-----|----|-----|-----|
| Local host | `http://127.0.0.1:4200` | `http://127.0.0.1:8080` | `http://127.0.0.1:9000` |
| Public HTTPS | `https://delena.buzz` | `https://delena.buzz/api` | `https://delena.buzz/auth` |

On delena, API paths are under `/api` via NGINX; WebSocket under `/ws`.

## Auth sequence (CSS)

1. `POST {CSS}/auth/login`  
   Body: `{ "username", "password", "clientId": "agent-portal" }`
2. Receive `accessToken` (and refresh if provided).
3. Call portal with `Authorization: Bearer <accessToken>`.
4. SockJS/STOMP: pass `access_token` query param on `/ws/**` when CSS is enabled.

Dev seeds (change for real prod): `admin` / `admin123`, `demo` / `demo123`.

Optional fallback: `X-API-Key` when portal API key mode is configured (see OPS).

## Core portal endpoints (summary)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/health` | Capabilities / readiness |
| GET | `/api/auth/config` | Whether CSS is required |
| GET | `/api/sessions` | List sessions |
| POST | `/api/sessions` | Create session (`title`, `workspacePath`, `provider`, …) |
| POST | `/api/sessions/{id}/prompt` | Send user prompt |
| GET | `/api/sessions/{id}/messages` | Transcript |
| GET/POST | `/api/sessions/{id}/changes`… | Diff / Keep / Restore |
| GET/PUT | `/api/sessions/{id}/guidance` | Rules & Skills for session |
| WS | `/ws` | STOMP topics `/topic/sessions/{id}` |

Full behavior: root [README.md](../../README.md) and [OPS.md](../OPS.md).

## Workspace path rule

`workspacePath` must resolve under `AGENT_WORKSPACE_ROOT`.

For API experiments, prefer:

```text
agent-api
```

(relative → `workspaces/agent-api`).

## Minimal curl sketch

```powershell
# Login
$login = curl.exe -sS -X POST http://127.0.0.1:9000/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"admin123","clientId":"agent-portal"}' | ConvertFrom-Json
$token = $login.accessToken

# Health
curl.exe -sS http://127.0.0.1:8080/api/health -H "Authorization: Bearer $token"

# Create session (example)
curl.exe -sS -X POST http://127.0.0.1:8080/api/sessions `
  -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d '{"title":"API Bridge","workspacePath":"agent-api","provider":"cursor"}'
```

## Protocol for external AIs

1. Read [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md) and [PORT-REGISTRY.md](PORT-REGISTRY.md).
2. Authenticate via CSS; never embed long-lived passwords in repos.
3. Create or reuse a session bound to `agent-api` (or a sandbox project path).
4. Prefer prompt + websocket events over scraping the Angular DOM.
5. Use Changes APIs for review; do not assume disk writes outside workspace root.
6. Log actions in chat/docs; Future: Postgres audit via State sub-agent.

## Future Implementation

- Machine-to-machine client credentials in CSS
- Signed agent-to-agent messaging bus
- Rate limits / quotas per external AI identity
- OpenAPI export checked into `workspaces/agent-api/openapi/`
