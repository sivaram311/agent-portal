# Hire — Machine Gateway v0 merge review (PR #1)

**Date:** 2026-07-16  
**Session:** `machine-gateway-v0-review-1`  
**App pack:** `agent-portal` / Machine Gateway  
**packEpoch:** `machine-gateway-v0-review-1`  
**Lead:** Cursor (Integrate)  
**Why:** Agy-recommended Layer B review crew for merge to `main`  
**PR:** https://github.com/sivaram311/agent-portal/pull/1  
**Provider:** cursor Task / agy (single-shot per lane)

## Workers

| Lane | Persona id | Name | Skill | Ownership (disjoint) | Tool | Exit |
|------|------------|------|-------|----------------------|------|------|
| A Security *(blocking)* | `sec-reviewer` | Security Reviewer | `ap-security` | `backend/.../machine/SecretRedactor.java`, `MachineToolGuard.java`, `MachineMode.java`, `MachineModeService.java`; `acp/AgentBridge.java` (permission + guard only); `acp/AgentProcessManager.java` (guard inject only) | Task/agy | SIGN-OFF APPROVE/HOLD |
| B Review *(blocking)* | `platform-reviewer` | Platform Reviewer | `ap-platform-review` | `machine/MachineChatService.java`, `MachineContextService.java`; `web/MachineController.java`, `AgentApiController.java`; `dto/MachineChat*`; `config/AppProperties.java`; `application.properties`; `service/PlatformRegistryService.java` (GATEWAY_* roles); `WorkspacePathResolver.java` | Task/agy | SIGN-OFF APPROVE/HOLD |
| C QA *(blocking)* | `qa-reviewer` | Platform QA | `ap-platform-qa` | `backend/src/test/java/com/agentportal/machine/**` (+ run unit tests) | Task/agy | SIGN-OFF APPROVE/HOLD + test log |
| D Ops *(advisory)* | `ops-reviewer` | Platform Ops | `ap-platform-ops` | `docs/platform/MACHINE-GATEWAY.md`, `PORT-REGISTRY.md`, `CLOUDFLARE-DNS-PROXY.md`, `AGENT-API.md`, `FUTURE-IMPLEMENTATION.md`, `indexes/INDEX-MYWORKSPACE.md`; `workspaces/agent-api/ACTIONS.md`; `workspaces/machine-gateway/**` | Task/agy | SIGN-OFF note (non-blocking) |

## Rules

- Disjoint ownership as above (Security vs Review deconflicted from Agy draft).  
- Single-shot per lane; stale `packEpoch` → stop.  
- No F:/G: deploy, no new ports/DNS, no mass-kill.  
- ACTIVITY-LOG one row per lane + Lead integrate row.  
- Sign-Off files: `agents/hires/sign-off/2026-07-16-<persona>.md`

## Integrate (Lead)

- [ ] Collect blocking Sign-Offs  
- [ ] Docs #12 / ACTIVITY-LOG  
- [ ] Merge PR #1 only if all blocking APPROVE  
- [ ] No promote in this hire  

## Forbidden

- Merging with any blocking HOLD  
- Promote / tag / F: G: writes  
- Editing ownership paths of another live lane  
