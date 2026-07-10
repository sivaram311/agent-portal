# Future Implementation — Master Plan

> **Scope of this document:** Planning and contracts. Nothing here is required to be running yet except what already ships in Agent Portal / CSS today.

## Why this exists

We treat this host like a **shared engineering office**:

- Multiple humans and AI agents work on projects.
- Everyone must follow the **same workflow and machine rules**.
- Agent Portal is the **operating system** for agent sessions today and for **VirtualDev Co** multi-agent orchestration later.
- Centralized Security System (CSS) is the **single identity** for every app we build.
- Cloudflare + NGINX hide the public IP behind **named subdomains**.
- Production is never “whatever the agent just wrote” — it is a **versioned promote** path.

## What already exists (foundation)

| Capability | Where |
|------------|--------|
| Dual-provider agent sessions (Cursor / Antigravity) | Agent Portal |
| Realtime streaming, Changes Keep/Restore, Guidance | Agent Portal |
| CSS JWT SSO + registered clients | `centralized-security-system` |
| Host stack + delena.buzz proxy | OPS / DELENA-PROXY |
| Playwright mobile QA | `e2e/` |
| Sample workspaces | `workspaces/demo`, `workspaces/FileBridge` |
| **Agent API workspace (new)** | `workspaces/agent-api` |

## What is Future Implementation

| Track | Summary | Doc |
|-------|---------|-----|
| Machine protocols | Access rules, port claims, sandbox-only writes | ACCESS-PROTOCOLS, PORT-REGISTRY, SANDBOX |
| Doc index | Discover all E: markdown from one catalog | indexes/* |
| CSS App Home | After login, launcher of production apps | CSS-APP-HOME |
| Cloudflare subdomains | Auto DNS + NGINX for sandbox/prod apps | CLOUDFLARE-DNS-PROXY |
| Versioned promote | Never direct-to-prod; sandbox → release → staging → prod | VERSIONING-PROMOTE |
| Workflow sub-agents | Deploy, Postgres state, Git review/merge, QA | SUBAGENTS-ROADMAP |
| VirtualDev Co | Hierarchical multi-agent company | VIRTUALDEV-CO |
| Postgres control plane | Port leases, agent runs, promote history | SUBAGENTS-ROADMAP |

## Phased roadmap

| Phase | Name | Outcome | When |
|-------|------|---------|------|
| **0** | Docs & protocols (this pass) | Handbook + `agent-api` workspace + indexes | Now |
| **1** | Sandbox cutover | `AGENT_WORKSPACE_ROOT` → `E:\MyWorkspace\sandbox`; portal enforces it | Next |
| **2** | Port registry service | Markdown → Postgres `port_lease`; agents must claim | Near-term |
| **3** | CSS App Home | Launcher UI + client metadata (url, subdomain, env) | Near-term |
| **4** | DNS + proxy automation | Cloudflare API + NGINX conf generation for subdomains | Mid |
| **5** | Workflow sub-agents | Deploy / Review-Merge / QA / State agents with Postgres audit | Mid |
| **6** | Role agents | Architecture, Backend, Frontend specialists | Mid |
| **7** | Orchestrator | Engineering Manager hierarchical delegation | Later |
| **8** | Full VirtualDev Co | Departments, shared memory, pipelines | Later |

## Locked product decisions

1. **Orchestration model:** Hierarchical (Manager → specialists) first.
2. **First departments:** Architecture, Backend, Frontend (then QA).
3. **Autonomy:** Heavily **human-supervised** at the start (Changes / approvals).
4. **Multi-project:** Yes — each project gets sandbox folder + CSS clientId + optional subdomain.

## Non-goals (Phase 0)

- Implementing orchestrator code
- Auto-killing processes by broad name match
- Mass-moving `E:\Source` markdown into this repo
- Pointing live `AGENT_WORKSPACE_ROOT` at sandbox until Phase 1 is explicitly executed
