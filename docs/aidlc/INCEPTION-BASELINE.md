# AI-DLC Inception Baseline - agent-portal

**Captured:** 2026-08-01 (as-is snapshot, not a target design)

## Purpose

Agent Portal is a Spring Boot + Angular web app for running autonomous AI agent sessions against local workspaces, with dual CLI providers (Cursor `agent acp` and Antigravity `agy`). It is the flagship hub on this machine: session UI, CSS JWT auth (`clientId=agent-portal`), Machine Gateway (`/api/machine/*`), platform APIs, and integration surfaces that other apps (ProdDeck OS events, ForgeCity rewrite, Agent API, Android client) consume.

## Tech stack

| Layer | Stack (versions as stated in-repo) |
|-------|-------------------------------------|
| Backend | Java **21**; Spring Boot **3.5.16** (`backend/pom.xml`); Maven (`mvnw`); Spring Web, Security, Data JPA, WebSocket; H2 (default) + PostgreSQL runtime; Lombok; Firebase Admin **9.4.3**; `com.css:css-spring-boot-starter` **0.1.0-SNAPSHOT** |
| Frontend | Angular **^19.2.0** / CLI **^19.2.27**; TypeScript **~5.7.2**; STOMP/SockJS; marked + DOMPurify; Monaco **^0.52.2**; `ng serve --host 0.0.0.0 --port 4200` |
| E2E | Playwright **^1.52.0** (`e2e/package.json`) — projects realme-p2-pro, tablet-1024, desktop-1440 |
| Data / compose | Default H2 file DB; optional Postgres profile; `docker-compose.yml` uses **postgres:16-alpine** (+ optional CSS/frontend containers) |
| Auth | CSS JWT resource-server (`css.client-id=agent-portal`); optional `X-API-Key`; hybrid password + OAuth/PKCE modes documented in OPS |

## Current features (as-built)

SPA (`frontend/src/app/app.routes.ts` is empty — single-root `AppComponent`):

- Multi-session workspace: create (Cursor or Antigravity), search/status filters, archive/unarchive, cancel in-flight runs
- Realtime streaming over SockJS/STOMP (`/ws`) with CSS-aware WS auth
- Session tabs: Transcript, Logs, Console, Code, Preview, Changes, History, Guidance, Activity (Preview hidden on compact/mobile strip)
- Sub-agent / task panel with Abandon; permission and plan approval dialogs (Cursor; Antigravity when ACP works)
- Change review Keep/Restore; History timeline; Rules & Skills library + per-session Guidance
- Collaborator sharing when CSS enabled; session presets; capability badges; mobile FAB create + responsive layout
- Monaco Code browser + sandboxed HTML Preview under workspace sandbox
- Apps / CSS App Home sheet; platform control APIs under `/api/platform/*` (ports, apps, tasks, org, memory, messages, pipelines, swarm, home)
- Machine Gateway: `GET /api/machine/context`, `POST /api/machine/chat` (and related `/api/machine`)
- Agent API actions: `GET /api/agent/actions`
- Auth: `GET /api/auth/config`, `POST /api/auth/oauth/token`; health `GET /api/health`
- Sessions REST under `/api/sessions` (messages, tools, permissions, files, changes, events, collaborators, guidance, archive, subagent abandon, …)
- Guidance packs `/api/guidance/*`; presets `/api/presets`; audit `/api/audit`; device tokens `/api/devices`; mobile diagnostics `/api/diagnostics/*`
- Integrations: `POST /api/os-events` (ProdDeck); `POST /api/integrations/forgecity/tamil-rewrite` (dedicated key)
- Persistence of chat/events (H2 or Postgres); optional webhooks + per-user workspace quota config keys
- Playwright e2e suite under `e2e/`; sample workspaces under `workspaces/`

## Deploy topology (known facts below - cross-check against what you find in-repo, note any discrepancy explicitly rather than silently picking one)

**Facts supplied for this capture (operator baseline):**

- **DEV:** API legacy port **8080** (migrating to preferred offset **3080**, reserved); UI legacy `ng serve` on **:4200**
- **PREPROD:** `F:/apps/agent-portal`, https://agent-portal-staging.delena.buzz (** :4080 **)
- **PROD:** `G:/apps/agent-portal`, https://agent-portal.delena.buzz (** :5080 **), auth via prod CSS **:5900**
- Auth: CSS JWT (`clientId=agent-portal`); flagship hub other apps on this machine talk to

**In-repo cross-check:**

| Topic | In-repo evidence | Discrepancy vs supplied facts |
|-------|------------------|-------------------------------|
| DEV API **8080** + UI **4200** | `application.properties` `server.port=8080`; README / OPS / PORT-REGISTRY; frontend `backend-url.ts` targets sibling `:8080` when on `:4200` | **Agreed** |
| DEV preferred **3080** | **No mention of port 3080 anywhere in this repo** (grep empty). PORT-REGISTRY lists DEV API as **8080** active | **Discrepancy:** 3080 migration/reservation is **not documented or configured in-repo** |
| PREPROD `F:\apps\…` **:4080** / staging URL | README, OPS, PORT-REGISTRY, `application-preprod.properties` default `5080`→**4080** | **Agreed** |
| PROD `G:\apps\…` **:5080** / prod URL | Same sources; `application-prod.properties` default **5080** | **Agreed** |
| Auth via classic prod CSS **:5900** | PORT-REGISTRY: **5900** = classic `css.delena.buzz` (keep). Portal live docs (**OPS.md**, **DELENA-PROXY.md**) and `application-prod.properties` / `application-preprod.properties` pin JWKS to **css-next `:5910`**; classic `:5900` described as left for other apps. `FUTURE-IMPLEMENTATION.md` still says PREPROD/PROD “CSS prod :5900” (stale vs OPS Wave 3) | **Discrepancy:** supplied fact says portal auth via **:5900**; current portal config/docs (OPS/DELENA/prod properties) say **css-next :5910**. Classic **:5900** still exists for other apps. `clientId=agent-portal` **agreed** in properties |
| Flagship hub | README + platform docs (Machine Gateway, Agent API, OS events, port registry on portal ports) | **Agreed** as role description |

## Known debt / gaps (as-is, factual)

- No `TODO` / `FIXME` hits in `*.java` / `*.ts` / `*.md` / `*.properties` during this inspection
- `docs/ROADMAP.md`: **P4 Dual-CLI promote jobs** marked **Backlog** (not shipped)
- `docs/platform/SUBAGENTS-ROADMAP.md` / `PORT-REGISTRY.md` / `FUTURE-IMPLEMENTATION.md`: dedicated Postgres control-plane tables (`port_lease`, `deploy_event`, …), long-lived sub-agent *services*, dual-CLI job runner — documented as **planned** / not implemented; port registry still “Phase 0” markdown
- `docs/platform/FUTURE-IMPLEMENTATION.md` auth row (**CSS :5900**) conflicts with OPS/DELENA Wave 3 (**css-next :5910**)
- Backend unit/integration tests present but thin relative to surface (7 `*Test*.java` files); frontend unit specs sparse (`app.component.spec.ts`, `history-format.spec.ts`); e2e coverage exists under `e2e/tests/`
- Antigravity mid-turn permissions remain limited vs Cursor (README provider matrix; skip-permissions default for portal runs)
- Frontend still hardcodes sibling API port **8080** when not behind nginx — no in-repo path for a **3080** DEV offset

## Sources consulted

- `README.md`
- `docs/OPS.md` (deployed environments, auth Wave 3, tabs)
- `docs/DELENA-PROXY.md`
- `docs/ROADMAP.md`
- `docs/platform/PORT-REGISTRY.md`
- `docs/platform/FUTURE-IMPLEMENTATION.md`
- `docs/platform/SUBAGENTS-ROADMAP.md`
- `docs/platform/README.md` (index only)
- `backend/pom.xml`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`
- `backend/src/main/resources/application-preprod.properties`
- `backend/src/main/java/com/agentportal/web/*Controller.java` (mapping scan)
- `frontend/package.json`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/services/backend-url.ts`
- `frontend/src/app/components/session-tabs/session-tabs.component.ts`
- `frontend/src/app/app.component.ts` / `app.component.html` (feature surface)
- `e2e/package.json`
- `docker-compose.yml`
- `.cursor/skills/agent-portal/SKILL.md`
- `git status --short` (pre-existing untracked: `workspaces/demo/.cursor/`, `workspaces/demo/AGENTS.md` — unrelated; not modified)
