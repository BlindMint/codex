# PDF Custom Fitz Renderer Plan

Date: 2026-04-01
Branch: `pdf-custom-fitz-renderer`
Status: In progress

## Goal

Replace the current fixed-scale PDF image pipeline with a dedicated low-level MuPDF `fitz` renderer that can match or exceed Librera for text clarity while preserving Codex reader behavior:

- tap menus
- tap or swipe navigation
- long-press text selection
- invert colors
- page progress and scroll restoration
- free pinch zoom with automatic horizontal lock while zoomed

This document is both the implementation plan and the working progress log.

## Current State

Current PDF rendering is implemented in `app/src/main/java/us/blindmint/codex/presentation/reader/PdfReaderLayout.kt` and routes rendered page bitmaps through `app/src/main/java/us/blindmint/codex/presentation/reader/ImageBasedReaderLayout.kt`.

Known limitations:

- fixed `RENDER_SCALE = 2.0f`
- whole-page bitmap rendering instead of viewport-aware rendering
- Compose image scaling after rasterization
- no PDF-specific tile pipeline
- no rerender-at-final-zoom behavior
- no PDF-specific layout or gesture state model

## Non-Goals

- Do not overhaul comic rendering at this time.
- Do not share rendering internals with comics unless a clean abstraction emerges naturally.
- Do not optimize for fastest delivery over quality; optimize for highest reasonable quality and long-term maintainability.

## High-Level Architecture

### 1. PDF Engine

Low-level wrapper around MuPDF `fitz` APIs.

Responsibilities:

- open and close PDF documents
- page count and page geometry
- page rendering at exact requested scale or region
- structured text extraction and hit-testing support
- normalized coordinate mapping for overlays and selection

### 2. PDF Render Controller

Owns render scheduling and cache policy.

Responsibilities:

- visible region calculation
- tile prioritization
- cancellation of stale requests
- preview vs refined render passes
- bitmap cache eviction

### 3. PDF Reader State

Compose-facing state for:

- current page
- viewport size
- zoom and pan
- auto horizontal lock
- selected text and handle positions
- current visible tiles

### 4. PDF Reader UI

Dedicated Compose reader surface for PDFs.

Responsibilities:

- draw page previews and tiles
- draw selection overlays
- handle taps, drags, pinch gestures, and long press
- preserve menu and page navigation behavior

## Phase Plan

## Phase 0 - Research and Technical Validation

Purpose:

- confirm the exact `fitz` APIs available in the selected MuPDF version
- decide whether official Java bindings are sufficient or whether a small JNI helper is needed
- define the benchmark PDFs used to compare against Librera

Tasks:

- [ ] Inspect official MuPDF Java binding support for page geometry, partial rendering, structured text, and links.
- [ ] Decide whether to keep only `fitz` or introduce a minimal JNI bridge for missing render controls.
- [ ] Build a benchmark set of PDFs for visual comparison and regression checking.
- [ ] Record any API limitations that affect tiling or text selection.

Notes:

- 2026-04-01: Prior inspection shows Codex currently uses `com.artifex.mupdf:fitz:1.15.+` and `com.artifex.mupdf:viewer:1.15.+`.
- 2026-04-01: Librera uses a lower-level custom integration and exposes anti-alias controls in its own native bridge.

## Phase 1 - Dependency Strategy and Baseline PDF Engine

Purpose:

- move PDF rendering toward low-level `fitz`
- establish Codex-owned PDF abstractions before replacing the UI path

Tasks:

- [ ] Pin MuPDF dependency versions instead of using `+`.
- [ ] Evaluate upgrading MuPDF to a newer line before deeper implementation.
- [ ] Create a `pdf` package structure for engine, render, model, and state classes.
- [ ] Implement a document wrapper that opens, closes, and counts pages safely.
- [ ] Implement page geometry access for width, height, and bounds.
- [ ] Implement a first Codex-owned full-page render path using `fitz` directly.

Progress log:

- 2026-04-01: Branch `pdf-custom-fitz-renderer` created.

## Phase 2 - Dedicated PDF Reader Layout

Purpose:

- stop routing PDFs through `ImageBasedReaderLayout`
- create a PDF-specific reader surface and state model

Tasks:

- [ ] Replace the PDF internals of `PdfReaderLayout.kt` with a dedicated layout implementation.
- [ ] Keep current external callbacks stable where possible.
- [ ] Support paged horizontal and continuous vertical PDF modes.
- [ ] Maintain page reporting, page indicator, loading complete, and scroll restoration callbacks.
- [ ] Preserve menu toggle and edge tap navigation behavior.

## Phase 3 - Viewport-Aware Whole-Page Rendering

Purpose:

- eliminate fixed render scale
- render according to the actual display size and current zoom

Tasks:

- [ ] Replace hardcoded render scale with viewport-derived target resolution.
- [ ] Render page previews sized to actual on-screen need.
- [ ] Rerender visible pages after zoom settles.
- [ ] Ensure no visual dependence on post-raster upscale for readable text.

## Phase 4 - Zoom Model and Automatic Horizontal Lock

Purpose:

- support seamless pinch zoom while maintaining stable vertical reading when zoomed

Tasks:

- [ ] Introduce explicit PDF zoom and pan state.
- [ ] Compute `baseFitScale` per page or viewport mode.
- [ ] Enable free pinch zoom and spread gestures.
- [ ] Automatically activate horizontal lock once zoom exceeds fit-width by threshold.
- [ ] Clamp or settle horizontal position after pinch without abrupt behavior.
- [ ] Keep vertical reading smooth while zoomed.

Behavior target:

- pinch to any zoom level
- no extra lock button required
- if zoomed in, sideways drift is suppressed while vertical reading continues
- changing zoom updates lock behavior seamlessly

## Phase 5 - Tile Rendering Pipeline

Purpose:

- achieve crisp text at high zoom without storing enormous full-page bitmaps

Tasks:

- [ ] Introduce a tile key model based on page, zoom bucket, tile bounds, and render mode.
- [ ] Render only visible tiles plus a small prefetch margin.
- [ ] Keep a lower-resolution page preview visible while refined tiles load.
- [ ] Add tile cache eviction and stale render cancellation.
- [ ] Tune tile size for memory and visual stability.

## Phase 6 - Text Selection and Hit Testing

Purpose:

- preserve and improve PDF text interaction while using a dynamic render pipeline

Tasks:

- [ ] Extract structured text in page coordinates.
- [ ] Map viewport touches to page coordinates independent of bitmap size.
- [ ] Restore long-press word lookup.
- [ ] Add selection overlay rendering in PDF coordinate space.
- [ ] Add drag handles for selection expansion.
- [ ] Keep paragraph or line context extraction for `ReaderEvent.OnTextSelected`.

## Phase 7 - Margin Detection and Crop-Aware Fit

Purpose:

- increase effective text size and improve readability on PDFs with large margins

Tasks:

- [ ] Add optional automatic margin detection.
- [ ] Support document-wide and page-specific crop bounds.
- [ ] Use content bounds for fit-width calculations when enabled.
- [ ] Validate against PDFs with uneven margins.

## Phase 8 - Color, Invert, and Render Variants

Purpose:

- preserve current reader appearance settings without compromising text sharpness

Tasks:

- [ ] Integrate invert mode into the PDF render pipeline and cache keys.
- [ ] Ensure overlays and selection remain legible in inverted mode.
- [ ] Evaluate whether advanced contrast or theme transforms belong in the render stage.

## Phase 9 - Performance, Caching, and Stability

Purpose:

- make the new renderer reliable for long reading sessions

Tasks:

- [ ] Add LRU bitmap cache with explicit memory budget.
- [ ] Cache visible and adjacent page geometry and text.
- [ ] Cancel obsolete render jobs on scroll and zoom changes.
- [ ] Add basic instrumentation logging for render latency and cache hit rate.
- [ ] Tune for large PDFs and high-density devices.

## Phase 10 - Verification and Librera Parity

Purpose:

- verify that the new renderer achieves the intended quality improvements

Tasks:

- [ ] Compare benchmark PDFs side-by-side with Librera on both target devices.
- [ ] Check fit-width clarity, 125/150/200 percent zoom clarity, and small serif readability.
- [ ] Verify vertical scrolling stability while zoomed.
- [ ] Verify text selection accuracy near small words and line edges.
- [ ] Verify memory behavior and absence of heavy flashing.

## Implementation Notes

### Existing code to replace or isolate

- `app/src/main/java/us/blindmint/codex/presentation/reader/PdfReaderLayout.kt`
- PDF usage of `app/src/main/java/us/blindmint/codex/presentation/reader/ImageBasedReaderLayout.kt`

### Existing code to preserve contract with

- `app/src/main/java/us/blindmint/codex/presentation/reader/ReaderLayout.kt`
- `app/src/main/java/us/blindmint/codex/ui/reader/ReaderModel.kt`
- `app/src/main/java/us/blindmint/codex/ui/reader/ReaderState.kt`
- `ReaderEvent.OnTextSelected`

## Progress Log

- 2026-04-01: Created branch `pdf-custom-fitz-renderer`.
- 2026-04-01: Added this implementation plan and progress tracker.
- 2026-04-01: Started Phase 1 with a new PDF package split:
  - `app/src/main/java/us/blindmint/codex/pdf/engine/`
  - `app/src/main/java/us/blindmint/codex/pdf/model/`
  - `app/src/main/java/us/blindmint/codex/pdf/state/`
- 2026-04-01: Added an initial `PdfDocumentSession` wrapper around low-level `fitz` document, page geometry, preview rendering, and structured-text hit testing.
- 2026-04-01: Replaced PDF use of `ImageBasedReaderLayout` with a dedicated `PdfReaderLayout` implementation.
- 2026-04-01: Added the first PDF-specific UI state for viewport, zoom, pan, and automatic horizontal lock activation when zoom exceeds the base threshold.
- 2026-04-01: Implemented first-pass viewport-aware rerendering based on actual container size instead of a fixed constant render scale.
- 2026-04-01: Preserved existing external callbacks for page changes, loading complete, scroll restoration, page selected, menu toggle, and text selection.
- 2026-04-01: Added first-pass paged tap-zone behavior and verified the refactor compiles with `./gradlew :app:compileDebugKotlin`.
- 2026-04-02: Fixed a recycled-bitmap crash caused by replacing cached page bitmaps while Compose still held the old `ImageBitmap` wrapper.
- 2026-04-02: Added bitmap version tracking so PDF page images are recreated when a page rerenders.
- 2026-04-02: Reduced unintended vertical-mode interference by separating zoom gesture state from vertical list scrolling and adding a dedicated zoomed drag path for vertical mode.
- 2026-04-02: Tightened first-pass gesture finalization so horizontal lock settles `panX` after zoom gestures.
- 2026-04-02: Recompiled successfully with `./gradlew :app:compileDebugKotlin` after the crash fix and gesture changes.
- 2026-04-02: User-side fixes addressed repeated recycled-bitmap and oversized bitmap runtime failures in the current branch state.
- 2026-04-02: Added a dedicated `PdfRenderController` and `PdfBitmapCache` as the first render-controller layer between the UI and `PdfDocumentSession`.
- 2026-04-02: Added `PdfRenderRequest` and `PdfRenderResult` models so render work is keyed by page, viewport, zoom, and render mode.
- 2026-04-02: Updated `PdfReaderLayout` to request renders through the new render controller rather than calling the PDF engine directly.
- 2026-04-02: Added `PdfPageLayout` and tile-request models to formalize page sizing, centering, and tile-ready visible-region calculation.
- 2026-04-02: Added first-pass pan clamping against viewport-aware page layout so zoomed pages stay within sensible bounds.
- 2026-04-02: Connected the reader to tile-ready visible-region priming through the render controller while still using whole-page preview rendering as the backing raster path.
- 2026-04-02: Recompiled successfully with `./gradlew :app:compileDebugKotlin` after page-layout and pan-bound changes.
- 2026-04-02: Added a first real tile bitmap path in `PdfDocumentSession.renderTile(...)`, currently implemented by cropping from a preview render rather than native partial rasterization.
- 2026-04-02: Extended `PdfRenderController` to cache and return tile bitmaps separately from whole-page previews.
- 2026-04-02: Updated `PdfReaderLayout` to draw page previews plus visible tile overlays.
- 2026-04-02: Added initial PDF selection state and highlight overlay groundwork using page-space bounds derived from MuPDF structured text.
- 2026-04-02: Recompiled successfully with `./gradlew :app:compileDebugKotlin` after tile and selection-overlay groundwork.
- 2026-04-02: Added conservative crop-bound estimation and crop-aware page layout support as groundwork for margin detection and fit-width improvement.
- 2026-04-02: Added render-cache instrumentation counters to `PdfRenderController` for preview/tile hit-miss logging.
- 2026-04-02: Audited resolved MuPDF runtime dependencies and confirmed Gradle was resolving `com.artifex.mupdf:viewer` and `com.artifex.mupdf:fitz` to `1.15.1`.
- 2026-04-02: Replaced wildcard MuPDF dependency declarations with pinned `1.15.1` versions in `app/build.gradle.kts`.
- 2026-04-02: Investigated current binding capabilities and found no confirmed local evidence yet of a straightforward Java-only partial-region raster API in the currently resolved integration path; tile rendering remains structured for a future lower-level MuPDF path.
- 2026-04-02: Cloned `mupdf` tag `1.27.2` locally to inspect the newer Java bindings and Android support directly from source.
- 2026-04-02: Confirmed `1.27.2` still ships Java bindings under `platform/java/src/com/artifex/mupdf/fitz/*`, including `DisplayList`, `Pixmap`, `Rect`, `RectI`, and `android/AndroidDrawDevice`.
- 2026-04-02: Confirmed the newer Java API exposes `Page.toDisplayList()` and `DisplayList.run(Device, Matrix, Rect, Cookie)`, which provides the missing scissored low-level render path needed for true tile rendering.
- 2026-04-02: Updated Codex tile rendering to use a display-list + scissor-device path compatible with the newer MuPDF Java API design, reducing dependence on full-page crop rendering.
- 2026-04-02: Built the MuPDF `1.27.2` Java bindings jar locally from source after patching the upstream Java make flags for modern JDK compatibility.
- 2026-04-02: Added a local `:mupdf-android-fitz` Android library module and imported the MuPDF `1.27.2` Java sources into the project.
- 2026-04-02: Replaced app dependency on `com.artifex.mupdf:viewer` and `com.artifex.mupdf:fitz:1.15.1` with the local `:mupdf-android-fitz` module as the first project-side step away from the old prebuilt artifact path.
- 2026-04-02: Verified Codex still compiles with the local MuPDF module via `./gradlew :app:compileDebugKotlin`.
- 2026-04-02: Installed/used local Android CLI tooling and successfully built MuPDF `1.27.2` Android native libraries with `ndk-build` for `arm64-v8a` and `x86_64`.
- 2026-04-02: Copied `libmupdf_java.so` into `mupdf-android-fitz/src/main/jniLibs/arm64-v8a/` and `mupdf-android-fitz/src/main/jniLibs/x86_64/`.
- 2026-04-02: Built a full debug APK successfully with `./gradlew :app:assembleDebug` using the local MuPDF `1.27.2` Java + native module.
- 2026-04-02: Android Gradle reported `libmupdf_java.so` could not be stripped automatically and packaged it as-is; this should be revisited during release/size/compliance validation.
- 2026-04-02: Device testing on the new runtime showed much better text quality but exposed interaction bugs: paged-mode zoom could not zoom back out reliably, paged navigation while zoomed was too easy to trigger, and vertical-mode zoom caused page overlap and instability.
- 2026-04-02: Fixed first-pass zoom interaction issues by allowing zoom below `1.0`, blocking paged edge navigation while zoomed, and reserving vertical list item height from the computed page layout so neighboring zoomed pages do not collapse into each other.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the zoom/layout fixes.
- 2026-04-02: Additional device testing found thin top/bottom artifact lines, occasional `Failed to render tile for page X`, persistent paged-mode zoom lock behavior, and remaining vertical-mode overlap/separation caused by mixing container scaling with independently laid-out pages.
- 2026-04-02: Updated tile request generation to follow the current viewport/pan window instead of always anchoring tiles at the page origin.
- 2026-04-02: Reworked tile rendering to use the tile rect directly as the display-list scissor/output region and added a preview fallback when a tile render returns null.
- 2026-04-02: Removed per-page content scaling inside `PdfPageItem`; the container now owns page size while pan translates content, which should eliminate the thin page-edge artifact lines and vertical per-item zoom mismatch.
- 2026-04-02: Disabled vertical page-progress updates while zoomed to avoid page snapping behavior in continuous mode.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the artifact/zoom fixes.
- 2026-04-02: Further device testing showed that the interim fix introduced stretched pages and a picture-in-picture zoom effect because preview content was being forced to fill container bounds while tiles were drawn with a different logical scale.
- 2026-04-02: Restored correct PDF aspect rendering by sizing vertical pages with `aspectRatio(...)`, returning the preview image to `ContentScale.Fit`, and making the page content container fill the allocated page bounds instead of width only.
- 2026-04-02: Suppressed tap handling while zoomed so gesture end-state does not accidentally behave like a tap/menu toggle.
- 2026-04-02: Tightened vertical programmatic scroll restoration so it does not fight user interaction while zoom gestures are active.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the aspect-ratio and snapping fixes.
- 2026-04-02: Additional device feedback showed the preview/tile mismatch was still visible because content was being fit inside the page box while tiles were placed as if they owned the full page-layout area.
- 2026-04-02: Adjusted the page content box to use the computed page layout offsets and aspect ratio directly, so preview, tiles, and selection overlays all share the same inner content bounds.
- 2026-04-02: Disabled pager and vertical list user scrolling while zoomed so pan gestures do not turn into page changes/snapping in zoomed states.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the content-box and scroll-lock fixes.
- 2026-04-02: Further device testing showed the menu could no longer toggle reliably and reading-mode changes were ignored after zooming because PDF reader state was not resetting when the mode changed.
- 2026-04-02: Reset PDF zoom/pan gesture state when `readingDirection` changes so switching between paged and vertical modes works after zooming.
- 2026-04-02: Unified preview, tile, and selection drawing inside one `matchParentSize()` content box so no layer can drift into a picture-in-picture scale relative to the others.
- 2026-04-02: Restored tap recognition regardless of zoom level so menu toggling works again while keeping pager/list scrolling disabled during zoomed states.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the mode-switch and content-layer fixes.
- 2026-04-02: Re-examined LibreraReader’s PDF implementation for zoom/layout behavior. Key findings:
  - Librera uses stable document/page bounds plus tile rerendering, not ad hoc per-page view scaling.
  - Vertical mode page positions come from a persistent cumulative layout (`VScrollController`) with fixed spacing, then zoomed bounds are derived from that shared model.
  - Overlays are recomputed in the same draw pass as page content (`EventDraw`), which avoids preview/overlay drift and picture-in-picture artifacts.
  - Current page/progress is derived from viewport visibility heuristics rather than snapping back to a page on every scroll event.
  - Movement lock is enforced at the gesture layer (`AdvGuestureDetector`) and should be mirrored in Codex’s pan/scroll model rather than mixed with pager/list snapping.
- 2026-04-02: Started Librera-inspired redesign of Codex PDF layout around stable document-space page bounds instead of `LazyColumn`/per-item zoom semantics.
- 2026-04-02: Replaced the previous PDF reader implementation with a first-pass document-space vertical stack and simplified paged viewport path, using explicit page bounds and current-page detection from viewport geometry rather than scroll snapping.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the Librera-inspired PDF layout rewrite.
- 2026-04-02: Device testing showed the first rewrite still mixed document-space geometry with per-page composable containers, so paged stretching, delayed zoom, and vertical jumpbacks persisted.
- 2026-04-02: Replaced that mixed approach with a more aggressive viewport-first rewrite: paged mode is now a single active-page surface with explicit neighbor preloading, vertical mode is a custom document-space stack over explicit page bounds, and pinch zoom now applies an immediate transient visual transform before rerender commit.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the viewport-first rewrite.
- 2026-04-02: The first viewport-first rewrite introduced a loading regression because document/session state and prepared layouts were being mutated from the background coroutine before the main-thread-visible state was reliably established.
- 2026-04-02: Fixed the loading regression by preparing MuPDF session data off the main thread, then publishing session/controller/page geometry/layout state back on the main thread in one step.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the loading regression fix.
- 2026-04-02: Debug logs showed the PDF session and first bitmaps were loading successfully while the UI still displayed a loading state, indicating the issue had shifted from session initialization to surface visibility/layout sizing.
- 2026-04-02: Adjusted the viewport-first page surface to size itself from explicit page width/height rather than inheriting `fillMaxSize()` blindly, so loaded bitmaps can actually become visible on screen.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the PDF surface visibility fix.
- 2026-04-02: Follow-up device logs confirmed the UI was still masking the PDF surface despite `isLoading=false`, `totalPages>0`, and rendered bitmaps being present.
- 2026-04-02: Removed the loading-screen branch once session initialization has completed and added explicit render-branch diagnostics so the PDF surface always composes after initialization instead of being hidden by the spinner.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the loading-mask fix.
- 2026-04-02: External reader-scaffold loading state was still the actual blocker: `ReaderModel` only hides the skull overlay after `OnComicScrollRestorationComplete`, and the rewritten PDF path had stopped sending the expected completion signals.
- 2026-04-02: Restored `onLoadingComplete()` and `onScrollRestorationComplete()` callbacks immediately after PDF session/layout initialization reaches the UI thread so the outer reader scaffold can dismiss the blocking loading overlay.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after restoring the outer reader completion callbacks.
- 2026-04-02: After loading was restored, paged mode still showed a large off-page placeholder region because the surface bounds and hit area were still wider than the actual page box.
- 2026-04-02: Fixed paged/vertical page surface sizing so the rendered surface itself matches the explicit page bounds, removed the fake drag-based tap shim, and restored explicit edge-tap navigation plus single-step paged swipes.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the paged surface bounds and gesture cleanup.
- 2026-04-02: Follow-up testing showed the first page/upper placeholder region was still being displaced because page layout offsets were being counted twice, and vertical pages were unloading too aggressively at the viewport edge.
- 2026-04-02: Removed double-counted outer page offsets, widened preload margins to roughly one viewport above/below the visible window, and changed paged swipes to accumulate distance and commit at drag end so one gesture can only move one page.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the preload and placement corrections.
- 2026-04-02: Further testing showed page surfaces were no longer displaced, but the actual page content was still being stretched because the image layer always filled whatever explicit box it received.
- 2026-04-02: Reconnected external `currentPage` changes to both paged and vertical viewport state, added a short paged transition animation, made vertical pages use viewport-width outer surfaces with explicit page heights, and restored aspect-correct page drawing inside those surfaces.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the aspect ratio and navigation-bridge fixes.
- 2026-04-02: Follow-up testing still showed stretched pages and vertical overlap/jumps, so the remaining issue was narrowed to stack math and page-tracking feedback rather than loading or gesture routing.
- 2026-04-02: Updated vertical pages to use their actual rendered width instead of forcing viewport width, suppressed current-page feedback while a transient zoom gesture is active, clamped vertical scroll to document bounds, and slightly lengthened the paged transition animation.
- 2026-04-02: Reverified compilation with `./gradlew :app:compileDebugKotlin` after the vertical stack and animation tuning.

## Phase Status Snapshot

- [ ] Phase 0 - Research and Technical Validation
- [~] Phase 0 - Research and Technical Validation
- [~] Phase 1 - Dependency Strategy and Baseline PDF Engine
- [~] Phase 2 - Dedicated PDF Reader Layout
- [~] Phase 3 - Viewport-Aware Whole-Page Rendering
- [~] Phase 4 - Zoom Model and Automatic Horizontal Lock
- [~] Phase 5 - Tile Rendering Pipeline
- [~] Phase 6 - Text Selection and Hit Testing
- [ ] Phase 7 - Margin Detection and Crop-Aware Fit
- [~] Phase 7 - Margin Detection and Crop-Aware Fit
- [ ] Phase 8 - Color, Invert, and Render Variants
- [ ] Phase 9 - Performance, Caching, and Stability
- [~] Phase 9 - Performance, Caching, and Stability
- [ ] Phase 10 - Verification and Librera Parity

## Known Gaps After Initial Refactor

- Current render path is still whole-page preview rendering, not tiles.
- Current render controller now caches whole-page preview renders, but tile requests are not implemented yet.
- Page layout and visible-region tile request models now exist, but tile rasterization still falls back to the whole-page render path.
- Tile overlays now render separately, but tile generation still uses cropped preview output rather than true MuPDF partial-region rasterization.
- Tile rendering now has a display-list/scissor-based implementation path in Codex, aligned with the newer `1.27.2` MuPDF Java API model, but the project is still linked against `1.15.1` until the dependency migration is completed.
- Horizontal paged tap navigation has a first pass, but still needs refinement and user-device validation.
- Zoom and pan are now PDF-specific, with first-pass pan clamping and bounds awareness, but settle behavior is still minimal.
- Long-press selection now has first-pass page-space highlight overlay support, but drag handles and selection expansion are not yet implemented.
- Margin detection now has conservative heuristic groundwork, but not real content analysis yet.
- Dependency version pinning and MuPDF upgrade evaluation are still pending.
- Dependency version pinning is now done for the current branch; MuPDF upgrade evaluation and 16 KB compliance remediation are still pending.
- Dependency version pinning is now done for the current branch; migration to `1.27.2` bindings and 16 KB compliance remediation are still pending.
- Dependency version pinning is done and the project now has a local `1.27.2` Java-module integration path, but native `1.27.2` Android `.so` packaging is still pending before the old runtime can be considered fully replaced.
- Dependency version pinning is done and the project now has a local `1.27.2` Java + Android native integration path for `arm64-v8a` and `x86_64`; additional ABIs and final release/compliance validation are still pending.
- Vertical-mode zoom interaction should now be more stable, but still needs polish because current implementation is intentionally conservative while the dedicated tile pipeline is pending.

## Immediate Next Steps

- [x] Write the first Codex-owned PDF engine classes.
- [x] Add a dedicated PDF state model for viewport, zoom, and page geometry.
- [x] Refactor `PdfReaderLayout.kt` away from `ImageBasedReaderLayout`.
- [ ] Clean up the first-pass PDF reader implementation and fix compile/runtime issues.
- [ ] Add correct paged tap-zone navigation and zoom-aware gesture arbitration.
- [~] Introduce render caching policy and prepare for tiles.
- [~] Add pan bounds and improved zoom settle behavior.
- [~] Add selection overlay rendering in PDF coordinate space.
- [~] Add margin/crop-aware fit groundwork.
- [~] Add basic render instrumentation and cache stats.
- [x] Pin MuPDF dependency versions instead of using `+`.
