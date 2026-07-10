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

Set `AGENT_PORTAL_API_KEY` or `app.security.api-key`. Clients must send `X-API-Key` (or `Authorization: Bearer …`). `/api/health` stays open.

Frontend: store key in `localStorage.agentPortalApiKey` (interceptor reads it when present).

## Production notes

- Put TLS termination (Caddy/nginx) in front of `:4200` / `:8080`
- Replace `http://*:4200` CORS with concrete origins
- Prefer `agent.antigravity.skip-permissions=false` when interactive permissions exist
- Disable H2 console (`spring.h2.console.enabled=false`) — already off on postgres profile
