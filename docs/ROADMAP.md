# Agent Portal roadmap

Prioritized improvements and the Cursor skills (subagents) that own each track.

## Status

| Priority | Track | Skill | Status |
|----------|-------|-------|--------|
| P0 | Sub-agent panel | `ap-subagents` | Implemented (child-scoped Cursor abandon) |
| P0 | Antigravity interactive | `ap-agy-interactive` | Soft Q&A + ACP attempt when CLI supports `--acp` |
| P1 | Code + Preview | `ap-code-preview` | Monaco + HTML preview |
| P1 | Diff / change review | `ap-diff-review` | Changes tab + Keep/Restore |
| P1 | Auth + harden | `ap-security` | CSS JWT + optional API key |
| P1 | Session isolation | `ap-isolation` | Owner + collaborators |
| P1 | WebSocket auth | `ap-ws-auth` | SockJS token + STOMP ACL |
| P2 | Templates / presets | `ap-templates` | `GET /api/presets` + create dialog |
| P2 | History / replay | `ap-history-replay` | Events API + History tab |
| P2 | Sharing | `ap-sharing` | Collaborator API + share bar |
| P2 | Audit UI | `ap-audit-ui` | Activity tab |
| P2 | UX polish | `ap-ux-polish` | Toasts + capability badges |
| P2 | E2E | `ap-e2e` | Device projects + auth.spec |
| P3 | Webhooks & quotas | `ap-webhooks-quotas` | `app.webhooks.url` + quota bytes |
| P3 | Ops | `ap-ops` | [OPS.md](OPS.md) |
| P3 | CSS starter | `ap-css-starter` | Portal uses `com.css:css-spring-boot-starter` |

## How to use

In Cursor, ask an agent to follow the matching skill under `.cursor/skills/` for follow-up work.

Keep root `README.md` and this roadmap updated when a track changes.
