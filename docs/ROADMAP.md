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
| P2 | System E2E loop | `ap-system-e2e-loop` | **Shipped 0.1.2** — [SYSTEM-E2E-LOOP.md](platform/SYSTEM-E2E-LOOP.md); session auto-invoke |
| P2b | Session Console tab | frontend `console-panel` | **Shipped 0.1.3** — plain `terminal_chunk` scrollback (PowerShell-like) |
| P3 | Webhooks & quotas | `ap-webhooks-quotas` | `app.webhooks.url` + quota bytes |
| P3 | Ops | `ap-ops` | [OPS.md](OPS.md) |
| P3 | CSS starter | `ap-css-starter` | Portal uses `com.css:css-spring-boot-starter` |
| P4 | Dual-CLI promote jobs | `ap-platform-em` + MyAgent `promote-*` | **Backlog** — Cursor CLI + Antigravity job runner; configure via Portal pipelines later (`E:\MyAgent\ideas\dual-cli-subagent-workflow.md`, platform SUBAGENTS-ROADMAP) |

## How to use

In Cursor, ask an agent to follow the matching skill under `.cursor/skills/` for follow-up work.

Keep root `README.md` and this roadmap updated when a track changes.

## Platform Future Implementation

Handbook: **[docs/platform/](platform/README.md)**. Runtime: sandbox, `/api/platform/*`, Agent API, role ACLs.

**Live envs:** DEV `delena.buzz` · PREPROD `agent-portal-staging.delena.buzz` · PROD `agent-portal.delena.buzz` — [OPS.md](OPS.md#deployed-environments-2026-07-11).

Skills (sub-agents):

- `ap-platform-ops` — Ops/Deploy
- `ap-platform-review` — Git review/merge
- `ap-platform-qa` — E2E + Agent API smoke
- `ap-e2e-realme-p2-pro` — Realme P2 Pro 360×780 multi-agent E2E
- `ap-platform-state` — Port/apps registry
- `ap-platform-em` — Engineering Manager (VirtualDev Co)

Workspace for external AIs: [`workspaces/agent-api/`](../workspaces/agent-api/).
