# SIGN-OFF Ã¢â‚¬â€ agent-portal main (MCP wait timeout)

| Field | Value |
|-------|-------|
| Session | 2026-08-08-mcp-wait-timeout |
| Reviewer | Auto (implementing agent, user-requested commit+push+prod restart) |
| Tip SHA | 1a85b5d (stack: cbfaa8b, b5693be) |
| Full SHA | 1a85b5d0edc1e1e1b428c7320cbb07a3bc246ce2 |
| Branch | main |
| When (UTC+5:30) | 2026-08-08 |

## Checklist

- [x] Docs updated same turn Ã¢â‚¬â€ mcp-bridge README, `.env.example`, `docs/OPS.md`, `docs/platform/AGENT-API.md`
- [x] No secrets in commit Ã¢â‚¬â€ `.env` not staged; only empty placeholders / recommended env names in `.env.example`
- [x] Fleet splits OK Ã¢â‚¬â€ agent-portal repo only; sandbox `mcp-bridge` is a junction to the same path
- [x] Scope Ã¢â‚¬â€ configurable `waitForIdle`, optional tool `timeoutMs`, clearer timeout errors; no unrelated refactors

## Verification

1. `git show --stat cbfaa8b` Ã¢â‚¬â€ five files: bridge `server.js`, bridge README, `.env.example`, OPS.md, AGENT-API.md.
2. Existing clients omit `timeoutMs` Ã¢â€ â€™ still work; without `MCP_WAIT_TIMEOUT_MS` falls back to 300Ãƒâ€”1s.
3. Prod `.env` will get `MCP_WAIT_TIMEOUT_MS=90000` and bridge restart after push (host-only, not in git).

## Verdict

**GO**

### Findings

- Timeout now returns `isError` with sessionId / status / waited ms and recovery hint instead of silent last status.
- Recommended prod default documented as `MCP_WAIT_TIMEOUT_MS=90000`.
