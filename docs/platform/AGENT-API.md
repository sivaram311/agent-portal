# Agent API — how any AI talks to our application

**Status:** Shipped contract + discovery + clients. Mutating calls require **CSS JWT** or **X-API-Key**. CORS defaults to **any origin** (`APP_CORS_ORIGINS=*`).

Workspace: [`workspaces/agent-api/`](../../workspaces/agent-api/).

## Quick links

| Artifact | Path |
|----------|------|
| Action map (UI → HTTP) | [`workspaces/agent-api/ACTIONS.md`](../../workspaces/agent-api/ACTIONS.md) |
| OpenAPI 3 | [`workspaces/agent-api/openapi/agent-api.yaml`](../../workspaces/agent-api/openapi/agent-api.yaml) |
| PowerShell client | [`workspaces/agent-api/client/`](../../workspaces/agent-api/client/) |
| Live catalog | `GET /api/agent/actions` (public) |

## Base URLs

| Env | UI | API | CSS |
|-----|----|-----|-----|
| **Public (default for clients)** | `https://delena.buzz` | `https://delena.buzz/api` | `https://delena.buzz/auth` |
| Local host | `http://127.0.0.1:4200` | `http://127.0.0.1:8080/api` | `http://127.0.0.1:9000` |

Use **`delena.buzz`**, not the public IP and not `delena.com` (unrelated domain).

## Security

| Layer | Behavior |
|-------|----------|
| CORS | `Access-Control-Allow-Origin` via pattern `*` (any browser/AI origin) |
| Auth | Still required for `/api/**` except health, auth/config, presets, **agent/actions** |
| CSS | `Authorization: Bearer <accessToken>` after `POST {CSS}/auth/login` with `clientId=agent-portal` |
| API key | Optional `X-API-Key` when `AGENT_PORTAL_API_KEY` / `app.security.api-key` is set |
| WS | SockJS `/ws/**` — pass `access_token` when CSS enabled |

Dev seeds: `admin` / `admin123` (change for real prod).

## Auth sequence

1. `POST {CSS}/auth/login` → `{ "username", "password", "clientId": "agent-portal" }`
2. Call portal with `Authorization: Bearer <accessToken>`
3. Prefer prompt + STOMP over scraping the UI

## UI-parity actions (summary)

Same verbs as the Angular app: sessions, prompt, cancel, permissions, files, changes Keep/Restore, share, guidance, archive, audit. Full table in **ACTIONS.md**; machine-readable list at **`GET /api/agent/actions`**.

Default `workspacePath` for API sessions: **`agent-api`**.

## PowerShell example

```powershell
cd E:\MyWorkspace\agent-portal\workspaces\agent-api\client
. .\AgentApi.ps1
Connect-AgentApi -Username admin -Password '<password>'
Get-AgentHealth
New-AgentSession -Title 'API bridge' -WorkspacePath agent-api -Provider cursor
```

## Protocol for external AIs

1. Read [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md) and [PORT-REGISTRY.md](PORT-REGISTRY.md).
2. Discover actions: `GET /api/agent/actions` or OpenAPI.
3. Authenticate; never commit tokens.
4. Create/reuse session on `agent-api` (or sandbox path).
5. Use Changes APIs before promote.

## Config

```properties
app.cors.allowed-origins=${APP_CORS_ORIGINS:*}
```

Override with a tight list only if you intentionally lock browsers down; Agent API consumers expect open CORS + strong auth.
