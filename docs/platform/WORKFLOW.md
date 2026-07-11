# Workflow — Team & AI operating model

**Status:** Operating protocol (in use). Promote/gates are live via `E:\MyAgent\workflow\promote\`; autonomous worker services remain future (see [SUBAGENTS-ROADMAP.md](SUBAGENTS-ROADMAP.md)).

This is how a **team of people and AI agents** works on projects on this machine. The goal is repeatable, reviewable delivery — not ad-hoc edits on production.

## Roles (human or agent)

| Role | Responsibility |
|------|----------------|
| **Requester** | States goal; does not bind ports or deploy |
| **Planner** | Reads platform docs; writes/updates plan; claims ports if needed |
| **Builder** | Implements in sandbox / feature branch only |
| **Reviewer** | Code review, protocol compliance, security |
| **QA** | Tests against checklist; files bugs; no silent prod changes |
| **Releaser** | Version bump, promote staging → prod per VERSIONING-PROMOTE |
| **Ops** | Ports, NGINX, Cloudflare, process restarts (narrow kill rules) |

Each role maps to a **skill** today (`ap-platform-*`, promote-*); long-lived services are future. Cursor agents **must act as if** these roles exist.

## Standard loop

```text
1. Orient     → Read ACCESS-PROTOCOLS + PORT-REGISTRY + relevant project README
2. Plan       → Short plan in chat or docs; list ports, sandbox path, CSS client
3. Claim      → Reserve port / subdomain in PORT-REGISTRY (doc now; DB later)
4. Build      → Work only under sandbox or repo working tree; no secrets in git
5. Verify     → Health checks, e2e/manual QA, Changes tab Keep/Restore
6. Review     → Human or Reviewer agent; merge only via PR protocol
7. Promote    → Versioned artifact → staging subdomain → prod (never skip)
8. Record     → Update docs/registry; Postgres audit when available
```

## Before any agent starts work

Checklist:

- [ ] Read [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md)
- [ ] Check [PORT-REGISTRY.md](PORT-REGISTRY.md) for free ports
- [ ] Confirm write path is under allowed sandbox / workspace
- [ ] Confirm CSS clientId if the app needs auth
- [ ] Confirm whether change is **sandbox**, **staging**, or **prod** (default: sandbox)

## Git protocol (until Review-Merge sub-agent exists)

1. Feature branch from `main` / `master`.
2. No force-push to main; no `--no-verify` unless human explicitly asks.
3. PR (or explicit human “commit and push”) with summary + test notes.
4. Reviewer checks: protocols, ports, secrets, sandbox boundaries.
5. Merge only after review.

## Talking to Agent Portal from another AI

Use workspace [`workspaces/agent-api/`](../../workspaces/agent-api/) and contract [AGENT-API.md](AGENT-API.md).

## Talking to this machine from Cursor

- Prefer skills under `.cursor/skills/` for Agent Portal tracks.
- Prefer this `docs/platform/` folder for machine-wide rules.
- Never `Stop-Process` by matching `cursor` / `node` / `agent` broadly.

## Definition of done (any task)

1. Code or docs updated in the right place.
2. Port registry / docs updated if ports or URLs changed.
3. No secrets committed.
4. Human can re-run from docs alone (SESSION-RESTORE / OPS / this handbook).
