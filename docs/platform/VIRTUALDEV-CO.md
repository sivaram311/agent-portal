# VirtualDev Co — Multi-Agent Orchestration Vision

**Status:** Product vision + shipped foundation. Agent Portal is the **runtime OS**; VirtualDev Co orchestration APIs (roles, tasks, pipelines, memory, org, ACLs) are live. Richer departments and autonomous workers remain future.

Also known as **AgentForge** — a virtual MNC-style software company where specialized AI agents collaborate across departments.

## Foundation (today)

Agent Portal already provides:

- Multi-session workspace, dual providers (Cursor / Antigravity)
- Realtime streaming, permissions, Changes Keep/Restore
- Guidance (Rules & Skills), sharing via CSS, audit
- Mobile-ready UI on delena.buzz (DEV); PREPROD/PROD on `agent-portal-*.delena.buzz`

That becomes the **runtime** for department agents and the Engineering Manager orchestrator.

## Department structure

| Department | Virtual roles | Responsibility | Fit now |
|------------|---------------|----------------|---------|
| Product | PM, BA | PRD, user stories | Medium |
| Design | UI/UX, design system | Wireframes, tokens | Medium |
| Architecture | Solution architect, tech lead | Design, stack | High |
| Backend | Backend engineers | APIs, DB, logic | Very High |
| Frontend | Frontend engineers | UI, state | Very High |
| Mobile | Mobile engineers | Cross-platform | High |
| QA | QA, automation | Tests, bugs | Medium |
| DevOps | DevOps, SRE | CI/CD, edge, monitor | Medium |
| Security | Security, reviewer | Audits, fixes | Medium |
| Documentation | Tech writer | Guides, API docs | Low |
| Project Management | EM, Scrum | Allocation, coordination | High |

## Orchestration models

### 1. Hierarchical (start here)

```text
User goal → Engineering Manager
  ├─ Product (clarify)
  ├─ Architect (design)
  ├─ Backend + Frontend (parallel)
  ├─ QA (test)
  └─ DevOps (deploy via promote gates)
```

### 2. Swarm / collaborative (later)

Agents message each other (shared memory + bus). Example: Backend asks Frontend for API contract.

### 3. Pipeline / workflow (later)

Fixed pipelines: Feature, Bugfix, Refactor, Security audit, **System E2E loop** (`SYSTEM_E2E_LOOP` — see [SYSTEM-E2E-LOOP.md](SYSTEM-E2E-LOOP.md)).

## Key capabilities to build

| Capability | Priority |
|------------|----------|
| Agent Registry (roles, skills, tools) | High |
| Task Orchestrator (Manager delegates) | High |
| Shared Memory / project knowledge | High |
| Inter-agent communication | Medium |
| Human-in-the-loop approvals | High |
| Agent memory per project | Medium |
| Tool access control per role | High | **Done** — runtime ACL on sessions |
| Org dashboard (active agents/tasks) | Medium | **Done** — `/api/platform/org` |

## Locked answers

1. **Model:** Hierarchical first.
2. **First departments:** Architecture, Backend, Frontend (then QA).
3. **Supervision:** Heavily human-supervised initially (Changes + Reviewer).
4. **Multi-project:** Yes — sandbox folder + CSS clientId + subdomain per project.

## Example future flow

**Prompt:** “Build user profile page with avatar upload and activity feed.”

1. Engineering Manager creates tasks.
2. Specialists run in portal sessions (or child sessions) with role prompts + tool ACLs.
3. Humans review via Changes / PR.
4. DevOps agent proposes staging deploy; Releaser promotes version.

## Phased delivery

| Phase | Focus | Status |
|-------|--------|--------|
| 1 | Portal polish + platform docs | Done |
| 2 | Role-based agent presets (prompts + tools) | Done (roles API + skills) |
| 3 | Manager delegation (task graph) | Done (`/api/platform/tasks`, pipelines) |
| 4 | Shared memory + messaging | Done (`/memory`, `/messages`) |
| 5 | Full virtual company + org dashboard | Done (`/api/platform/org`, role ACLs, `/swarm/tick`) |

See also [FUTURE-IMPLEMENTATION.md](FUTURE-IMPLEMENTATION.md) and [SUBAGENTS-ROADMAP.md](SUBAGENTS-ROADMAP.md).
