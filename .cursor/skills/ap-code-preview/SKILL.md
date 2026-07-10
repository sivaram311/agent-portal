---
name: ap-code-preview
description: >-
  Implements Agent Portal Code and Preview tabs (workspace file browser and
  preview). Use when building code viewer, file tree API, or preview pane.
---

# Agent Portal — Code & Preview tabs (P1)

## Goal
Replace empty Code/Preview states with a workspace file browser + read-only viewer and a simple preview surface.

## Build plan
1. **API** — `GET /api/sessions/{id}/files?path=` (list), `GET .../files/content?path=` (text, size-capped); path must stay under session workspace root.
2. **UI Code tab** — tree + Monaco or lightweight highlighted `<pre>`; copy button.
3. **UI Preview tab** — render markdown/html/images safely (DOMPurify); otherwise “unsupported type”.
4. **Security** — reject `..` and symlinks escaping workspace.
5. **E2E** — open Code tab, assert tree or empty-workspace message.

## Constraints
- No arbitrary shell from this API
- Match navy/teal design tokens
