# QA Review Sign-Off

**Date:** 2026-07-16
**Verdict:** PASS
**Test Result:** 6 tests run, 6 passed, 0 failed

## Coverage Gaps Identified

### SecretRedactor
- `redactJsonNode` is completely untested (no tests pass `JsonNode` instances to the redactor).
- Helper/utility methods `redactMap`, `looksLikeSecretValue`, `sensitiveKeyHints`, `redactedMarker`, `locale` have no direct test coverage.
- Edge cases for `isSensitiveKey` (such as `null` or blank input) are not tested.

### MachineToolGuard
- `isGatewayRole` is not covered by unit tests.
- `denyReasonOrNull` (the primary integration endpoint for checking actions) has no tests.
- `isFilesystemMutating` lacks coverage for category matches (e.g. "deploy") or custom commands containing mutating actions (e.g. "delete", "move", "rename", etc.).
- Path validation errors are not fully covered:
  - Rejection due to empty paths is not tested.
  - Rejection of path traversal (`..`) is not tested.
  - Parsing of `file:` URI schemas is not tested.
- Shell command safety guards are only partially covered:
  - Allowed windows commands like `taskkill` and port querying (`Get-NetTCPConnection`) are not tested.
  - Command input parsing/normalizing behavior (e.g. quotes/whitespace handling) is not tested.

### MachineMode & MachineModeService
- `MachineMode.platformRole()` and `MachineMode.roleFor()` are not tested.
- HTTP header limits check (`headerMaxMode`) is only tested for a basic matching case. The following cases are untested:
  - Request when `headerMaxMode` is lower than config ceiling.
  - Request when `headerMaxMode` is higher than config ceiling (should cap at config).
  - Invalid `headerMaxMode` values (which should throw HTTP 400).
- Fallback behaviour when `configuredMaxMode` is invalid/null is not tested.
