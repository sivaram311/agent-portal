# Mobile QA (Realme P2 Pro)

Phone-first UX for Agent Portal (~360×780). Automated coverage lives in `e2e/`; visual regression references live in `e2e/mobile-audit/`.

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

Against the public HTTPS front door:

```powershell
$env:APP_URL = "https://delena.buzz"
# Or PREPROD/PROD:
# $env:APP_URL = "https://agent-portal-staging.delena.buzz"
# $env:APP_URL = "https://agent-portal.delena.buzz"
npm run test:mobile
```
