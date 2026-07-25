# Evidence — ForgeCity Tamil rewrite DEV enablement (2026-07-20)

| Field | Value |
|-------|-------|
| Env | DEV (`E:\MyWorkspace\agent-portal`, `:8080` / `https://delena.buzz`) |
| Jar tip | Rebuilt `backend-0.0.1-SNAPSHOT.jar` including ForgeCity classes |
| Enable | `FORGECITY_REWRITE_ENABLED=true` + dedicated key in local `.env` only |
| Wrong key | HTTP **401** |
| Loopback | HTTP **200**, `status:ok`, Tamil Unicode present, `Cache-Control: no-store` |
| HTTPS | `https://delena.buzz/.../tamil-rewrite` HTTP **200**, Tamil present |
| PREPROD/PROD | **Not** enabled (OPS gate) |
| Phone E2E | **Blocked** — no `adb` device |

Do not paste API keys into this file.
