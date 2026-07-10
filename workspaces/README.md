# Agent Portal workspaces

Default sandbox root for session `workspacePath` values (`AGENT_WORKSPACE_ROOT` / `agent.workspace.root`). Relative paths like `demo` or `FileBridge` resolve here.

## Tracked samples

| Path | Purpose |
|------|---------|
| `demo/` | Small Java sample + **materialized Rules & Skills** (`.cursor/rules`, `.cursor/skills`, `AGENTS.md`) for guidance QA |
| `FileBridge/` | Full demo app (Spring Boot file manager) used for longer agent sessions |

## Not committed (runtime)

Gitignored under any workspace:

- `.agent-portal/` — Keep/Restore baselines
- `data/`, `logs/`, `target/`, `node_modules/`
- Scratch agent outputs (`.test-*`, `agy-*.txt`, `*.class`, etc.)

Create new session folders as needed; keep secrets out of this tree.
