---
name: ap-subagents
description: >-
  Implements first-class sub-agent UI and APIs for Agent Portal (live child
  state + Abandon). Use when building subagent panels, child tool trees,
  abandon-window UX, or Cursor ACP nested agent events.
---

# Agent Portal — Sub-agents (P0)

## Goal
Show nested/child agent or tool runs with live status and an **Abandon** action that cancels only that child when the provider supports it; otherwise cancel the parent run safely.

## Current baseline
- Logs tab lists `ToolRun` + terminal chunks
- Cancel is session-wide only (`SessionService.cancel` / process kill)
- No `subagent_id` model or abandon-per-child API

## Build plan
1. **Model** — Add optional `parentToolCallId` / `subagentId` on `ToolRun` (or new `SubAgentRun` entity); persist status: `running|completed|failed|abandoned`.
2. **Events** — Map Cursor ACP nested tool/agent updates into WS events: `subagent_started`, `subagent_progress`, `subagent_finished`. Antigravity: best-effort from brain `tasks/` filenames until ACP exists.
3. **API** — `POST /api/sessions/{id}/subagents/{subId}/abandon` → provider-specific cancel; if unsupported, return 501 and document fallback to session cancel.
4. **UI** — `SubagentPanelComponent` in Logs (or side sheet): name, status pill, elapsed, Abandon button (44px touch). Wire realtime updates.
5. **E2E** — Add Playwright coverage for panel visibility + abandon button enabled when running.

## Constraints
- Do not break existing Transcript streaming
- Prefer Cursor ACP first; Antigravity may be read-only status until interactive mode exists
- Update `docs/ROADMAP.md` and README when shipping
