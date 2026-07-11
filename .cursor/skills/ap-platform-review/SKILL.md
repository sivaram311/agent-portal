---
name: ap-platform-review
description: >-
  Git review/merge sub-agent for platform work. Use when checking diffs,
  protocol compliance, secrets, commit style, or merge readiness.
---

# Platform Review / Merge

## Read first
1. `docs/platform/ACCESS-PROTOCOLS.md`
2. `docs/platform/PORT-REGISTRY.md`
3. `docs/platform/WORKFLOW.md`
4. `docs/platform/VERSIONING-PROMOTE.md`
5. `docs/platform/AGENT-API.md` for API work

## Goal
Review changes for correctness, platform protocol compliance, and safe merge behavior.

## Workflow
1. Run `git status`, inspect staged and unstaged diffs, and check recent commit message style.
2. Scan for secrets, tokens, `.env` content, public IP references, and accidental sandbox/prod crossover.
3. Verify port claims, app records, docs, and promote gates match the platform protocols.
4. Review API work against `docs/platform/AGENT-API.md` and the client under `workspaces/agent-api/client`.
5. Lead with blocking findings; include test gaps and a concise merge recommendation.
6. Never force-push `main`; do not bypass hooks unless a human explicitly directs it.

## Hard rules
- Never mass-kill by process name `cursor`, `node`, or `agent`; kill by port/PID only.
- Prefer `https://delena.buzz` over the public IP.
- Claim ports via `GET /api/platform/ports` and `POST /api/platform/ports` before binding.
- Agent writes stay under sandbox (`E:\MyWorkspace\sandbox`) or the declared workspace.
- No secrets in git.
