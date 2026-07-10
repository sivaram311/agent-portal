# Mobile audit artifacts (Realme P2 Pro)

Reference screenshots and layout-overlap JSON from the Realme (~360×780) mobile polish pass. Use with [CHECKLIST.md](CHECKLIST.md) when regressing UI.

## Files

| Pattern | Meaning |
|---------|---------|
| `01-login*.png` / `*-audit.json` | Pre-fix login / list / create captures + overlap audit |
| `10-` … `13-` | Mid-fix verification shots |
| `20-` … `23-` | Post-fix list / detail reference frames |

JSON audits list overlapping element pairs (`pairs`) and horizontal overflow (`overflow`) for the captured viewport.

## How to use

1. Open the portal at phone width (or run Playwright Realme project — see [../README.md](../README.md)).
2. Walk [CHECKLIST.md](CHECKLIST.md) against the latest `2x-*.png` frames.
3. Re-capture under this folder when shipping another mobile UX change (keep filenames sequential).

These PNGs are intentional fixtures (~3 MB total), not Playwright failure artifacts (`test-results/` stays gitignored).
