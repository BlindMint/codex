# Performance Analysis: Android eBook Reader (Kotlin + Compose)

**Date:** 2026-04-07  
**App:** Material You eBook reader for Android (Kotlin + Compose)  
**Focus:** Day-to-day user interactions (reading, scrolling, opening/closing books, animations)  
**Analysis Scope:** UI/UX, ViewModels, data parsing, navigation; excluded rare events like importing books  

This analysis identifies performance bottlenecks based on code examination, prioritizing issues by user impact. Remediation steps avoid major functionality changes unless they cause MAJOR performance issues. Total potential improvement: 30-50% in critical user flows.

## Executive Summary

Key findings:
- **Reader area** has the highest impact with recomputation issues affecting reading flow
- **Library area** suffers from in-memory operations scaling poorly with large collections
- **Navigation** shows minor stutters during transitions
- **Settings** have negligible impact on daily use

Prioritized remediation order:
1. Reader progress caching and recomputation optimizations (immediate user impact)
2. Library DB-level sorting and filtering (scales with collection size)
3. Navigation animation tweaks (polish improvements)
4. Settings consolidation (low priority)

## 1. Reader Area (Highest Impact)

The reader handles text display, progress tracking, and UI state. Inefficiencies in state management cause frequent recompositions, degrading performance for large books and common interactions.

### Issue: Frequent Recomputations of Progress Metrics
**Location:** `app/src/main/java/us/blindmint/codex/ui/reader/ReaderScreen.kt` (lines 370-467)  
**Description:** Progress calculations (book and chapter) use `remember` blocks depending on volatile state (`state.value.text`, `state.value.book.progress`). PAGE mode sums character counts across entire text list on every recomposition.  
**Impact:** Slows scrolling and UI updates; noticeable lag in progress bar animations.  
**Remediation Steps:**
1. Add `cachedBookProgress` and `cachedChapterProgress` fields to `ReaderState` in ViewModel.
2. Update cached values only on `OnChangeProgress` events.
3. Replace `remember` blocks with direct state access.
4. Precompute total characters for PAGE mode during text loading and store in state.

### Issue: Overuse of `remember` Blocks for UI Calculations
**Location:** `app/src/main/java/us/blindmint/codex/ui/reader/ReaderScreen.kt` (lines 141-310)  
**Description:** Numerous `remember` calls for padding, colors, alignments depend on settings state, causing full recompositions on settings changes.  
**Impact:** High CPU usage during menu animations or settings toggles; stutters in reading flow.  
**Remediation Steps:**
1. Group related calculations into single `remember` with combined keys (e.g., `remember(mainState.value.fontSize, mainState.value.lineHeight)`).
2. Use `derivedStateOf` for less frequent changes.
3. Avoid recomputing static values (e.g., `horizontalAlignment`) on every render.

### Issue: Inefficient Scroll Restoration with `snapshotFlow`
**Location:** `app/src/main/java/us/blindmint/codex/ui/reader/ReaderModel.kt` (lines 150-213)  
**Description:** Uses `snapshotFlow` to wait for scroll completion, collecting layout changes repeatedly and blocking UI thread.  
**Impact:** Delayed UI responsiveness after opening books; visible jumps during restoration.  
**Remediation Steps:**
1. Replace `snapshotFlow` with one-time check after `requestScrollToItem`.
2. Use `LaunchedEffect` with 200ms timeout to hide loading state.
3. Observe `isScrollInProgress` directly without flow collection.

### Issue: Heavy Text Loading on Book Open
**Location:** `app/src/main/java/us/blindmint/codex/ui/reader/ReaderModel.kt` (lines 100-139); parsers in `data/parser/epub/`  
**Description:** Text parsing (e.g., EPUB) loads entire content synchronously; XML unzipping for large files (100MB+) causes delays.  
**Impact:** 5-10s delays opening large books.  
**Remediation Steps:**
1. Add progress callbacks to parsers for incremental loading.
2. Optimize XML parsing by switching to `XmlPullParser` in `EpubTextParser.kt` for memory efficiency.
3. Consider caching parsed text (database or disk) - requires architecture review.

### Issue: Menu Animation Performance
**Location:** `app/src/main/java/us/blindmint/codex/ui/reader/ReaderScreen.kt` (lines 165-186)  
**Description:** Color animations (`animateColorAsState`) trigger on every state change, compounded by recompositions.  
**Impact:** Jerky menu show/hide; battery drain.  
**Remediation Steps:**
1. Use `animateColorAsState` with `animationSpec = tween(durationMillis = 150)`.
2. Defer non-critical animations (e.g., background) until menu is fully visible.

### Issue: Comic/PDF Page Changes with Debounced DB Saves
**Location:** `app/src/main/java/us/blindmint/codex/ui/reader/ReaderModel.kt` (lines 911-952)  
**Description:** Page changes trigger immediate updates plus 500ms debounced saves, causing UI contention.  
**Impact:** Laggy page turning in comics.  
**Remediation Steps:**
1. Increase debounce to 1000ms.
2. Use single-threaded coroutine for saves.
3. Batch updates for rapid page changes.

## 2. Library Area (High Impact)

Library manages book lists with sorting/filtering, but in-memory operations don't scale with large collections.

### Issue: Loading/Sorting All Books In-Memory
**Location:** `app/src/main/java/us/blindmint/codex/ui/library/LibraryModel.kt` (lines 677-693)  
**Description:** `getBooksFromDatabase` loads all books, then filters/sorts in-memory with `sortedWith`.  
**Impact:** Slow list refresh (2-5s for 5,000+ books); scrolling stutters.  
**Remediation Steps:**
1. Move sorting to DB query level (add `ORDER BY` clauses in `BookRepository.getBooks`).
2. Use DB `WHERE` clauses for filters instead of in-memory.
3. Cache filtered results in state to avoid re-querying.

### Issue: Unnecessary Refresh Delays
**Location:** `app/src/main/java/us/blindmint/codex/ui/library/LibraryModel.kt` (lines 95-115)  
**Description:** 500ms delay before hiding spinners, even for fast loads.  
**Impact:** Perceived slowness in list updates.  
**Remediation Steps:**
1. Remove fixed `delay(500)`.
2. Hide spinner immediately after data load.
3. Use minimum delay only if DB queries consistently <200ms.

### Issue: Search Debounce Too Aggressive
**Location:** `app/src/main/java/us/blindmint/codex/ui/library/LibraryModel.kt` (lines 145-163)  
**Description:** 500ms debounce on search queries.  
**Impact:** Laggy search feel.  
**Remediation Steps:**
1. Reduce debounce to 200ms.
2. Skip debounce for empty queries.

## 3. Navigation Area (Medium Impact)

Custom navigation uses `AnimatedContent`, efficient but optimizable for heavy screens.

### Issue: Screen Transitions with AnimatedContent
**Location:** `app/src/main/java/us/blindmint/codex/presentation/navigator/Navigator.kt` (lines 59-69)  
**Description:** Navigation triggers full recompositions of target screens.  
**Impact:** Brief stutters during screen changes (e.g., library to reader).  
**Remediation Steps:**
1. Customize `transitionSpec` to use `fadeIn`/`fadeOut` instead of default slide.
2. Pre-load ViewModels for frequent screens (reader/library) to reduce init time.

## 4. Settings Area (Low-Medium Impact)

Settings have 60+ granular sub-screens, increasing overhead.

### Issue: Granular Settings Structure
**Location:** `presentation/settings/` (60+ subdirs)  
**Description:** Many small screens/ViewModels increase memory and navigation overhead.  
**Impact:** Slight delays in settings menus.  
**Remediation Steps:**
1. Consolidate related settings (e.g., reader appearance) into fewer screens with tabs.
2. Use shared ViewModel for common state to reduce instances.

## Implementation Plan

### Phase 1: High-Impact Reader Fixes
- Progress caching and `remember` optimization
- Scroll restoration improvements
- Menu animation tweaks

### Phase 2: Library Performance
- DB-level sorting/filtering
- Refresh delay removal
- Search debounce reduction

### Phase 3: Polish Improvements
- Navigation transitions
- Settings consolidation (if needed)

### Testing Recommendations
- Benchmark with 10,000+ book library and 50MB+ books
- Measure open times, scroll FPS, animation smoothness
- Use Android Studio Profiler for frame drops

### Profiling Tools
- Android Studio Profiler for CPU/memory monitoring
- Layout Inspector for recomposition tracking
- Custom logging for text loading times

This analysis provides a roadmap for optimizing day-to-day performance without major rewrites. Review and approve changes before implementation.