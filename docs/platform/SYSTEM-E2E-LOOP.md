# System E2E Loop — Category System Agent

**Status:** Shipped on PREPROD/PROD as **agent-portal 0.1.2** (`SYSTEM_E2E_LOOP`, category `SYSTEM`). Steps auto-invoke role-bound portal sessions.

Closed-loop multi-agent workflow: Playwright QA reports flaws → fixer → docs → human review/commit gate → retest. Stops early on green, or blocks at max iterations (default **20**).

## Start a run

App Home → **Pipelines** → **System E2E loop** → Run

Or API:

```http
POST /api/platform/pipelines/SYSTEM_E2E_LOOP/run
{
  "title": "Agent Portal E2E harden",
  "projectSlug": "agent-portal-e2e",
  "description": "Target https://delena.buzz using repo e2e/",
  "maxIterations": 20
}
```

PowerShell:

```powershell
Start-PlatformPipeline -PipelineId SYSTEM_E2E_LOOP -Title "Agent Portal E2E" -ProjectSlug agent-portal-e2e
```

## Roles per iteration

| Step | Role | Duty |
|------|------|------|
| QA | `QA` | Run Playwright; set `outcome` `PASS` / `FAIL` / `FLAKY`; put report in task description |
| FIX | `BACKEND` | Fix product bugs from QA report; message `FRONTEND` if UI-owned; never weaken tests to force green |
| DOCS | `PRODUCT` | Update docs / evidence paths |
| REVIEW | `SECURITY` | Human gate for commit/push (no auto-push in v1) |

## Branching

```text
QA PASS  → DOCS → REVIEW → parent DONE (GREEN, stop early)
QA FAIL  → FIX → DOCS → REVIEW → QA (next iteration)
After REVIEW if still not green and iteration == max → parent BLOCKED
```

Missing QA `outcome` on DONE is treated as **FAIL**.

## Progress tracking

| Surface | How |
|---------|-----|
| Task fields | `iteration`, `maxIterations`, `outcome`, `stepKey` |
| Memory | `e2e-loop/{runId}/progress` kind `PROGRESS` (JSON) |
| API | `GET /api/platform/pipelines/runs/{runId}` |
| UI | App Home → Pipelines (timeline) and Tasks (iter/outcome chips) |

Mark QA done with outcome:

```http
PATCH /api/platform/tasks/{qaTaskId}
{ "status": "DONE", "outcome": "FAIL", "description": "…bug report…" }
```

Handoffs fire automatically on DONE (same as other pipelines). Swarm tick still activates the first OPEN child.

## Skills

- `ap-system-e2e-loop` — orchestrator / EM guidance for this pipeline
- `ap-platform-qa` — Playwright / bug format
- `ap-platform-review` — commit/push review gate

## Guardrails (v1)

- Commit/push require **human approval** via REVIEW (`SECURITY` has `humanApprovalRequired`)
- Max iterations default **20**; override with `maxIterations` on run (1–100)
- Do not “fix” flaky tests by deleting assertions; file `FLAKY` and investigate

## Agent sessions (same as other work)

Each ASSIGNED step **auto-invokes** a portal session (role ACL + handoff prompt), like New Session → prompt:

| Step | Session role | Skill hint in prompt |
|------|--------------|----------------------|
| QA | QA | `ap-platform-qa` / Realme e2e |
| FIX | BACKEND | implement from QA report |
| DOCS | PRODUCT | docs/evidence |
| REVIEW | SECURITY | `ap-platform-review` — human permissions gate |

Manual / retry:

- App Home → **Tasks** → **Open agent session**
- `POST /api/platform/tasks/{id}/invoke`

Then work in that session (chat, Changes, permissions) exactly like any other agent session. Mark the platform task `DONE` when the step finishes (REVIEW: after you approve).
