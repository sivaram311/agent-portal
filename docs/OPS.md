# Operations

## Deployed environments (2026-07-12)

Machine standing orders: `E:\MyAgent\workflow\CONSCIOUS.md` (drives, ports, DB schemas, CSS, promote evidence).

| Env | Drive | API | Public URL | Spring profile | DB |
|-----|-------|-----|------------|----------------|-----|
| DEV | `E:\MyWorkspace\agent-portal` | `:8080` (+ UI `:4200`) | https://delena.buzz | default / H2 or `postgres` | local H2 or legacy |
| PREPROD | `F:\apps\agent-portal` | `:4080` | https://agent-portal-staging.delena.buzz | `preprod` | `app_agent_portal`.`preprod` |
| PROD | `G:\apps\agent-portal` | `:5080` | https://agent-portal.delena.buzz | `prod` | `app_agent_portal`.`prod` |

| Piece | Location |
|-------|----------|
| Release package | **0.1.10** · `H:\releases\agent-portal-0.1.10\` (hybrid OAuth/PKCE + password) · F/G may still show 0.1.9 until promote |
| Promote evidence | `H:\releases\agent-portal-0.1.10\evidence\` (oauth) · prior Wave 3 IdP: `0.1.9` |
| Consumed by | ProdDeck ≥ **0.6.2** (`OS_EVENTS_FORWARD`) · AV classic ≥ **0.3.16** sessions |
| Start script | `F:\` / `G:\apps\agent-portal\start.ps1` |
| Nginx confs | `E:\Source\Deployment\conf\apps\agent-portal*.delena.buzz.conf` |
| Machine port registry | `E:\MyAgent\workflow\ports\REGISTRY.md` (source of truth for 4080/5080) |

**UI pack rule:** Angular `:application` output is `frontend/dist/frontend/browser/`. Release `ui/` must be a **flat** copy of `browser/*` (`scripts/pack-release-ui.ps1`). Copying `dist/frontend/*` nests `browser/` and nginx returns **403** on `/` (`directory index forbidden` / `index.html` redirect cycle). Always smoke public `GET /` plus hashed JS/CSS from `index.html`, not only `/api/health`.

### Auth (DEV + PREPROD + PROD) — css-next (Wave 3 + OAuth hybrid)

Portal pins **css-next** (classic `:5900` / `css.delena.buzz` left for other apps). **Hybrid** = password form + CSS SSO (OAuth/PKCE).

| Setting | Value |
|---------|--------|
| IdP | `https://css-next.delena.buzz` / local `:5910` |
| `clientId` | `agent-portal` |
| Auth mode | `CSS_AUTH_MODE=hybrid` (or `password` / `oauth`) |
| Password lane | Same-origin `POST /auth/login` (nginx → `:5910`); empty `CSS_AUTH_URL` is OK |
| SSO lane | Browser → `{CSS_ISSUER}/oauth/authorize` → `/oauth/login` → code → Portal BFF `POST /api/auth/oauth/token` → issuer (PKCE S256) |
| OAuth callback | `{origin}/oauth/callback` — **not** under `/auth/` (nginx proxies `/auth/` to IdP) |
| JWKS | `http://127.0.0.1:5910/.well-known/jwks.json` |
| Issuer | `https://css-next.delena.buzz` |

| Host | nginx `/auth` upstream |
|------|------------------------|
| `delena.buzz` | `:5910` (strips `Origin` — apex not matched by css-next `https://*.delena.buzz` CORS) |
| `agent-portal-staging.delena.buzz` | `:5910` |
| `agent-portal.delena.buzz` | `:5910` |

Redirect allow-list (css-next): `http(s)://localhost|127.0.0.1…` and `https://delena.buzz` / `https://*.delena.buzz` — covers Portal callbacks. No css-next config change required for those hosts.

Unauthenticated `/api/**` → **403** is expected. Use a css-next JWT (`Authorization: Bearer …`).

Admin password: `CSS_ADMIN_PASSWORD` in `G:\apps\css-next\.env` (prod schema) — never commit it. README `admin`/`admin123` is DEV classic seed only.

### ProdDeck OS events (DEV)

`POST /api/os-events` accepts ProdDeck OS event envelopes (`permitAll`). Audits as `os.event.<type>`; returns `{ "ok": true }`. Contract: ProdDeck `docs/os/portal-events.md`. Do **not** enable on F:/G: cutovers without an explicit promote.

**F/G cutover:** Live as Portal **0.1.9** on `:4080` / `:5080` (2026-07-15 Wave 3 css-next IdP). ProdDeck F/G sets `OS_EVENTS_FORWARD=1` + matching `PLATFORM_APPS_URL`. Scaffold: `H:\releases\proddeck-0.6.1\evidence\portal-os-events-cutover-scaffold.md` · release `H:\releases\agent-portal-0.1.8`.

### Postgres text columns (do not use `@Lob` / CLOB)

On PostgreSQL, JPA `@Lob` / `columnDefinition = "CLOB"` maps to large objects (`oid`) and causes:

- `ERROR: type "clob" does not exist` (DDL skip → missing tables)
- `Unable to access lob stream` / `Large Objects may not be used in auto-commit mode`

**Rule:** map long strings with `@JdbcTypeCode(SqlTypes.LONGVARCHAR)` + `columnDefinition = "TEXT"`. Evidence: `H:\releases\agent-portal-0.1.0\evidence\jdbc-fix.md`, `e2e-sanity-prod.md`.

### E2E sanity (API)

After deploy, with a CSS token against `:5080` (or Host `agent-portal.delena.buzz`):

1. `GET /api/health`, `GET /api/auth/config`
2. Login → `GET /api/sessions` → `POST /api/sessions`
3. Session messages / events / `GET /api/guidance/packs`

Promote gates: `E:\MyAgent\workflow\promote\` (`promote-em` + evidence packs).

### Session UI tabs

| Tab | Purpose |
|-----|---------|
| Transcript | Chat (markdown) |
| Logs | Subagents + tool runs + **collapsible** embedded terminal (same `terminal_chunk` stream as Console) |
| **Console** | Plain terminal scrollback — same live agent output you would see in PowerShell (`terminal_chunk` WS + event replay) |
| Code / Preview | Workspace files |
| Changes | Diff accept/reject |
| History | Merged timeline, **newest first**; event payloads parsed to human lines (Cursor + Antigravity); protocol noise collapsed by default; scroll ↑/↓ FABs |
| Guidance | Rules/skills |
| Activity | User audit |

### Session list vs detail

If search/filter hides the currently open session, the detail pane clears (avoids master–detail desync).

### Mobile

See [MOBILE-QA.md](MOBILE-QA.md). On phones: share collapsed, Preview folded into Code, auto session titles, bottom-sheet create dialog.

### Tool / subagent names

Cursor ACP often sends a useful title on the first `tool_call`, then later updates with empty fields that would overwrite the DB name to `tool`. AgentBridge keeps the best label, prefers `cursor/task` descriptions for running subagents, and the UI re-derives display names from event history when opening a session.

**Logs tab:** Sub-agents only appear in the Sub-agents panel (finished collapsed by default; **Show finished**). Tool runs excludes `kind=subagent` so rows are not doubled. Cancel marks open tool/subagent rows `cancelled` so they do not stick as `in_progress`. Embedded terminal is collapsed until chunks arrive (or the user expands it); full scrollback stays on **Console**. Works for both **Cursor** and **Antigravity** (`terminal_chunk` from each bridge).

**History tab:** Events are formatted via a provider-agnostic parser (no raw JSON dump for known types). List is newest-first so auto-refresh keeps the latest activity at the top; use the floating ↑/↓ buttons to jump.

**Env configs:** Keep Cursor/Antigravity and CSS settings in `application.properties` (DEV), `application-preprod.properties`, and `application-prod.properties` separately — do not copy port/DB/CORS values across profiles.

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

Defaults in `application-postgres.properties`: legacy DB `agentportal`, user/password `agent` (change for real deploys).

**PREPROD / PROD** use shared Postgres `:5432` with schema-per-env (machine policy):

| Env | Database | Schema | Role |
|-----|----------|--------|------|
| preprod | `app_agent_portal` | `preprod` | `app_agent_portal_preprod` |
| prod | `app_agent_portal` | `prod` | `app_agent_portal_prod` |

Registry: `E:\MyAgent\workflow\db\SCHEMA-REGISTRY.md`. Secrets: `E:\MyAgent\workflow\db\secrets\postgres.env` (gitignored). Profiles `application-preprod.properties` / `application-prod.properties` set JDBC URL + CSS JWKS to prod CSS.

### Backup / restore

**H2 (default local):** stop the backend, copy `backend/data/agent-portal.mv.db` (and `.trace.db` if present).

**Postgres:**

```powershell
docker compose exec postgres pg_dump -U agent agentportal > backup.sql
docker compose exec -T postgres psql -U agent agentportal < backup.sql
```

**CSS (Docker file H2 volume `css-data`):** survives `docker compose restart`; wiped by `docker compose down -v`.

## CORS (Agent API)

Default `APP_CORS_ORIGINS=*` — any browser/AI origin may call `/api/**`. **Authentication is unchanged** (CSS JWT or `X-API-Key`). Discovery: `GET /api/agent/actions` (public). Contract: [platform/AGENT-API.md](platform/AGENT-API.md) and `workspaces/agent-api/`.


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
| `AGENT_DEFAULT_AUTO_APPROVE` | Cursor ACP: `true` = **allow-always** (no per-tool clicks), including role gates. PREPROD/PROD: `true` |
| `AGENT_ANTIGRAVITY_SKIP_PERMISSIONS` | Antigravity `--dangerously-skip-permissions`. PREPROD/PROD: `true` |
| `CURSOR_AGENT_CMD` | Absolute path to `agent.cmd` (required on Windows for CreateProcess) |
| `CURSOR_API_KEY` | Cursor ACP auth |
| `CSS_ENABLED` | Enable JWT resource-server mode |
| `CLOUDFLARE_API_TOKEN` | Cloudflare API token (Zone Edit: DNS read/write + zone read) |
| `CLOUDFLARE_ZONE_ID` | Zone ID for DNS / tunnel APIs |
| `CLOUDFLARE_ZONE_NAME` | Zone hostname (e.g. `delena.buzz`) |
| `CLOUDFLARE_ACCOUNT_ID` | Cloudflare account ID |

When restarting only the API, stop the Java process listening on the **target env port** — DEV **8080**, PREPROD **4080**, PROD **5080** (or matching JAR on that drive). Do **not** `Stop-Process` by broad name match on `cursor` / `node` / `agent` — those include the Cursor IDE agent and will kill your editing session.

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
- Absolute paths outside that root are **not allowed** unless they stay under an entry in `agent.workspace.allowed-roots` / `AGENT_WORKSPACE_ALLOWED_ROOTS` (comma-separated absolute prefixes)
- `..` segments are rejected; file browser skips symlinks and uses `toRealPath` checks
- If the backend is started with cwd `backend/` and no `AGENT_WORKSPACE_ROOT`, `./workspaces` resolves to `backend\workspaces` and existing sessions under `agent-portal\workspaces\…` will fail create/prompt validation

Example (DEV — allow control-plane and Source trees outside the sandbox):

```env
AGENT_WORKSPACE_ROOT=E:\MyWorkspace\sandbox
AGENT_WORKSPACE_ALLOWED_ROOTS=E:\MyWorkspace,E:\Source,G:\apps\agent-portal\workspaces
```

PREPROD/PROD: set `AGENT_WORKSPACE_ALLOWED_ROOTS` when AgentVerse / Dispatch need absolute app trees (intentional Ops flex for work-plane sessions):

```env
AGENT_WORKSPACE_ALLOWED_ROOTS=E:\MyWorkspace,E:\Source,E:\wt,F:\apps,G:\apps
```

Sandbox root remains `AGENT_WORKSPACE_ROOT` (F/G `…\agent-portal\workspaces`). Empty allowlist = sandbox-only.

Tracked samples under `workspaces/` (see [workspaces/README.md](../workspaces/README.md)):

| Folder | Use |
|--------|-----|
| `demo` | Small sample + committed `.cursor` / `AGENTS.md` guidance materialization |
| `FileBridge` | Demo Spring Boot file manager for longer agent runs |
| `agent-api` | Workspace for external AIs calling the portal/CSS HTTP API |

Machine-wide **Future Implementation** handbook (workflow, ports, VirtualDev Co, sub-agents): [platform/README.md](platform/README.md).

Runtime baselines (`.agent-portal/`), `data/`, and `logs/` stay gitignored.

## Mobile QA

Realme P2 Pro checklist, audit screenshots, and Playwright commands: [MOBILE-QA.md](MOBILE-QA.md).

## Cursor ACP troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Prompt 500 / hang after resume | Stale `cursorSessionId`; `session/load` wedges stdio | Fixed in `AgentBridge` (15s timeout → close → `session/new`). Rebuild/restart backend. |
| `workspacePath is not allowed` / `must stay under …` | Path outside sandbox and allowlist, or missing `AGENT_WORKSPACE_ROOT` | Point at a path under the root, or add a prefix to `AGENT_WORKSPACE_ALLOWED_ROOTS`; set root via `run-host-stack.ps1` |
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
- [x] Rate limit tuned (`app.rate-limit.per-minute` — **180** on preprod/prod as of 2026-07-15 hotfix)
- [ ] Confirm `/api/health` capabilities badges match expected matrix
- [ ] Backup schedule for Postgres (or H2 file copy)

## Rate limit (loopback / AgentVerse proxy)

`RateLimitFilter` keys by **authenticated principal + client IP** when possible. When the direct peer is loopback/private (Next.js `/api/portal` proxy on `:4312`/`:5312` → Portal `:4080`/`:5080`), honor `X-Forwarded-For` / `X-Real-IP` / `CF-Connecting-IP` so all AV users are not merged into one `127.0.0.1` bucket. CSS JWT filter runs **before** rate limit so `sub` is available.

Live preprod/prod: `app.rate-limit.per-minute=180`. Hotfix JAR swapped to F/G `agent-portal.jar` 2026-07-15 (VERSION **0.1.9** IdP env + nginx; jar body still 0.1.8 artifact until next bake).

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
