# Agent Portal MCP Bridge (Gemini Spark)

Exposes Agent Portal (Cursor CLI via ACP) as an **MCP** server so **Gemini Spark** (Chrome / gemini.google.com Connected Apps) can call it.

## Spark URL (PROD) — use this

```
https://agent-portal.delena.buzz/mcp/
```

Gemini Spark expects **Streamable HTTP** (not legacy `/sse`). Leave Client ID / Secret empty.

| Alias | Notes |
|-------|--------|
| `https://agent-portal.delena.buzz/mcp/` | **Preferred** Streamable HTTP |
| `https://agent-portal.delena.buzz/mcp/mcp` | Same transport, alternate path |
| `https://agent-portal.delena.buzz/mcp/sse` | Legacy SSE only — Spark URL check often fails |

## Tools

| Tool | What it does |
|------|----------------|
| `portal_health` | Portal `/api/health` |
| `list_sessions` | List sessions |
| `create_session` | New Cursor/Antigravity session |
| `send_prompt` | Prompt + wait for reply |
| `get_session_transcript` | Full transcript |
| `cancel_run` | Cancel in-flight run |
| `machine_context` | Host context snapshot |
| `machine_chat` | Machine Gateway chat (+ wait) |

## Connect in Gemini Spark

1. Open [gemini.google.com](https://gemini.google.com) → **Settings** → **Connected Apps**.
2. Remove any previous Agent Portal custom app if it failed.
3. **Add a custom app** and paste **only**:
   ```
   https://agent-portal.delena.buzz/mcp/
   ```
4. Leave **Advanced** Client ID / Secret **empty**.
5. Click **Next**.
6. In chat, type `@` and pick the app.

Requirements (Google): personal Google Account, US eligibility, Keep Activity on, Gemini Spark access.

## Auth model

| Layer | Behavior |
|-------|----------|
| Spark → bridge | Open by default (`MCP_REQUIRE_OAUTH=false`). Optional OAuth 2.1 + DCR/PKCE when set `true`. |
| Bridge → Portal | `CSS_USERNAME` + `CSS_PASSWORD` + `CSS_CLIENT_ID=agent-portal`, or `PORTAL_API_KEY` |

PROD Portal has `apiKeyFallbackEnabled=false`, so use **CSS login** credentials in `.env`.

Do **not** commit `.env`. Portal `clientId=agent-portal` is public.

OAuth discovery (when enabled):  
`https://agent-portal.delena.buzz/.well-known/oauth-protected-resource/mcp`

## Run (PROD host)

```powershell
cd E:\MyWorkspace\agent-portal\workspaces\agent-api\mcp-bridge
copy .env.example .env   # set CSS_PASSWORD
npm install
npm start                # listens :5430
```

Nginx (Deployment `conf/apps/agent-portal.delena.buzz.conf`):

- `location = /mcp` → 308 `/mcp/`
- `location /mcp/` → `http://127.0.0.1:5430/`
- `/.well-known/oauth-*` → `:5430` (before CSS JWKS `.well-known`)

Health: `https://agent-portal.delena.buzz/mcp/health`

## Ports

| Env | Port |
|-----|------|
| DEV | 3430 |
| PREPROD | 4430 |
| PROD | 5430 |

Platform index: [docs/platform/AGENT-API.md](../../../docs/platform/AGENT-API.md) · [PORT-REGISTRY.md](../../../docs/platform/PORT-REGISTRY.md)
