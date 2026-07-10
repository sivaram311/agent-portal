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

## Status
- Starter implemented with JWKS RS256 validation
- Agent Portal consumes it via Maven (`css.resource-server.*`)
- Set `css.resource-server.register-servlet-filter=false` when wiring the filter in `SecurityFilterChain`

## Build
```powershell
cd E:\MyWorkspace\centralized-security-system\clients\spring-boot-starter
mvn -q install
```
