# Security Review Sign-Off: Machine Gateway v0

- **Reviewer Persona**: sec-reviewer
- **Skill**: ap-security
- **Pack Epoch**: machine-gateway-v0-review-1
- **Date**: 2026-07-16

---

## Verdict
**HOLD**

---

## Executive Summary
A comprehensive security review of the Machine Gateway components was conducted. While the privilege ceiling implementation ([MachineModeService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineModeService.java)) successfully restricts maximum privilege escalation via HTTP headers, several critical security vulnerabilities exist in the path jailing, tool classification/ACL checks, and Antigravity execution bridge. These issues present high risks of sandbox escape, arbitrary command execution, and permission bypass. Consequently, sign-off is on **HOLD** pending remediation of the blocking issues.

---

## Checklist

### 1. Path Jail
- [ ] **Junction / Symlink Resolution**: **FAILED**  
  [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java) uses lexical normalization via `toAbsolutePath().normalize()` rather than resolving symlinks with `toRealPath()`. This permits path jail escape via symbolic links or directory junctions.
- [ ] **Path Extraction Completeness**: **FAILED**  
  Path extraction in [MachineToolGuard.collectPaths](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java#L168) only matches a hardcoded list of keys. Valid path parameters under keys like `directory`, `dir`, `workspace`, `location`, or `output` will bypass extraction and validation.
- [ ] **Antigravity print-mode Protection**: **FAILED**  
  No path jail validation is applied when the Antigravity provider runs in print-mode via [AntigravityBridge.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AntigravityBridge.java).

### 2. Redaction
- [ ] **Deep Redaction**: **PARTIAL**  
  [SecretRedactor.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/SecretRedactor.java) successfully performs recursive key-based redaction on standard JSON-like types (`Map`, `Iterable`, `JsonNode`). However, it does not support serialization or traversal of custom Java bean objects.
- [ ] **Key Matcher Patterns**: **PARTIAL**  
  The sensitive key pattern covers standard tokens and secrets but misses common credentials like `ssh`, `key`, `cert`, `passphrase`, `cookie`, `session`, and `salt`.
- [ ] **Stdout/Stderr Protection**: **FAILED**  
  Secrets printed to stdout or logs (e.g., during tool execution errors) bypass the map/JSON-based redactor.

### 3. Shell Allowlist
- [ ] **Classification Security**: **FAILED**  
  [RoleAclService.classifyTool](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/RoleAclService.java#L52) classifies tools as `"shell"` based on keyword presence in the parameters. Shell-executing tools (e.g., `run_command`) that run commands without containing those specific keywords (e.g. `whoami` or `taskkill`) are misclassified as `"read"`. This completely bypasses the shell allowlist and ACL checks.
- [ ] **Mass Kill & Termination Restriction**: **FAILED**  
  The shell allowlist in [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java#L72) only flags commands matching process termination keywords. Any other arbitrary shell command (e.g. `rm -rf /` or `curl`) does not match the termination hint and is executed unhindered.

### 4. Mode Ceiling
- [ ] **Ceiling Integrity**: **PASSED**  
  [MachineModeService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineModeService.java#L20) enforces the privilege ceiling securely by resolving the effective mode to the minimum of the HTTP header `X-Machine-Max-Mode` and the server-configured `app.machine-gateway.max-mode`.
- [ ] **Role Mapping**: **PASSED**  
  [MachineMode.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineMode.java) defines and maps roles sequentially (`GATEWAY_OBSERVE` -> `GATEWAY_ADVISE` -> `GATEWAY_ACT` -> `GATEWAY_OPS`).

### 5. AgentBridge Intercept
- [ ] **ACL Enforcement**: **PASSED**  
  [AgentBridge.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AgentBridge.java#L734) intercepts ACP tool runs, performing ACL checks against the role and triggering `MachineToolGuard` validation before executing tools.
- [ ] **Human Approval Integrity**: **FAILED**  
  If the global configuration property `agent.default-auto-approve` is enabled, the role-level human approval requirement (e.g., `GATEWAY_OPS` requiring human confirmation) is completely bypassed, auto-approving sensitive commands.
- [ ] **Runtime Coverage**: **FAILED**  
  [AgentProcessManager.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AgentProcessManager.java#L106) fails to inject `MachineToolGuard` when starting the Antigravity print-mode runtime ([AntigravityBridge.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AntigravityBridge.java)), allowing complete escape of all backend intercepts.

---

## Findings & Technical Details

### Finding 1: Path Jail Symlink Escape
- **Component**: [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java)
- **Severity**: **Critical**
- **Description**: The path jail uses `toAbsolutePath().normalize()` which resolves path segments lexically. If the agent creates a symbolic link or a Windows directory junction inside the workspace pointing to an external directory (e.g., `workspace/symlink` -> `C:/Windows`), paths under `workspace/symlink/` will bypass the prefix containment checks, permitting reading and writing arbitrary files on the host system.

### Finding 2: ACL and Shell Allowlist Bypass via Classification Misclassification
- **Component**: [RoleAclService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/RoleAclService.java) / [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java)
- **Severity**: **Critical**
- **Description**: `RoleAclService.classifyTool` checks the entire string representation of tool parameters for keywords like `"shell"`, `"bash"`, etc., to identify shell tools. If a shell-executing tool (such as `run_command`) is called with parameters that do not contain these keywords (e.g., running `whoami`), the category defaults to `"read"`. This results in:
  1. The role ACL permitting the tool (since `"read"` is allowed).
  2. [MachineToolGuard.denyReasonOrNull](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java#L92) bypassing `assertShellSafe` because `category` is not `"shell"`.
  This allows arbitrary, un-jailed command execution on the host for any gateway session.

### Finding 3: Lack of Guarding in Antigravity Print-Mode
- **Component**: [AgentProcessManager.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AgentProcessManager.java) / [AntigravityBridge.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AntigravityBridge.java)
- **Severity**: **Critical**
- **Description**: When the agent runtime falls back to print-mode (`AntigravityBridge`), the `MachineToolGuard` and ACL checks are completely bypassed. Since print-mode runs the agent CLI (`agy`) as a standalone command-line process executing tools directly, and the bridge lacks intercepts, the agent can execute any tool (write, shell, etc.) with host process permissions.

### Finding 4: Incomplete Path Parameter Capture in Jail
- **Component**: [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java)
- **Severity**: **High**
- **Description**: `collectPaths` only targets a limited subset of parameter keys. If a tool utilizes parameters such as `directory`, `workspace`, or `output` for target paths, they are omitted from the containment validation. If the tool is not classified as filesystem-mutating, it bypasses the path jail.

### Finding 5: Global Auto-Approve Overriding Role-Level Human Approval
- **Component**: [AgentBridge.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/acp/AgentBridge.java)
- **Severity**: **Medium**
- **Description**: Setting `agent.default-auto-approve = true` auto-approves all permission requests, overriding the `humanApprovalRequired` flag defined in the role (e.g., `GATEWAY_OPS` requiring human confirmation). This allows execution of highly privileged tools without user consent.

---

## Blocking Issues
1. **Remediate Symlink Escape**: Modify `resolveCandidate` in [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java) to resolve the real target path using `toRealPath()` if the candidate exists.
2. **Fix Tool Classification Security**: Rather than classifying tools based on keyword scans of the parameter string, rely on the registered tool schema name/type or require explicit metadata from the provider client to determine if a tool is a shell or filesystem-mutating tool.
3. **Extend Shell Guarding**: In [MachineToolGuard.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineToolGuard.java), the shell guard should restrict all non-termination shell commands or block shell execution completely unless explicitly allowed by the session role (e.g., restricting `GATEWAY_OPS` to port/PID lookup tools, rather than allowing all non-matching shell commands).
4. **Harden Antigravity print-mode**: Ensure that `AntigravityBridge` runs in a restricted context or disable print-mode fallback for sessions with sensitive roles (e.g., `GATEWAY_OBSERVE`, `GATEWAY_ACT`), forcing the use of ACP mode where checks are enforced.
5. **Ensure Human Approval Override Protection**: Restrict the global `autoApprove` setting from bypassing `humanApprovalRequired` constraints of the assigned role.
