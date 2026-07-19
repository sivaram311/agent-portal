# SIGN-OFF — agent-portal feature/forgecity-tamil-rewrite

| Field | Value |
|-------|-------|
| Session | CONSCIOUS #17 readonly reviewer — forgecity-tamil-rewrite push |
| Reviewer agent id | readonly Reviewer (generalPurpose, no push/commit/edit beyond this SIGN-OFF) |
| Provider | cursor |
| Tip SHA | f4e97f59615ab3eea6dbba47c005ed01d081bf12 |
| Branch / tag | feature/forgecity-tamil-rewrite (branch push) |
| When (UTC+5:30) | 2026-07-20 00:20 |

## Checklist

- [x] Docs updated same turn (CONSCIOUS #12) — `docs/platform/FORGECITY-TAMIL-REWRITE.md` (new) + `docs/OPS.md` privacy-sensitive section, both in tip
- [x] No secrets in commit — `application.properties` uses env-var placeholders with empty/false defaults (`FORGECITY_REWRITE_API_KEY:` blank, `FORGECITY_REWRITE_ENABLED:false`); no literal keys
- [x] Fleet splits OK — N/A (single backend endpoint; no classic/css-next/AV matrix impact)
- [x] DEV E2E green if release tag (#16) — N/A (branch push, no tag)
- [x] Login E2E on DEV public domain (#18) — N/A (dedicated `X-ForgeCity-Key` route, not CSS login)
- [x] Tag ≠ live understood — N/A (no tag)

### Requested audit items

- [x] Dedicated `X-ForgeCity-Key` auth — controller checks header via `ForgeCityKeyAuthenticator.matches()` using constant-time SHA-256 (`MessageDigest.isEqual`); CSS JWT and portal `X-API-Key` explicitly do not authenticate this route
- [x] Default-disabled endpoint — `ForgeCityRewriteProperties.enabled` defaults `false`; `isAvailable()` requires enable + non-blank key + bounds; controller returns `503 unavailable` when off
- [x] No ChatMessage/session body persistence — `ForgeCityTamilRewriteService` runs an ephemeral ACP turn; no repository/DB writes, no ChatMessage/AgentSession; request body read to bounded `byte[]`, parsed, never logged
- [x] No payload logs — service constructs `new AcpClient(process, mapper, false)`; `logPayloads=false` suppresses `ACP >>`/`ACP <<` debug and redacts parse-warn lines
- [x] Rate-limit open-access fix — `RateLimitFilter` excludes `/api/integrations/forgecity/tamil-rewrite` from the `open-access` bypass, so the endpoint is always rate-limited (returns `{"status":"busy"}` + `no-store` on 429)
- [x] Docs presence — `docs/platform/FORGECITY-TAMIL-REWRITE.md` + `docs/OPS.md` present in tip
- [x] Unrelated working-tree files NOT in tip — confirmed via `git status`: `.env.docker.example` (CURSOR_MODEL) and `docs/platform/PORT-REGISTRY.md` are modified but unstaged; `.agent-portal/` and `workspaces/agent-api/PREPROD-curl-recipe.sh` are untracked. None appear in `git show --name-status HEAD`

## Verdict

**GO**

### Findings
- Tip commit is scoped exactly to the ForgeCity Tamil rewrite feature (17 files: backend main/test, docs, openapi/ACTIONS). No unrelated working-tree changes leaked into the commit.
- Security model is sound: dedicated header + constant-time comparison, disabled by default, strict request contract (exact field set, size/codepoint caps, NUL rejection), strict Tamil output validation, tool/permission/extension rejection, ephemeral process force-killed via `closeImmediately()`.
- Privacy: no persistence and no payload logging paths for the ephemeral session.
- Rate-limit gap for open-access mode is closed for this route.
- Minor (non-blocking): `RateLimitFilter.isTrustedProxyPeer()` contains a stray literal `"https://example.net/id/garnet".equals(remote)` — dead/never-matching code (a remote IP can never equal a URL string); no security impact and not a secret. Recommend cleanup in a follow-up.
- Cosmetic (non-blocking): `application.properties` comment em-dashes rendered as mojibake (`â€"`) in this diff — encoding artifact only, no functional impact.
