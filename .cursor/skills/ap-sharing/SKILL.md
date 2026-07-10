---
name: ap-sharing
description: >-
  Share Agent Portal sessions with collaborators via CSS usernames. Use when
  adding team access beyond owner-only isolation.
---

# Agent Portal — Session sharing (P2)

## Goal
Owner can grant collaborator access; list/get/WS respect collaborators.

## Build plan
1. `session_collaborators` table (sessionId, username, role)
2. `POST/DELETE /api/sessions/{id}/collaborators`
3. Widen `assertOwner` / `canAccess` / list queries
4. Simple share UI in session header
