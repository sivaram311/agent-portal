---
name: ap-ux-polish
description: >-
  Polishes Agent Portal UX (toasts, header actions, mobile list cleanup). Use
  when improving empty states, wiring Run/Archive actions, or mobile chrome.
---

# Agent Portal — UX polish (P2)

## Goal
Ship the remaining redesign polish without new backend providers.

## Build plan
1. Toast/snackbar for create, archive, cancel, errors (replace or complement banner).
2. Session header: wire Archive/Cancel; hide or implement Duplicate only if API exists — no dead buttons.
3. Mobile: remove duplicate brand/search from embedded sidebar inside `mobile-list` (list-only mode input).
4. Empty states for Transcript/Logs consistent with `ap-empty`.
5. Extend Playwright mobile checks for toasts if added.

## Constraints
- Keep design tokens; Inter + teal accent
- Preserve `data-testid` hooks used by e2e
