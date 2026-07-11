# Sub-agents roadmap

Specialized agents that know **this machine’s protocols**. Cursor **skills** + portal APIs are live; long-lived worker *services* and dedicated control-plane event tables remain planned.

## Sub-agent roster

| Agent | Knows | Owns today | Writes to Postgres (planned) |
|-------|-------|------------|------------------------------|
| **Ops / Deploy** | Ports, NGINX, Cloudflare, process-by-port | Promote ops + DNS scripts | `deploy_event`, `port_lease` |
| **State** | Registry schemas | Port doc + `/api/platform/ports` | `port_lease`, `project`, `agent_run` |
| **Review / Merge** | Git protocol, secrets scan, ACCESS-PROTOCOLS | `ap-platform-review` skill | `review_event` |
| **QA / Test** | e2e, checklists, bug format | `ap-platform-qa`, `ap-e2e-realme-p2-pro` | `test_run`, `bug` |
| **Engineering Manager** | VIRTUALDEV-CO | `/api/platform/tasks`, pipelines, swarm | `task`, `assignment` |

All sub-agents must load:

- [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md)
- [PORT-REGISTRY.md](PORT-REGISTRY.md)
- [WORKFLOW.md](WORKFLOW.md)
- [VERSIONING-PROMOTE.md](VERSIONING-PROMOTE.md)

## Postgres control plane (planned)

Shared Postgres `:5432` already hosts `app_agent_portal` schemas `preprod` / `prod` for the portal app. A dedicated control-plane (or extra tables) for promote/review/QA events is still planned.

Core tables (sketch):

- `port_lease` — see PORT-REGISTRY
- `project` — slug, sandbox_path, client_id, subdomains
- `agent_run` — who, session_id, role, status, timestamps
- `task` / `assignment` — orchestrator graph (portal `platform_tasks` is the interim)
- `deploy_event` — version, env, result
- `review_event` — PR, verdict
- `test_run` / `bug` — QA output

State sub-agent is the only writer to registry tables; others call its API.

## Cursor skills bridge (shipped)

Encoded under `.cursor/skills/`:

- `ap-platform-ops` · `ap-platform-review` · `ap-platform-qa` · `ap-platform-state` · `ap-platform-em`
- `ap-e2e-realme-p2-pro` — Realme 360×780 E2E
- Machine promote skills live under `E:\MyAgent\workflow\promote\` (`promote-em`, `promote-qa`, …)

## Commit / review / merge agent behavior

1. Diff against base branch.
2. Reject secrets, port theft, sandbox escapes.
3. Require PORT-REGISTRY / docs updates when needed.
4. Approve → human or bot merge with message conventions.
5. Record `review_event`.

## Deploy agent behavior

1. Read version + artifact.
2. Confirm staging smoke green.
3. Apply NGINX/DNS only from templates.
4. Restart **by port**.
5. Record `deploy_event`; roll back on health fail.

## QA agent behavior

1. Run Playwright / checklists for the app version.
2. File bugs with repro + env + port.
3. Block promote if severity ≥ configured threshold.
