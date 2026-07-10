# Agent Portal — Mobile Fixes Checklist

**Task:** Fix mobile UI issues on Realme P2 Pro based on audit screenshots. Prioritize consistency and readability.

## Priority 1 (High)

1. Remove large "New session" button on mobile. Keep only FAB (+).
2. Fix long workspace paths — truncate with ellipsis + add copy on long press.
3. Clean up session detail header (better hierarchy, prevent overflow).
4. Complete "Share with CSS username" input with proper states.

## Priority 2 (Medium)

5. Improve empty states across tabs (Changes, Code, Preview, Logs).
6. Add timestamps to Transcript messages.
7. Add bottom padding so FAB doesn't overlap last session card.
8. Make session detail tabs horizontally scrollable on small screens.

## Polish

- Consistent badge/pill sizing and spacing.
- Better visual hierarchy in session cards and headers.
- Subtle improvements to Logs terminal and chat bubbles.

**Style:** Keep current dark navy + teal theme. Focus on mobile (360×780). Use existing components.

See [README.md](README.md) for how these screenshots map to before/after captures.
