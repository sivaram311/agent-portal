---
name: ap-history-replay
description: >-
  Expose persisted agent events and a History timeline for Agent Portal
  sessions. Use when adding run replay or event browsing.
---

# Agent Portal — History / replay (P2)

## Goal
Browse persisted events + messages + tools in one timeline.

## Build plan
1. `GET /api/sessions/{id}/events`
2. History tab merging messages, tools, events chronologically
3. Optional “jump to tool” from timeline
