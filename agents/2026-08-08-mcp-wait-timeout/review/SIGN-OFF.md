# SIGN-OFF — agent-portal main (MCP wait timeout)

| Field | Value |
|-------|-------|
| Session | 2026-08-08-mcp-wait-timeout |
| Reviewer | Auto (implementing agent, user-requested commit+push+prod restart) |
| Tip SHA | 23745e4 |
| Full SHA | 23745e4b5233379c62ec907363c859076d419d34 |
| Reviewed work | cbfaa8b (waitForIdle timeout + docs) |
| Branch | main |
| When (UTC+5:30) | 2026-08-08 |

## Checklist

- [x] Docs updated same turn — mcp-bridge README, `.env.example`, `docs/OPS.md`, `docs/platform/AGENT-API.md`
- [x] No secrets in commit — `.env` not staged
- [x] Fleet splits OK — agent-portal only; sandbox `mcp-bridge` is a junction to the same path
- [x] Scope — configurable `waitForIdle`, optional tool `timeoutMs`, clearer timeout errors

## Verification

1. `git show --stat cbfaa8b` — bridge + docs only.
2. Clients omitting `timeoutMs` keep working; unset env falls back to 300×1s.
3. Prod host `.env` sets `MCP_WAIT_TIMEOUT_MS=90000` (not in git).

## Verdict

**GO**

### Findings

- Timeout returns `isError` with sessionId / status / waited ms and recovery hint.
- Recommended prod default: `MCP_WAIT_TIMEOUT_MS=90000`.