---
name: ap-diff-review
description: >-
  Show files changed during an Agent Portal session turn with accept/reject or
  view diffs. Use when adding change review next to the Code tab.
---

# Agent Portal — Diff / change review (P1)

## Goal
After a run, show which workspace files changed so users can review agent edits.

## Build plan
1. Snapshot file digests (path → size/mtime/hash) at run start; compare at run end.
2. `GET /api/sessions/{id}/changes` returns changed paths + optional unified diff for text files.
3. UI tab **Changes** with list + Monaco/diff view.
4. Prefer git when `.git` exists; else snapshot compare.

## Constraints
- Stay inside workspace sandbox
- Cap diff size (e.g. 256KB per file)
