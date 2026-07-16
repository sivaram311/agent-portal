# Hire — Machine Gateway v0 merge review (round 2)

**Date:** 2026-07-16  
**packEpoch:** `machine-gateway-v0-review-2`  
**Prior:** `machine-gateway-v0-review-1` — Security HOLD + Platform HOLD; Ops APPROVE; QA PASS w/ gaps  
**Lead:** Cursor  
**Why:** Re-hire blocking lanes after HOLD remediation on `feature/machine-gateway-v0`

## Remediation applied (review-1 → review-2)

1. Exact Path equals for session workspace reuse (no suffix collision)  
2. `toRealPath()` path jail + expanded path keys  
3. GATEWAY shell = allowlist-only (not termination-only)  
4. Stronger `RoleAclService` shell classification  
5. Gateway chat forces `provider=cursor`  
6. `GATEWAY_*` + `humanApprovalRequired` not auto-approved  

## Workers (blocking re-hire)

| Lane | Persona | Exit file |
|------|---------|-----------|
| Security | `sec-reviewer` | `sign-off/2026-07-16-sec-reviewer-r2.md` |
| Review | `platform-reviewer` | `sign-off/2026-07-16-platform-reviewer-r2.md` |
| QA | `qa-reviewer` | `sign-off/2026-07-16-qa-reviewer-r2.md` |

Ops advisory from r1 remains APPROVE (docs unchanged materially).
