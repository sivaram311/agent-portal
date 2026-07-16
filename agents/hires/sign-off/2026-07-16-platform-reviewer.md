# Platform Review Sign-Off: Machine Gateway v0

- **Reviewer Persona**: platform-reviewer
- **Skill**: ap-platform-review
- **Pack Epoch**: machine-gateway-v0-review-1
- **Date**: 2026-07-16

---

## Verdict
**HOLD**

---

## Executive Summary

A comprehensive platform review of the Machine Gateway and related components in Lane B ownership was conducted. The components reviewed include [MachineChatService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineChatService.java), [MachineContextService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineContextService.java), [MachineController.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/web/MachineController.java), [AgentApiController.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/web/AgentApiController.java), the MachineChat DTOs ([MachineChatRequest.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/dto/MachineChatRequest.java) and [MachineChatResponse.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/dto/MachineChatResponse.java)), [AppProperties.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/config/AppProperties.java) (MachineGateway), [application.properties](file:///E:/MyWorkspace/agent-portal/backend/src/main/resources/application.properties), the GATEWAY roles within [PlatformRegistryService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/PlatformRegistryService.java), and [WorkspacePathResolver.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/WorkspacePathResolver.java).

The platform architecture is well-designed with strong role segregation, appropriate default configuration fallback keys, and rate-limiting support. However, a critical logic flaw was identified in the session resolution logic that can lead to cross-workspace session crosstalk. As a result, the review is on **HOLD** pending resolution of this blocking issue.

---

## Checklist

### 1. MachineChat DTOs & Controller
- [x] **Request Validation**: **PASSED**  
  [MachineChatRequest.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/dto/MachineChatRequest.java) successfully uses `@NotBlank` for the prompt payload validation.
- [x] **Controller Endpoints**: **PASSED**  
  [MachineController.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/web/MachineController.java) exposes `/api/machine/context` and `/api/machine/chat` with appropriate assertions ensuring that the gateway is enabled via [AppProperties.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/config/AppProperties.java).

### 2. MachineChatService Session Management
- [ ] **Workspace Isolation / Session Reuse**: **FAILED**  
  [MachineChatService.resolveSession](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineChatService.java#L110) uses string suffix matching to resolve existing sessions: `wp.endsWith(marker)`. If a user has a session configured for a workspace path like `other-machine-gateway`, a new request for `machine-gateway` will incorrectly match and reuse that session, leading to session hijacking or crosstalk between different workspaces.
- [x] **Context Injection**: **PASSED**  
  [MachineChatService.chat](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineChatService.java#L57) injects live redacted context into the agent prompt, ensuring size safety by truncating the serialized context if it exceeds 12,000 characters.

### 3. Machine Context & App Properties
- [x] **Environment Redaction**: **PASSED**  
  [MachineContextService.buildContext](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineContextService.java#L50) successfully constructs a structural metadata map containing system information, active ports, open sessions, and platform tasks, passing the result to the redactor.
- [x] **AppProperties Keys**: **PASSED**  
  [AppProperties.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/config/AppProperties.java) properties align completely with [application.properties](file:///E:/MyWorkspace/agent-portal/backend/src/main/resources/application.properties) default keys under the prefix `app.machine-gateway`.

### 4. Platform Registry Service GATEWAY Roles
- [x] **Role Segregation & Definitions**: **PASSED**  
  [PlatformRegistryService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/PlatformRegistryService.java#L123) correctly registers `GATEWAY_OBSERVE`, `GATEWAY_ADVISE`, `GATEWAY_ACT`, and `GATEWAY_OPS` roles. 
- [x] **Privilege & Tool Mapping**: **PASSED**  
  Role mappings align with the threat model (Observe is read-only, Advise includes memory and port listings, Act permits edits, Ops permits shell execution with PID/Port filters).

### 5. WorkspacePathResolver Security
- [x] **Jail Path Resolution**: **PASSED**  
  [WorkspacePathResolver.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/WorkspacePathResolver.java#L44) securely normalizes input paths, restricts path traversal (`..`), and performs strict prefix matching against the allowed roots.
- [x] **Windows Separator Compatibility**: **PASSED**  
  [WorkspacePathResolver.isUnder](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/service/WorkspacePathResolver.java#L84) handles separator and drive letter case variations to prevent Windows-specific bypasses.

---

## Findings & Technical Details

### Finding 1: Suffix Match Path Collision in Session Resolution
- **Component**: [MachineChatService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineChatService.java#L129)
- **Severity**: **High**
- **Description**: The method `resolveSession` matches previous workspace paths using suffix checks:
  ```java
  boolean sameWs = wp.endsWith("/" + marker) || wp.endsWith(marker);
  ```
  If `marker` is `machine-gateway` and `wp` is `E:/MyWorkspace/agent-portal/backend/workspaces/other-machine-gateway`, `wp.endsWith(marker)` evaluates to `true`. This causes the backend to route requests intended for one workspace to another workspace's session.
- **Remediation**: Use exact path validation or check components to ensure `marker` represents a complete directory/file name matching the end of the path:
  ```java
  boolean sameWs = wp.endsWith("/" + marker) || wp.equals(marker);
  ```

---

## Blocking Issues
1. **Fix Session Suffix Match**: Correct the `sameWs` logic in [MachineChatService.java](file:///E:/MyWorkspace/agent-portal/backend/src/main/java/com/agentportal/machine/MachineChatService.java#L129) to prevent suffix-based collision matching across different workspace folders.
