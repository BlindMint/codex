You are an expert Android developer specializing in Jetpack Compose, Material 3, e-reader/comic apps, and Tachiyomi forks (Mihon/Book's Story). Create a COMPLETE, STEP-BY-STEP ENHANCEMENT PLAN to upgrade "Codex" (~/dev/codex/, fork of Book's Story) into a premium hybrid eBook + comic reader, referencing the below Codex Update spec.

CONTEXT:
- BASE: Codex (local ~/dev/codex/) = customized Book's Story fork (eBook-focused Tachiyomi/Mihon lineage) with recently-added OPDS integration (Catalogs UI: Local/OPDS tabs).
- MIHON SOURCE: Local ~/dev/git/Mihon/. Cherry-pick ONLY comic viewer/pager (ui/reader/viewer/pager, ContinuousViewer/PagerState), library queue/notifs. Add support for reading digital comic formats such as CBZ/CBR/CB7. Consider Coil/zip4j.
- CATALOG SOURCING: eBooks and comics can be added from local directories (SAF) or Calibre/OPDS sources. The app should detect file format to determine the proper reader such as ReflowViewer (existing for books/text) or ComicPager (ported from Mihon to handle comics/images).
- SPEC: Reference below Codex Update SPEC (masonry grid, immersion, overlays, themes, Calibre pre-configs). Tech: Compose-only, Room (TB indexes), SAF, WorkManager downloads, MuPDF/Coil, etc. Build upon the existing Codex app structure - we don't want to re-invent the wheel, but there is ample room for improvement and some alterations. The largest change to UI/UX is laid out with the process of adding local files. Currently, users add local folders under Settings > Browse, which populates recursively-scanned directories and all found books under Catalogs > Local. Users then select desired books from Catalogs > Local to add to the Library, but are taken to another confirmation prompt ("Add books?" panel), and after a final confirmation the selected books are added to the library. The Codex Update spec below simplifies this process: reference the spec for details, but essentially users simply add the top-level folder they desire, and books are recursively added directly to the Library from all subfolders - there is no intermediary process of selecting specific books from Catalogs > Local, and in fact Catalogs is renamed to "Discover" or similar and is used almost entirely for handling OPDS sources. 
- SCALE: Several TB of comics (100k+): Lazy indexing, scoped storage, 200+ GB of ebooks (these will grow as more books are added over time)
- GOALS: Seamlessly integrate Mihon's elite-level Comic reading into Codex. Smooth Library management with intelligent filters - users should be able to sort and filter the Library as a whole (all added books + comics in unified Library view), with ability to filter by metadata (search, authors, date, etc) and ability to view only comics or only books with the press of a button/chip. Preserve general look and feel of Codex, but Codex is strongly based off of Book's Story which is strongly based off of Mihon - polish and improvements are welcome, but preserve general Mihon/Codex look and feel. Full OPDS support (initial OPDS support is already added to Codex). Privacy-first, 60fps (120fps?).
- WORKFLOW: Plan executes via mid-tier models (Claude 3.5 Sonnet, Grok Code Fast 1)—ATOMIC STEPS (1-2 files/task), EXACT PROMPTS, git commands, tests. Reference local repo paths for Codex and Mihon.

OUTPUT STRICT FORMAT (Markdown):
1. **REPO SETUP**: Git branches/tags in ~/dev/codex/ (e.g., codex-hybrid-m3), deps (coil-compose:2.7.0, zip4j:2.11.5, etc.). Mihon cherry-pick strategy (diff ~/dev/git/Mihon/).
2. **CHERRY-PICK LIST**: Table: Mihon files/paths to copy/adapt (viewer/pager only), est. lines.
3. **PHASED PLAN**: Table: Phase | Steps (numbered, atomic) | Files Touched (Codex paths) | Est. Time | Tests/Verify (gradlew/emulator cmds) | Grok/Sonnet Prompt.
   - Phase 1: Setup/Comics MVP (2 days: Dual reader).
   - Phase 2: OPDS/Calibre Hybrid (2 days: Comics facets/downloads).
   - Phase 3: Codex Update Spec UI (2 days: Grid/overlays/bottom nav/themes).
   - Phase 4: Polish/Test/TB Scale (1-2 days: Perf, e-ink, Play-ready).
4. **RISKS/GOTCHAS**: Table (Issue | Mitigation | Test Cmd).
5. **POST-MVP**: Quick wins (widgets/TTS/CSS injection).
6. **HANDOVER PROMPTS**: 5 ready prompts (e.g., "Port Mihon ComicPager to Codex ReaderScreen").

Codex Update SPEC: 

Codex is a modern, highly customizable eBook reader app designed from the ground up to adhere strictly to Material 3 design principles, delivering a fluid, intuitive experience across phones, tablets, foldables, and large screens. It supports popular ebook format such as EPUB (including EPUB3 with multimedia), PDF, MOBI/AZW3, FB2, CBZ/CBR/CB7 comics, TXT, RTF, HTML, and FODT. The app emphasizes offline-first reading with seamless integration for local files via Android's Storage Access Framework (SAF) and full OPDS/OPDS-Pub (v1 & v2) catalog browsing/downloading. Separate from per-book reading customizations, the app offers dynamic Material You theming (system light/dark modes with 10+ tonal palettes like Indigo, Teal, or custom hex uploads), accent colors, and rounded corner radii that adapt to device shape (e.g., pill-shaped FABs on foldables). Animations use shared motion: bottom sheets slide up with elevation lift and scrim fade-ins, top app bars expand/collapse with interpolator easing, lists stagger in with fade-scale reveals, and page turns employ realistic curl effects (customizable speed/opacity) or instant snap for e-ink devices. There is initial setup wizard - users launch directly into a clean library screen with prominent empty-state CTAs for adding books, ensuring immediate usability while onboarding via contextual tooltips and a searchable help FAB.

#### Main Library Screen: Clean, Adaptive Grid/List Layout
The home screen is a full-bleed Material 3 Scaffold with a Large Top App Bar (medium on compact phones) showing the app title/logo (a stylized open book icon with dynamic theming) and search/search-as-you-type field. A bottom navigation bar provides tabs: **Library** (default), **Discover** (OPDS catalogs), **History** (recent/progress stats), **Settings**. The library defaults to a responsive masonry grid (2-4 columns based on screen width, e.g., 3 on tablets), with each card featuring a rounded (16dp) cover thumbnail (auto-fetched from metadata or generated via device ML), progress arc (circular determinate behind cover), title/author in dynamic type (headline small/medium), series/tag chips, and file size/format badge. Long-press enables multi-select mode: selected cards gain tonal overlay (elevated 4dp with scrim), a floating toolbar slides in from the bottom-right (fade-scale animation) with options like Delete, Share (via Intent), Add to Collection (modal sheet), or Export Highlights. Swipe-to-dismiss for previews; horizontal swipe on cards reveals quick actions (Resume, Mark Finished, Details). Empty state: Hero illustration of stacked books with overlay text "Your library awaits" and two large outlined buttons—"Add Local Books" (SAF picker) and "Browse OPDS Catalogs"—pulsing with gentle scale animation. Overflow menu (3-dot icon in app bar) offers Sort (by Title/Author/Recent/Progress/Size), View (Grid/List/Compact), Import Folder (persistent SAF), and Backup Library (to SAF/export JSON+images). Behavior: Grid items animate in sequentially on scroll (staggered fade from bottom), search filters live with debounce (300ms), and pulling down refreshes metadata/covers.

```
┌─────────────────────────────────────┐  ← Large Top App Bar (expands on scroll)
│ 📖 Codex          🔍 [Search...] ⋮ │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────┐ ┌─────────────┐   │  ← Masonry Grid (adaptive columns)
│  │   [Cover]   │ │   [Cover]   │   │
│  │ Progress    │ │ Progress    │   │     Cards: Rounded 16dp, elevation 2dp
│  │ Arc         │ │ Arc         │   │     Long-press: Tonal select overlay
│  │ Title       │ │ Title       │   │
│  │ Author      │ │ Series #1   │   │
│  └─────────────┘ └─────────────┘   │
│                                     │
├─────────────────────────────────────┤  ← Bottom Nav: Library | Discover | Reading | Settings
│ Library  Discover  History  Settings│
└─────────────────────────────────────┘
```

#### Adding Books: Frictionless Local and OPDS Workflows
**Local Books (SAF-First):** From Library or overflow menu, tap "Add Local Books" → Full-screen SAF picker opens (slide-up modal, no back gesture interrupt). Users grant persistent permission to folders (e.g., /storage/emulated/0/Books); app scans recursively (progress indicator with cancel), extracts metadata (async, with thumbnail placeholders), and imports to internal DB (Room SQLite for speed/offline). Multi-select files/folders; auto-detect duplicates by hash. Confirmation sheet slides up: "X books added. View now?" with grid preview. Imported books fade in to library with hero animation from FAB.

**OPDS Sources:** Discover tab shows a searchable list of pre-configured catalogs (e.g., Standard Ebooks, Mobilism mirrors, Project Gutenberg) + custom add button. Tap catalog → Root-level RecyclerView loads (shimmer placeholders fade to content), with facets/facets (authors/series/genres) as expandable chips. Drill-down: Book list with covers/descriptions, "Download" FAB downloads in foreground service (notification with progress/pause). Authenticated catalogs prompt for basic/ OAuth login (saved encrypted). Downloads save to app's scoped storage (/Android/data/.../Downloads), auto-imported. Behavior: Infinite scroll with prefetch, error states (network/offline) with retry button, pull-to-refresh syncs catalog.

Workflow: Library → Overflow → Import Folder → SAF → Scan → Auto-import → Library updates with entrance animation.

#### Reader Screen: Immersive, Gesture-Heavy Fullscreen Experience
Entering a book: Hero animation scales cover to fullscreen background (blur + scrim), text fades in center. Fullscreen immersion: Hide system bars (edge gesture exit), status/progress bar at bottom (Reader Bar: thin Material 3 bar with sliders/buttons that slides up on tap/swipe). Core layout: Scrollable text container with configurable viewport margins (0-20%). Page turns: Right-edge swipe → curl animation (ripple from touch point, customizable to slide/fade/instant); volume keys default to next/prev (remappable). Double-tap centers/zooms (PDF/comics). Quick settings overlay (top-right corner tap): Semi-transparent Material Card slides down (overlap no-clip), with sliders for Font Size (12-48pt), Brightness (0-100% + auto), Themes (10+ dynamic + sepia/night/twilight), and toggle chips (Bold/Italic/Hyphenation/Justify). Persistent across books but per-book overrides saved.

**Customizable Reading Settings:** Accessed via Reader Bar → Gear icon → Full bottom sheet (expandable to fullscreen on tablets). Deep customization in Material 3 groups:
- **Typography:** 50+ Google Fonts (downloadable) + custom TTF/OTF import; Size (pt/em), Line Height (1.0-2.5x), Paragraph Spacing (0-2em), Letter Spacing (kerning -0.1 to 0.2em), Word Spacing.
- **Layout:** Margins (px all sides), Indent First Line, Page Width (screen-fit/columnar), Dual-Page (landscape).
- **Colors:** 20+ palettes (dynamic app tie-in + presets: Cream/Parchment/Onyx); Foreground tint, Background gradient/image (sepia textures), Highlight colors (6 swatches).
- **Advanced:** Auto-scroll speed slider (pixel/line/page modes), TTS voice/speed (system + offline engines), Hyphenation dicts (24 langs), RTL flip.
Changes preview live (split-view on tablets). Save as Global/Default or Per-Book (auto-save on exit).

```
Reader Fullscreen (tap to show bar):
[Text Content... Margins: Configurable padding]
Swipe ↓ to show Reader Bar:
─────────────────────────────  ← Thin bar (24dp height, fade up)
< Progress Slider > Ch. 5 23% ▶ TTS ⚙ Settings 📚 TOC
─────────────────────────────
Quick Settings Overlay (slide down):
┌─────────────────────────┐
│ Font: Roboto 18pt ●     │  ← Sliders + chips, tonal elevation
│ Line Ht: 1.5x ■■■□□     │
│ Brightness: 75% ■■■■■□  │
│ Theme: Twilight ○       │
└─────────────────────────┘
```

**In-Book Menus/Overlaps:** Menu (Reader Bar ⋮) → Bottom sheet with Bookmarks (list + add), Highlights/Notes (color-coded list, export TXT/MD), Dictionary (tap word → popup tooltip, long-press → full def/share), Search (global/in-book, regex). Multi-select highlights via long-press (rubber-band marquee overlaps text). TOC sidebar slides from left (gesture or button, overlaps 80% with scrim).

#### App Settings: Hierarchical, Searchable Sheet
Bottom Nav → Settings tab: Vertical list of Material 3 Cards/Groups. Searchable (type-filter), with toggles/sliders. Sections: **Appearance** (Dynamic Theme picker with live preview, Corner Radius 0-24dp, Anim Speed), **Library** (Auto-scan intervals, Cover Quality, Sort Defaults), **Reading** (Gesture Map: 24 zones assignable to 15 actions like Night Toggle/Page/Bookmark/TTS), **OPDS** (Custom Catalogs list/add, Auth cache), **Advanced** (TTS Engine, Backup to Drive/SAF, Stats Export CSV/JSON, Widget Config). App themes fully separate: e.g., Teal app UI with Onyx reading night mode. Primary settings here; in-book quick menu links to subset.

#### Other Popular & Requested Features
- **Library Management:** Collections (custom shelves), Tags (multi-add, color-coded chips), Series auto-grouping (progress per series), Multi-select bulk ops (delete/export/move), Recent/Reading/Finished filters. Books from OPDS can include metadata for things like collections, tags, series, etc. This information should be stored in local DB for sorting and filtering. Users should also be able to edit existing metadata values or add their own tags and similar. There should be a user-prompted option for overwriting existing books' metadata from OPDS (triggered on OPDS sync, etc.) 
- **Progress & Stats:** Reading time tracker (session/total), Pace graphs (bar charts), Goals (pages/day), Export reports. Mihon utilizes this - reference Mihon for implementation
- **TTS & Accessibility:** Full-system TTS (pause/resume/speed/skip sentences), Musician mode (auto-scroll w/ BPM tap), Screen reader optimized labels.
- **Annotations:** Infinite highlights/notes (searchable/export/share), PDF markup (draw/highlight/fill forms via MuPDF).
- **Widgets/Home Shortcuts:** Resizable bookshelf widget (4-20 books, tappable), Book shortcuts (progress badge), Quick resume grid.
- **Search/Discovery:** Library-wide full-text (fuzzy/regex), OPDS facets, "Discover Similar" via metadata genres.
- **Sync/Backup:** Per-book JSON export (positions/highlights), OPDS server push (Calibre integration), optional Drive sync (opt-in).
- **Creative Extras:** E-ink mode (no animations, grayscale), Focus Ruler (vertical highlight bar, 6 styles), Custom CSS injection (EPUB), Night Shift filter (blue-light reduction), Multi-window (split-screen on tablets).

Behavior edge cases: Selections overlap reader with dim scrim (tap outside dismiss); menus slide/fade without blocking gestures; foldables auto-reflow dual-page on unfold. Performance: 60fps scrolls (is 120fps possible/realistic for phones/screens that support it?), lazy metadata, DB indexes for 100k+ books. Privacy: No trackers, local-first, opt-in analytics.

OTHER NOTES: This should be considered a revision and, most importantly, and improvement to the existing codex application to add functionality (comic reading + management) and improve UI/UX where possible. Aside from the changes to workflow for adding local books (aka no more pit stop under the Catalogs > Local tab), features, design, and functionality should be added and integrated into codex, but we shouldn't be removing much from codex.

LOCAL PATHS:
- Codex: ~/dev/codex/
- Mihon: ~/dev/git/Mihon/

Prioritize: Format detect (EPUB=Reflow, CBZ=Pager), Calibre comics OPDS, TB perf (Room indexes/Paging3), Codex OPDS integration.
