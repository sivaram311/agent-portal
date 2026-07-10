---
name: ap-ws-auth
description: >-
  Authenticate Agent Portal SockJS/STOMP when CSS JWT is enabled. Use when
  securing /ws handshake or topic subscriptions by session ownership.
---

# Agent Portal — WebSocket auth (P1)

## Goal
Do not leave `/ws` fully open when CSS is on; bind topic subscriptions to session owners.

## Build plan
1. Accept `access_token` on all `/ws/**` HTTP requests (not only Upgrade).
2. When CSS enabled, require authentication for `/ws/**`.
3. STOMP `ChannelInterceptor`: on SUBSCRIBE to `/topic/sessions/{id}`, verify caller owns the session.
4. Frontend already passes `?access_token=` — keep reconnect after login.

## Constraints
- Local mode without CSS stays open for DX
- Never log raw JWTs
