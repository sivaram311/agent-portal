# Agent Portal roadmap

Prioritized improvements and the Cursor skills (subagents) that own each track.

## Status

| Priority | Track | Skill | Status |
|----------|-------|-------|--------|
| P0 | Sub-agent panel | `ap-subagents` | Implemented (child-scoped Cursor abandon) |
| P0 | Antigravity interactive turns | `ap-agy-interactive` | Soft Q&A + CLI capability probe (`interactive-protocol=auto`) |
| P1 | Code + Preview tabs | `ap-code-preview` | Monaco (vendored) + HTML iframe preview |
| P1 | Auth + harden | `ap-security` | CSS JWT resource server + optional API key |
| P1 | Session isolation | `ap-isolation` | `ownerUsername` + workspace root sandbox |
| P1 | WebSocket auth | `ap-ws-auth` | SockJS token + STOMP subscribe ACL when CSS on |
| P2 | Audit UI | `ap-audit-ui` | `GET /api/audit` + Activity tab |
| P2 | UX polish | `ap-ux-polish` | Toasts + capability badges |
| P2 | E2E expansion | `ap-e2e` | tablet/desktop + `auth.spec.ts` |
| P3 | Production data / ops | `ap-ops` | See [OPS.md](OPS.md) prod checklist |
| P3 | CSS starter | `ap-css-starter` | `clients/spring-boot-starter` implemented (portal still in-repo copy) |

## How to use

In Cursor, ask an agent to follow the matching skill under `.cursor/skills/` for follow-up work.

Keep root `README.md` and this roadmap updated when a track changes.
