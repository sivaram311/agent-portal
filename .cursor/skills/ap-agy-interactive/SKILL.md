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
1. Detect whether installed `agy` supports `--acp`, `--prompt-interactive`, or stdin JSON protocol; feature-flag in `AgentProperties`.
2. If interactive available: keep process alive, multiplex stdin/stdout, emit `permission_required` / `input_required` WS events; reuse Cursor permission dialog patterns where possible.
3. If not: implement portal “agent asked a question” detection from assistant text + UI chip “Reply” that sends next `-p` with conversation id (soft interactive).
4. Never reintroduce `Start-Process -ArgumentList` (truncates prompts).
5. Document capability matrix in README.

## Constraints
- Preserve Windows UTF-8 stdout capture
- Keep skip-permissions configurable; default safer when interactive permissions work
