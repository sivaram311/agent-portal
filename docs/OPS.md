# Operations

## Postgres profile

```powershell
cd E:\MyWorkspace\agent-portal
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Defaults in `application-postgres.properties`: DB `agentportal`, user/password `agent` (change for real deploys).

### Backup / restore

**H2 (default local):** stop the backend, copy `backend/data/agent-portal.mv.db` (and `.trace.db` if present).

**Postgres:**

```powershell
docker compose exec postgres pg_dump -U agent agentportal > backup.sql
docker compose exec -T postgres psql -U agent agentportal < backup.sql
```

## API key (optional)

Set `AGENT_PORTAL_API_KEY` or `app.security.api-key`. Clients must send `X-API-Key`. `/api/health` and `/api/auth/config` stay open.

Frontend: store key in `localStorage.agentPortalApiKey` (interceptor reads it when present). Prefer CSS JWT for multi-app SSO.

## Centralized Security (CSS)

Agent Portal is a CSS **resource server** (`clientId: agent-portal`).

1. Run CSS on `:9000`
2. Enable portal auth: `CSS_ENABLED=true` (or `css.enabled=true` / `application-prod.properties`)
3. Open the portal — login overlay posts to `POST {css.auth-url}/auth/login` with `clientId=agent-portal`
4. API calls send `Authorization: Bearer <accessToken>`; SockJS passes `access_token` on `/ws/**`
5. Sessions are owned by the JWT subject (`ownerUsername`); admins can list all
6. STOMP subscriptions to `/topic/sessions/{id}` are ACL-checked when CSS is on

Dev users (seeded in CSS): `admin` / `admin123`, `demo` / `demo123`.

JWKS: `http://localhost:9000/.well-known/jwks.json`

Reusable starter: portal depends on `com.css:css-spring-boot-starter` (`css.resource-server.*` mirrors `css.*`). Keep `css.auth-url` for the login overlay.

## Workspace sandbox

- Relative `workspacePath` values resolve under `agent.workspace.root`
- Absolute paths are rejected unless they stay under that root
- `..` segments are rejected; file browser skips symlinks and uses `toRealPath` checks

## Production checklist

- [ ] TLS termination (Caddy/nginx) in front of `:4200` / `:8080` / `:9000`
- [ ] `spring.profiles.active=prod` (or postgres + prod)
- [ ] `CSS_ENABLED=true` and CSS reachable with stable RSA keys
- [ ] `APP_CORS_ORIGINS` = concrete origins only (no `http://*:4200`)
- [ ] `agent.antigravity.skip-permissions=false`
- [ ] `spring.h2.console.enabled=false`
- [ ] Secrets only via env (`CURSOR_API_KEY`, DB password, never commit)
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

## Sharing

When CSS is enabled, owners can `POST /api/sessions/{id}/collaborators` with `{ "username": "demo" }`. Collaborators can list/open/subscribe; only owners manage the share list.

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
