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
- Soft interactive via question detection + follow-up `-p`
- Capability probe (`AntigravityCapabilityService`)
- When `prefer-acp=true` and help lists `--acp`, portal tries `agy acp` via shared `AgentBridge` and falls back to print-mode on failure

## Build plan
1. Keep probe + soft mode as reliable default.
2. Prefer ACP launch when advertised; reuse Cursor permission dialogs when ACP works.
3. Document effective mode on `/api/health` capabilities.
4. Never reintroduce `Start-Process -ArgumentList`.

## Constraints
- Preserve Windows UTF-8 stdout capture for print-mode
- ACP success depends on installed `agy` protocol compatibility
