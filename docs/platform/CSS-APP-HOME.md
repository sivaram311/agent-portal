# CSS App Home (Future Implementation)

## Vision

After a user signs in with the **Centralized Security System**, they land on a **home / launcher** page that lists every application they may open — production and sandbox — without memorizing ports or IPs.

Agent Portal remains one tile among many; new apps register once in CSS and appear automatically.

## UI (shipped)

Agent Portal top bar **Apps** opens App Home:

- **Apps** — tiles from `GET /api/platform/home` / `apps` (open URL)
- **Roles** — VirtualDev departments from `GET /api/platform/roles`
- **Tasks** — Engineering Manager tasks from `GET /api/platform/tasks`
- **Memory** — shared project knowledge from `GET /api/platform/memory`
- **Messages** — inter-agent bus from `GET /api/platform/messages`
- **Pipelines** — run `FEATURE` / `BUGFIX` / `REFACTOR` / `SECURITY_AUDIT`

## Data sources

| Source | Fields |
|--------|--------|
| CSS `RegisteredApplication` | `clientId`, name, roles |
| Future app metadata table | `baseUrl`, `env` (sandbox/staging/prod), `subdomain`, `icon`, `healthUrl` |
| Cloudflare | DNS presence for subdomain |
| Port registry | Upstream port for ops |

## UX sketch

1. User opens `https://home.delena.buzz` (or `/home` on portal).
2. Redirect/login via CSS if needed.
3. Grid of apps filtered by token `aud` / roles.
4. Click → open app URL (subdomain preferred).
5. Admin view: register app, assign subdomain, claim port.

## Auto-configuration rules

When an app is “production ready”:

1. CSS client registered + CORS origins include its HTTPS origin.
2. Subdomain created (CLOUDFLARE-DNS-PROXY).
3. NGINX upstream added.
4. Metadata row marked `env=prod`, `version=x.y.z`.
5. Home launcher picks it up on next load (no hardcode).

## Relationship to Agent Portal

- Short term: launcher can live as a new route in Agent Portal frontend.
- Long term: thin CSS-owned shell app so portal downtime does not block SSO home.

## Out of scope until built

- Implementing the UI
- Auto DNS from the launcher button
