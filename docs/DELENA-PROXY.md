# delena.buzz reverse proxy (Agent Portal)

Operational notes for serving Agent Portal at [https://delena.buzz/](https://delena.buzz/) via Cloudflare → NGINX → host services.

Canonical NGINX files live outside this repo: `E:\Source\Deployment\` (`conf\delena.buzz.conf`, `scripts\*.ps1`, `nginx-setup.md`).

## Topology

### DEV apex (`delena.buzz`)

```
Browser (HTTPS)
  → Cloudflare (Flexible SSL, proxied DNS)
  → Origin 103.118.183.185:80 (Windows Firewall: allow TCP 80)
  → NGINX (C:\nginx-1.30.3)
       ├─ /api/*, /ws     → 127.0.0.1:8080  Agent Portal (DEV)
       ├─ /auth/*, /.well-known/* → 127.0.0.1:5910  css-next (Portal Wave 3)
       └─ /*              → 127.0.0.1:4200  Angular (ng serve)
```

### PREPROD / PROD subdomains (versioned release)

| Host | UI | API | Auth / JWKS |
|------|----|-----|-------------|
| `agent-portal-staging.delena.buzz` | static `F:\apps\agent-portal\ui` | `:4080` | nginx → **css-next** `:5910` |
| `agent-portal.delena.buzz` | static `G:\apps\agent-portal\ui` | `:5080` | nginx → **css-next** `:5910` |
| `css-next.delena.buzz` | — | `:5910` | css-next IdP (Portal Wave 3) |
| `css.delena.buzz` | — | `:5900` | Classic CSS IdP (other apps) |

Confs: `E:\Source\Deployment\conf\apps\agent-portal-staging.delena.buzz.conf`, `agent-portal.delena.buzz.conf`.

## Required host env (`.env`, not committed)

```
CSS_ENABLED=true
CSS_AUTH_URL=https://delena.buzz
CSS_JWKS_URI=http://127.0.0.1:5910/.well-known/jwks.json
CSS_ISSUER=https://css-next.delena.buzz
APP_CORS_ORIGINS=https://delena.buzz,https://www.delena.buzz,http://delena.buzz,http://www.delena.buzz,http://localhost:4200,http://127.0.0.1:4200
AGENT_WORKSPACE_ROOT=E:\MyWorkspace\agent-portal\workspaces
```

Admin password: `G:\apps\css-next\.env` (`CSS_ADMIN_PASSWORD`) — not README `admin123`.

## Cloudflare checklist

| Setting | Value |
|---------|--------|
| DNS A `delena.buzz` / `www` | `103.118.183.185`, **proxied** |
| SSL/TLS mode | **Flexible** (origin is HTTP-only on :80) |
| Firewall on origin | Inbound allow **TCP 80** (`NGINX HTTP 80`) |

## Common failures

| Symptom | Cause | Fix |
|---------|--------|-----|
| Connection timed out | Port 80 blocked or SSL Full→:443 | Open firewall :80; set SSL Flexible |
| Mixed content on login | `CSS_AUTH_URL=http://…` on HTTPS page | Use `https://delena.buzz`; client also same-origin upgrades |
| `403 Invalid CORS request` on `/auth/login` | CSS CORS missing `https://delena.buzz` | Update CSS `application.yml`, **rebuild + restart CSS JAR** |
| Angular “development mode” banner | `ng serve` | Expected; production = `ng build` + static root in nginx |

## Scripts

```powershell
E:\Source\Deployment\scripts\status-nginx.ps1
E:\Source\Deployment\scripts\start-nginx.ps1
E:\Source\Deployment\scripts\stop-nginx.ps1
# After editing conf: prefer stop/start if reload Access Denied
Copy-Item E:\Source\Deployment\conf\delena.buzz.conf C:\nginx-1.30.3\conf\ -Force
```

## Related portal features

- **Rules & Skills** — see [OPS.md](OPS.md#rules--skills-guidance) and README
- Same-origin API/WS when the UI is on port 80/443 — `frontend/src/app/services/backend-url.ts`
- Auth same-host HTTPS — `frontend/src/app/services/auth.service.ts`
