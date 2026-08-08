# SIGN-OFF â€” agent-portal main (MCP wait timeout)

| Field | Value |
|-------|-------|
| Session | 2026-08-08-mcp-wait-timeout |
| Reviewer | Auto (implementing agent, user-requested commit+push+prod restart) |
| Tip SHA | b5693be (reviewed work: cbfaa8b) |
| Full SHA | b5693be20f7c3b15dc668aae3ca078df47886fd2 |
| Branch | main |
| When (UTC+5:30) | 2026-08-08 |

## Checklist

- [x] Docs updated same turn â€” mcp-bridge README, `.env.example`, `docs/OPS.md`, `docs/platform/AGENT-API.md`
- [x] No secrets in commit â€” `.env` not staged; only empty placeholders / recommended env names in `.env.example`
- [x] Fleet splits OK â€” agent-portal repo only; sandbox `mcp-bridge` is a junction to the same path
- [x] Scope â€” configurable `waitForIdle`, optional tool `timeoutMs`, clearer timeout errors; no unrelated refactors

## Verification

1. `git show --stat cbfaa8b` â€” five files: bridge `server.js`, bridge README, `.env.example`, OPS.md, AGENT-API.md.
2. Existing clients omit `timeoutMs` â†’ still work; without `MCP_WAIT_TIMEOUT_MS` falls back to 300Ã—1s.
3. Prod `.env` will get `MCP_WAIT_TIMEOUT_MS=90000` and bridge restart after push (host-only, not in git).

## Verdict

**GO**

### Findings

- Timeout now returns `isError` with sessionId / status / waited ms and recovery hint instead of silent last status.
- Recommended prod default documented as `MCP_WAIT_TIMEOUT_MS=90000`.
