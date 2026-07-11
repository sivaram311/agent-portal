---
name: ap-system-e2e-loop
description: >-
  Category System Agent for Agent Portal SYSTEM_E2E_LOOP. Use when running or
  operating the Playwright QA → fix → docs → review → retest loop with progress
  tracking (default max 20 iterations, early stop on green).
---

# System E2E Loop

## Read first
1. `docs/platform/SYSTEM-E2E-LOOP.md`
2. `docs/platform/ACCESS-PROTOCOLS.md`
3. `docs/platform/WORKFLOW.md`
4. Skill `ap-platform-qa` for Playwright execution

## Start
1. `POST /api/platform/pipelines/SYSTEM_E2E_LOOP/run` with title, projectSlug, optional maxIterations (default 20).
2. Link role sessions to child tasks via `POST /api/platform/tasks/{id}/session`.
3. Track with `GET /api/platform/pipelines/runs/{runId}` and memory key `e2e-loop/{runId}/progress`.

## Per-role behavior
- **QA:** Prefer existing `e2e/` for agent-portal. Run Playwright, write structured bug report into task description, set `outcome` then `status=DONE`.
- **FIX (BACKEND):** Fix product code from QA report; message FRONTEND for UI; never weaken tests to pass.
- **DOCS (PRODUCT):** Update evidence/docs only.
- **REVIEW (SECURITY):** Human gate — approve commit/push explicitly; do not auto-push.

## Stop conditions
- QA `PASS` → DOCS → REVIEW → run `DONE` (green, stop before further iterations)
- Still failing at maxIterations → run `BLOCKED`
- Human may `CANCELLED` the parent run

## Hard rules
- No secrets in git
- No auto-push in v1
- Kill processes by port/PID only
- Keep agent writes under sandbox / declared workspace
