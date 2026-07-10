# Sandbox

**Status:** Protocol + stub paths. Full cutover of `AGENT_WORKSPACE_ROOT` is Future Implementation (Phase 1).

## Purpose

Isolate agent-created and agent-edited application trees from the rest of the disk. Agents may not treat the whole E: drive as a workspace.

## Canonical roots

| Root | Path | Role |
|------|------|------|
| **Sandbox (target)** | `E:\MyWorkspace\sandbox\` | Future default for all agent project work |
| **Portal workspaces (today)** | `E:\MyWorkspace\agent-portal\workspaces\` | Current `AGENT_WORKSPACE_ROOT` |
| **Agent API workspace** | `...\workspaces\agent-api\` | Docs + scratch for calling the portal API |

## Layout (target)

```text
E:\MyWorkspace\sandbox\
  README.md
  _registry.md          # list of project folders + owner + ports
  <project-slug>/       # one app or feature tree per folder
    .git/               # optional
    ...
```

Rules:

- One project per directory under `sandbox/`.
- No secrets; use env outside the tree or gitignored `.env`.
- Baselines (`.agent-portal/`), `data/`, `logs/`, `target/`, `node_modules/` stay gitignored when published.

## Agent Portal configuration (Phase 1)

When cutting over:

```powershell
$env:AGENT_WORKSPACE_ROOT = "E:\MyWorkspace\sandbox"
```

Until then, keep using `agent-portal\workspaces` but **prefer creating new agent-facing folders** under sandbox when starting greenfield work, and document the path in PORT-REGISTRY / project README.

## Allowed vs denied

| Allowed | Denied |
|---------|--------|
| Create/edit under sandbox or declared workspace | Rewrite `C:\Users\...\.cursor` blindly |
| Read docs anywhere for orientation | Delete unrelated repos |
| Install deps inside the project folder | Bind privileged ports without claim |

## Relationship to VirtualDev Co

Each VirtualDev Co **project** = one sandbox folder + CSS `clientId` + optional subdomain. Orchestrator agents receive that triad as context.
