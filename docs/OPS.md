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
4. API calls send `Authorization: Bearer <accessToken>`; SockJS may pass `access_token` query param

Dev users (seeded in CSS): `admin` / `admin123`, `demo` / `demo123`.

JWKS: `http://localhost:9000/.well-known/jwks.json`

## Production notes

- Put TLS termination (Caddy/nginx) in front of `:4200` / `:8080`
- Replace `http://*:4200` CORS with concrete origins
- Prefer `agent.antigravity.skip-permissions=false` when interactive permissions exist
- Disable H2 console (`spring.h2.console.enabled=false`) — already off on postgres profile
- Keep `css.enabled=true` and do not ship with open `/api/**`
