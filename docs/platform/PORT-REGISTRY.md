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

**Machine source of truth:** `E:\MyAgent\workflow\ports\REGISTRY.md` (update both when claiming).

| port | service | owner_app | env | bind | status | notes |
|------|---------|-----------|-----|------|--------|-------|
| 80 | nginx-http | Deployment | host | 0.0.0.0 | active | Cloudflare Flexible → origin |
| 443 | nginx-https | Deployment | host | — | reserved | Enable when origin TLS ready |
| 3010 | h-drive-server | h-drive-server | host | 0.0.0.0 | active | Exposes H:\ over HTTP; open CORS |
| 4010 | h-drive-server | h-drive-server | staging | 0.0.0.0 | active | PREPROD `F:\apps\h-drive-server` |
| 4080 | agent-portal-api | agent-portal | staging | 0.0.0.0 | active | PREPROD `F:\apps\agent-portal`; agent-portal-staging.delena.buzz; Machine Gateway same `/api/machine/*` after promote |
| 4081 | agent-portal-public-edge | agent-portal | staging | 0.0.0.0 | active | PREPROD public-IP NGINX edge only; `http://103.118.183.185:4081` → `:4080` + `/auth` → `:5910`; no new CF DNS |
| 4200 | agent-portal-ui | agent-portal | host | 0.0.0.0 | active | DEV `ng serve` / delena.buzz / |
| 4900 | css-auth | css | staging | 0.0.0.0 | active | Classic Preprod CSS IdP — keep |
| 4910 | css-auth-next | css-next | staging | 0.0.0.0 | active | Side-fleet PREPROD; css-next-staging.delena.buzz → :4910 |
| 5010 | h-drive-server | h-drive-server | prod | 0.0.0.0 | active | PROD `G:\apps\h-drive-server`; https://hdrive.delena.buzz |
| 5080 | agent-portal-api | agent-portal | prod | 0.0.0.0 | active | PROD `G:\apps\agent-portal`; agent-portal.delena.buzz; Machine Gateway same `/api/machine/*` after promote |
| 5432 | postgres | docker/local | host | — | active | Shared; schemas per app |
| 5900 | css-auth | css | prod | 0.0.0.0 | active | Classic Prod CSS; css.delena.buzz — keep |
| 5910 | css-auth-next | css-next | prod | 0.0.0.0 | active | Side-fleet PROD; css-next.delena.buzz → :5910 |
| 8080 | agent-portal-api | agent-portal | host | 0.0.0.0 | active | DEV Spring Boot JAR; also serves Machine Gateway `/api/machine/*` (no extra port) |
| 8081 | legacy-grok-or-other | grok_dev / misc | host | — | active | Confirm before reuse |
| 8082 | filebridge | agent-portal/workspaces/FileBridge | host | 0.0.0.0 | active | Sample app |
| 8091 | stack-pilot | stack-pilot | host | — | active | Remote/ops tooling |
| 9000 | css-auth | centralized-security-system | host | 0.0.0.0 | active | DEV SSO + JWKS |
| 3320 | proddeck | proddeck | host | 0.0.0.0 | active | DEV `E:\wt\proddeck-integrate`; Cloud OS **0.6.1** |
| 4320 | proddeck | proddeck | staging | 0.0.0.0 | active | PREPROD `F:\apps\proddeck`; https://home-staging.delena.buzz **0.6.1** |
| 5320 | proddeck | proddeck | prod | 0.0.0.0 | active | PROD `G:\apps\proddeck`; https://home.delena.buzz **0.6.1** |
| 3311 | agentverse-v2 | agentverse-v2 | host | 0.0.0.0 | active | DEV feature/stable-v2; npm -p 3311 (SoT MyAgent ports) |
| 4311 | agentverse-v2 | agentverse-v2 | staging | 0.0.0.0 | active | PREPROD `F:\apps\agentverse-v2`; agentverse-v2-staging.delena.buzz |
| 5311 | agentverse-v2 | agentverse-v2 | prod | 0.0.0.0 | active | PROD side `G:\apps\agentverse-v2`; agentverse-v2.delena.buzz |
| 3312 | agentverse-upgrade | agentverse-upgrade | host | 0.0.0.0 | active | DEV feature/upgradation-functionality; npm -p 3312 |
| 4312 | agentverse-upgrade | agentverse-upgrade | staging | 0.0.0.0 | active | PREPROD `F:\apps\agentverse-upgrade`; agentverse-upgrade-staging.delena.buzz |
| 5312 | agentverse-upgrade | agentverse-upgrade | prod | 0.0.0.0 | active | PROD `G:\apps\agentverse-upgrade`; agentverse-upgrade.delena.buzz |
| 3330 | library | library | host | 0.0.0.0 | reserved | DEV `E:\MyWorkspace\sandbox\library`; https://library-dev.delena.buzz |
| 4330 | library | library | staging | 0.0.0.0 | reserved | PREPROD `F:\apps\library`; https://library-staging.delena.buzz |
| 5330 | library | library | prod | 0.0.0.0 | reserved | PROD `G:\apps\library`; https://library.delena.buzz |

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
| `https://delena.buzz/api/machine/` | `127.0.0.1:8080` (Machine Gateway — **no new port / no new DNS**) |
| `https://delena.buzz/ws/` | `127.0.0.1:8080` |
| `https://delena.buzz/auth/` | `127.0.0.1:5910` (css-next; Portal Wave 3) |
| `https://delena.buzz/.well-known/` | `127.0.0.1:5910` |
| `https://agent-portal-staging.delena.buzz/` | static `F:\apps\agent-portal\ui` + API `:4080`; `/auth` → `:5910` |
| `http://103.118.183.185:4081/` | PREPROD public-IP escape hatch (NGINX `:4081` → API `:4080` + `/auth` → `:5910`); prefer staging hostname when reachable |
| `https://agent-portal.delena.buzz/` | static `G:\apps\agent-portal\ui` + API `:5080`; `/auth` → `:5910` |
| `https://hdrive.delena.buzz/` | `127.0.0.1:5010` (H: file expose) |
| `https://css.delena.buzz/` | `127.0.0.1:5900` |
| `https://home-staging.delena.buzz/` | `127.0.0.1:4320` (ProdDeck **0.6.1**) |
| `https://home.delena.buzz/` | `127.0.0.1:5320` (ProdDeck **0.6.1**) |
| `https://agentverse-staging.delena.buzz/` | `127.0.0.1:4310` (classic) |
| `https://agentverse.delena.buzz/` | `127.0.0.1:5310` (classic) |
| `https://agentverse-v2-staging.delena.buzz/` | `127.0.0.1:4311` (stable-v2 0.4.0) |
| `https://agentverse-v2.delena.buzz/` | `127.0.0.1:5311` (stable-v2 0.4.0) |
| `https://agentverse-upgrade-staging.delena.buzz/` | `127.0.0.1:4312` (upgradation 0.3.0) |
| `https://agentverse-upgrade.delena.buzz/` | `127.0.0.1:5312` (upgradation 0.3.0) |
| `https://library-dev.delena.buzz/` | `127.0.0.1:3330` (Library DEV) |
| `https://library-staging.delena.buzz/` | `127.0.0.1:4330` (Library PREPROD — app TBD) |
| `https://library.delena.buzz/` | `127.0.0.1:5330` (Library PROD — app TBD) |

Future app subdomains: see [CLOUDFLARE-DNS-PROXY.md](CLOUDFLARE-DNS-PROXY.md).

## Machine Gateway (Host Consciousness API) — edge contract

| Decision | Value |
|----------|-------|
| New TCP port? | **No for Machine Gateway itself** — reuses agent-portal `:8080` / `:4080` / `:5080`. PREPROD public-IP sandbox edge is separate NGINX `:4081` (see PORT-REGISTRY). |
| New Cloudflare DNS / subdomain? | **No** — paths under existing hosts (`/api/machine/*`) |
| New NGINX server block? | **No** — existing `/api` proxy is enough |
| New CSS `clientId`? | **No** — `clientId=agent-portal` |
| Public DEV URLs | `https://delena.buzz/api/machine/context`, `…/chat` |
| Idea / collab SoT | `E:\MyWorkspace\machine-gateway` · [MACHINE-GATEWAY.md](MACHINE-GATEWAY.md) |

Do **not** create `machine-gateway.delena.buzz` unless a future wave claims a dedicated port + CF upsert + NGINX (Ops-owned).

## Future: dedicated `port_lease` table

Interim: portal API `/api/platform/ports*` (+ doc registry above). Machine SoT: `E:\MyAgent\workflow\ports\REGISTRY.md`.

```sql
-- Planned control-plane table (beyond portal app schemas)
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
