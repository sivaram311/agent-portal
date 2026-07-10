# Operations

## Docker hybrid deploy (recommended on this Windows host)

**Requires [Docker Desktop](https://www.docker.com/products/docker-desktop/) for Windows** (`docker` on PATH).

Docker runs **Postgres + CSS + static frontend**. The portal **backend stays on the host** so Windows `agent` / `agy` CLIs keep working (Linux containers cannot run those `.exe` binaries).

> **Windows Server + Docker CE note:** If `docker info` shows `OSType=windows`, Linux compose images will not run. Use the host stack instead:
>
> ```powershell
> cd E:\MyWorkspace\agent-portal
> .\scripts\run-host-stack.ps1
> ```
>
> See also root `docker-startup-guide.md` (CE service auto-start) and `SESSION-RESTORE.md`.

```powershell
cd E:\MyWorkspace\agent-portal
copy .env.docker.example .env   # once — set PUBLIC_HOST to your LAN/public IP
.\scripts\run-backend-docker-deps.ps1
```

Or step by step:

```powershell
cd E:\MyWorkspace\agent-portal
copy .env.docker.example .env
docker compose up -d --build postgres css frontend
# then host backend:
cd backend
$env:CSS_ENABLED="true"
$env:CSS_AUTH_URL="http://<PUBLIC_HOST>:9000"
$env:CSS_JWKS_URI="http://localhost:9000/.well-known/jwks.json"
$env:CSS_ISSUER="http://localhost:9000"
$env:APP_CORS_ORIGINS="http://<PUBLIC_HOST>:4200,http://localhost:4200"
$env:SPRING_PROFILES_ACTIVE="postgres"
.\mvnw.cmd -DskipTests package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

| Service | URL |
|---------|-----|
| UI | `http://<PUBLIC_HOST>:4200` or [https://delena.buzz/](https://delena.buzz/) via Cloudflare → NGINX |
| API | `http://<PUBLIC_HOST>:8080` (also `https://delena.buzz/api/` via NGINX) |
| CSS | `http://<PUBLIC_HOST>:9000` (also `https://delena.buzz/auth/` via NGINX) |

### Cloudflare + NGINX (delena.buzz)

Full runbook: [DELENA-PROXY.md](DELENA-PROXY.md). Source configs: `E:\Source\Deployment\`.

| Check | Expect |
|-------|--------|
| nginx running | `Get-Process nginx` / `E:\Source\Deployment\scripts\status-nginx.ps1` |
| Firewall | Inbound **NGINX HTTP 80** allow (Cloudflare must reach origin) |
| SSL/TLS mode | **Flexible** until origin has real TLS on 443 |
| `CSS_AUTH_URL` | `https://delena.buzz` when users open the site over HTTPS |
| `APP_CORS_ORIGINS` | Include `https://delena.buzz` and `https://www.delena.buzz` |
| CSS CORS | `https://delena.buzz` in CSS `allowed-origin-patterns` + CSS JAR restarted |

Mixed content (HTTPS page → HTTP `/auth/login`) is blocked by browsers; the portal client matches auth calls to the page origin/protocol.

If login returns **403 Invalid CORS request**, restart CSS after ensuring `https://delena.buzz` is in `centralized-security-system` `css.cors.allowed-origin-patterns`.

**Optional API-only container** (no Cursor/Agy on Windows):

```powershell
docker compose --profile container-api up -d --build
```

Stop Docker pieces: `docker compose down` (add `-v` only if you intend to wipe Postgres/CSS H2 volumes).

This is still **not** full production: no TLS, seeded CSS users (`admin`/`admin123`), ports published on the host.

## Postgres profile

```powershell
cd E:\MyWorkspace\agent-portal
docker compose up -d postgres
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Defaults in `application-postgres.properties`: DB `agentportal`, user/password `agent` (change for real deploys). Override with `POSTGRES_PASSWORD` / `SPRING_DATASOURCE_PASSWORD`.

### Backup / restore

**H2 (default local):** stop the backend, copy `backend/data/agent-portal.mv.db` (and `.trace.db` if present).

**Postgres:**

```powershell
docker compose exec postgres pg_dump -U agent agentportal > backup.sql
docker compose exec -T postgres psql -U agent agentportal < backup.sql
```

**CSS (Docker file H2 volume `css-data`):** survives `docker compose restart`; wiped by `docker compose down -v`.

## API key (optional)

Set `AGENT_PORTAL_API_KEY` or `app.security.api-key`. Clients must send `X-API-Key`. `/api/health` and `/api/auth/config` stay open.

Frontend: store key in `localStorage.agentPortalApiKey` (interceptor reads it when present). Prefer CSS JWT for multi-app SSO.

## Centralized Security (CSS)

Agent Portal is a CSS **resource server** (`clientId: agent-portal`).

1. Run CSS on `:9000` (Docker service `css`, or `mvn spring-boot:run` in the CSS repo)
2. Enable portal auth: `CSS_ENABLED=true` (or `css.enabled=true` / `application-prod.properties`)
3. Open the portal — login overlay posts to `POST {css.auth-url}/auth/login` with `clientId=agent-portal`
4. API calls send `Authorization: Bearer <accessToken>`; SockJS passes `access_token` on `/ws/**`
5. Sessions are owned by the JWT subject (`ownerUsername`); admins can list all
6. STOMP subscriptions to `/topic/sessions/{id}` are ACL-checked when CSS is on

Dev users (seeded in CSS): `admin` / `admin123`, `demo` / `demo123`.

JWKS: `http://localhost:9000/.well-known/jwks.json` (from the host backend). Browser login uses `CSS_AUTH_URL` (public host).

Reusable starter: portal depends on `com.css:css-spring-boot-starter` (`css.resource-server.*` mirrors `css.*`). Keep `css.auth-url` for the login overlay.

Install the starter once locally (or CI will clone it):

```powershell
cd E:\MyWorkspace\centralized-security-system\clients\spring-boot-starter
mvn -q install
```

## Host stack script

`scripts/run-host-stack.ps1` starts CSS (if needed), packages/runs the portal JAR, and starts `ng serve` on `:4200`.

Important env (from `.env` or process):

| Variable | Purpose |
|----------|---------|
| `PUBLIC_HOST` | LAN/public IP used in CORS / CSS auth URL hints |
| `AGENT_WORKSPACE_ROOT` | Absolute workspace sandbox (script sets `…\agent-portal\workspaces`) |
| `AGENT_DEFAULT_AUTO_APPROVE` | Wire into `agent.default-auto-approve` for Cursor permissions |
| `CURSOR_API_KEY` | Cursor ACP auth |
| `CSS_ENABLED` | Enable JWT resource-server mode |
| `CLOUDFLARE_API_TOKEN` | Cloudflare API token (Zone Edit: DNS read/write + zone read) |
| `CLOUDFLARE_ZONE_ID` | Zone ID for DNS / tunnel APIs |
| `CLOUDFLARE_ZONE_NAME` | Zone hostname (e.g. `delena.buzz`) |
| `CLOUDFLARE_ACCOUNT_ID` | Cloudflare account ID |

When restarting only the API, stop the Java process listening on **8080** (or matching `backend-0.0.1-SNAPSHOT.jar`). Do **not** `Stop-Process` by broad name match on `cursor` / `node` / `agent` — those include the Cursor IDE agent and will kill your editing session.

## Cloudflare DNS / zone

Store credentials in gitignored `.env` (see `.env.docker.example` placeholders). Never commit the token.

| Variable | Example / notes |
|----------|-----------------|
| `CLOUDFLARE_API_TOKEN` | Token with `#dns_records:edit`, `#dns_records:read`, `#zone:read` |
| `CLOUDFLARE_ZONE_ID` | From `GET /zones` (e.g. `delena.buzz`) |
| `CLOUDFLARE_ZONE_NAME` | Apex domain name |
| `CLOUDFLARE_ACCOUNT_ID` | Account owning the zone |

Verify the token and list zones:

```powershell
# Load from .env or set explicitly
$token = $env:CLOUDFLARE_API_TOKEN
curl.exe -sS -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  "https://api.cloudflare.com/client/v4/zones" | ConvertFrom-Json | Select-Object -ExpandProperty result
```

List DNS records for the configured zone:

```powershell
$zoneId = $env:CLOUDFLARE_ZONE_ID
curl.exe -sS -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records"
```

Use these for TLS / tunnel cutover (e.g. pointing `delena.buzz` at the host) — keep secrets out of git and rotate the token if it was ever pasted into chat or logs.

## Workspace sandbox

- Relative `workspacePath` values resolve under `agent.workspace.root` / `AGENT_WORKSPACE_ROOT`
- Absolute paths are rejected unless they stay under that root
- `..` segments are rejected; file browser skips symlinks and uses `toRealPath` checks
- If the backend is started with cwd `backend/` and no `AGENT_WORKSPACE_ROOT`, `./workspaces` resolves to `backend\workspaces` and existing sessions under `agent-portal\workspaces\…` will fail create/prompt validation

Tracked samples under `workspaces/` (see [workspaces/README.md](../workspaces/README.md)):

| Folder | Use |
|--------|-----|
| `demo` | Small sample + committed `.cursor` / `AGENTS.md` guidance materialization |
| `FileBridge` | Demo Spring Boot file manager for longer agent runs |

Runtime baselines (`.agent-portal/`), `data/`, and `logs/` stay gitignored.

## Mobile QA

Realme P2 Pro checklist, audit screenshots, and Playwright commands: [MOBILE-QA.md](MOBILE-QA.md).

## Cursor ACP troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Prompt 500 / hang after resume | Stale `cursorSessionId`; `session/load` wedges stdio | Fixed in `AgentBridge` (15s timeout → close → `session/new`). Rebuild/restart backend. |
| `workspacePath must stay under …\backend\workspaces` | Missing `AGENT_WORKSPACE_ROOT` | Set to `E:\MyWorkspace\agent-portal\workspaces` (or use `run-host-stack.ps1`) |
| Permissions stuck | Auto-approve off + pending dialog | Set `AGENT_DEFAULT_AUTO_APPROVE=true` or decide in UI |
| Orphan `cursor-agent` after backend kill | ACP child left behind | Kill only the child whose parent was the dead portal Java PID — never mass-kill by process name |

## Sessions API extras

- `POST /api/sessions/{id}/unarchive` — restore an `ARCHIVED` session to `IDLE`
- List includes archived sessions so the UI filter can show them

## Rules & Skills (guidance)

Per-user library + per-session overrides.

| Scope | Where | Behavior |
|-------|--------|----------|
| Global | Top-bar **Rules** → Rules & Skills sheet | CRUD packs; **Default** toggle = enabled for new sessions |
| Session | **Guidance** tab | Checklist of library packs + optional session-only note; **Effective** chips |

**Precedence:** session checklist wins. Create dialog option **Use my Rules & Skills defaults** (on by default) copies enabled packs onto the new session.

**Delivery**

- **Cursor:** writes managed files under the session workspace:
  - `.cursor/rules/<slug>.mdc`
  - `.cursor/skills/<slug>/SKILL.md`
  - `AGENTS.md` index  
  Files include `<!-- agent-portal-managed -->` and are rewritten on guidance save / each prompt.
- **Antigravity (and Cursor fallback):** compact instruction **prefix** prepended to the agent prompt (rules full text + skill summaries). Chat history stores only the user’s raw message.

APIs:

- `GET/POST/PATCH/DELETE /api/guidance/packs`
- `GET/PUT /api/guidance/defaults`
- `GET /api/guidance/templates` + `POST /api/guidance/templates/install`
- `GET/PUT /api/sessions/{id}/guidance`

## Sharing

When CSS is enabled, owners can `POST /api/sessions/{id}/collaborators` with `{ "username": "demo" }`. Collaborators can list/open/subscribe; only owners manage the share list. UI share bar disables Share while empty/busy.

## Production checklist

- [ ] TLS termination (Caddy/nginx) in front of `:4200` / `:8080` / `:9000`
- [ ] `spring.profiles.active=prod` (or postgres + prod)
- [ ] `CSS_ENABLED=true` and CSS reachable with stable RSA keys
- [ ] `APP_CORS_ORIGINS` = concrete origins only (no `http://*:4200`)
- [ ] `agent.antigravity.skip-permissions=false`
- [ ] `spring.h2.console.enabled=false`
- [ ] Secrets only via env (`CURSOR_API_KEY`, `CLOUDFLARE_API_TOKEN`, DB password, never commit)
- [ ] Cloudflare zone vars set when using DNS/tunnel (`CLOUDFLARE_ZONE_ID`, `CLOUDFLARE_ZONE_NAME`, `CLOUDFLARE_ACCOUNT_ID`)
- [ ] Rate limit tuned (`app.rate-limit.per-minute`)
- [ ] Confirm `/api/health` capabilities badges match expected matrix
- [ ] Backup schedule for Postgres (or H2 file copy)

## Audit

`GET /api/audit?limit=50` — own events for normal users; all events for `ROLE_ADMIN`.  
Optional `?sessionId=` filter. UI: session **Activity** tab.

## Changes / history

- `GET /api/sessions/{id}/changes` — git porcelain when `.git` exists, else snapshot vs last prompt baseline
- `GET /api/sessions/{id}/changes/diff?path=` — unified diff for text files
- `POST /api/sessions/{id}/changes/accept` `{ "path" }` — keep current file
- `POST /api/sessions/{id}/changes/reject` `{ "path" }` — restore from `.agent-portal/baseline/...` or `git checkout`
- `GET /api/sessions/{id}/events` — persisted agent events for History tab

## Presets

`GET /api/presets` (public) returns starter templates used by the create dialog.

## Webhooks & quotas

```properties
app.webhooks.url=https://example.com/hooks/agent-portal
agent.workspace.quota-bytes-per-user=2147483648
agent.cursor.model=
agent.antigravity.model=
```

Webhooks fire on `run_completed`, `run_failed`, `input_required`, `run_cancelled`.
