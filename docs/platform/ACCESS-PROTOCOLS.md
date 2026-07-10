# Access protocols (machine rules)

**Status:** Binding rules for humans and AI. Enforcement is social/docs today; automation is Future Implementation.

## Identity of this host

| Item | Value |
|------|--------|
| Control plane | `E:\MyWorkspace` |
| Product / legacy trees | `E:\Source` |
| Edge configs | `E:\Source\Deployment` |
| Public zone | `delena.buzz` (Cloudflare → NGINX → host) |
| SSO | Centralized Security System `:9000` |

## Hard rules

### 1. Process safety

- To restart Agent Portal API: stop the Java process on **port 8080** (or `backend-0.0.1-SNAPSHOT.jar`) only.
- **Never** mass-kill by process name: `cursor`, `node`, `agent`, `agy`. That kills the IDE chat session and orphan tools incorrectly.
- Prefer `Get-NetTCPConnection -LocalPort <port>` → stop that PID.

### 2. Ports

- Before binding any port, read and update [PORT-REGISTRY.md](PORT-REGISTRY.md).
- Do not steal ports owned by CSS (9000), portal (8080/4200), or NGINX (80/443).
- Prefer registry “free” ranges for new apps.

### 3. Filesystem writes

- Agent-generated project work belongs under the **sandbox** (see [SANDBOX.md](SANDBOX.md)).
- Do not write secrets into git (`.env`, API tokens, Cloudflare tokens).
- Do not reorganize `E:\Source\erpnext` or vendor trees unless the task explicitly says so.

### 4. Auth

- New apps integrate with **CSS** (`clientId`, JWKS, CORS origins).
- Browser apps on HTTPS must use `https://delena.buzz` (or their subdomain) for auth URL to avoid mixed content.

### 5. Network / DNS

- Prefer Cloudflare **subdomains** over advertising the public IP.
- NGINX config changes live under `E:\Source\Deployment`; reload NGINX after edits.
- Document every new subdomain in PORT-REGISTRY / CLOUDFLARE-DNS-PROXY.

### 6. Promote

- Sandbox and local `ng serve` are **not** production.
- Production changes follow [VERSIONING-PROMOTE.md](VERSIONING-PROMOTE.md).

### 7. Documentation

- If you change a protocol, update this folder in the same PR/commit.
- Docs are the interface between AI agents — tribal chat memory is not enough.

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
| CSS seed passwords | Dev only | Change before real prod users |
| JWT secrets | Env | Per-app; no defaults in prod |

## Quick “may I?” decision tree

```text
Need a port?     → PORT-REGISTRY claim
Need disk write? → Under sandbox or the git repo working tree?
Need restart?    → Kill by port/PID of that service only
Need public URL? → Subdomain + NGINX, not raw IP
Need prod?       → VERSIONING-PROMOTE gates
Unsure?          → Stop and ask human; update docs after answer
```
