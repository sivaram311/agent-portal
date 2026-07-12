# Mobile QA (Realme P2 Pro)

Phone-first UX for Agent Portal (~360×780). Automated coverage lives in `e2e/`; visual regression references live in `e2e/mobile-audit/`.

## Chrome budget (2026-07-12)

On mobile session detail, keep **topbar + session header + tabs ≤ ~30–35%** of viewport height:

- Compact topbar (~44px) with brand + truncated user chip + **⋯ overflow** (Apps / Rules / Sign out)
- Single-line session header (title + badges + compact Archive); path truncated; long-press/tap copy
- **Share** collapsed behind a toggle (expand only when needed)
- Pill-style horizontally scrollable tabs with edge fade; **Preview tab hidden** — use Code → Preview toggle instead
- Session auto-title from first prompt / ACP `session_info_update` (no more stuck “New session” after a real run)
- Small fonts preserved; tap targets ≥ 44px via padding, not larger type
- Friendly empty / restricted / loading states + Retry (no raw "Forbidden")
- Create dialog as bottom sheet on phones
- Edge-to-edge Console; denser Logs

## Checklist

Product checklist (FAB-only create, path truncate + long-press/copy, share-bar states, empty states, transcript timestamps, scrollable tabs): [e2e/mobile-audit/CHECKLIST.md](../e2e/mobile-audit/CHECKLIST.md).

Live-site prompt (delena.buzz): [LIVE-MOBILE-FIXES.md](LIVE-MOBILE-FIXES.md).

## Artifacts

Before/after screenshots and overlap JSON: [e2e/mobile-audit/README.md](../e2e/mobile-audit/README.md).

## Playwright

```powershell
cd e2e
npm install
npx playwright install chromium
npm run test:mobile
```

Full flow and `data-testid` hooks: [e2e/README.md](../e2e/README.md).

Against local DEV (`ng serve` :4200 + API :8080 + CSS :9000):

```powershell
$env:APP_URL = "http://127.0.0.1:4200"
$env:CSS_USER = "admin"
$env:CSS_PASSWORD = "admin123"
npm run test:mobile
```

Against the public HTTPS front door:

```powershell
$env:APP_URL = "https://delena.buzz"
# Or PREPROD/PROD:
# $env:APP_URL = "https://agent-portal-staging.delena.buzz"
# $env:APP_URL = "https://agent-portal.delena.buzz"
npm run test:mobile
```

**DEV auth note:** CSS `authUrl` on a different port than the UI (e.g. `:9000` vs `:4200`) must stay absolute — same-origin rewrite only applies when host **and** port match (nginx front doors).
