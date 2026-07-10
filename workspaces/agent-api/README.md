# agent-api workspace

**Purpose:** Dedicated Agent Portal workspace where **external AI agents and scripts talk to our application** via the HTTP/WS Agent API — not by scraping the UI.

## Use this folder when

- You are an AI integrating with Agent Portal + CSS
- You need a safe `workspacePath` for API-created sessions
- You are drafting OpenAPI samples, curl scripts, or inter-agent notes

## Protocol

1. Read [docs/platform/AGENT-API.md](../../docs/platform/AGENT-API.md)
2. Read [docs/platform/ACCESS-PROTOCOLS.md](../../docs/platform/ACCESS-PROTOCOLS.md)
3. Read [docs/platform/PORT-REGISTRY.md](../../docs/platform/PORT-REGISTRY.md)
4. Follow [AGENTS.md](AGENTS.md) in this folder

## Create a portal session bound here

```json
{
  "title": "Agent API bridge",
  "workspacePath": "agent-api",
  "provider": "cursor"
}
```

`workspacePath` is relative to `AGENT_WORKSPACE_ROOT` (today: `agent-portal/workspaces`).

## Layout

| Path | Role |
|------|------|
| `README.md` | This file |
| `AGENTS.md` | Standing orders for agents in this workspace |
| `PROTOCOL.md` | Short call sequence |
| `scratch/` | Optional notes/scripts (no secrets) |

## Future

- Checked-in OpenAPI snapshot
- Example STOMP client
- Machine-to-machine CSS client for automation
