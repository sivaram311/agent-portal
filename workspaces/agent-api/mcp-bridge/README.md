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
| `send_prompt` | Prompt + wait for reply (optional `timeoutMs`) |
| `get_session_transcript` | Full transcript |
| `cancel_run` | Cancel in-flight run |
| `machine_context` | Host context snapshot |
| `machine_chat` | Machine Gateway chat (+ wait; optional `timeoutMs`, `waitForReply`) |

### Wait / timeout behaviour

`send_prompt` and `machine_chat` poll until the session leaves `STREAMING`. External MCP clients (Grok, Spark, etc.) often time out in 10–60s, so keep bridge waits shorter than the client.

| Parameter / env | Default | Notes |
|-----------------|---------|--------|
| Tool arg `timeoutMs` | — | Per-call absolute wait; overrides env |
| `MCP_WAIT_TIMEOUT_MS` | unset | **Recommended prod: `90000`** |
| `MCP_WAIT_INTERVAL_MS` | `1000` | Poll interval |
| `MCP_WAIT_MAX_ATTEMPTS` | `300` | Fallback when no `timeoutMs` (≈5 min at 1s) |

On timeout the tool returns `isError: true` with `sessionId`, final status, waited duration, and a hint to use `get_session_transcript` later or `waitForReply=false` on `machine_chat`.

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
