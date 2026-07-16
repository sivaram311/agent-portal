# QA Review Sign-Off: Machine Gateway v0 (Round 2)

- **Reviewer Persona**: qa-reviewer
- **Pack Epoch**: machine-gateway-v0-review-2
- **Date**: 2026-07-16

---

## Verdict
**PASS**

---

## Test Result Summary
Tests run for `SecretRedactorTest`, `MachineToolGuardTest`, and `MachineModeServiceTest`:
- **Total Tests Run**: 8
- **Passed**: 8
- **Failed**: 0
- **Errors**: 0
- **Skipped**: 0

### Breakdown by Test Set
- [SecretRedactorTest](file:///E:/MyWorkspace/agent-portal/backend/src/test/java/com/agentportal/machine/MachineGatewayTest.java#L21): **1/1 Passed**
- [MachineToolGuardTest](file:///E:/MyWorkspace/agent-portal/backend/src/test/java/com/agentportal/machine/MachineGatewayTest.java#L42): **6/6 Passed**
- [MachineModeServiceTest](file:///E:/MyWorkspace/agent-portal/backend/src/test/java/com/agentportal/machine/MachineGatewayTest.java#L109): **1/1 Passed**

---

## Coverage Status & Gaps

The test suite was run successfully. The coverage gaps identified in Round 1 remain, as no new test coverage was added during this remediation cycle (remediations were focused on security and path-jail logic).

### SecretRedactor
- `redactJsonNode` is completely untested (no tests pass `JsonNode` instances to the redactor).
- Helper/utility methods `redactMap`, `looksLikeSecretValue`, `sensitiveKeyHints`, `redactedMarker`, `locale` have no direct test coverage.
- Edge cases for `isSensitiveKey` (such as `null` or blank input) are not tested.

### MachineToolGuard
- `isGatewayRole` is not covered by unit tests.
- `denyReasonOrNull` (the primary integration endpoint for checking actions) has no unit tests.
- `isFilesystemMutating` lacks coverage for category matches (e.g. "deploy") or custom commands containing mutating actions (e.g. "delete", "move", "rename", etc.).
- Path validation errors are not fully covered:
  - Rejection due to empty paths is not tested.
  - Rejection of path traversal (`..`) is not tested.
  - Parsing of `file:` URI schemas is not tested.
- Shell command safety guards are only partially covered:
  - Allowed windows commands like `taskkill` and port querying (`Get-NetTCPConnection`) are tested, but additional options/flags are not.
  - Command input parsing/normalizing behavior (e.g. quotes/whitespace handling) is not tested.

### MachineMode & MachineModeService
- `MachineMode.platformRole()` and `MachineMode.roleFor()` are not tested.
- HTTP header limits check (`headerMaxMode`) is only tested for a basic matching case. The following cases are untested:
  - Request when `headerMaxMode` is lower than config ceiling.
  - Request when `headerMaxMode` is higher than config ceiling (should cap at config).
  - Invalid `headerMaxMode` values (which should throw HTTP 400).
- Fallback behaviour when `configuredMaxMode` is invalid/null is not tested.
