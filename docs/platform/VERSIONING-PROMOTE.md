# Versioning and promote (Future Implementation)

## Principle

Agent edits are **not** production. Every production change is a **named version** with tested functionality, promoted through environments.

## Environments

| Env | Where | Who writes |
|-----|-------|------------|
| sandbox | `sandbox/<app>` + `*-sandbox.delena.buzz` | Builder / agents |
| staging | versioned build + `*-staging` subdomain | Releaser after QA |
| prod | tagged release + prod subdomain | Releaser after approval |

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
  → record version in Postgres (future)
```

## Version definition

A version is:

- Git tag `vX.Y.Z` (or release branch)
- Changelog of **functionality** (not only commits)
- Artifact (JAR/docker image/static `dist`)
- Known port + subdomain mapping
- CSS client + CORS verified

## Agent Portal Changes tab

Until full promote automation exists, use Keep/Restore + human review as the **inner** gate for workspace file edits. Outer gate (staging/prod) remains this document.

## Forbidden

- Hot-editing prod static files in place without a version
- Pointing prod DNS at a developer `ng serve`
- Reusing sandbox DB data as prod without migration plan
