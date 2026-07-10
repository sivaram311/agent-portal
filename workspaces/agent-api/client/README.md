# Agent API PowerShell client

Defaults to the **public domain** (not the raw public IP):

| Setting | Default |
|---------|---------|
| API | `https://delena.buzz/api` |
| CSS | `https://delena.buzz` |

(`delena.com` is a different site — use **`delena.buzz`**.)

## Quick start

```powershell
cd E:\MyWorkspace\agent-portal\workspaces\agent-api\client
. .\AgentApi.ps1

Connect-AgentApi -Username admin -Password '<password>'
Get-AgentApiConfig
Get-AgentHealth

$session = New-AgentSession -Title 'API bridge' -WorkspacePath agent-api -Provider cursor
Get-AgentMessages -SessionId $session.id
```

Local stack instead:

```powershell
Connect-AgentApi -Local -Username admin -Password '<password>'
```

## Auth

- CSS JWT (default): `Connect-AgentApi -Username … -Password …`
- API key: `Connect-AgentApi -ApiKey …` when portal has `AGENT_PORTAL_API_KEY` set

Mutations require auth. `GET /api/agent/actions` and health stay public.

## Docs

- [ACTIONS.md](../ACTIONS.md)
- [openapi/agent-api.yaml](../openapi/agent-api.yaml)
- [docs/platform/AGENT-API.md](../../../docs/platform/AGENT-API.md)
