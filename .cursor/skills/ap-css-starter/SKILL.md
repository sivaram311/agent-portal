---
name: ap-css-starter
description: >-
  Build or consume the CSS Spring Boot resource-server starter shared by
  Agent Portal and other apps. Use when extracting Jwt/JWKS validation.
---

# CSS Spring Boot Starter (cross-repo)

## Goal
Replace copy-pasted `CssJwtValidator` / filter with `com.css:css-spring-boot-starter`.

## Location
`E:\MyWorkspace\centralized-security-system\clients\spring-boot-starter\`

## Build plan
1. Implement auto-config + properties (`css.resource-server.*`).
2. Port JWKS RS256 validation matching agent-portal `CssJwtValidator`.
3. Document dependency + YAML in CSS README and agent-portal OPS.
4. Optionally migrate agent-portal to the starter in a follow-up (keep local classes until published).

## Constraints
- Starter must not depend on CSS server internals
- Compatible with Spring Boot 3.3+
