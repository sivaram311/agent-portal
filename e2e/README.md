# Agent Portal E2E (Playwright)

Mobile UI validation for the Agent Portal Angular app, emulating **Realme P2 Pro** (dark navy/teal responsive shell, FAB, drawer, session tabs).

## Device profiles

| Project | Viewport | Notes |
|---------|----------|--------|
| `realme-p2-pro` | 360×800 @3× | Phone (default `npm run test:mobile`) |
| `tablet-1024` | 1024×768 | Tablet / pad landscape |
| `desktop-1440` | 1440×900 | Desktop shell |

```powershell
npx playwright test --project=tablet-1024
npx playwright test --project=desktop-1440
```

## Auth e2e

`tests/auth.spec.ts` checks the CSS login overlay when `css.enabled=true`, or the main shell when auth is off.

```powershell
$env:CSS_AUTH_URL = "http://localhost:9000"
$env:CSS_USER = "admin"
$env:CSS_PASSWORD = "admin123"
npx playwright test tests/auth.spec.ts --project=desktop-1440
```

Requires CSS running for a full login assertion.

Install dependencies once:

```powershell
cd e2e
npm install
npx playwright install chromium
```

## Run

Default base URL is `http://127.0.0.1:4200` (from `playwright.config.ts`).

```powershell
cd e2e
npm test
```

Same as targeting the Realme project explicitly:

```powershell
npm run test:mobile
```

Against a remote or LAN host (frontend bound via `ng serve --host 0.0.0.0`):

```powershell
$env:APP_URL = "http://103.118.183.185:4200"
npm run test:mobile
```

| Script | Purpose |
|--------|---------|
| `npm run test:mobile` | All Realme-project specs |
| `npm run test:realme-e2e` | Multi-agent E2E (`realme-p2-pro-multi-agent.spec.ts`, 360×780) |
| `npm run test:ui` | Playwright UI mode |
| `npm run headed` | Headed browser on Realme profile |
| `npm run report` | Open HTML report (`playwright show-report`) |

## Realme P2 Pro multi-agent E2E (sub-agent)

Skill: `.cursor/skills/ap-e2e-realme-p2-pro/SKILL.md`  
Spec: `tests/realme-p2-pro-multi-agent.spec.ts` (viewport **360×780**)

Covers login → FAB create session → `/api/agent/actions` → session tabs → Changes → optional prompt → App Home.

```powershell
cd e2e
$env:APP_URL = "https://delena.buzz"
$env:CSS_USER = "admin"
$env:CSS_PASSWORD = "admin123"   # local only — do not commit
npm run test:realme-e2e
```

Screenshots: `screenshots/realme/` (`01-after-login.png`, `05-changes-tab.png`, `06-final-state.png`, …).

## Output

- **Screenshots:** `e2e/screenshots/realme-p2-pro/` (unit shots `01-home.png` … `07-code-tab.png`, plus full-flow shots below)
- **Mobile audit fixtures:** `e2e/mobile-audit/` — committed before/after PNGs + overlap JSON + [CHECKLIST.md](mobile-audit/CHECKLIST.md) (see [mobile-audit/README.md](mobile-audit/README.md))
- **HTML report:** `e2e/playwright-report/` (open with `npm run report`)
- **Artifacts on failure:** `e2e/test-results/` (trace, video when enabled)

## What the suite checks

File: `tests/realme-p2-pro.spec.ts`

1. Viewport is 360×800 with no horizontal overflow on home
2. Brand chrome and **New Session** FAB (≥ 44px touch target, bottom-right on mobile)
3. Create-session dialog fits narrow screen; Cursor / Antigravity provider radios; Cancel closes dialog
4. Mobile session list: search input, filter chips (all / active / failed / archived)
5. Session detail: tabs **Transcript**, **Logs**, **Code**, **Preview**; prompt input near bottom; Logs and Code empty states

If no sessions exist, the detail test creates one via the FAB (Antigravity + workspace `demo`).

## Realme P2 Pro full-screen flow

File: `tests/realme-p2-pro-full-test.spec.ts`

End-to-end mobile walkthrough that logs in (when CSS overlay is present), exercises the FAB create dialog, opens a session (creates one if the list is empty), captures every session tab, and screenshots drawer navigation.

```powershell
cd e2e
npx playwright test realme-p2-pro-full-test.spec.ts --project=realme-p2-pro
# headed:
npx playwright test realme-p2-pro-full-test.spec.ts --project=realme-p2-pro --headed
```

Optional env overrides: `APP_URL`, `CSS_USER`, `CSS_PASSWORD`.

Full-flow screenshots written under `screenshots/realme-p2-pro/`:

| File | Step |
|------|------|
| `01-login-screen.png` | Login / CSS overlay |
| `02-after-login.png` | Post-login shell |
| `03-session-list.png` | Mobile session list |
| `04-create-session-fab.png` | New session dialog via FAB |
| `05-session-detail.png` | Opened session |
| `06-*-tab.png` | Transcript, Logs, Code, Preview, Changes, History |
| `08-drawer-menu.png` | Mobile drawer / menu (when present) |
| `09-final-overview.png` | Final frame |

```powershell
explorer screenshots\realme-p2-pro
```

## `data-testid` hooks

Stable selectors used by tests (and recommended for new UI work):

| `data-testid` | Element |
|---------------|---------|
| `fab-new-session` | Mobile/desktop floating “+” new session button |
| `create-session-dialog` | New session modal form |
| `mobile-session-list` | Mobile session list panel |
| `mobile-search` | Session search input |
| `session-card` | Individual session card in the list |
| `session-detail` | Active session detail pane (tabs + input) |

Additional assertions use roles/labels (e.g. tab names, “New agent session” heading, “Code viewer” empty state).

## Notes

- Tests run **serially** (`workers: 1`) to avoid conflicting session state.
- Set `APP_URL` to match how you open the app in a browser; API/WebSocket calls use the same hostname on port 8080 automatically (`frontend/src/app/services/backend-url.ts`).
- For CI, set `CI=1` to enable retries and forbid `.only` tests.
