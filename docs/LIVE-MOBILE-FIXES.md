# Agent Portal — Live Site Fixes (delena.buzz)

**Task:** Fix remaining mobile UI issues on the live site (https://delena.buzz/).

## High Priority (Fix Immediately)

1. Remove the large "New session" button on mobile. Keep only the FAB (+).
2. Fix long workspace paths — add proper truncation + copy on long press.
3. Improve session detail header layout and fix text overflow.
4. Complete the "Share with CSS username" input with proper styling and states.

## Medium Priority

5. Improve empty states in Code, Preview, Changes, and Logs tabs.
6. Add timestamps to messages in the Transcript tab.
7. Add bottom padding to session list so FAB doesn't overlap cards.
8. Make tab bar horizontally scrollable on small mobile screens.

## Polish

- Consistent badge and pill sizing/spacing.
- Better visual hierarchy in session cards and headers.
- Minor improvements to chat bubbles and terminal styling in Logs.

Keep the current dark navy + teal theme. Optimize for mobile. Use existing components.

## Status

Implemented in the Angular frontend (FAB-only create, path truncate + Copy, share grid + error state, Transcript-first scrollable tabs, empty states, message timestamps, FAB list padding). See [MOBILE-QA.md](MOBILE-QA.md).
