# ForgeCity Tamil rewrite

`POST /api/integrations/forgecity/tamil-rewrite` is a disabled-by-default,
privacy-sensitive integration for rewriting a ForgeCity notification as concise
Tamil. It is not an Agent Portal chat session.

## Request

Send `Content-Type: application/json` and the dedicated `X-ForgeCity-Key`
header. CSS tokens and the portal-wide `X-API-Key` do not authenticate this
route.

```json
{
  "schemaVersion": 1,
  "appLabel": "ForgeCity",
  "title": "<notification title>",
  "text": "<notification body>",
  "maxChars": 120
}
```

The object must contain exactly these fields. `appLabel` is limited to 80
Unicode code points, `title` to 200, `text` to 2,000, and the JSON body to
16,384 bytes. At least one of `title` or `text` must be non-blank. `maxChars`
must be between 1 and the configured maximum.

Success:

```json
{
  "schemaVersion": 1,
  "status": "ok",
  "tamil": "<Tamil rewrite>"
}
```

Errors return only a `status`: `unavailable` (503), `unauthorized` (401),
`invalid_request` (400), `busy` (429), `timeout` (504), or `failed` (502).
Application responses set `Cache-Control: no-store`.

## Environment configuration

| Variable | Default | Enforced range / purpose |
|---|---:|---|
| `FORGECITY_REWRITE_ENABLED` | `false` | Must be `true` to expose processing |
| `FORGECITY_REWRITE_API_KEY` | empty | Dedicated secret; keep only in environment/secret storage |
| `FORGECITY_REWRITE_TIMEOUT_MS` | `30000` | 1,000–120,000 ms hard request deadline |
| `FORGECITY_REWRITE_MAX_CHARS` | `500` | 1–2,000 Unicode code points |
| `FORGECITY_REWRITE_MAX_CONCURRENT` | `1` | 1–8 ephemeral ACP processes |
| `FORGECITY_REWRITE_WORKSPACE` | `./workspaces/forgecity-rewrite` | Empty/dedicated working directory |

The normal API rate limit also applies to this route, including when Agent
Portal is in open-access mode. Keep the endpoint disabled unless both the
consumer and server have the dedicated key.

## Privacy and retention limits

- The endpoint does not create `AgentSession`, `ChatMessage`, tool-run, event,
  or audit database records.
- Request bodies, prompts, ACP payloads, and response text are not written to
  application logs. ACP stderr is discarded.
- Each request creates a new ACP process and session. Client file and terminal
  capabilities are disabled, MCP servers are empty, permission and extension
  requests are rejected, and the prompt forbids tools.
- The ACP process tree is forcibly terminated on success, failure, or timeout.
- Agent Portal provides no durable retention for this flow. The configured
  Cursor/model provider may process or retain submitted content independently
  under its own service terms and account settings. Do not send secrets or
  notification content that is not approved for that provider.

## Run and smoke check

Use redacted values; never paste a real key into documentation, shell history,
screenshots, or logs.

1. Set the server environment:

   ```text
   FORGECITY_REWRITE_ENABLED=true
   FORGECITY_REWRITE_API_KEY=<REDACTED_DEDICATED_KEY>
   ```

2. Start the backend using the normal local run procedure.
3. Smoke with placeholders:

   ```powershell
   $headers = @{
     "X-ForgeCity-Key" = "<REDACTED_DEDICATED_KEY>"
     "Content-Type" = "application/json"
   }
   $body = @{
     schemaVersion = 1
     appLabel = "ForgeCity"
     title = "<TEST_TITLE>"
     text = "<TEST_NOTIFICATION_TEXT>"
     maxChars = 120
   } | ConvertTo-Json -Compress
   Invoke-RestMethod -Method Post `
     -Uri "http://127.0.0.1:8080/api/integrations/forgecity/tamil-rewrite" `
     -Headers $headers -Body $body
   ```

4. Confirm `schemaVersion: 1`, `status: ok`, a single-line Tamil result within
   `maxChars`, and `Cache-Control: no-store`.
5. Repeat with `<WRONG_REDACTED_KEY>` and confirm 401. Set
   `FORGECITY_REWRITE_ENABLED=false`, restart, and confirm 503.

Do not deploy or enable this integration in PREPROD/PROD without a separate
approved operations decision.
