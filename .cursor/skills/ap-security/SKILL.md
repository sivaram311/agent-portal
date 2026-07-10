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
1. CSS JWT resource server (`css.enabled`) + optional `X-API-Key` fallback.
2. Protect `/api/**` and `/ws/**` when CSS is on; SockJS `access_token` query.
3. Session ownership (`ap-isolation`) and STOMP ACLs (`ap-ws-auth`).
4. Config: default `agent.antigravity.skip-permissions=false` on prod profile.
5. CORS: allowlist concrete origins in prod (no `http://*:4200`).
6. Disable H2 console in prod; document TLS reverse-proxy in OPS.md.
7. Prefer migrating to `com.css:css-spring-boot-starter` when ready (`ap-css-starter`).

## Constraints
- Keep a `local` profile that stays easy for developers
- Never commit secrets; use env vars
