# Agent Portal

Spring Boot 3.5 + Angular 19 web portal for autonomous AI agent sessions with **dual providers**:

- **Cursor CLI** (`agent acp`) — native realtime ACP streaming + mid-turn permissions
- **Antigravity CLI** (`agy`) — near-realtime streaming via brain artifacts (`messages/`, `tasks/`, `transcript*.jsonl`)

The UI uses a **dark navy/teal responsive redesign** (design tokens in `frontend/src/app/theme/tokens.scss`: Inter + IBM Plex Mono, touch-friendly controls, mobile drawer + FAB).

## Features

- Multi-session chat workspace with search and status filters (all / active / failed / archived)
- Provider choice at session create (Cursor or Antigravity; cannot switch later)
- Realtime streaming over WebSocket (STOMP/SockJS)
- Persistent history (H2 file DB by default; PostgreSQL profile available)
- Markdown-rendered assistant replies (marked + DOMPurify)
- Session detail tabs: **Transcript** | **Logs** | **Code** | **Preview** (Code and Preview show empty-state placeholders until a file/preview API exists)
- Live task / terminal panel from agent tool events (Logs tab)
- Permission and plan approval dialogs (**Cursor only**)
- Cancel in-flight runs and archive sessions
- Responsive layout: desktop sidebar, mobile session list + drawer navigation + bottom FAB

## Prerequisites

- Java 21+
- Maven 3.9+ (or use included `mvnw.cmd`)
- Node.js 22+ (Angular 19 frontend)
- Cursor CLI (`agent`) installed and authenticated (for Cursor sessions)
- Antigravity CLI (`agy`) installed and authenticated (for Antigravity sessions)
- `CURSOR_API_KEY` recommended for Cursor

## Project layout

```
agent-portal/
  backend/                        Spring Boot 3.5 API + Cursor ACP + Antigravity bridges
  frontend/                       Angular 19 UI
  e2e/                            Playwright mobile QA (Realme P2 Pro)
  workspaces/                     Default workspace root for relative session paths
  .cursor/skills/agent-portal/    Cursor skill for agents working on this repo
  docker-compose.yml              Optional PostgreSQL
  README.md
```

## Run backend

```powershell
cd backend
$env:CURSOR_API_KEY = "cursor_..."
.\mvnw.cmd spring-boot:run
```

Or package and run the JAR:

```powershell
cd backend
.\mvnw.cmd -q -DskipTests package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

API: `http://localhost:8080`  
Health: `http://localhost:8080/api/health`  
H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/agent-portal`)

### PostgreSQL (optional)

```powershell
cd ..
docker compose up -d
.\mvnw.cmd -f backend\pom.xml spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Run frontend

```powershell
cd frontend
npm start
```

`npm start` runs `ng serve --host 0.0.0.0 --port 4200`, so the UI is reachable on your LAN/public IP (not only localhost).

UI: `http://localhost:4200` (or `http://<your-ip>:4200`)

### API / WebSocket host

The frontend derives the backend origin from the page URL (`frontend/src/app/services/backend-url.ts`): same hostname as the browser, port **8080** for REST and `/ws`. Opening the app via a LAN or public IP automatically targets that host for API and STOMP/SockJS — no hardcoded localhost in the client.

### CORS and firewall

Backend CORS (`app.cors.allowed-origins`):

```
http://localhost:4200,http://127.0.0.1:4200,http://*:4200
```

For access from other machines, allow inbound TCP **4200** (Angular dev server) and **8080** (Spring Boot) in the host firewall.

## Typical flow

1. Open the UI and create a session (desktop sidebar or mobile FAB).
2. Choose **Cursor** or **Antigravity** before creating.
3. Point the session at workspace `demo` or an absolute path under `workspaces/`.
4. Send a prompt; assistant text streams into **Transcript** (markdown); tool output appears under **Logs**.
5. For Cursor: approve/reject permissions when prompted.
6. For Antigravity: tools auto-run when `agent.antigravity.skip-permissions=true`.
7. Use **Cancel** to stop an in-flight run; **Archive** to hide a session from the active list.
8. Refresh the page — history reloads from the database.

## Provider comparison

| Capability | Cursor | Antigravity |
|------------|--------|-------------|
| Text / response streaming | Native ACP `session/update` | Watch brain `messages/` + `transcript*.jsonl` + `.agent-portal-reply.txt` + captured stdout |
| Tool / task events | ACP tool_call updates | Brain `messages/` (finished tasks) + `tasks/*.log` |
| Mid-turn permission UI | Yes | No (uses `--dangerously-skip-permissions`) |
| Cancel mid-run | `session/cancel` | Kill `agy` process |
| Resume conversation | ACP session id | `--conversation <id>` stored on session |

Antigravity streaming is **near-realtime** (sub-second polling of growing brain artifacts, reply file, CLI log, and captured stdout), not token-perfect ACP.

### Antigravity on Windows (launcher)

- The prompt must follow `-p` immediately (otherwise `agy` can hang or mis-parse).
- **Do not** launch via PowerShell `Start-Process -ArgumentList` — it joins arguments with spaces and drops quoting, so multi-word prompts are truncated and `agy` replies that the message was cut off.
- **Current approach:** a generated PowerShell script uses the call operator with splatting: `& agy.exe @agyArgs`, captures stdout, and writes UTF-8 via `[System.IO.File]::WriteAllText` (avoids UTF-16 redirect corruption).
- Direct Java `ProcessBuilder` pipes often hang `agy` in print mode on Windows; the script wrapper avoids that.
- Near-realtime updates come from brain artifacts plus `.agent-portal-reply.txt` and the stdout capture file; portal runs use skip-permissions for unattended execution.
- On some Windows installs `transcript.jsonl` stays empty due to an upstream path bug (`/Users/...` vs `%USERPROFILE%`); the portal therefore also watches brain `messages/` and task logs when present.
- If Google later ships `agy --acp`, the bridge can be swapped without changing the Angular event model.

## SockJS / `global is not defined`

SockJS expects Node-style `global` in the browser. The portal polyfills it in two places (both load before SockJS):

- `frontend/src/global-polyfill.ts` — listed first in `angular.json` polyfills
- Inline script in `frontend/src/index.html` (`window.global = window`)

## Configuration

`backend/src/main/resources/application.properties`:

| Key | Purpose |
|-----|---------|
| `agent.cursor.command` | Path to `agent.cmd` |
| `agent.cursor.api-key` | From `CURSOR_API_KEY` |
| `agent.antigravity.command` | Path to `agy.exe` |
| `agent.antigravity.brain-root` | Antigravity brain root (`~/.gemini/antigravity-cli`) |
| `agent.antigravity.skip-permissions` | Auto-approve tools for portal runs (default `true`) |
| `agent.antigravity.print-timeout` | `agy --print-timeout` (default `5m`) |
| `agent.antigravity.poll-interval-ms` | Brain artifact poll interval |
| `agent.workspace.root` | Root for relative workspace paths |
| `agent.default-auto-approve` | Cursor auto-allow tool permissions (default `false`) |
| `app.cors.allowed-origins` | Allowed Angular origins |

## Antigravity auth

Run `agy` once interactively to sign in, or set the env vars your Antigravity install expects (for example `ANTIGRAVITY_API_KEY` / Google AI key, depending on version). The portal does not open a browser login for you.

## E2E mobile QA

Playwright tests emulate **Realme P2 Pro** (360×800 CSS @ 3× DPR). See [`e2e/README.md`](e2e/README.md) for setup, `APP_URL`, and `data-testid` hooks.

```powershell
cd e2e
npm test
# or explicitly:
npm run test:mobile
```

## Cursor skill for agents

`.cursor/skills/` contains Cursor Agent Skills for this repo:

| Skill | Focus |
|-------|--------|
| `agent-portal` | Day-to-day run/fix conventions |
| `ap-subagents` | Sub-agent panel + Abandon (P0) |
| `ap-agy-interactive` | Antigravity mid-turn input (P0) |
| `ap-code-preview` | Code/Preview tabs (P1) |
| `ap-security` | Auth + harden (P1) |
| `ap-ux-polish` | Toasts / mobile polish (P2) |
| `ap-e2e` | Tablet/desktop Playwright (P2) |
| `ap-ops` | Postgres / ops (P3) |

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for priorities.

## Local config

Copy `backend/src/main/resources/application-local.properties.example` to `application-local.properties` (gitignored) for machine-specific `agent` / `agy` paths.

## Security note

This portal can edit files and run shell commands on the host through the selected agent. Keep it local/single-user unless you add auth and sandboxing. Antigravity portal runs default to skip-permissions for unattended execution — tighten that for shared machines.
