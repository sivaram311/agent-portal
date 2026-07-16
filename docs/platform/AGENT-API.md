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
| **DEV (default client)** | `https://delena.buzz` | `https://delena.buzz/api` | `https://delena.buzz/auth` → css-next `:5910` |
| **PREPROD** | `https://agent-portal-staging.delena.buzz` | same-origin `/api` (`:4080`) | same-origin `/auth` → css-next `:5910` |
| **PROD** | `https://agent-portal.delena.buzz` | same-origin `/api` (`:5080`) | same-origin `/auth` → css-next `:5910` |
| Local host | `http://127.0.0.1:4200` | `http://127.0.0.1:8080/api` | `http://127.0.0.1:9000` |

Use **`delena.buzz` hostnames**, not the public IP and not `delena.com` (unrelated domain). PREPROD/PROD login against **prod CSS** (`clientId=agent-portal`). Unauthenticated `/api/**` → **403** is expected.

## Security

| Layer | Behavior |
|-------|----------|
| CORS | `Access-Control-Allow-Origin` via pattern `*` (any browser/AI origin) |
| Auth | Still required for `/api/**` except health, auth/config, presets, **agent/actions** |
| CSS | `Authorization: Bearer <accessToken>` after `POST {CSS}/auth/login` with `clientId=agent-portal` |
| API key | Optional `X-API-Key` when `AGENT_PORTAL_API_KEY` / `app.security.api-key` is set |
| WS | SockJS `/ws/**` — pass `access_token` when CSS enabled |

Dev seeds (DEV CSS only): `admin` / `admin123`. Prod admin password is in `G:\apps\css\.env` — never commit it.

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
# Defaults to https://delena.buzz (use -Local for 127.0.0.1)
Connect-AgentApi -Username admin -Password '<password>'
Get-AgentHealth
New-AgentSession -Title 'API bridge' -WorkspacePath agent-api -Provider cursor
```

## Protocol for external AIs

1. Read [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md) and [PORT-REGISTRY.md](PORT-REGISTRY.md).
2. Discover actions: `GET /api/agent/actions` or OpenAPI.
3. Authenticate; never commit tokens.
4. Prefer **Machine Gateway** for host-wide awareness: [MACHINE-GATEWAY.md](MACHINE-GATEWAY.md) / [MACHINE-GATEWAY-USAGE.md](MACHINE-GATEWAY-USAGE.md). Canonical: `POST /api/machine` (context + optional chat). Aliases: `GET /api/machine/context`, `POST /api/machine/chat`. Live run output: `/ws` or poll session `messages`/`events`.
5. Create/reuse session on `agent-api` (or sandbox path) for workspace-scoped work.
6. Use Changes APIs before promote.

## Config

```properties
app.cors.allowed-origins=${APP_CORS_ORIGINS:*}
```

Override with a tight list only if you intentionally lock browsers down; Agent API consumers expect open CORS + strong auth.
