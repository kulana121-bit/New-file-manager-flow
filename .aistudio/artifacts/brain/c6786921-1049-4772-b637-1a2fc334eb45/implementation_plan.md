# FileFlow - Advanced Android File & Archive Manager

Production-grade Android File and Archive Manager combining Google Files elegance with ZArchiver power, featuring a frosted glass aesthetic matching reference designs, real scoped-storage browsing, archive creation/extraction (ZIP, 7Z, TAR, GZIP, password-protected), native document/PDF/code/media viewers, and an encrypted Safe Vault.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product preferences were clarified and incorporated into the design:

- **Confirmed: Visual Theme**: Auto system theme switching supporting both dark frosted glass (Dashboard & Code viewer) and light frosted glass (Safe Storage, Photos, and Document viewers) with glassmorphic blur and glowing borders.
- **Confirmed: Secured Storage Authentication**: Direct access with optional 4-digit PIN lock configured in Settings (with realistic 0-9 numeric keypad and lock screen).
- **Confirmed: Storage Source**: Real device filesystem access via Scoped Storage, Storage Access Framework, and Environment storage directories (no simulated mock items).
- **Archive Engine**: Integrated Zip4j and Java streaming decompression for ZIP (with AES password support), TAR, and GZIP, with archive creation, extraction, and in-archive browsing.

---

## 1. Overview & Core Concept

- **What It Does**: A complete file management and compression workstation on Android. Users can navigate internal storage and SD cards, perform batch operations (copy, cut, move, rename, delete, share), create/extract archives (ZIP, 7Z, TAR, GZIP, with passwords), preview documents (PDF via native `PdfRenderer`, code with syntax coloring, plain text, markdown), view/stream photos and video/audio with seek controls, and safeguard sensitive files in a Private Vault.
- **Target Audience**: Power users, developers, photographers, and daily users who need robust file operations without ads, trackers, or fake buttons.
- **Key Value**: Unifies the clean organization of Google Files, the deep compression power of ZArchiver, and a glassmorphic visual language with zero dead-ends.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard & Storage Hub**:
   - Dynamic storage usage ring meter calculating real free and used space via `StatFs`.
   - Frosted search pill for quick cross-storage search.
   - Quick access category tiles (Images in warm peach, Videos in lavender, Documents in soft blue, Audio in mint green).
   - Recent files list with quick context actions.
   - Floating frosted bottom navigation bar (Dashboard, File Browser, Settings).

2. **Storage Browser (Internal, SD Card, OTG)**:
   - Breadcrumb navigation path with direct folder jumping.
   - Dual view modes: Grid view (for photos/visual media) and List view (with size, date, item count).
   - Multi-select batch mode: Copy, Cut, Paste toolbar, Batch Archive, Batch Delete, Batch Share.
   - Search, filter by file type, and sort (Name, Date, Size, Type).
   - Hidden files toggle (`.filename`) in toolbar.

3. **Archive Management (ZArchiver Suite)**:
   - Create archive modal: Format selection (ZIP, TAR, GZIP), compression level, optional password encryption.
   - Extract dialog: Extract here, extract to folder, or browse archive contents before extraction.
   - Password prompt for encrypted ZIP archives.

4. **Multi-Format In-App Viewers**:
   - **Photos Gallery & Media Viewer**: Date-grouped photo wall (Today, Yesterday), fullscreen swipeable viewer with zoom, private photo badge, bottom action bar (Lock, Favorite, Adjust, Share, Delete).
   - **Video Player**: Video preview with 10s skip forward/backward, scrubber seek bar, time display, picture-in-picture / aspect ratio toggle, mute, and lock controls.
   - **Audio Player**: Dedicated music screen with gradient artwork disc, title, metadata badge, timeline scrubber, 10s skip buttons, and waveform icon.
   - **PDF Document Viewer**: Native Android `PdfRenderer` page-by-page rendering, zoom, page counter ("Page 1 of 4"), Print/Export, and Lock/Unhide.
   - **Code & Text Viewer**: Dual tabs: `[Preview]` and `[Source Code]`, line numbering gutter, syntax colorization (HTML, XML, JSON, Kotlin, JS, CSS, Python), search/replace, and direct text editing.

5. **Secured Storage & Private Vault**:
   - 4-digit PIN authentication pad with haptic feedback.
   - Private media hub (Private Photos, Secure Video, Key Documents).
   - Add to Private Photos bottom sheet (Take Photo directly into vault, Import from Gallery, Upload from Files).
   - AES-encrypted or isolated private sandbox storage.

6. **Storage Analyzer & Tools**:
   - Large file finder (> 50MB / 100MB).
   - Duplicate file detector (matching size & MD5 hash).
   - Recycle Bin / Trash with restore and permanent delete capabilities.

### Visual Identity & Theme Tokens

- **Aesthetic**: Frosted Glassmorphism / Modern Neumorphic Depth with semi-transparent surfaces, subtle glowing borders, and rounded corners (24dp–28dp).
- **Dark Theme Palette**:
  - Background: Deep Navy Slate (`#0F141C`)
  - Glass Surface: Semi-transparent Slate (`rgba(255, 255, 255, 0.08)`) with border `rgba(255, 255, 255, 0.15)`
  - Storage Ring: Cyan gradient (`#38E1FF` to `#9B51E0`)
  - Accent Peach: `#FFA585` (Images)
  - Accent Lavender: `#D8B4F8` (Videos)
  - Accent Sky Blue: `#A5C4F8` (Documents)
  - Accent Mint: `#A3E7C9` (Music)
  - Danger / Delete: Coral Red (`#FF6B6B`)
- **Light Theme Palette**:
  - Background: Pearl White (`#F3F5F9`)
  - Glass Surface: `rgba(255, 255, 255, 0.70)` with frosted border `rgba(255, 255, 255, 0.90)`
  - Text Primary: `#1E2530`, Secondary: `#6B788E`
- **Floating Navigation Bar**: 32dp floating capsule pill with frosted background, icons for Home, Files/Browser, and Settings.

---

## 3. Key Product Decisions & Trade-Offs

- **Storage Access Model**: Use `Environment.getExternalStorageDirectory()` with fallback to Scoped Storage `MediaStore` and `StorageAccessFramework` (`ACTION_OPEN_DOCUMENT_TREE`) to ensure full compliance on Android 11+ (API 30–36) while requesting `MANAGE_EXTERNAL_STORAGE` / `READ_MEDIA_*` for full device management.
- **Archive Engine**: `net.lingala.zip4j:zip4j:2.11.5` for industry-standard encrypted ZIP extraction/creation, coupled with Java `GZIPInputStream` and `TarArchiveInputStream` for TAR/GZ archives.
- **Document Rendering**: Android native `android.graphics.pdf.PdfRenderer` generates hardware-accelerated bitmaps from PDF pages without heavy external binary bloat.
- **Text & Code Engine**: Compose text editor with syntax lexer supporting line numbers, keywords, strings, tags, and comments.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
├───────────────┬─────────────────┬───────────────────┬──────────────────┤
│ DashboardView │ StorageBrowser  │ Media & DocViewer │  Secured Vault   │
│ - StorageRing │ - List / Grid   │ - Photo Viewer    │ - 4-Digit Pad    │
│ - Categories  │ - Multi-Select  │ - Video Player    │ - Private Media  │
│ - RecentFiles │ - Breadcrumbs   │ - PDF Renderer    │ - Import Sheet   │
│ - FloatNavBar │ - Sort & Filter │ - Code Editor     │ - Encrypted Store│
└───────┬───────┴────────┬────────┴─────────┬─────────┴────────┬─────────┘
        │                │                  │                  │
┌───────▼────────────────▼──────────────────▼──────────────────▼─────────┐
│                    FileManagerViewModel & StateHub                     │
│  - StorageUsageFlow  - CurrentDirectoryFlow  - SelectedFilesFlow       │
│  - SearchQueryFlow   - VaultLockedStateFlow   - ArchiveProgressFlow    │
└───────┬────────────────┬──────────────────┬──────────────────┬─────────┘
        │                │                  │                  │
┌───────▼────────┐┌──────▼─────────┐┌───────▼────────┐┌────────▼─────────┐
│ FileRepository ││ ArchiveService ││ Doc/MediaEngine││ VaultManager     │
│ - Real File IO ││ - Zip4j Engine ││ - PdfRenderer  ││ - PIN Validator  │
│ - MediaStore   ││ - Tar / Gzip   ││ - Syntax Lexer ││ - Private Vault  │
│ - StatFs Stats ││ - Extract/Pack ││ - MediaPlayer  ││ - Trash Recycler │
└────────────────┘└────────────────┘└────────────────┘└──────────────────┘
```

### Component & State Mapping

| Component | State Flow | Action Handlers |
|---|---|---|
| `DashboardScreen` | `storageStats`, `recentFiles`, `themeMode` | Category click, search query, quick file open, notification tap |
| `FileBrowserScreen` | `currentDirectory`, `fileList`, `isMultiSelect`, `selectedSet`, `sortOrder` | Navigate folder, toggle selection, copy/cut/paste, batch delete, archive |
| `ArchiveDialogs` | `archiveTargetFiles`, `isCompressing`, `passwordInput` | Compress files with/without password, extract target directory |
| `MediaViewerScreen` | `mediaItem`, `isPlaying`, `playbackPosition`, `duration` | Play/pause, seek 10s back/forward, toggle lock, delete, share |
| `PdfViewerScreen` | `pdfPages`, `currentPage`, `pageCount` | Next/previous page, jump to page, print/export, lock |
| `CodeViewerScreen` | `sourceCode`, `activeTab` (Preview/Source), `lineNumbers` | Edit text, save file, find & replace |
| `VaultScreen` | `isUnlocked`, `pinEntry`, `privateFiles` | Enter digit, verify PIN, import photo/file, export/unhide |

---

## 5. Implementation Phases & Verification Plan

1. **Phase 1: Dependencies & Android Manifest Setup**
   - Add `zip4j` to `libs.versions.toml` and `build.gradle.kts`. Enable `coil-compose`.
   - Update `AndroidManifest.xml` with `MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `INTERNET`.
2. **Phase 2: Core Architecture & Repositories**
   - Implement `FileItem`, `StorageStats`, and `FileRepository` with real filesystem browsing and `StatFs`.
   - Implement `ArchiveService` with ZIP/TAR/GZIP compression and extraction (supporting passwords).
   - Implement `VaultManager` and `RecycleBinManager` with secure private storage.
3. **Phase 3: Visual System & Theme**
   - Build frosted glass components: `FrostedGlassCard`, `FrostedTopBar`, `FrostedBottomNavBar`, and storage ring gauge.
4. **Phase 4: Screens & Feature Integration**
   - Build `DashboardScreen` matching Image 1 & 6.
   - Build `FileBrowserScreen` with breadcrumbs, list/grid, multi-select, sort/search, copy/paste buffer.
   - Build `PhotoViewerScreen`, `VideoViewerScreen`, and `AudioPlayerScreen` matching Images 2, 3, 5, 6, 7.
   - Build `PdfViewerScreen` using `android.graphics.pdf.PdfRenderer` and `CodeEditorScreen` matching Image 4.
   - Build `VaultScreen` with 4-digit PIN pad and import bottom sheet matching Image 6.
   - Build `StorageAnalyzerScreen` and `SettingsScreen`.
5. **Phase 5: Build Verification**
   - Verify complete compilation with `compile_applet`.
