---
name: ap-e2e-realme-p2-pro
description: >-
  Sub-agent for Realme P2 Pro (360×780) end-to-end Playwright testing of Agent
  Portal on mobile: login, FAB create session, Agent API actions, tabs, Changes,
  and prompt. Use when the user asks for Realme / mobile e2e / P2 Pro QA.
---

# Realme P2 Pro E2E Sub-Agent

## Device profile

| Property | Value |
|----------|--------|
| Device | Realme P2 Pro |
| CSS viewport | **360 × 780** |
| Base device | Playwright `Pixel 5` + overrides |
| Touch | `isMobile: true`, `hasTouch: true` |
| Project | `realme-p2-pro` |

## Read first

1. `e2e/README.md`
2. `docs/platform/ACCESS-PROTOCOLS.md`
3. Prefer **`https://delena.buzz`** over public IP (`<ORIGIN_IP>`)

## Spec

Canonical multi-agent flow: `e2e/tests/realme-p2-pro-multi-agent.spec.ts`

Also available:

- `e2e/tests/realme-p2-pro.spec.ts` — viewport / FAB / dialog unit checks
- `e2e/tests/realme-p2-pro-full-test.spec.ts` — screenshot walkthrough

## How to run

```powershell
cd E:\MyWorkspace\agent-portal\e2e
npm install
npx playwright install chromium

# Against production domain (preferred)
$env:APP_URL = "https://delena.buzz"
$env:CSS_USER = "admin"
$env:CSS_PASSWORD = "<from local secrets — never commit>"
npm run test:realme-e2e

# Local ng serve
$env:APP_URL = "http://127.0.0.1:4200"
npm run test:realme-e2e

# Headed debug
npx playwright test tests/realme-p2-pro-multi-agent.spec.ts --project=realme-p2-pro --headed
```

## Flow under test

1. Login (CSS overlay when enabled)
2. Create session via mobile FAB (`demo` workspace, Cursor)
3. `GET /api/agent/actions` — assert catalog has many actions
4. Session tabs: Transcript, Logs, Code, Preview, Changes, History (+ Activity if present)
5. Changes tab — Keep / Restore / empty state
6. Optional prompt send + screenshots under `e2e/screenshots/realme/`

## Hard rules

- Never mass-kill by process name `cursor`, `node`, or `agent`; kill by port/PID only.
- Prefer `https://delena.buzz` over the public IP.
- Do not commit passwords or tokens; use `CSS_USER` / `CSS_PASSWORD` env.
- Agent writes stay under sandbox / declared workspace.
- File bugs with: env, URL, viewport, steps, expected/actual, screenshot path, severity.

## Definition of done

- Spec exits 0 on target `APP_URL`
- Screenshots written for Changes + final state
- Failures include Playwright report / trace under `e2e/test-results/`
