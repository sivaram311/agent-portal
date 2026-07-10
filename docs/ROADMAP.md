# Agent Portal roadmap

Prioritized improvements and the Cursor skills (subagents) that own each track.

## Status

| Priority | Track | Skill | Status |
|----------|-------|-------|--------|
| P0 | Sub-agent panel | `ap-subagents` | Implemented (model + abandon API + UI) |
| P0 | Antigravity interactive turns | `ap-agy-interactive` | Soft mode (`input_required` on questions) |
| P1 | Code + Preview tabs | `ap-code-preview` | Implemented (files API + viewer) |
| P1 | Auth + harden | `ap-security` | Optional `app.security.api-key` |
| P2 | UX polish | `ap-ux-polish` | Toasts + compact mobile list |
| P2 | E2E expansion | `ap-e2e` | tablet-1024 + desktop-1440 projects |
| P3 | Production data | `ap-ops` | See [OPS.md](OPS.md) |

## How to use

In Cursor, ask an agent to follow the matching skill under `.cursor/skills/` for follow-up work.

Keep root `README.md` and this roadmap updated when a track changes.
