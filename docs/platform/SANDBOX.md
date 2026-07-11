# Sandbox

**Status:** Phase 1 active. `AGENT_WORKSPACE_ROOT` defaults to `E:\MyWorkspace\sandbox` via host `.env` / scripts.

## Canonical root

`E:\MyWorkspace\sandbox\`

Junctions (legacy portal workspaces remain the real folders):

| Junction | Target |
|----------|--------|
| `sandbox/agent-api` | `agent-portal/workspaces/agent-api` |
| `sandbox/demo` | `agent-portal/workspaces/demo` |
| `sandbox/FileBridge` | `agent-portal/workspaces/FileBridge` |

New greenfield projects: create `sandbox/<slug>/` and register in `sandbox/_registry.md` + claim ports via `/api/platform/ports/claim`.

## Config

```powershell
$env:AGENT_WORKSPACE_ROOT = "E:\MyWorkspace\sandbox"
```

`scripts/run-host-stack.ps1` and `run-backend-docker-deps.ps1` honor `AGENT_WORKSPACE_ROOT` from `.env`, else sandbox under MyWorkspace.

## Rules

| Allowed | Denied |
|---------|--------|
| Create/edit under sandbox | Arbitrary writes across E:\ |
| Claim ports before bind | Steal 8080/9000/4200/80 |
| Use delena.buzz URLs | Prefer raw public IP in clients |
