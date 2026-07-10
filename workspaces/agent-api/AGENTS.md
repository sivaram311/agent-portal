# Agent standing orders — agent-api workspace

You are operating against **Agent Portal** and **CSS** on this machine via **https://delena.buzz** (not the public IP; not delena.com).

## Must read first

- `docs/platform/ACCESS-PROTOCOLS.md`
- `docs/platform/PORT-REGISTRY.md`
- `docs/platform/AGENT-API.md`
- `docs/platform/WORKFLOW.md`
- `workspaces/agent-api/client/README.md`

## Allowed

- Call CSS login and portal REST/WS as documented (`AgentApi.ps1` defaults to delena.buzz)
- Create sessions with `workspacePath: "agent-api"`
- Write notes/scripts under this folder (no secrets)

## Forbidden

- Killing processes by name (`cursor`, `node`, `agent`)
- Binding ports without updating PORT-REGISTRY
- Committing `.env`, tokens, or passwords
- Using raw public IP in client defaults when domain works

## Default identity

- CSS `clientId`: `agent-portal`
- Public base: `https://delena.buzz` / `https://delena.buzz/api`
- Local override: `Connect-AgentApi -Local …`
