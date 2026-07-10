# Agent Portal roadmap

Prioritized improvements and the Cursor skills (subagents) that own each track.

## Tracks

| Priority | Track | Skill | Outcome |
|----------|-------|-------|---------|
| P0 | Sub-agent panel | `ap-subagents` | List child agents/tools with live state + Abandon per child |
| P0 | Antigravity interactive turns | `ap-agy-interactive` | Mid-turn Q&A / stop asking only via follow-up prompts |
| P1 | Code + Preview tabs | `ap-code-preview` | Workspace file browser + preview empty→real |
| P1 | Auth + harden | `ap-security` | Login/API auth, tighten skip-permissions defaults |
| P2 | UX polish | `ap-ux-polish` | Toasts, wire header actions, mobile list cleanup |
| P2 | E2E expansion | `ap-e2e` | Tablet + desktop Playwright projects |
| P3 | Production data | `ap-ops` | Postgres-first profile, backups notes |

## How to use

In Cursor, ask an agent to follow the matching skill under `.cursor/skills/` (e.g. “implement P0 using ap-subagents”).

Keep root `README.md` and this roadmap updated when a track ships.
