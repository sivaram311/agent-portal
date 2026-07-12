# Agent Portal

Spring Boot 3.5 + Angular 19 web portal for autonomous AI agent sessions with **dual providers**:

- **Cursor CLI** (`agent acp`) — native realtime ACP streaming + mid-turn permissions
- **Antigravity CLI** (`agy`) — near-realtime streaming via brain artifacts (`messages/`, `tasks/`, `transcript*.jsonl`)

The UI uses a **dark navy/teal responsive redesign** (design tokens in `frontend/src/app/theme/tokens.scss`: Inter + IBM Plex Mono, touch-friendly controls, mobile drawer + FAB).

## Features

- Multi-session chat workspace with search and status filters (all / active / failed / archived), including archived sessions + **unarchive**
- Provider choice at session create (Cursor or Antigravity; cannot switch later)
- Realtime streaming over WebSocket (STOMP/SockJS) with CSS-aware WS auth
- Persistent history (H2 file DB by default; PostgreSQL profile available)
- Markdown-rendered assistant replies (marked + DOMPurify) with **message timestamps**
- Session detail tabs: **Transcript** | **Logs** | **Code** | **Preview** | **Changes** | **History** | **Guidance** | **Activity** (horizontally scrollable on phones)
- **Sub-agent / task panel** with Abandon (Cursor: child-scoped mark + suppress further tool updates; other providers may cancel the session run)
- Antigravity **soft interactive** + optional **ACP** (`agy acp` / `agy --acp`, then print-mode fallback)
- Cursor ACP **stale `session/load` recovery** — timed-out loads restart the ACP process and fall back to `session/new`
- **Change review** with **Keep** / **Restore** (git or `.agent-portal/baseline` snapshot) and **History** timeline
- **Session presets** and optional starter prompt on create
- **Collaborator sharing** when CSS is enabled (share bar with busy/disabled states)
- **Rules & Skills** — per-user library (global defaults) + per-session **Guidance** tab; materializes `.cursor/rules` / `.cursor/skills` for Cursor and a prompt prefix for Antigravity
- Workspace **file browser** with sandbox under `agent.workspace.root`
- **CSS JWT auth** via `com.css:css-spring-boot-starter` (`css.resource-server.*`) + optional API-key fallback
- Monaco Code tab (vendored) + sandboxed HTML Preview
- Audit API + webhooks + optional per-user workspace quota
- Capability badges in the top bar (desktop/tablet)
- Live task / terminal panel from agent tool events (Logs tab)
- Permission and plan approval dialogs (**Cursor**; also Antigravity when ACP mode works)
- Cancel in-flight runs and archive / restore sessions
- Mobile-first polish (~360×780): FAB-only create, truncated paths with long-press copy, clearer empty states, FAB-safe list padding
- Playwright e2e: Realme P2 Pro, tablet 1024, desktop 1440, auth shell

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
  e2e/mobile-audit/               Phone screenshot fixtures + checklist
  docs/                           OPS, delena proxy, mobile QA, roadmap
  scripts/                        Host stack + Docker-deps helpers
  workspaces/                     Default workspace root (demo + FileBridge samples)
  .cursor/skills/agent-portal/    Cursor skill for agents working on this repo
  docker-compose.yml              Postgres + optional CSS/frontend containers
  README.md
```

Mobile checklist and audit frames: [docs/MOBILE-QA.md](docs/MOBILE-QA.md). Sample workspaces: [workspaces/README.md](workspaces/README.md).

**Platform Future Implementation** (machine workflow, ports, sandbox, VirtualDev Co, Agent API): [docs/platform/README.md](docs/platform/README.md).

## Live environments

| Env | URL | API port |
|-----|-----|----------|
| DEV (workspace) | https://delena.buzz | `:8080` (+ UI `:4200`) |
| PREPROD | https://agent-portal-staging.delena.buzz | `:4080` on `F:\apps\agent-portal` |
| PROD | https://agent-portal.delena.buzz | `:5080` on `G:\apps\agent-portal` |

PREPROD/PROD auth → **prod CSS** (`https://css.delena.buzz`, `clientId=agent-portal`). Release + evidence: `H:\releases\agent-portal-0.1.3\`. Ops detail: [docs/OPS.md](docs/OPS.md#deployed-environments-2026-07-11). System E2E loop: [docs/platform/SYSTEM-E2E-LOOP.md](docs/platform/SYSTEM-E2E-LOOP.md). Session **Console** tab streams plain agent terminal output. History collapses noisy ACP deltas by default; session filter clears detail when the open session is hidden; tool/subagent labels prefer ACP titles / task descriptions over generic `tool`.

## Quick start (Windows host stack)

Runs CSS + portal backend + Angular on the host (recommended when Docker is Windows containers / CE):

```powershell
cd E:\MyWorkspace\agent-portal
# optional: copy .env.docker.example .env  and set PUBLIC_HOST / CURSOR_API_KEY / CLOUDFLARE_*
.\scripts\run-host-stack.ps1
```

Sets `AGENT_WORKSPACE_ROOT` to `.\workspaces` and honors `AGENT_DEFAULT_AUTO_APPROVE` from `.env`. Cloudflare zone credentials (`CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ZONE_ID`, …) live in `.env` for DNS/tunnel ops — see [docs/OPS.md](docs/OPS.md#cloudflare-dns--zone).

## Docker hybrid (Windows host)

Postgres + CSS + static UI in Docker; portal backend on the host (so `agent` / `agy` work):

```powershell
copy .env.docker.example .env
.\scripts\run-backend-docker-deps.ps1
```

Details: [docs/OPS.md](docs/OPS.md).

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
Auth config: `http://localhost:8080/api/auth/config`  
H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/agent-portal`)

### CSS auth (optional locally, on in prod)

Install the resource-server starter once (local Maven repo), then run CSS + portal:

```powershell
# One-time: css-spring-boot-starter
cd E:\MyWorkspace\centralized-security-system\clients\spring-boot-starter
mvn -q install

# Terminal A — Centralized Security
cd E:\MyWorkspace\centralized-security-system
mvn spring-boot:run

# Terminal B — Agent Portal with CSS
cd E:\MyWorkspace\agent-portal\backend
$env:CSS_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

Login overlay uses `clientId=agent-portal` against CSS (`admin`/`admin123`). JWT validation uses `css.resource-server.*` (see `docs/OPS.md`).

### PostgreSQL (optional)

```powershell
cd E:\MyWorkspace\agent-portal
docker compose up -d postgres
.\mvnw.cmd -f backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

## Run frontend

```powershell
cd frontend
npm start
```

`npm start` runs `ng serve --host 0.0.0.0 --port 4200` (**development mode** — the console banner is expected). For a production build without that banner, use `ng build` and serve `dist/` behind NGINX.

UI: `http://localhost:4200` (or `https://delena.buzz` via Cloudflare → NGINX — see [docs/OPS.md](docs/OPS.md) and `E:\Source\Deployment\nginx-setup.md`).

### API / WebSocket host

The frontend derives the backend origin from the page URL (`frontend/src/app/services/backend-url.ts`): same hostname as the browser, port **8080** for REST and `/ws`. Opening the app via a LAN or public IP automatically targets that host for API and STOMP/SockJS — no hardcoded localhost in the client.

### CORS and firewall

Backend CORS (`app.cors.allowed-origins`):

```
http://localhost:4200,http://127.0.0.1:4200,http://*:4200
```

For access from other machines, allow inbound TCP **4200** (Angular dev server) and **8080** (Spring Boot) in the host firewall.

## Typical flow

1. Open the UI and create a session (desktop **New session**, or mobile **FAB** only).
2. Choose **Cursor** or **Antigravity** before creating.
3. Point the session at workspace `demo` or a path under `AGENT_WORKSPACE_ROOT` / `workspaces/`.
4. Send a prompt; assistant text streams into **Transcript** (markdown + timestamps); tool output appears under **Logs**.
5. For Cursor: approve/reject permissions when prompted (or set `AGENT_DEFAULT_AUTO_APPROVE=true`).
6. For Antigravity: tools auto-run when `agent.antigravity.skip-permissions=true`.
7. Use **Cancel** to stop an in-flight run; **Archive** / **Restore** from the session list filters.
8. Long-press a truncated workspace path on mobile to copy it.
9. Configure **Rules & Skills** from the top-bar **Rules** button; tweak per session on the **Guidance** tab.
10. Refresh the page — history reloads from the database.

## Provider comparison

| Capability | Cursor | Antigravity |
|------------|--------|-------------|
| Text / response streaming | Native ACP `session/update` | Watch brain `messages/` + `transcript*.jsonl` + `.agent-portal-reply.txt` + captured stdout |
| Tool / task events | ACP tool_call updates | Brain `messages/` (finished tasks) + `tasks/*.log` |
| Mid-turn permission UI | Yes | No (uses `--dangerously-skip-permissions`) |
| Cancel mid-run | `session/cancel` | Kill `agy` process |
| Resume conversation | ACP session id (stale ids → short `session/load` timeout, then fresh `session/new`) | `--conversation <id>` stored on session |

Antigravity streaming is **near-realtime** (sub-second polling of growing brain artifacts, reply file, CLI log, and captured stdout), not token-perfect ACP.

### Cursor ACP resume notes

- Portal stores `cursorSessionId` and tries `session/load` on the next prompt/resume.
- Stale ids often hang; `AgentBridge` uses a **15s** load timeout, closes the wedged stdio client, respawns ACP, then calls `session/new`.
- Prefer restarting only the **portal backend Java process** when debugging — do not broad-kill `cursor` / `node` / `agent` processes (that can take down the IDE agent session).

### Antigravity on Windows (launcher)

- The prompt must follow `-p` immediately (otherwise `agy` can hang or mis-parse).
- **Do not** launch via PowerShell `Start-Process -ArgumentList` — it joins arguments with spaces and drops quoting, so multi-word prompts are truncated and `agy` replies that the message was cut off.
- **Current approach:** a generated PowerShell script uses the call operator with splatting: `& agy.exe @agyArgs`, captures stdout, and writes UTF-8 via `[System.IO.File]::WriteAllText` (avoids UTF-16 redirect corruption).
- Direct Java `ProcessBuilder` pipes often hang `agy` in print mode on Windows; the script wrapper avoids that.
- Near-realtime updates come from brain artifacts plus `.agent-portal-reply.txt` and the stdout capture file; portal runs use skip-permissions for unattended execution.
- On some Windows installs `transcript.jsonl` stays empty due to an upstream path bug (`/Users/...` vs `%USERPROFILE%`); the portal therefore also watches brain `messages/` and task logs when present.
- If Google later ships `agy --acp`, the bridge can be swapped without changing the Angular event model.

## Rules & Skills

1. Sign in → top-bar **Rules** → create rules/skills (or **Install starters**).
2. Toggle **Default** on packs you want on every new session.
3. Create a session (defaults applied when “Use my Rules & Skills defaults” is checked).
4. Open the session **Guidance** tab to enable/disable packs or add a session-only note.
5. Next prompt materializes `.cursor/rules` + `.cursor/skills` (Cursor) and/or a prompt prefix (Antigravity).

Details: [docs/OPS.md](docs/OPS.md#rules--skills-guidance).

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
| `agent.workspace.root` | Root for relative workspace paths (`AGENT_WORKSPACE_ROOT`) |
| `agent.default-auto-approve` | Cursor auto-allow tool permissions (`AGENT_DEFAULT_AUTO_APPROVE`, default `false`) |
| `app.cors.allowed-origins` | Allowed Angular origins |

## Antigravity auth

Run `agy` once interactively to sign in, or set the env vars your Antigravity install expects (for example `ANTIGRAVITY_API_KEY` / Google AI key, depending on version). The portal does not open a browser login for you.

## E2E mobile QA

Playwright projects: **realme-p2-pro**, **tablet-1024**, **desktop-1440**. See [`e2e/README.md`](e2e/README.md).

```powershell
cd e2e
npm test
# or:
npm run test:mobile
```

Ops / Postgres backups: [`docs/OPS.md`](docs/OPS.md).  
delena.buzz / Cloudflare / NGINX: [`docs/DELENA-PROXY.md`](docs/DELENA-PROXY.md).  
Roadmap: [`docs/ROADMAP.md`](docs/ROADMAP.md).

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
