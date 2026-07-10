# Agent Portal - Comprehensive UI/UX Redesign Prompt for Cursor AI

**Project:** Agent Portal (Autonomous AI Agent Sessions Manager)  
**Current Tech:** Angular (already built)  
**Target Devices:** Desktop + Realme P2 Pro (smartphone) + Realme Pad 2 (tablet)  
**Goal:** Transform the existing app into a modern, professional, highly responsive, and delightful developer tool.

---

## 1. Overall Vision & Goals

You are an expert Angular + Tailwind UI/UX engineer. Refactor the existing Agent Portal application to match a modern, clean, professional AI agent management platform (inspired by tools like Cursor, Linear, and VS Code).

**Key Objectives:**
- Beautiful dark theme with navy + teal accents
- Excellent responsive experience on Realme P2 Pro and Realme Pad 2
- Clear information hierarchy and breathing room
- Fast, touch-friendly interactions on mobile/tablet
- Professional developer aesthetic while remaining approachable
- Maintain all existing functionality (session list, code viewing, agent interaction, etc.)

**Reference Visuals (use these as strict visual guide):**
- Desktop version should feel like a polished desktop app
- Mobile version must be thumb-friendly with FAB + bottom input
- Tablet version should use the extra screen real estate with split views

---

## 2. Design System (Apply Globally)

Use **Tailwind CSS** (recommended) or create consistent component styles.

### Colors
- Background (dark): `#0F172A` (navy-950)
- Sidebar / Cards: `#1E293B` (slate-800)
- Accent / Primary: `#14B8A6` (teal-500) or `#0EA5E9` (sky-500)
- Text Primary: `#F8FAFC` (slate-50)
- Text Secondary: `#94A3B8` (slate-400)
- Success / IDLE: `#22C55E` (green-500)
- Danger / FAILED: `#EF4444` (red-500)
- Warning: `#F59E0B` (amber-500)

### Typography & Spacing
- Font: Inter / system-ui, good readability on all devices
- Rounded corners: `rounded-xl` (12px) or `rounded-2xl` (16px)
- Subtle shadows for elevation
- Generous whitespace (avoid cramped feel from current version)

### Status Badges
Create a reusable `StatusPillComponent`:
- IDLE → green with dot
- FAILED → red with dot
- Other states as needed

---

## 3. New Responsive Layout Structure

### Breakpoints (Define clearly in code)
```ts
// Recommended
const breakpoints = {
  mobile: 640,      // Realme P2 Pro focus (<640px)
  tablet: 1024,     // Realme Pad 2 focus (641-1024px)
  desktop: 1025     // Full desktop experience
};
```

### Mobile Layout (Realme P2 Pro - Portrait)
- Top header: Logo + Hamburger menu + Search bar
- Scrollable vertical list of **Session Cards** (compact but readable)
- Large **green FAB** (bottom-right) for "New Session"
- **Fixed bottom input bar** ("Ask the agent to inspect, edit, or run tasks...")
- When user taps a session → opens in **bottom sheet modal** or navigates to detail view with back button
- No sidebar on mobile (use drawer if needed)

### Tablet Layout (Realme Pad 2 - Landscape preferred)
- Persistent left sidebar (sessions list + filters)
- Main content area shows selected session with **tabs** (Code | Transcript | Logs | Preview)
- Optional right panel for live transcript / agent history (collapsible)
- Larger fonts and touch targets
- Good use of horizontal space

### Desktop Layout (>1024px)
- Collapsible left sidebar with session list
- Top navbar: Logo | Search | User avatar + New Session button
- Main panel: Session header with quick actions + tabs
- Bottom persistent elegant input bar
- Comfortable spacing and professional look

---

## 4. Components to Create or Refactor

### New / Major Components

1. **AppShellComponent** (or update existing root layout)
   - Responsive shell that switches between mobile / tablet / desktop layouts
   - Handles sidebar collapse, drawer on mobile

2. **SessionListComponent**
   - Searchable + filterable list
   - On mobile: vertical cards
   - On tablet/desktop: richer rows or cards in sidebar
   - Filter chips: All | Active | Failed | Archived

3. **SessionCardComponent** (reusable)
   - Displays: Session name, Agent type badge (e.g. ANTI GRAVITY), Status pill, Last activity time, short description
   - Hover/focus states
   - On mobile: more compact version

4. **SessionDetailHeaderComponent**
   - Shows current session name, status, agent type
   - Quick action buttons: **Run**, **Edit with AI**, **Archive**, **Share**, **Duplicate**

5. **SessionTabsComponent**
   - Tabs: `Code` | `Transcript` | `Logs` | `Preview`
   - Use Angular Material Tabs or custom implementation with good keyboard support

6. **CodeViewerComponent**
   - Beautiful dark-themed code display (use existing Monaco/Codemirror if present, or enhance it)
   - Line numbers, syntax highlighting for Java / MQL5 / etc.
   - Copy button, line highlighting if needed

7. **CompilationInstructionsCardComponent**
   - Nicely styled card below code editor with numbered steps + copy buttons

8. **AgentInputBarComponent**
   - Persistent bottom bar on all screen sizes
   - Textarea or input + Send button + Microphone icon
   - Elegant dark styling with teal accent on focus

9. **StatusPillComponent** (small reusable)
10. **AgentTypeBadgeComponent**

### Existing Components to Refactor
- Update current sidebar into the new modern collapsible version
- Improve the main content area to use the new tab + header structure
- Make the code viewing area much more polished
- Ensure the input at the bottom is always visible and styled consistently

---

## 5. Detailed Implementation Instructions

### Step 1: Global Theme & Design System
- Update `styles.scss` or global Tailwind config with the color palette above
- Create a `design-system.md` or constants file for colors, spacing, radii

### Step 2: Responsive Shell
- Create or refactor `app-shell` / layout component
- Use Tailwind responsive classes (`md:`, `lg:`) or Angular CDK BreakpointObserver
- On mobile (<640px): Hide sidebar, show hamburger + FAB + bottom bar
- On tablet (641-1024px): Show sidebar, enable split views where helpful
- On desktop: Full featured layout with collapsible sidebar

### Step 3: Session List & Cards (Mobile First)
- Build `SessionCardComponent` first (mobile version)
- Make list scrollable and performant (virtual scroll if >50 sessions)
- Add search input that filters in real-time
- Add filter chips row

### Step 4: Session Detail View
- When a session is selected:
  - Show beautiful header with quick actions
  - Implement tab navigation (Code / Transcript / Logs / Preview)
  - In **Code** tab: Show enhanced `CodeViewerComponent` + `CompilationInstructionsCardComponent` below it
  - In **Transcript** tab: Show agent conversation (improve existing if present)

### Step 5: Persistent Input Bar
- Create `AgentInputBarComponent`
- Place it fixed at the bottom on mobile and desktop
- On tablet it can be at the bottom of the main panel or global
- Make it look premium (subtle border, focus ring in teal)

### Step 6: Polish & Micro-interactions
- Add smooth transitions (especially when switching tabs or opening sheets)
- Loading states for sessions and code
- Empty states with nice illustrations/messages
- Toast notifications for actions (Run, Archive, etc.)

### Step 7: Realme Device Optimization
- Test touch targets (minimum 44×44px)
- Ensure no horizontal scrolling on mobile
- Use safe-area insets for notched phones if possible
- Larger fonts and spacing on tablet
- Consider PWA manifest for installable experience on Realme devices

---

## 6. Recommended Implementation Order for Cursor

1. **Design System & Theme** (30 mins)
2. **Responsive App Shell + Breakpoints** (1 hour)
3. **SessionCardComponent + SessionListComponent** (mobile first) (1.5 hours)
4. **Session Detail Header + Quick Actions** (45 mins)
5. **Tabs + CodeViewer + Compilation Card** (1.5 hours)
6. **AgentInputBarComponent** (persistent) (45 mins)
7. **Tablet & Desktop layout refinements** (1 hour)
8. **Polish, animations, empty states, testing on devices** (1–2 hours)

---

## 7. Acceptance Criteria / Visual QA Checklist

**Mobile (Realme P2 Pro):**
- [ ] Clean vertical list of cards with good spacing
- [ ] Large, reachable FAB for New Session
- [ ] Bottom input bar always visible and thumb-friendly
- [ ] Session detail opens smoothly (bottom sheet or new view)
- [ ] No horizontal scroll, all text readable without zooming
- [ ] Status pills and badges clearly visible

**Tablet (Realme Pad 2):**
- [ ] Sidebar + main content visible together
- [ ] Comfortable code reading experience
- [ ] Good use of screen real estate without feeling empty or cramped
- [ ] Touch targets generous

**Desktop:**
- [ ] Professional, clean, modern developer tool look
- [ ] Collapsible sidebar works well
- [ ] Tabs and quick actions feel natural
- [ ] Matches the high-quality reference images provided earlier

**General:**
- [ ] Consistent teal accent usage
- [ ] Beautiful dark theme with proper contrast
- [ ] All existing functionality preserved
- [ ] Fast and responsive interactions

---

## 8. Additional Notes for Cursor AI

- Preserve all existing backend integration, services, and data models.
- If using Angular Material, enhance it with custom theming. Otherwise, Tailwind is preferred for speed.
- Make components highly reusable.
- Add proper TypeScript interfaces for new components.
- Use signals or proper change detection strategy where beneficial.
- Keep the "Antigravity", "Cursor", agent type badges concept.
- The input bar should support sending natural language commands to the agent (existing behavior).

---

**You now have everything needed.** Start with the Design System and Responsive Shell, then build outward.

After you're done, the app should feel like a significant upgrade — modern, delightful to use on both phone and tablet, while remaining powerful on desktop.

Good luck! This redesign will make your Agent Portal stand out. 

If you need me to generate specific component code snippets, Tailwind examples, or further refinements to this prompt, just ask.
