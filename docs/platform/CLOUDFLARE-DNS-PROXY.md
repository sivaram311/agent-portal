# Cloudflare DNS + reverse proxy (Future Implementation)

## Today

- Zone: `delena.buzz`
- Cloudflare → origin NGINX `:80` (Flexible SSL until origin TLS)
- Routes: `/` UI, `/api`+`/ws` portal, `/auth` CSS  
  See [DELENA-PROXY.md](../DELENA-PROXY.md).

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
| `delena.buzz` | Portal / home | `:4200` / future home |
| `auth.delena.buzz` | CSS (optional split) | `:9000` |
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
