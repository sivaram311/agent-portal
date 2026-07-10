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

Other scripts:

| Script | Purpose |
|--------|---------|
| `npm run test:ui` | Playwright UI mode |
| `npm run headed` | Headed browser on Realme profile |
| `npm run report` | Open HTML report (`playwright show-report`) |

## Output

- **Screenshots:** `e2e/screenshots/realme-p2-pro/` (`01-home.png` … `07-code-tab.png`)
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
