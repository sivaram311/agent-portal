# Access protocols (machine rules)

**Status:** Binding rules for humans and AI. Port/DNS/CSS/promote contracts are in force on this host; see [../OPS.md](../OPS.md#deployed-environments-2026-07-11).

## Identity of this host

| Item | Value |
|------|--------|
| Control plane | `E:\MyWorkspace` |
| Machine standing orders | `E:\MyAgent\workflow\` (ports, DB schemas, promote) |
| Product / legacy trees | `E:\Source` |
| Edge configs | `E:\Source\Deployment` |
| Public zone | `delena.buzz` (Cloudflare → NGINX → host) |
| SSO DEV | Centralized Security System `:9000` |
| SSO PREPROD/PROD | CSS prod `:5900` / `https://css.delena.buzz` |

## Hard rules

### 1. Process safety

- To restart Agent Portal API: stop the Java process on the **env port only** — DEV `:8080`, PREPROD `:4080`, PROD `:5080` (or the matching JAR under that env’s drive).
- **Never** mass-kill by process name: `cursor`, `node`, `agent`, `agy`. That kills the IDE chat session and orphan tools incorrectly.
- Prefer `Get-NetTCPConnection -LocalPort <port>` → stop that PID.

### 2. Ports

- Before binding any port, read and update [PORT-REGISTRY.md](PORT-REGISTRY.md) **and** `E:\MyAgent\workflow\ports\REGISTRY.md`.
- Do not steal ports owned by CSS (`9000` DEV, `4900` staging, `5900` prod), portal (`8080` / `4080` / `5080` / `4200`), or NGINX (`80` / `443`).
- Prefer registry “free” ranges for new apps.

### 3. Filesystem writes

- Agent-generated project work belongs under the **sandbox** (see [SANDBOX.md](SANDBOX.md)).
- PREPROD/PROD app trees: `F:\apps\agent-portal`, `G:\apps\agent-portal` — Ops/Releaser only.
- Do not write secrets into git (`.env`, API tokens, Cloudflare tokens, DB passwords).
- Do not reorganize `E:\Source\erpnext` or vendor trees unless the task explicitly says so.

### 4. Auth

- New apps integrate with **CSS** (`clientId`, JWKS, CORS origins).
- Browser apps on HTTPS must use their HTTPS origin for auth (same-origin `/auth/*` preferred) to avoid mixed content.
- PREPROD and PROD Agent Portal use **prod CSS** (`clientId=agent-portal`, issuer `https://css.delena.buzz`).

### 5. Network / DNS

- Prefer Cloudflare **subdomains** over advertising the public IP.
- NGINX config changes live under `E:\Source\Deployment`; reload NGINX after edits.
- Document every new subdomain in PORT-REGISTRY / CLOUDFLARE-DNS-PROXY.

### 6. Promote

- Sandbox and local `ng serve` (DEV `delena.buzz`) are **not** production.
- Production changes follow [VERSIONING-PROMOTE.md](VERSIONING-PROMOTE.md) and `E:\MyAgent\workflow\promote\`.

### 7. Documentation

- If you change a protocol, update this folder in the same PR/commit.
- Docs are the interface between AI agents — tribal chat memory is not enough.

### 8. Postgres string columns

- Never map long text with JPA `@Lob` / `CLOB` on PostgreSQL — use `@JdbcTypeCode(SqlTypes.LONGVARCHAR)` + `TEXT` (see [../OPS.md](../OPS.md#postgres-text-columns-do-not-use-lob--clob)).

## Allowed operations by role

| Action | Builder | Reviewer | Ops | Releaser |
|--------|---------|----------|-----|----------|
| Edit repo code | Yes | Suggest | Rare | Rare |
| Bind new port | After claim | No | Yes | No |
| Kill process by port | Own app only | No | Yes | Own release |
| Edit NGINX / Cloudflare | No | No | Yes | With Ops |
| Merge to main | No | Approve | No | After approve |
| Touch prod subdomain | No | No | With Releaser | Yes |

## Secrets

| Secret | Location | Rule |
|--------|----------|------|
| `CURSOR_API_KEY` | `.env` (gitignored) | Never commit / paste into docs |
| `CLOUDFLARE_API_TOKEN` | `.env` | Zone edit only; rotate if leaked |
| CSS seed passwords | Dev only (`admin`/`admin123`) | Prod password in `G:\apps\css\.env` only |
| DB roles | `E:\MyAgent\workflow\db\secrets\` | Never commit |
| JWT secrets | Env | Per-app; no defaults in prod |

## Quick “may I?” decision tree

```text
Need a port?     → PORT-REGISTRY claim (+ machine REGISTRY.md)
Need disk write? → Under sandbox or the git repo working tree?
Need prod change?→ VERSIONING-PROMOTE + promote evidence pack
Need kill?       → By LocalPort PID only (8080/4080/5080/…)
```
