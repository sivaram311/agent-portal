# Future Implementation — Master Plan

> Handbook + incremental delivery. Phases 0–3 have runnable pieces in Agent Portal.

## Why this exists

We treat this host like a **shared engineering office**: CSS for SSO, Agent Portal as the agent OS, Cloudflare/NGINX for named URLs, sandbox + port registry so AIs do not collide, and promote gates so nothing jumps straight to prod.

## Status (updated)

| Phase | Name | Status |
|-------|------|--------|
| **0** | Docs & protocols | **Done** — `docs/platform/*` |
| **1** | Sandbox cutover | **Done** — `E:\MyWorkspace\sandbox` + junctions; `AGENT_WORKSPACE_ROOT` via `.env` / host scripts |
| **2** | Port registry service | **Done (H2/JPA)** — `GET/POST /api/platform/ports*` |
| **3** | CSS App Home data | **API done** — `GET /api/platform/apps`, `/api/platform/home` (UI launcher still later) |
| **4** | DNS + proxy automation | Planned |
| **5** | Workflow sub-agents | **Skills done** — `ap-platform-ops|review|qa|state|em` |
| **6–8** | Role agents / EM / VirtualDev Co | Planned |

## Cursor skills (sub-agents)

| Skill | Role |
|-------|------|
| `ap-platform-ops` | Deploy / restart by port / edge |
| `ap-platform-review` | Git review / merge gates |
| `ap-platform-qa` | E2E + Agent API smoke |
| `ap-platform-state` | Ports / apps registry |
| `ap-platform-em` | Engineering Manager orchestration |

## Locked product decisions

1. Hierarchical orchestration first.
2. First departments: Architecture, Backend, Frontend (then QA).
3. Heavily human-supervised initially.
4. Multi-project: sandbox folder + CSS clientId + optional subdomain.

## APIs

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/platform/ports` | JWT |
| POST | `/api/platform/ports/claim` | JWT |
| POST | `/api/platform/ports/{port}/release` | JWT |
| GET | `/api/platform/apps` | JWT |
| GET | `/api/platform/home` | JWT |
| GET | `/api/agent/actions` | Public |

Client: `workspaces/agent-api/client/AgentApi.ps1` (`Get-PlatformPorts`, `Claim-PlatformPort`, `Get-PlatformHome`, …).
