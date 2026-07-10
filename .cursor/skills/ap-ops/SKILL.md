---
name: ap-ops
description: >-
  Production ops for Agent Portal (Postgres-first, backups, deploy notes). Use
  when hardening data layer, docker compose prod, or ops documentation.
---

# Agent Portal — Ops / Postgres (P3)

## Goal
Document and default a production-ready data path.

## Build plan
1. Verify `application-postgres.properties` + `docker-compose.yml`.
2. Add backup/restore notes to README (H2 file copy vs `pg_dump`).
3. Optional compose profile with backend+db for demo deploys.
4. Health endpoint already exists — add readiness checks for DB if missing.

## Constraints
- No secrets in compose; use env files gitignored
