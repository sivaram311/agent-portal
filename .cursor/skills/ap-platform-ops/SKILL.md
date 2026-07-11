---
name: ap-platform-ops
description: >-
  Ops/Deploy sub-agent for this Windows host. Use when restarting services,
  checking delena.buzz health, or coordinating NGINX, Cloudflare, and deploy gates.
---

# Platform Ops / Deploy

## Read first
1. `docs/platform/ACCESS-PROTOCOLS.md`
2. `docs/platform/PORT-REGISTRY.md`
3. `docs/platform/WORKFLOW.md`
4. `docs/platform/VERSIONING-PROMOTE.md`
5. `docs/platform/AGENT-API.md` for API work

## Goal
Keep host services restartable, reachable, and promoted through the documented gates.

## Workflow
1. Identify the target service by port, URL, and workspace.
2. Claim or confirm ports via `GET /api/platform/ports` and `POST /api/platform/ports` before binding.
3. Restart only the process owning the target port/PID; verify with `Get-NetTCPConnection -LocalPort <port>`.
4. Check health through `https://delena.buzz` routes, not the public IP.
5. For NGINX or Cloudflare changes, use configs under `E:\Source\Deployment`, reload deliberately, and document the route.
6. Enforce promote gates: sandbox -> QA report -> review approve -> version -> staging smoke -> human go/no-go -> prod.

## Hard rules
- Never mass-kill by process name `cursor`, `node`, or `agent`; kill by port/PID only.
- Prefer `https://delena.buzz` over the public IP.
- Claim ports via `GET /api/platform/ports` and `POST /api/platform/ports` before binding.
- Agent writes stay under sandbox (`E:\MyWorkspace\sandbox`) or the declared workspace.
- No secrets in git.
