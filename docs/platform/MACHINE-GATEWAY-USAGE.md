# Machine Gateway — usage guide

One REST entry for external AIs to read live host/control-plane context and optionally start a Cursor Auto chat with that context injected.

## Base URLs

| Env | API base |
|-----|----------|
| DEV | `https://delena.buzz/api` (or `http://127.0.0.1:8080/api`) |
| PREPROD | `https://agent-portal-staging.delena.buzz/api` |
| PROD | `https://agent-portal.delena.buzz/api` |

Canonical path: **`POST /api/machine`**.

Use **hostname URLs only**. Bare public-IP edges (`http://103.118.183.185` / `:4081`) are **disabled**.

## Auth

CSS JWT is required (PREPROD and PROD). `app.security.open-access` defaults to **false**.

1. Login once:

```http
POST https://agent-portal-staging.delena.buzz/auth/login
Content-Type: application/json

{ "username": "admin", "password": "<secret>", "clientId": "agent-portal" }
```

2. Call the gateway with:

```http
Authorization: Bearer <accessToken>
```

Or `X-API-Key` when the portal has `AGENT_PORTAL_API_KEY` configured. Never commit tokens or passwords.

Check: `GET /api/auth/config` → `"openAccess": false`, `"authRequired": true` (when CSS enabled).

## Canonical call — `POST /api/machine`

Always returns redacted live **context**. If `message` is present, also starts chat and returns **chat** with `status: "accepted"` and a `sessionId`. The agent reply is **not** in this HTTP response; follow the session (below).

Optional header: `X-Machine-Max-Mode: observe|advise|act|ops` (cannot raise above server `app.machine-gateway.max-mode`).

### Context only

Empty body, `{}`, or omit `message`:

```http
POST /api/machine
Authorization: Bearer <accessToken>
Content-Type: application/json

{}
```

```json
{
  "context": { "generatedAt": "...", "ttlSeconds": 15 },
  "chat": null
}
```

Poll context again using `ttlSeconds` as a hint (typically ~15s).

### Context + chat

```http
POST /api/machine
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "message": "What ports are leased?",
  "mode": "observe",
  "provider": "cursor",
  "sessionId": null
}
```

```json
{
  "context": { "...": "snapshot" },
  "chat": {
    "sessionId": "...",
    "status": "accepted",
    "mode": "observe",
    "platformRole": "GATEWAY_OBSERVE",
    "workspacePath": "...",
    "userMessage": { }
  }
}
```

Reuse `sessionId` on later calls to continue the same gateway session.

## Reading the agent reply

`POST /api/machine` (and `/chat`) only **accept** the run. Live output uses existing Agent Portal session channels:

| Method | Path | Use |
|--------|------|-----|
| GET | `/api/sessions/{sessionId}/messages` | Poll transcript |
| GET | `/api/sessions/{sessionId}/events` | Poll history events |
| WS | `/ws` (SockJS/STOMP) | Stream like the UI; pass `access_token` when CSS is enabled |

## Modes

| Mode | Platform role | Intent |
|------|---------------|--------|
| `observe` | `GATEWAY_OBSERVE` | Read-only |
| `advise` | `GATEWAY_ADVISE` | Read + memory notes |
| `act` | `GATEWAY_ACT` | Edits under workspace/sandbox |
| `ops` | `GATEWAY_OPS` | Ops tools; shell kills only via LocalPort→PID allowlisted shapes |

Default mode is `act`, still clamped by server max mode (often `act`).

## Aliases (v0)

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/machine` | **Canonical** — context + optional chat |
| `GET` | `/api/machine/context` | Context snapshot only |
| `POST` | `/api/machine/chat` | Chat accept only (`message` required) |

Discovery: `GET /api/agent/actions` → `machine`, `machineContext`, `machineChat`.

## Hard rules

Follow [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md): no secrets in git, no mass-kill by process name, write under sandbox/workspace, prefer hostnames over advertising the public IP.

Contract detail: [MACHINE-GATEWAY.md](MACHINE-GATEWAY.md).
