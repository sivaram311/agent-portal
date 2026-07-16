# Machine Gateway — Host Consciousness API

**Status:** Implemented on Agent Portal (`main`, PREPROD from `v0.1.12+`). Idea SoT: [`machine-gateway`](https://github.com/sivaram311/machine-gateway).

**Note (0.1.12):** `PlatformRegistryService.getRole` resolves `GATEWAY_*` from the full role catalog (`ROLE_DTOS`), not the VirtualDev task-role allowlist — required for `POST /api/machine/chat` session ACL.

## Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| **POST** | `/api/machine` | CSS JWT or `X-API-Key` | **Canonical** — redacted context; if `message` present, also starts chat (`status=accepted`) |
| GET | `/api/machine/context` | CSS JWT or `X-API-Key` | Context snapshot only (alias) |
| POST | `/api/machine/chat` | CSS JWT or `X-API-Key` | Chat accept only; `message` required (alias) |

Optional header: `X-Machine-Max-Mode: observe|advise|act|ops` (cannot raise above `app.machine-gateway.max-mode`).

**Usage guide:** [MACHINE-GATEWAY-USAGE.md](MACHINE-GATEWAY-USAGE.md) (auth, examples, poll/ws — no CORS section).

## Transport model (v0)

- **Gateway surface is REST only.** Canonical: `POST /api/machine`. Aliases: `GET /api/machine/context`, `POST /api/machine/chat`.
- **No gRPC** and no dedicated Machine Gateway websocket endpoint in v0.
- Context is a point-in-time JSON snapshot. Use `ttlSeconds` as a poll hint.
- Chat returns `status=accepted`; the agent run continues asynchronously.
- For live token/event updates, use the existing Agent Portal session channels (`/ws`, `/api/sessions/{id}/messages`, `/api/sessions/{id}/events`).

### Unified body (`POST /api/machine`)

```json
{ "message": "What ports are leased?", "mode": "observe", "provider": "cursor", "sessionId": null }
```

Omit or blank `message` for context-only (`chat: null`).

### Chat-only body (`POST /api/machine/chat`)

```json
{ "message": "What ports are leased?", "mode": "observe", "provider": "cursor", "sessionId": null }
```

Modes map to roles: `GATEWAY_OBSERVE` · `GATEWAY_ADVISE` · `GATEWAY_ACT` · `GATEWAY_OPS`.

## Security (Must)

- **SecretRedactor** on context payload (token/secret/password/apiKey/…)
- **Role ACL** via existing `AgentBridge.handlePermissionRequest`
- **MachineToolGuard**: edit paths must stay under gateway workspace ∪ sandbox; shell process kills must match allowlisted port→PID / `-Id` shapes
- **Mode ceiling**: requesting above max → `403`

## Edge / ports / Cloudflare (binding)

| Item | Decision |
|------|----------|
| TCP ports | Reuses portal **8080 / 4080 / 5080** — no new claim |
| Cloudflare DNS | **No new subdomain** |
| NGINX | Existing `/api` upstream |
| CSS | Existing `clientId=agent-portal` |
| Registry | Documented in [PORT-REGISTRY.md](PORT-REGISTRY.md#machine-gateway-host-consciousness-api--edge-contract) |

## Config

```properties
app.machine-gateway.enabled=true
app.machine-gateway.workspace-path=machine-gateway
app.machine-gateway.sandbox-root=E:/MyWorkspace/sandbox
app.machine-gateway.ttl-seconds=15
app.machine-gateway.max-mode=act
```

## Collab

Cursor↔Agy loop: `E:\MyWorkspace\machine-gateway\docs\collab\2026-07-16-build-loop\`

## Related

- [AGENT-API.md](AGENT-API.md)
- [MACHINE-GATEWAY-USAGE.md](MACHINE-GATEWAY-USAGE.md)
- [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md)
- Discovery: `GET /api/agent/actions` → `machine`, `machineContext`, `machineChat`
