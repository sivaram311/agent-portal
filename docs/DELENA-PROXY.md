# delena.buzz reverse proxy (Agent Portal)

Operational notes for serving Agent Portal at [https://delena.buzz/](https://delena.buzz/) via Cloudflare → NGINX → host services.

Canonical NGINX files live outside this repo: `E:\Source\Deployment\` (`conf\delena.buzz.conf`, `scripts\*.ps1`, `nginx-setup.md`).

## Topology

### DEV apex (`delena.buzz`)

```
Browser (HTTPS)
  → Cloudflare (Flexible SSL, proxied DNS)
  → Origin <ORIGIN_IP>:80 (Windows Firewall: allow TCP 80)
  → NGINX (C:\nginx-1.30.3)
       ├─ /api/*, /ws     → 127.0.0.1:8080  Agent Portal (DEV)
       ├─ /auth/*, /.well-known/* → 127.0.0.1:5910  css-next (password + JWKS)
       └─ /*              → 127.0.0.1:4200  Angular (ng serve)
            including /oauth/callback (SPA — never under /auth/)
```

OAuth authorize / token / login HTML: authorize + login HTML go **directly** to issuer `https://css-next.delena.buzz` (full browser navigation). Token exchange uses Portal BFF `POST /api/auth/oauth/token` → issuer (avoids apex CORS: `https://delena.buzz` is not matched by IdP pattern `https://*.delena.buzz`). Password form stays same-origin `/auth/login`.

**css-next CORS:** Prefer including explicit `https://delena.buzz` in `CSS_CORS_ORIGINS` and restart css-next after env changes. Subdomain consumers (`agent-portal*.delena.buzz`) already match `https://*.delena.buzz`.

### PREPROD / PROD subdomains (versioned release)

| Host | UI | API | Auth / JWKS |
|------|----|-----|-------------|
| `agent-portal-staging.delena.buzz` | static `F:\apps\agent-portal\ui` | `:4080` | nginx → **css-next** `:5910` |
| `agent-portal.delena.buzz` | static `G:\apps\agent-portal\ui` | `:5080` | nginx → **css-next** `:5910` |
| `css-next.delena.buzz` | — | `:5910` | css-next IdP (Portal Wave 3) |
| `css.delena.buzz` | — | `:5900` | Classic CSS IdP (other apps) |

Confs: `E:\Source\Deployment\conf\apps\agent-portal-staging.delena.buzz.conf`, `agent-portal.delena.buzz.conf`. Static UI uses `try_files … /index.html` so `/oauth/callback` lands on the SPA.

## Required host env (`.env`, not committed)

```
CSS_ENABLED=true
CSS_AUTH_URL=https://delena.buzz
CSS_JWKS_URI=http://127.0.0.1:5910/.well-known/jwks.json
CSS_ISSUER=https://css-next.delena.buzz
CSS_AUTH_MODE=hybrid
CSS_OAUTH_REDIRECT_PATH=/oauth/callback
APP_CORS_ORIGINS=https://delena.buzz,https://www.delena.buzz,http://delena.buzz,http://www.delena.buzz,http://localhost:4200,http://127.0.0.1:4200
AGENT_WORKSPACE_ROOT=E:\MyWorkspace\agent-portal\workspaces
```

Admin password: `G:\apps\css-next\.env` (`CSS_ADMIN_PASSWORD`) — not README `admin123`.

## Cloudflare checklist

| Setting | Value |
|---------|--------|
| DNS A `delena.buzz` / `www` | `<ORIGIN_IP>`, **proxied** |
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
