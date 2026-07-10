# Agent standing orders — agent-api workspace

You are operating against **Agent Portal** and **CSS** on this machine.

## Must read first

- `docs/platform/ACCESS-PROTOCOLS.md`
- `docs/platform/PORT-REGISTRY.md`
- `docs/platform/AGENT-API.md`
- `docs/platform/WORKFLOW.md`

## Allowed

- Call CSS login and portal REST/WS as documented
- Create sessions with `workspacePath: "agent-api"` (or sandbox paths when Phase 1 lands)
- Write notes/scripts under this folder (no secrets)
- Update platform docs when you discover protocol gaps

## Forbidden

- Killing processes by name (`cursor`, `node`, `agent`)
- Binding ports without updating PORT-REGISTRY
- Committing `.env`, tokens, or passwords
- Promoting sandbox work straight to production hostnames
- Editing unrelated repos unless the human task says so

## Default identity

- CSS `clientId`: `agent-portal`
- Prefer HTTPS public URLs (`https://delena.buzz`) when the user is on the live site

<!-- agent-portal-managed: human-authored standing orders for API bridge workspace -->
