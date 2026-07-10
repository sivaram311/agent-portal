# Sub-agents roadmap (Future Implementation)

Specialized agents that know **this machine’s protocols** and keep state in **PostgreSQL**. They are not implemented yet; this is the contract.

## Sub-agent roster

| Agent | Knows | Owns | Writes to Postgres (future) |
|-------|-------|------|------------------------------|
| **Ops / Deploy** | Ports, NGINX, Cloudflare, process-by-port | Staging/prod deploys | `deploy_event`, `port_lease` |
| **State** | Registry schemas | Port leases, project registry, run history | `port_lease`, `project`, `agent_run` |
| **Review / Merge** | Git protocol, secrets scan, ACCESS-PROTOCOLS | PR review comments, merge gates | `review_event` |
| **QA / Test** | e2e, checklists, bug format | Test reports, bug tickets | `test_run`, `bug` |
| **Engineering Manager** | VIRTUALDEV-CO | Task graph, delegation | `task`, `assignment` |

All sub-agents must load:

- [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md)
- [PORT-REGISTRY.md](PORT-REGISTRY.md)
- [WORKFLOW.md](WORKFLOW.md)
- [VERSIONING-PROMOTE.md](VERSIONING-PROMOTE.md)

## Postgres control plane (planned)

Suggested database: shared Postgres (compose already used for portal profile) or dedicated `platform` DB.

Core tables (sketch):

- `port_lease` — see PORT-REGISTRY
- `project` — slug, sandbox_path, client_id, subdomains
- `agent_run` — who, session_id, role, status, timestamps
- `task` / `assignment` — orchestrator graph
- `deploy_event` — version, env, result
- `review_event` — PR, verdict
- `test_run` / `bug` — QA output

State sub-agent is the only writer to registry tables; others call its API.

## Cursor skills bridge (near-term)

Until services exist, encode each sub-agent as a **Cursor skill** under `.cursor/skills/` that points at these docs (similar to existing `ap-*` skills). Future Implementation: add `ap-platform-ops`, `ap-platform-review`, `ap-platform-qa`.

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
