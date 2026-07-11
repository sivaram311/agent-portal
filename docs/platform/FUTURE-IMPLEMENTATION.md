# Future Implementation — Master Plan

> Incremental delivery. Phases 0–8 have runnable pieces. **DEV / PREPROD / PROD** are live on this machine (see [../OPS.md](../OPS.md#deployed-environments-2026-07-11)).

## Status

| Phase | Name | Status |
|-------|------|--------|
| **0** | Docs & protocols | **Done** |
| **1** | Sandbox cutover | **Done** — `E:\MyWorkspace\sandbox` |
| **2** | Port registry API | **Done** — `/api/platform/ports*` |
| **3** | CSS App Home | **Done** — API + Angular **Apps** sheet |
| **4** | DNS helpers | **Done** — `scripts/cloudflare-dns.ps1` (Ops-owned) |
| **5** | Workflow sub-agents | **Done** — skills + `/api/platform/tasks` + roles |
| **6** | Shared memory + swarm + pipelines | **Done** — memory, messages, pipeline presets, session link |
| **7** | Org dashboard + role ACLs + auto-swarm | **Done** — `/org`, role tools/actions, `/swarm/tick` |
| **8** | Runtime ACL enforcement | **Done** — session `platformRole`, permission deny, action gates, prompt hints |

## Live environments

| Env | URL | API | Profile / notes |
|-----|-----|-----|-----------------|
| DEV | `https://delena.buzz` | `:8080` | Workspace tree; CSS `:9000` |
| PREPROD | `https://agent-portal-staging.delena.buzz` | `:4080` | `preprod` + Postgres schema `preprod`; CSS prod `:5900` |
| PROD | `https://agent-portal.delena.buzz` | `:5080` | `prod` + schema `prod`; CSS prod `:5900` |

Postgres long strings: use `TEXT` / `@JdbcTypeCode(LONGVARCHAR)` — never `@Lob`/`CLOB` (see OPS).

## Still planned (beyond phases 0–8)

1. Dedicated Postgres control-plane event tables (`deploy_event`, `review_event`, …)
2. Long-lived sub-agent *services* (skills exist today)
3. Richer VirtualDev Co (more departments, auto session spawn)
4. Full NGINX-from-template deploy automation
5. Enforced review/merge bot writing `review_event`
6. **Dual-CLI subagent job runner** (Cursor `agent` + Antigravity `agy`) for promote/incident/docs — MyAgent role packs + Agent Portal predefined pipelines (`PROMOTE_Q1`/`Q2`). See `E:\MyAgent\ideas\dual-cli-subagent-workflow.md` and [SUBAGENTS-ROADMAP.md](SUBAGENTS-ROADMAP.md#dual-cli--portal-predefined-roadmap).

## Skills

`ap-platform-ops` · `ap-platform-review` · `ap-platform-qa` · `ap-platform-state` · `ap-platform-em` · `ap-e2e-realme-p2-pro` · `ap-system-e2e-loop`

## Key APIs

| Path | Purpose |
|------|---------|
| `/api/platform/ports` | Port leases |
| `/api/platform/apps` · `/home` | App launcher data |
| `/api/platform/org` | Org dashboard |
| `/api/platform/roles` · `/roles/{id}` | Role catalog + ACL/prompt hints |
| `/api/platform/tasks` | EM task graph (CRUD) |
| `/api/platform/tasks/{id}/session` | Link task → session (copies role onto session) |
| `/api/platform/memory` | Shared project knowledge |
| `/api/platform/messages` | Inter-agent message bus |
| `/api/platform/pipelines` · `.../{id}/run` | Workflow presets → task graph |
| `/api/platform/pipelines/runs/{runId}` | SYSTEM_E2E_LOOP progress timeline |
| `/api/platform/swarm/tick` | Advance pipeline handoffs |
| `/api/sessions` (+ `platformRole`) | Create session with role ACL |
| `/api/sessions/{id}/platform-role` | Bind/change VirtualDev role |
| `/api/agent/actions` | Discovery |

## Runtime ACL

When a session has `platformRole`:

1. Prompt prefix includes role tools/actions/hint.
2. ACP permission requests for disallowed tool categories are auto-rejected (`permission_acl_denied`).
3. Roles with `humanApprovalRequired` skip global auto-approve.
4. Portal actions (`listFiles`, `acceptChange`, …) return **403** if not in `allowedActions`.
5. Linking a platform task to a session copies the task role onto the session.

```powershell
New-AgentSession -Title "Backend work" -WorkspacePath "agent-api" -PlatformRole BACKEND
Set-AgentSessionRole -SessionId <id> -PlatformRole ARCHITECTURE
```

## UI

Top bar **Apps** → App Home (**Org** / apps / roles / tasks / memory / messages / pipelines).
Session cards/headers show a role chip when bound.

## Swarm

Marking a pipeline child `DONE` auto-assigns the next OPEN sibling and sends an EM handoff message. Or run:

```powershell
Invoke-PlatformSwarmTick -ProjectSlug profile-v1
Update-PlatformTask -TaskId <id> -Status DONE
```

## Pipelines

`FEATURE` · `BUGFIX` · `REFACTOR` · `SECURITY_AUDIT` · **`SYSTEM_E2E_LOOP`** (category SYSTEM — looping QA→fix→docs→review, default max 20)

See [SYSTEM-E2E-LOOP.md](SYSTEM-E2E-LOOP.md). Progress: `GET /api/platform/pipelines/runs/{runId}`.

## DNS

```powershell
.\scripts\cloudflare-dns.ps1 -List
.\scripts\cloudflare-dns.ps1 -Upsert -Name myapp-sandbox -Proxied
```
