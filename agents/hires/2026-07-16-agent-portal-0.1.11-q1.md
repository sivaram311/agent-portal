# Hire — agent-portal 0.1.11 Q1 PREPROD (Machine Gateway)

**Date:** 2026-07-16  
**Gate:** Q1 DEV → PREPROD  
**App:** agent-portal **0.1.11**  
**Why:** Ship Machine Gateway (`/api/machine/*`) to F:`:4080` / agent-portal-staging.delena.buzz  
**packEpoch:** `agent-portal-0.1.11-q1`  
**Lead / EM:** Cursor (`promote-em`)  
**Human intent:** User requested PREPROD promote (this session)

## Crew (mandatory)

| Lane | Persona / skill | Blocking | Ownership |
|------|-----------------|----------|-----------|
| EM | `promote-em` | Yes (GO/NO-GO) | SUMMARY, CHECKLIST, DEPENDENCIES, matrix |
| QA | `promote-qa` | Yes | `evidence/q1/qa/` |
| Security | `promote-security` | Yes | `evidence/q1/security/` |
| Review | `promote-review` | Yes | `evidence/q1/review/` + REVIEWER-SIGN-OFF |
| Ops | `promote-ops` | After GO | `evidence/q1/ops/` · F: deploy `:4080` |
| Field-ops | `promote-field-ops` | Advisory | field-lessons in ops/qa notes |

## Ports / edge (no new claim)

- PREPROD API `:4080` (existing) · UI static `F:\apps\agent-portal\ui`
- Public: https://agent-portal-staging.delena.buzz
- Dep: css-next **0.2.1** / `v0.2.1` · `clientId=agent-portal`

## Forbidden

- Q2 / G: overwrite without separate human GO  
- Deploy before EM GO  
- New Cloudflare DNS for Machine Gateway  
