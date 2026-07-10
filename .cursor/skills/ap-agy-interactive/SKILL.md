---
name: ap-agy-interactive
description: >-
  Improves Antigravity from single-shot agy -p toward interactive mid-turn
  prompts in Agent Portal. Use when adding Antigravity Q&A, permissions, or
  replacing print-mode with ACP/interactive CLI.
---

# Agent Portal — Antigravity interactive (P0)

## Goal
Allow Antigravity sessions to pause for user input (questions / permissions) instead of only single-shot `-p` runs.

## Current baseline
- `AntigravityBridge` launches `agy -p` via PowerShell call-operator splat
- `--dangerously-skip-permissions` on by default
- Follow-ups = new `-p` with optional `--conversation`

## Build plan
1. Detect whether installed `agy` supports `--acp`, `--prompt-interactive`, or stdin JSON protocol; feature-flag in `AgentProperties` (`interactive-protocol=auto|soft|none`).
2. Probe via `AntigravityCapabilityService` and expose on `/api/health`.
3. Soft mode: question detection → `input_required`; allow Antigravity follow-up prompts while `WAITING_PERMISSION`.
4. If ACP available later: keep process alive, multiplex stdin/stdout, emit `permission_required` / `input_required`; reuse Cursor permission dialogs.
5. Never reintroduce `Start-Process -ArgumentList` (truncates prompts).
6. Document capability matrix in README / health badges.

## Constraints
- Preserve Windows UTF-8 stdout capture
- Keep skip-permissions configurable; default safer when interactive permissions work
- Soft mode is the production path until a true ACP bridge lands
