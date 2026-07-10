---
name: ap-security
description: >-
  Adds auth and hardens Agent Portal for shared or public exposure. Use when
  implementing login, API keys, CORS lockdown, or disabling skip-permissions.
---

# Agent Portal — Auth & harden (P1)

## Goal
Make the portal safer beyond local single-user use.

## Build plan
1. Session/API auth (Spring Security): basic token or form login; protect `/api/**` and WS handshake.
2. Config: default `agent.antigravity.skip-permissions=false` for non-local profiles; require explicit opt-in.
3. CORS: allowlist concrete origins in prod (no `http://*:4200`).
4. Disable H2 console in prod; document TLS reverse-proxy.
5. Update README security section.

## Constraints
- Keep a `local` profile that stays easy for developers
- Never commit secrets; use env vars
