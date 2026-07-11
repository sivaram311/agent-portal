# Future Implementation — Master Plan

> Incremental delivery. Phases 0–6 have runnable pieces.

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
| **7–8** | Full VirtualDev Co runtime | Planned (org dashboard, richer ACLs, auto-swarm) |

## Skills

`ap-platform-ops` · `ap-platform-review` · `ap-platform-qa` · `ap-platform-state` · `ap-platform-em`

## Key APIs

| Path | Purpose |
|------|---------|
| `/api/platform/ports` | Port leases |
| `/api/platform/apps` · `/home` | App launcher data |
| `/api/platform/roles` | VirtualDev role catalog |
| `/api/platform/tasks` | EM task graph (CRUD) |
| `/api/platform/tasks/{id}/session` | Link task → portal session |
| `/api/platform/memory` | Shared project knowledge (upsert by slug+key) |
| `/api/platform/messages` | Inter-agent message bus |
| `/api/platform/pipelines` · `.../{id}/run` | Workflow presets → task graph |
| `/api/agent/actions` | Discovery |

## UI

Top bar **Apps** → App Home (apps / roles / tasks / memory / messages / pipelines).

## Pipelines

`FEATURE` · `BUGFIX` · `REFACTOR` · `SECURITY_AUDIT`

```powershell
Start-PlatformPipeline -PipelineId FEATURE -Title "Profile page" -ProjectSlug profile-v1
Set-PlatformMemory -ProjectSlug profile-v1 -Key "api/contract" -Kind CONTRACT -Value "..."
Send-PlatformMessage -ProjectSlug profile-v1 -FromRole BACKEND -ToRole FRONTEND -Subject "DTO ready" -Body "..."
```

## DNS

```powershell
.\scripts\cloudflare-dns.ps1 -List
.\scripts\cloudflare-dns.ps1 -Upsert -Name myapp-sandbox -Proxied
```
