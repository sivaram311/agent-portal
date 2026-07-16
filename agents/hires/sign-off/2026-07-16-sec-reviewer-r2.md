# Security Review Sign-Off: Machine Gateway v0 (Round 2)

- **Reviewer Persona**: sec-reviewer
- **Skill**: ap-security
- **Pack Epoch**: machine-gateway-v0-review-2
- **Date**: 2026-07-16

---

## Verdict
**APPROVE**

---

## Executive Summary
A follow-up security review has been performed on the `feature/machine-gateway-v0` tip to assess the remediation of the critical blocking issues identified in round 1 (`machine-gateway-v0-review-1`). All identified findings have been successfully addressed:
1. Path jailing was hardened by resolving the longest existing parent prefix to its real path using `toRealPath()`, preventing directory traversal and symbolic link escapes on both existing and non-existent targets.
2. Shell command execution was restricted to an allowlist-only structure, rejecting arbitrary commands and only permitting specific port lookup and process termination shapes.
3. Tool classification was corrected to prevent shell tools with harmless argument payloads from being misclassified as read-only.
4. Chat interactions were restricted to Cursor (ACP mode) only, explicitly blocking print-mode which bypasses tool guards.
5. Global auto-approve settings are now prevented from overriding the role-level human approval requirement for `GATEWAY_OPS`.

All unit tests pass successfully. Therefore, sign-off is **APPROVED**.

---

## Checklist Status

### 1. Path Jail: PASSED
- [x] **Junction / Symlink Resolution**: Resolved by walking up parent directories and resolving the longest existing parent prefix using `toRealPath()`. This prevents symlink bypasses on new file paths.
- [x] **Path Extraction Completeness**: The parameter keys list has been extended to match terms like `directory`, `dir`, `workspace`, `location`, `output`, `cwd`, and `outdir`.
- [x] **Antigravity print-mode Protection**: Handled by forcing Cursor ACP mode for chat.

### 2. Redaction: PASSED
- [x] **Key Matcher Patterns**: Key matcher patterns in `SecretRedactor` have been expanded to cover credentials like `ssh`, `key`, `cert`, `passphrase`, `cookie`, `session`, and `salt`.

### 3. Shell Allowlist: PASSED
- [x] **Classification Security**: Shell-executing tools are classified based on explicit tool identities/schemas (e.g. `run_command`, `shell`) rather than scanning parameters, eliminating classification bypass.
- [x] **Mass Kill & Termination Restriction**: The shell guard now rejects all non-allowlisted command shapes, transforming the safety mechanism from a blacklist into an allowlist-only filter.

### 4. Mode Ceiling: PASSED
- [x] **Ceiling Integrity**: Confirmed working.
- [x] **Role Mapping**: Confirmed working.

### 5. AgentBridge Intercept: PASSED
- [x] **Human Approval Integrity**: `AgentBridge` was updated to explicitly block auto-approval of `GATEWAY_` roles that require human approval, even when default auto-approval is enabled globally.
- [x] **Runtime Coverage**: Bypassing guards via print-mode fallback is prevented by restricting gateway chat exclusively to the Cursor provider.

---

## Technical Audit & Verification Details

### Path Jailing Symlink Traversal (Finding 1 & 4)
- **Code Reference**: [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java#L124)
- **Details**: Added `resolveRealPath` helper that walks up parent directories until it finds an existing directory, calls `toRealPath()` on it, and then resolves the relativized candidate path against it. Tested with a new JUnit test in `MachineGatewayTest.java` verifying that non-existent files targeted through symbolic links are jailed properly.

### Shell Execution Allowlist (Finding 3)
- **Code Reference**: [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java#L68)
- **Details**: Evaluated `assertShellSafe` to enforce match patterns for `Stop-Process -Id <pid>`, `taskkill /PID <pid>`, and `Get-NetTCPConnection -LocalPort <port>` only. Arbitrary shell commands are explicitly denied.

### Tool Classification (Finding 2)
- **Code Reference**: [RoleAclService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/RoleAclService.java#L68)
- **Details**: The classification logic prioritizes explicit tool names (such as `run_command` or `run_terminal`) in its checking sequence, preventing shell commands from being misclassified under lesser categories.

### Cursor Provider Restriction
- **Code Reference**: [MachineChatService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineChatService.java#L141)
- **Details**: Direct validation was added to prevent session creation for `antigravity` provider when requested for Gateway mode.

### Human Approval Protection (Finding 5)
- **Code Reference**: [AgentBridge.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AgentBridge.java#L780)
- **Details**: Auto-approval logic will ignore the global `autoApprove` flag if a gateway role requires human approval (`GATEWAY_OPS`), guaranteeing human authorization remains interactive.

---

## Verdict
**APPROVE**
