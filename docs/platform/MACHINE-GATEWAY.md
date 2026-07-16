# Machine Gateway — Host Consciousness API

**Status:** Implemented on Agent Portal (`main`, PREPROD from `v0.1.12+`). Idea SoT: [`machine-gateway`](https://github.com/sivaram311/machine-gateway).

**Note (0.1.12):** `PlatformRegistryService.getRole` resolves `GATEWAY_*` from the full role catalog (`ROLE_DTOS`), not the VirtualDev task-role allowlist — required for `POST /api/machine/chat` session ACL.

## Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/machine/context` | CSS JWT or `X-API-Key` | Live redacted host/control-plane snapshot |
| POST | `/api/machine/chat` | CSS JWT or `X-API-Key` | Chat via AgentBridge (Cursor Auto) with injected context |

Optional header: `X-Machine-Max-Mode: observe|advise|act|ops` (cannot raise above `app.machine-gateway.max-mode`).

## Transport model (v0)

- **Gateway surface is REST only** (`GET /api/machine/context`, `POST /api/machine/chat`).
- **No gRPC** and no dedicated Machine Gateway websocket endpoint in v0.
- `GET /context` returns a point-in-time JSON snapshot. Use `ttlSeconds` as a poll hint.
- `POST /chat` accepts the prompt into an Agent Portal session and returns `status=accepted`; the agent run continues asynchronously.
- For live token/event updates, use the existing Agent Portal session channels (`/ws`, `/api/sessions/{id}/messages`, `/api/sessions/{id}/events`).

### Chat body

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
- [ACCESS-PROTOCOLS.md](ACCESS-PROTOCOLS.md)
- Discovery: `GET /api/agent/actions` → `machineContext`, `machineChat`
