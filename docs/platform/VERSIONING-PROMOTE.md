# Versioning and promote

## Status (2026-07-11)

Promote is **live** via machine workflow (not only future):

- Gates: `E:\MyAgent\workflow\promote\` + skills `promote-em` / `promote-qa` / `promote-security` / `promote-review` / `promote-ops`
- Evidence: `H:\releases\<app>-<version>\evidence\q1|q2\`
- Agent Portal **0.1.0** Q1+Q2 GO — see `H:\releases\agent-portal-0.1.0\evidence\`

This doc remains the product-facing checklist; machine standing orders win on drives/ports/DB/CSS.

## Principle

Agent edits are **not** production. Every production change is a **named version** with tested functionality, promoted through environments.

## Environments

| Env | Where | Who writes |
|-----|-------|------------|
| sandbox | `sandbox/<app>` + `*-sandbox.delena.buzz` | Builder / agents |
| DEV | `E:\MyWorkspace\agent-portal` + `delena.buzz` | Builder (not a promote target) |
| staging (PREPROD) | `F:\apps\agent-portal` + `agent-portal-staging.delena.buzz` (`:4080`) | Releaser after QA |
| prod | `G:\apps\agent-portal` + `agent-portal.delena.buzz` (`:5080`) | Releaser after approval |

## Promote gates

```text
sandbox work
  → QA sub-agent / human test report
  → Reviewer approve
  → cut version (semver) + changelog
  → deploy staging
  → smoke + regression
  → human go/no-go
  → deploy prod
  → record version + evidence under `H:\releases\` (and Postgres `deploy_event` when control plane lands)
```

## Version definition

A version is:

- Git tag `vX.Y.Z` (or release branch / commit SHA if untagged — must still be recorded)
- Changelog of **functionality** (not only commits)
- Artifact (JAR/docker image/static `dist`)
- Known port + subdomain mapping
- CSS client + CORS verified
- **Dependency record:** CSS (and other upstreams) **version + git tag** in `H:\releases\<app>-<ver>\DEPENDENCIES.md` + machine matrix `E:\MyAgent\workflow\deps/`

## Promote gates (dependency bar)

Every Q1/Q2 SUMMARY and ACTIVITY-LOG entry must name:

1. This app’s **git tag** (or commit)
2. Each dependent service’s **version + git tag** (CSS required for auth apps)

Missing → **NO-GO**. See `E:\MyAgent\workflow\deps/README.md` and CONSCIOUS rule **13**.

## Agent Portal Changes tab

Use Keep/Restore + human review as the **inner** gate for workspace file edits. Outer gate (PREPROD/PROD) is the promote workflow + evidence packs under `H:\releases\`.

## Forbidden

- Hot-editing prod static files in place without a version
- Pointing prod DNS at a developer `ng serve`
- Reusing sandbox DB data as prod without migration plan
- Promoting without recording git tag + dependency versions/tags
