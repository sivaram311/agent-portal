# Machine map

**Status:** Living map of this Windows host for humans and AI.

## Top-level E: drive

| Path | Purpose |
|------|---------|
| `E:\MyWorkspace\` | **Control plane** — active product repos, sandbox, session restore |
| `E:\Source\` | Product/legacy trees, Deployment (NGINX), grok_dev, stack-pilot, mcp-tools, analyses |
| `E:\Logs\` | Host logs (if present) |
| `E:\ProgramFiles\` | Local installs |
| `E:\GitBackup\` | Backups |

## MyWorkspace (hub)

| Path | Purpose |
|------|---------|
| `agent-portal\` | Agent sessions UI/API — DEV tree; foundation for VirtualDev Co |
| `centralized-security-system\` | SSO / JWT / JWKS (DEV `:9000`) |
| `sandbox\` | Target agent write root (see SANDBOX.md) |
| `persistent-agent-platform\` | Earlier agent platform experiments |
| `mt5-dev\`, `MT5_Docs\` | Trading / MT5 work |
| `erpnext\`, `erpnext-docs\` | ERPNext |
| `SESSION-RESTORE.md` | Reboot continue guide |
| `docker-startup-guide.md` | Docker CE notes |

## Source (selected)

| Path | Purpose |
|------|---------|
| `E:\Source\Deployment\` | NGINX configs, setup scripts for delena.buzz |
| `E:\Source\grok_dev\` | Grok trading stack |
| `E:\Source\stack-pilot\` | Stack Pilot |
| `E:\Source\mcp-tools\` | MCP tooling |
| `E:\Source\docs\` | Machine tool setup (Git, Flutter, …) |

## Runtime topology (typical)

### DEV (`delena.buzz`)

```text
Internet → Cloudflare (delena.buzz)
        → NGINX :80
           ├─ /          → :4200 Agent Portal UI (ng serve)
           ├─ /api /ws   → :8080 Agent Portal API
           └─ /auth      → :9000 CSS (DEV)
```

### PREPROD / PROD

| Host | UI | API | Auth |
|------|----|-----|------|
| `agent-portal-staging.delena.buzz` | `F:\apps\agent-portal\ui` | `:4080` | CSS prod `:5900` |
| `agent-portal.delena.buzz` | `G:\apps\agent-portal\ui` | `:5080` | CSS prod `:5900` |
| `css.delena.buzz` | — | `:5900` | Prod CSS IdP |

Release packages: `H:\releases\`. Standing orders: `E:\MyAgent\workflow\`. Details: [../OPS.md](../OPS.md#deployed-environments-2026-07-11).

## Markdown indexing

Do **not** mass-move markdown out of git repos. Discover docs via:

- [indexes/INDEX-MYWORKSPACE.md](indexes/INDEX-MYWORKSPACE.md)
- [indexes/INDEX-SOURCE.md](indexes/INDEX-SOURCE.md)

Authoritative platform handbook: this `docs/platform/` folder inside **agent-portal** (versioned + pushed).
