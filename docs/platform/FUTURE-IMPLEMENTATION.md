# Future Implementation — Master Plan

> Incremental delivery. Phases 0–5 have runnable pieces.

## Status

| Phase | Name | Status |
|-------|------|--------|
| **0** | Docs & protocols | **Done** |
| **1** | Sandbox cutover | **Done** — `E:\MyWorkspace\sandbox` |
| **2** | Port registry API | **Done** — `/api/platform/ports*` |
| **3** | CSS App Home | **Done** — API + Angular **Apps** sheet |
| **4** | DNS helpers | **Done** — `scripts/cloudflare-dns.ps1` (Ops-owned) |
| **5** | Workflow sub-agents | **Skills + EM tasks API** — `ap-platform-*`, `/api/platform/tasks` |
| **6–8** | Full VirtualDev Co runtime | Planned (shared memory, swarm, pipelines) |

## Skills

`ap-platform-ops` · `ap-platform-review` · `ap-platform-qa` · `ap-platform-state` · `ap-platform-em`

## Key APIs

| Path | Purpose |
|------|---------|
| `/api/platform/ports` | Port leases |
| `/api/platform/apps` · `/home` | App launcher data |
| `/api/platform/roles` | VirtualDev role catalog |
| `/api/platform/tasks` | EM task graph (CRUD) |
| `/api/agent/actions` | Discovery |

## UI

Top bar **Apps** → App Home (apps / roles / tasks).

## DNS

```powershell
.\scripts\cloudflare-dns.ps1 -List
.\scripts\cloudflare-dns.ps1 -Upsert -Name myapp-sandbox -Proxied
```
