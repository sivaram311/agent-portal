# VPS FileBridge
## Technical Requirements & Design Document
**Version:** 1.0  
**Date:** July 10, 2026  
**Stack:** Angular 18+ (Frontend) + Spring Boot 3.3+ (Backend)  
**Target Devices:** Realme P2 Pro (Mobile), Realme Pad 2 (Tablet), Modern Browsers  
**Hosting:** Self-hosted on Windows Server 2025 VPS  
**Primary Use Cases:** Secure file upload & download from anywhere

---

## 1. Executive Summary

**VPS FileBridge** is a lightweight, elegant, self-hosted web-based file manager. It provides a modern, mobile-first Progressive Web App (PWA) experience for browsing, uploading, and downloading files stored on your VPS.

**Core Philosophy**
- **Elegant & Minimal**: Premium dark theme with glassmorphism, generous spacing, large touch targets.
- **Mobile-First**: Optimized for Realme P2 Pro (portrait) and Realme Pad 2 (landscape/grid).
- **Fast & Reliable**: Streaming uploads/downloads, progress indicators, resumable uploads for mobile networks.
- **Secure by Default**: JWT authentication, optional TOTP 2FA, HTTPS, path traversal protection.
- **Simple MVP**: Focus on upload/download + basic file management. Expand later.

The goal is a clean alternative to heavy solutions like Nextcloud — fast, private, and tailored to your workflow.

---

## 2. Functional Requirements (FR)

### Authentication & Security
- **FR1**: Secure login with username/password + JWT tokens.
- **FR2**: Optional TOTP-based 2FA (Google Authenticator compatible).
- **FR3**: Session management with token refresh / logout.
- **FR4**: Role-based access (single admin user for MVP; multi-user later).

### File Management
- **FR5**: Browse files/folders in grid view (thumbnails) and list view (toggle).
- **FR6**: Search by filename (real-time or debounced).
- **FR7**: Create new folders.
- **FR8**: Rename files/folders.
- **FR9**: Delete files/folders (with confirmation).
- **FR10**: Move files between folders (drag-drop or menu — Phase 1.1).
- **FR11**: Multi-select for batch operations (download as ZIP, delete).

### Upload & Download (Core Focus)
- **FR12**: Drag & drop upload with live progress bar(s).
- **FR13**: Multiple file upload support.
- **FR14**: Chunked / resumable uploads for large files and unstable mobile connections (recommended for v1).
- **FR15**: Direct streaming download for single files.
- **FR16**: Batch download as ZIP (on-the-fly generation).
- **FR17**: Upload queue with pause/resume/cancel (nice-to-have).

### Previews & Usability
- **FR18**: Inline preview for images, text files, PDFs (browser native where possible).
- **FR19**: File detail view with metadata (size, modified date, type).
- **FR20**: Breadcrumbs navigation + quick path jump.
- **FR21**: Empty states, loading skeletons, error handling with friendly messages.

### PWA & Cross-Device
- **FR22**: Installable as PWA on Realme devices (Add to Home Screen).
- **FR23**: Responsive layout: Bottom nav on phone, persistent sidebar on tablet.
- **FR24**: Dark mode (system preference + manual toggle).
- **FR25**: Offline shell (UI cached; file operations require connection).

---

## 3. Non-Functional Requirements (NFR)

- **Performance**:
  - File list loads < 1s for 500+ items.
  - Uploads use streaming to keep memory low on VPS.
  - Downloads stream directly (no full file buffering).
- **Responsiveness**: Mobile-first. Breakpoints: <640px (phone), 641-1024px (tablet), >1024px (desktop).
- **Security**:
  - All traffic over HTTPS.
  - JWT with short expiry + refresh tokens (HttpOnly cookies preferred where possible).
  - Input validation, file type/size whitelisting (configurable).
  - Prevent path traversal (`../` attacks).
  - Rate limiting on auth and upload endpoints.
- **Accessibility**: WCAG 2.1 AA basics — large tap targets (≥48px), good contrast, ARIA labels.
- **Maintainability**: Clean code, standalone Angular components, clear service layer in Spring Boot.
- **Resource Usage**: Lightweight on VPS (target < 512MB RAM for the app).

---

## 4. UI/UX Design Specification (Elegant Dark Theme)

### 4.1 Color Palette (Premium Dark Mode)

Use these CSS variables / Tailwind config:

```css
:root {
  --bg-primary: #0F172A;        /* Deep navy/slate-900 */
  --bg-surface: #1E2937;        /* Slate-800 cards */
  --bg-surface-hover: #334155;  /* Subtle hover */
  --accent-primary: #6366F1;    /* Indigo-500 — modern & elegant */
  --accent-secondary: #22D3EE;  /* Cyan-400 for highlights */
  --text-primary: #F8FAFC;      /* Almost white */
  --text-secondary: #94A3B8;    /* Slate-400 */
  --text-muted: #64748B;        /* Slate-500 */
  --success: #22C55E;
  --warning: #F59E0B;
  --danger: #EF4444;
  --border: #475569;
  --glass-bg: rgba(30, 41, 55, 0.8);
}
```

- **Glassmorphism**: Subtle `backdrop-blur-md` + semi-transparent cards.
- **Shadows**: Soft `shadow-xl` with low opacity for depth.
- **Accents**: Indigo primary for buttons/FAB. Cyan for secondary actions.

### 4.2 Typography
- Font: System UI stack or Inter / Satoshi (sans-serif).
- Headings: 600-700 weight, tight tracking.
- Body: 400-500 weight.
- Mobile: Slightly larger base font (16-17px) for readability.

### 4.3 Key Components & Patterns
- **Floating Action Button (FAB)**: Large circular Upload button (bottom-right on mobile).
- **Bottom Navigation** (Phone): Home / Browse / Upload / Settings.
- **Sidebar** (Tablet+): Persistent folder tree + quick actions.
- **Cards**: Rounded-2xl or 3xl, subtle border, hover lift.
- **Modals / Bottom Sheets**: For upload queue, file actions, confirmations (slide-up on mobile).
- **Progress**: Linear bars with percentage + speed estimate.
- **Skeletons**: For loading file lists.
- **Empty States**: Friendly illustrations + call-to-action.

### 4.4 Responsive Strategy
- **Phone (Realme P2 Pro)**: Single column, bottom nav, large touch targets, pull-to-refresh.
- **Tablet (Realme Pad 2)**: Two-column or sidebar + main grid (3-5 columns), more spacious.
- **Desktop**: Full sidebar + comfortable grid.

---

## 5. Screen-by-Screen Specifications (For Cursor AI)

### Screen 1: Login
- Centered elegant card on dark gradient background.
- Logo + "VPS FileBridge".
- Username / Password fields (floating labels).
- "Login" button (full width, accent color).
- Link: "Enable 2FA" or TOTP input field (conditional).
- Subtle footer with version or "Self-hosted".

**Actions**: Submit → JWT token → redirect to Home. Error toasts.

### Screen 2: Home / Dashboard (Mobile)
- Top: Greeting + Storage usage ring/chart (e.g., "45% used • 128 GB free").
- Prominent FAB (Upload).
- Section: "Recent Files" — horizontal scroll or grid of 4-6 beautiful thumbnail cards.
- Quick stats cards.
- Bottom nav.

**Tablet variant**: Sidebar visible, larger recent grid.

### Screen 3: File Browser (Main View)
- Top bar: Breadcrumbs (clickable) + Search input (debounced) + View toggle (Grid/List) + Sort dropdown.
- Main area:
  - **Grid View**: Responsive cards with thumbnail (or icon), filename (truncated), size, date. Long-press or right-click for actions menu.
  - **List View**: Dense table-like with columns.
- Multi-select mode toggle (checkboxes appear).
- Floating or bottom action bar when items selected: "Download ZIP", "Delete", "Move".

**Actions**:
- Tap folder → navigate in.
- Tap file → open preview or download.
- FAB always visible for upload.

### Screen 4: Upload Experience
- Drag & drop large zone (or click to select).
- Below: Upload queue list with individual progress bars, filename, size, status (Uploading / Paused / Completed / Failed).
- Buttons per item: Cancel / Retry.
- Global: "Add more files", "Clear completed".
- For chunked: Show overall progress + per-chunk status.

**Backend note**: Support both simple multipart and chunked (with uploadId + chunk index).

### Screen 5: File Preview / Detail
- Modal or full screen on mobile.
- Large preview area (image / PDF viewer / text).
- Sidebar or bottom panel: Metadata + Actions (Download, Rename, Delete, Copy link if public later).
- Close button.

### Screen 6: Settings
- Profile / Account info.
- Storage path configuration (admin only).
- Theme toggle (Dark / System).
- 2FA management.
- Logout button (prominent danger style).
- App info (version, VPS status).

---

## 6. Technical Architecture

### 6.1 High-Level
```
[Realme Devices / Browser]
   ↓ HTTPS + JWT
[Reverse Proxy: Caddy / Nginx (HTTPS, compression, static serving)]
   ↓
[Angular PWA Frontend]  ↔  [Spring Boot Backend]
   ↓ API calls
[Filesystem on VPS]  (configurable root directory)
```

### 6.2 Frontend (Angular)
- **Version**: Angular 18+ (standalone components preferred).
- **Styling**: Tailwind CSS 3.4+ + custom design tokens.
- **PWA**: `@angular/pwa` — manifest, service worker for offline shell.
- **State**: Angular Signals (preferred for simplicity) or lightweight NgRx if complex.
- **HTTP**: `HttpClient` + Interceptor for JWT injection + error handling.
- **File Handling**: `ngx-upload` or custom with `tus-js-client` for chunked (recommended).
- **Icons**: Lucide Angular or inline SVGs.
- **Routing**: Angular Router with lazy-loaded modules.
- **Recommended Structure**:
  ```
  src/app/
    core/          (auth service, http interceptor, guards)
    shared/        (components, pipes, directives)
    features/
      auth/
      dashboard/
      file-browser/
      upload/
      settings/
    models/        (interfaces: FileItem, Folder, User, etc.)
  ```

### 6.3 Backend (Spring Boot)
- **Version**: Spring Boot 3.3+ (Java 21 recommended).
- **Security**: Spring Security + `jjwt` or `spring-boot-starter-oauth2-resource-server` for JWT.
- **File Handling**:
  - Streaming downloads: `Resource` + `InputStreamResource`.
  - Uploads: Start with `@RequestParam MultipartFile` + streaming to disk.
  - For resumable: Custom endpoint accepting chunks + metadata (uploadId, chunkNumber, totalChunks). Store temp chunks and assemble on completion.
- **Storage Config**: Externalized via `application.yml` (`file.root-path=/data/files`).
- **Validation**: File size limits, allowed extensions (configurable).
- **API Documentation**: Springdoc OpenAPI (Swagger UI).
- **Recommended Structure**:
  ```
  src/main/java/com/vpsfilebridge/
    config/        (SecurityConfig, FileStorageConfig)
    controller/    (AuthController, FileController)
    service/       (AuthService, FileService, StorageService)
    security/      (JwtUtil, JwtFilter)
    model/         (dto, entity if using DB)
    exception/
  ```

### 6.4 Key API Endpoints (REST)

**Auth**
- `POST /api/auth/login` → `{token, refreshToken?}`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/2fa/setup` / verify (optional)

**Files**
- `GET /api/files?path=/optional/subfolder` → List files/folders (with metadata)
- `POST /api/files/folder` → Create folder
- `PUT /api/files/rename` → Rename
- `DELETE /api/files` → Delete (body with paths)
- `POST /api/files/upload` → Simple multipart upload
- `POST /api/files/upload/chunk` → Chunked upload (with uploadId, chunkIndex, etc.)
- `GET /api/files/download?path=...` → Streaming download (Content-Disposition)
- `POST /api/files/download/zip` → Generate ZIP for multiple paths

**Other**
- `GET /api/storage/stats`

Use consistent DTOs and proper HTTP status codes.

### 6.5 Data Model (Lightweight)
No heavy database required for MVP.
- Files/folders read directly from filesystem.
- Optional: SQLite (via Spring Data JPA) for:
  - User accounts & hashed passwords
  - TOTP secrets
  - Upload session metadata (for resumable)
  - User preferences

---

## 7. Implementation Guidelines for Cursor AI

1. **Start with Project Setup**
   - Angular CLI: `ng new vps-filebridge --standalone`
   - Add Tailwind, PWA, Angular Material (optional) or pure Tailwind.
   - Spring Boot: Spring Initializr with Web, Security, Validation, etc.

2. **Security First**
   - Implement JWT filter early.
   - Protect all `/api/**` except login.

3. **File Upload Strategy (Recommended Order)**
   - Phase 1: Simple multipart streaming upload.
   - Phase 1.5: Add chunked upload with resumability (highly recommended for mobile).

4. **Responsive Implementation**
   - Use Tailwind responsive prefixes heavily.
   - Detect device or use CSS media queries + Angular `BreakpointObserver`.

5. **Testing Locally**
   - Run Angular dev server + Spring Boot.
   - Use CORS config in dev.

6. **Polish**
   - Add loading skeletons, toasts (ngx-toastr or Angular Material Snackbar).
   - Error boundaries / global error handler.

---

## 8. Deployment on Windows VPS

**Recommended Stack**:
- WSL2 + Docker (easiest for modern tooling) **or**
- Native Windows services.

**Steps Outline**:
1. Build Angular: `ng build --configuration production` → `dist/`
2. Place built files in Spring Boot `src/main/resources/static` (or serve separately).
3. Or run Angular build as static files served by Caddy/Nginx, Spring Boot on port 8080.
4. Configure reverse proxy with HTTPS (Caddy is excellent and simple).
5. Expose securely: **Cloudflare Tunnel** (strongly recommended) or Tailscale/ZeroTier.
6. Set `file.root-path` to your desired storage location.
7. Run as service (NSSM on Windows or Docker compose).

**Environment Variables**:
- `FILE_ROOT_PATH`
- `JWT_SECRET`
- `MAX_UPLOAD_SIZE` (e.g., 5GB)
- Allowed file extensions list

---

## 9. Future Enhancements (Phase 2+)
- Multi-user support with permissions
- Public share links with expiry
- Version history / trash bin
- Thumbnail generation for images/videos (background job)
- Integration with your other self-hosted tools (AI agents, trading data folders)
- WebDAV or SFTP bridge
- Mobile native wrapper (Capacitor)

---

## 10. Appendix

### Icon Recommendations
- Use **Lucide** icons (modern, consistent): Upload, Folder, File, Download, Trash, Edit, Search, etc.

### Accessibility Notes
- All interactive elements ≥ 48×48 px.
- Proper `aria-label`s.
- Keyboard navigation support.

### References for Cursor
- Elegant dark UI examples from previous prototypes (glassmorphism, FAB, grid cards).
- Follow Material Design 3 or Apple HIG principles adapted to web for mobile feel.

---

**This document is ready to be fed directly to Cursor AI or Claude / GPT for implementation.**
