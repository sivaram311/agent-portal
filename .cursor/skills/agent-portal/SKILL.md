---
name: agent-portal
description: >-
  Maintains and operates the Agent Portal (Spring Boot + Angular dual-provider
  Cursor/Antigravity app). Use when working in agent-portal, running backend or
  frontend, fixing Antigravity/agy Windows launch, SockJS global polyfill,
  redesign UI (navy/teal), public-IP binding, or Playwright Realme P2 Pro e2e.
---

# Agent Portal

## Layout

- `backend/` — Spring Boot 3.5, Java 21; Cursor ACP + Antigravity bridges
- `frontend/` — Angular 19 dark navy/teal UI
- `e2e/` — Playwright Realme P2 Pro mobile suite
- `workspaces/` — default relative workspace root
- Root `README.md` — operator docs (keep in sync when behavior changes)

## Run

```powershell
# Backend
cd backend
$env:CURSOR_API_KEY = "..."   # optional but recommended for Cursor
.\mvnw.cmd -q -DskipTests package
java -jar target\backend-0.0.1-SNAPSHOT.jar

# Frontend (binds 0.0.0.0:4200)
cd frontend
npm start

# Mobile e2e (app must be up)
cd e2e
$env:APP_URL = "http://127.0.0.1:4200"
npm test
```

API/WS use `window.location.hostname:8080` (works on LAN/public IP). CORS patterns: `http://localhost:4200`, `http://127.0.0.1:4200`, `http://*:4200`.

## Antigravity (Windows)

- Prompt must follow `-p` immediately.
- Do **not** use PowerShell `Start-Process -ArgumentList` (splits multi-word prompts → “message cut off”).
- Launch via generated `.ps1` with call-operator splat: `& agy.exe @agyArgs`, write stdout UTF-8 via `[IO.File]::WriteAllText`.
- Stream via brain artifacts + `.agent-portal-reply.txt` + stdout file; skip mid-turn permissions.

## Frontend conventions

- Design tokens: `frontend/src/app/theme/tokens.scss` (navy `#0F172A`, teal `#14B8A6`).
- Breakpoints: mobile `<640`, tablet `640–1024`, desktop `>1024`.
- Tabs: Transcript (chat/markdown), Logs (tools/terminal), Code/Preview empty states.
- Prefer `data-testid` for e2e (`fab-new-session`, `mobile-session-list`, `session-detail`, etc.).
- SockJS needs `global` polyfill (`src/global-polyfill.ts` + `index.html` inline).

## Docs duty

When changing run steps, providers, UI shell, CORS/host binding, or e2e: update root `README.md` and `e2e/README.md` in the same change.

Improvement tracks and specialized skills live in `docs/ROADMAP.md` and `.cursor/skills/ap-*`.

## More detail

See [reference.md](reference.md) for config keys and provider matrix.
