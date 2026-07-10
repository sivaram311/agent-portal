# VPS FileBridge (MVP)

Self-hosted file manager used as an **Agent Portal sample workspace**. Runs as a single Spring Boot app (API + static UI under `backend/src/main/resources/static`).

## Run

```powershell
cd E:\MyWorkspace\agent-portal\workspaces\FileBridge\backend
.\mvnw.cmd -DskipTests spring-boot:run
```

Default bind: `0.0.0.0:8082`

## Login (dev defaults only)

- Username: `admin`
- Password: `admin123`

Override with `FILEBRIDGE_USER` / `FILEBRIDGE_PASSWORD` / `JWT_SECRET` before any shared deploy.

## Env

- `FILE_ROOT_PATH` — storage root (default `../data`, gitignored)
- `FILEBRIDGE_USER` / `FILEBRIDGE_PASSWORD`
- `JWT_SECRET`

## Agent Portal

Create a session with workspace path `FileBridge` (relative to `AGENT_WORKSPACE_ROOT`). Runtime baselines land in `.agent-portal/` (gitignored).
