---
name: ap-audit-ui
description: >-
  Expose Agent Portal audit events in the API and UI. Use when adding admin
  activity history or session audit trails.
---

# Agent Portal — Audit UI (P2)

## Goal
Operators can see who created/prompted/cancelled/abandoned sessions.

## Build plan
1. `GET /api/audit?limit=` (admin or authenticated) returning recent `AuditEvent`s.
2. Optional `?sessionId=` filter.
3. Compact UI panel or Logs-adjacent “Activity” strip.
4. Keep write path in `AuditService.record`.

## Constraints
- No PII beyond username already stored
- Rate-limit applies
