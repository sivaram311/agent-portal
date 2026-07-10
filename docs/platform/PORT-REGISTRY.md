# Port registry

**Status:** Canonical markdown registry (Phase 0). Future: Postgres `port_lease` table maintained by a State sub-agent.

Any human or AI **must** read this before binding a port. After claiming, update this file in the same change set.

## Claim protocol

1. Pick a free port from **Available ranges** (or an unused listed port).
2. Add a row under **Active leases** with owner, app, env, bind, notes.
3. Start the service.
4. On teardown, mark the row `released` or remove it and note in git history.

### Row format

| Column | Meaning |
|--------|---------|
| port | TCP port |
| service | Logical name |
| owner_app | Repo / product |
| env | `host` \| `docker` \| `sandbox` \| `staging` \| `prod` |
| bind | e.g. `0.0.0.0` or `127.0.0.1` |
| status | `active` \| `reserved` \| `released` |
| notes | URL, process hint |

## Active leases (host — observed 2026-07-11)

| port | service | owner_app | env | bind | status | notes |
|------|---------|-----------|-----|------|--------|-------|
| 80 | nginx-http | Deployment | host | 0.0.0.0 | active | Cloudflare Flexible → origin |
| 443 | nginx-https | Deployment | host | — | reserved | Enable when origin TLS ready |
| 4200 | agent-portal-ui | agent-portal | host | 0.0.0.0 | active | `ng serve` / static UI |
| 5432 | postgres | docker/local | host | — | active | Optional portal postgres profile |
| 8080 | agent-portal-api | agent-portal | host | 0.0.0.0 | active | Spring Boot JAR |
| 8081 | legacy-grok-or-other | grok_dev / misc | host | — | active | Confirm before reuse |
| 8082 | filebridge | agent-portal/workspaces/FileBridge | host | 0.0.0.0 | active | Sample app |
| 8091 | stack-pilot | stack-pilot | host | — | active | Remote/ops tooling |
| 9000 | css-auth | centralized-security-system | host | 0.0.0.0 | active | SSO + JWKS |

## Available ranges (prefer these for new apps)

| Range | Use |
|-------|-----|
| 8100–8199 | New Spring Boot / API sandboxes |
| 4300–4399 | New frontend dev servers |
| 9100–9199 | Extra auth/sidecar services |
| 3000–3099 | Node utilities (avoid if conflicting) |

## Public URL mapping (via NGINX / Cloudflare)

| Public path / host | Upstream |
|--------------------|----------|
| `https://delena.buzz/` | `127.0.0.1:4200` |
| `https://delena.buzz/api/` | `127.0.0.1:8080` |
| `https://delena.buzz/ws/` | `127.0.0.1:8080` |
| `https://delena.buzz/auth/` | `127.0.0.1:9000` |
| `https://delena.buzz/.well-known/` | `127.0.0.1:9000` |

Future app subdomains: see [CLOUDFLARE-DNS-PROXY.md](CLOUDFLARE-DNS-PROXY.md).

## Future: Postgres schema (not implemented)

```sql
-- Future Implementation
CREATE TABLE port_lease (
  port          INT PRIMARY KEY,
  service       TEXT NOT NULL,
  owner_app     TEXT NOT NULL,
  env           TEXT NOT NULL,
  bind_address  TEXT NOT NULL DEFAULT '0.0.0.0',
  status        TEXT NOT NULL,
  claimed_by    TEXT,
  claimed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  released_at   TIMESTAMPTZ,
  notes         TEXT
);
```

State sub-agent will own CRUD; Builder agents call an API instead of editing this markdown.
