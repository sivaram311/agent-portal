# Agent Portal reference

## Config keys (`application.properties`)

| Key | Purpose |
|-----|---------|
| `agent.cursor.command` | Path to `agent.cmd` |
| `agent.cursor.api-key` | From `CURSOR_API_KEY` |
| `agent.antigravity.command` | Path to `agy.exe` |
| `agent.antigravity.brain-root` | Default `~/.gemini/antigravity-cli` |
| `agent.antigravity.skip-permissions` | Default `true` for portal runs |
| `agent.antigravity.print-timeout` | Default `5m` |
| `agent.antigravity.poll-interval-ms` | Brain poll interval |
| `agent.workspace.root` | Relative workspace root |
| `app.cors.allowed-origins` | Comma list / patterns for Angular origins |

## Provider matrix

| Capability | Cursor | Antigravity |
|------------|--------|-------------|
| Streaming | ACP `session/update` | Brain messages / transcript / reply / stdout |
| Permissions UI | Yes | No (`--dangerously-skip-permissions`) |
| Cancel | `session/cancel` | Kill process tree |
| Resume | ACP session id | `--conversation` on portal session |

## Playwright Realme P2 Pro

- Project: `e2e/` config project `realme-p2-pro`
- Viewport: 360×800, DPR 3 (physical 1080×2412)
- Screenshots: `e2e/screenshots/realme-p2-pro/`
- Scope selectors to visible mobile DOM (`data-testid`); avoid matching hidden sidebar clones

## UI shell map

- Mobile: list + FAB; session opens full-screen detail with back; drawer for session switch
- Tablet/desktop: collapsible sidebar + detail tabs + bottom input bar
- Markdown: assistant messages via `marked` + DOMPurify pipe
