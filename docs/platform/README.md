# Platform docs (Future Implementation)

**Status:** Documentation and workflow contracts only. Runtime orchestration, CSS App Home, Postgres port leases, and deploy/review sub-agents are **not built yet**.

This folder is the **control-plane handbook** for humans and AI agents working on this machine and evolving Agent Portal into a multi-agent platform (“VirtualDev Co”).

## Start here

| Doc | Purpose |
|-----|---------|
| [FUTURE-IMPLEMENTATION.md](FUTURE-IMPLEMENTATION.md) | Master roadmap: phases, what exists vs planned |
| [WORKFLOW.md](WORKFLOW.md) | Team/AI workflow — how we plan, build, review, promote |
| [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md) | Hard rules for touching this machine |
| [PORT-REGISTRY.md](PORT-REGISTRY.md) | Who owns which TCP port |
| [SANDBOX.md](SANDBOX.md) | Sandbox root and agent write boundaries |
| [MACHINE-MAP.md](MACHINE-MAP.md) | E: drive / MyWorkspace / Source layout |
| [AGENT-API.md](AGENT-API.md) | How any AI talks to Agent Portal + CSS |
| [CSS-APP-HOME.md](CSS-APP-HOME.md) | Post-login app launcher (planned) |
| [CLOUDFLARE-DNS-PROXY.md](CLOUDFLARE-DNS-PROXY.md) | Subdomains → NGINX → host services |
| [VERSIONING-PROMOTE.md](VERSIONING-PROMOTE.md) | Sandbox → version → staging → prod |
| [VIRTUALDEV-CO.md](VIRTUALDEV-CO.md) | Multi-agent virtual company vision |
| [SUBAGENTS-ROADMAP.md](SUBAGENTS-ROADMAP.md) | Deploy / Postgres / Git-review / QA agents |
| [indexes/INDEX-MYWORKSPACE.md](indexes/INDEX-MYWORKSPACE.md) | Indexed markdown under MyWorkspace |
| [indexes/INDEX-SOURCE.md](indexes/INDEX-SOURCE.md) | Indexed doc roots under E:\Source |

## Related (current product)

- [../OPS.md](../OPS.md) — run Agent Portal + CSS today
- [../DELENA-PROXY.md](../DELENA-PROXY.md) — delena.buzz reverse proxy
- [../ROADMAP.md](../ROADMAP.md) — shipped Agent Portal tracks
- Workspace for API experiments: [`../../workspaces/agent-api/`](../../workspaces/agent-api/)

## Rule for every agent

1. Read **ACCESS-PROTOCOLS** and **PORT-REGISTRY** before binding ports or killing processes.
2. Write agent-generated code only under **sandbox** / allowed workspace paths.
3. Prefer **docs over tribal knowledge** — update this folder when protocols change.
4. Do not promote to production without the **VERSIONING-PROMOTE** gates (when implemented).
