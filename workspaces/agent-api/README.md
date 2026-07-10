# agent-api workspace

**Purpose:** Bridge for external AIs and scripts to drive Agent Portal with the **same actions as the UI**.

## Artifacts

| Path | Role |
|------|------|
| [ACTIONS.md](ACTIONS.md) | UI action → HTTP map |
| [openapi/agent-api.yaml](openapi/agent-api.yaml) | OpenAPI 3 contract |
| [client/](client/) | PowerShell client (`AgentApi.ps1`) |
| [PROTOCOL.md](PROTOCOL.md) | Short call sequence |
| [AGENTS.md](AGENTS.md) | Standing orders for agents in this folder |

## Live discovery

```http
GET /api/agent/actions
```

Public. All other `/api/**` session mutations need Bearer JWT or `X-API-Key`.

CORS: any origin by default (`APP_CORS_ORIGINS=*`). Auth is unchanged.

## Create a session bound here

```json
{
  "title": "Agent API bridge",
  "workspacePath": "agent-api",
  "provider": "cursor"
}
```

## Docs

- [docs/platform/AGENT-API.md](../../docs/platform/AGENT-API.md)
- [docs/platform/ACCESS-PROTOCOLS.md](../../docs/platform/ACCESS-PROTOCOLS.md)
