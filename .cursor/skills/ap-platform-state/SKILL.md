---
name: ap-platform-state
description: >-
  Port registry and Postgres state sub-agent. Use when claiming ports, tracking
  apps, or keeping platform state docs and APIs in sync.
---

# Platform State / Registry

## Read first
1. `docs/platform/ACCESS-PROTOCOLS.md`
2. `docs/platform/PORT-REGISTRY.md`
3. `docs/platform/WORKFLOW.md`
4. `docs/platform/VERSIONING-PROMOTE.md`
5. `docs/platform/AGENT-API.md` for API work

## Goal
Keep machine state consistent across APIs, Postgres-backed records, and markdown registry docs.

## Workflow
1. Read current leases with `GET /api/platform/ports` before any bind decision.
2. Claim or update leases with `POST /api/platform/ports`; include service, owner_app, env, bind, status, and notes.
3. Track app metadata with `GET /api/platform/apps` and `POST /api/platform/apps` when a workspace, URL, CSS client, or owner changes.
4. Keep `docs/platform/PORT-REGISTRY.md` in sync until the API is the canonical store.
5. Record public routes with `https://delena.buzz` hostnames and note NGINX/Cloudflare ownership.
6. Flag stale leases, mismatched docs, or apps writing outside sandbox/declared workspaces.

## Hard rules
- Never mass-kill by process name `cursor`, `node`, or `agent`; kill by port/PID only.
- Prefer `https://delena.buzz` over the public IP.
- Claim ports via `GET /api/platform/ports` and `POST /api/platform/ports` before binding.
- Agent writes stay under sandbox (`E:\MyWorkspace\sandbox`) or the declared workspace.
- No secrets in git.
