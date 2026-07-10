---
name: ap-templates
description: >-
  Session templates and prompt presets for Agent Portal. Use when adding
  one-click create flows or starter prompts.
---

# Agent Portal — Session templates (P2)

## Goal
One-click session presets (provider, workspace hint, starter prompt).

## Build plan
1. Static catalog `GET /api/presets` (JSON in resources or config).
2. Extend create dialog with preset picker; optional auto-send first prompt.
3. Document presets in README.
