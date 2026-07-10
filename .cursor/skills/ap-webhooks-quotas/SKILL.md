---
name: ap-webhooks-quotas
description: >-
  Webhook notifications and per-user workspace quotas for Agent Portal. Use when
  adding ops integrations or multi-user resource limits.
---

# Agent Portal — Webhooks & quotas (P3)

## Goal
Notify external systems on run completion; limit disk per user.

## Build plan
1. `app.webhooks.url` + fire on run_completed / input_required / failed
2. `agent.workspace.quota-bytes-per-user` enforced on create/prompt
3. Document in OPS.md
