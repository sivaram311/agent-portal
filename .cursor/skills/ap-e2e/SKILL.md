---
name: ap-e2e
description: >-
  Expands Agent Portal Playwright coverage beyond Realme P2 Pro to tablet and
  desktop. Use when adding e2e projects, visual baselines, or CI Playwright.
---

# Agent Portal — E2E expansion (P2)

## Goal
Add tablet (Realme Pad–like) and desktop Playwright projects alongside `realme-p2-pro`.

## Build plan
1. Projects in `e2e/playwright.config.ts`: `tablet-1024`, `desktop-1440`.
2. Specs: sidebar visible on tablet/desktop; FAB hidden on desktop; create dialog; tabs.
3. Full mobile walkthrough: `tests/realme-p2-pro-full-test.spec.ts` (login → FAB → session tabs → drawer screenshots).
4. Optional CI workflow: install browsers, start backend/frontend, run `npm test`.
5. Keep `data-testid` as primary selectors; update `e2e/README.md`.

## Constraints
- `workers: 1` if sharing backend session state
- Do not commit `test-results/` or `playwright-report/`
- Prefer Agent Portal `data-testid` hooks over generic CSS when writing full-flow specs
