---
name: ap-isolation
description: >-
  Session ownership and workspace sandbox hardening for Agent Portal multi-user
  CSS deployments. Use when scoping sessions to JWT subjects or locking paths.
---

# Agent Portal — Isolation (P1)

## Goal
Users only see/act on their own sessions; workspaces cannot escape the configured root.

## Build plan
1. Persist `ownerUsername` on `AgentSession`; set from SecurityContext on create.
2. Filter list/get/mutate by owner when CSS (or any auth) is active; local/dev may use `anonymous`.
3. Harden `resolveWorkspace` via `WorkspacePathResolver` — relative paths under `agent.workspace.root`; reject absolute escapes unless listed in `agent.workspace.allowed-roots` / `AGENT_WORKSPACE_ALLOWED_ROOTS`.
4. Harden `WorkspaceFileService.resolveSafe` with `toRealPath()` when possible.
5. Document multi-user model + allowlist in OPS.md / README.

## Constraints
- Do not break local unauthenticated mode (`css.enabled=false`)
- Keep H2 schema additive (nullable owner for old rows → treat as shared/legacy)
