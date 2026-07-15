# Cloudflare DNS + reverse proxy

## Today

- Zone: `delena.buzz`
- Cloudflare → origin NGINX `:80` (Flexible SSL until origin TLS)
- **DEV** apex routes: `/` UI `:4200`, `/api`+`/ws` `:8080`, `/auth` css-next `:5910`  
- **PREPROD/PROD** app hosts + `css.delena.buzz` are live (see table below)  
  Details: [DELENA-PROXY.md](../DELENA-PROXY.md), [../OPS.md](../OPS.md#deployed-environments-2026-07-11).

Credentials: gitignored `.env` (`CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ZONE_ID`, …). Token has zone edit.

## Script

```powershell
cd E:\MyWorkspace\agent-portal
.\scripts\cloudflare-dns.ps1 -List
.\scripts\cloudflare-dns.ps1 -Upsert -Name myapp-sandbox -Content 103.x.x.x -Proxied
```

## Target pattern

| Hostname | Purpose | Upstream |
|----------|---------|----------|
| `delena.buzz` | Portal DEV / home | `:4200` + `:8080` + CSS `:9000` |
| `css.delena.buzz` | CSS prod IdP | `:5900` |
| `control.delena.buzz` | Stack Pilot **PROD** | nginx → `:5091` (`G:\apps\stack-pilot`); DNS via `cloudflare-dns.ps1` (wrapper reserves name `control`). Static CSS/JS edge TTL **4h** (`max-age=14400`) — hard-refresh or purge after UI promotes. |
| `agent-portal.delena.buzz` | Agent Portal **PROD** | UI static `G:\…\ui` + API `:5080`; `/auth` → css-next `:5910` |
| `agent-portal-staging.delena.buzz` | Agent Portal **PREPROD** | UI static `F:\…\ui` + API `:4080`; `/auth` → css-next `:5910` |
| `auth.delena.buzz` | CSS (optional split) | `:9000` / `:5900` |
| `<app>.delena.buzz` | Production app | claimed port |
| `<app>-sandbox.delena.buzz` | Sandbox / preview | claimed port |
| `<app>-staging.delena.buzz` | Pre-prod | claimed port |

## Workflow (Ops / Deploy sub-agent later)

1. Claim port in PORT-REGISTRY.
2. Create DNS A/AAAA/CNAME via Cloudflare API to this host (or tunnel).
3. Generate NGINX server block from template under `E:\Source\Deployment`.
4. Reload NGINX.
5. Add HTTPS origin to CSS CORS + app CORS.
6. Register URL in CSS App Home metadata.
7. Document in PORT-REGISTRY notes.

## Rules

- Prefer subdomain over public IP in all docs and app configs.
- Sandbox and prod **never** share the same hostname.
- DNS changes are Ops-owned; Builders request via workflow, do not freestyle.

## Docker note

Linux compose images need a Linux engine. Until then, host processes + NGINX remain the default edge path; Docker is for services that already run (or future Linux VM).
