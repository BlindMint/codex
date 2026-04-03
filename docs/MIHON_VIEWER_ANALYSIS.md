# Codex vs Mihon Reader Implementation Analysis

**Date:** 2026-04-03  
**Purpose:** Evaluate Mihon's viewer structure vs Codex's current implementation for paged and vertical/webtoon modes

---

## Executive Summary

Mihon and Codex use fundamentally different UI frameworks—**Mihon uses Android Views**, while **Codex uses Jetpack Compose**. A "migration" to Mihon's exact structure would require abandoning Compose for the viewer layer, essentially a partial rewrite. A "migration" of Mihon's patterns/concepts to Codex's existing Compose structure is more practical and recommended.

---

## 1. Architecture Comparison

### 1.1 UI Framework

| Aspect | Mihon | Codex |
|--------|-------|-------|
| **Framework** | Android Views (XML + ViewBinding) | Jetpack Compose |
| **Paged Mode** | `DirectionalViewPager` (ViewPager2 wrapper) | `HorizontalPager` (Compose) |
| **Vertical/Webtoon** | `RecyclerView` + `WebtoonLayoutManager` | `LazyColumn` + `WebtoonRecyclerView` |
| **Image Zoom** | `SubsamplingScaleImageView` (library) | Custom `ZoomableImageView` (Android View) |
| **Entry Point** | `ReaderActivity` + `ReaderViewModel` | `ReaderScreen` + `ReaderModel` |

### 1.2 Key Structural Differences

**Mihon (View-based):**
```
ReaderActivity
├── ReaderViewModel
├── pager/
│   ├── PagerViewer (base)
│   ├── L2RPagerViewer / R2LPagerViewer / VerticalPagerViewer
│   ├── Pager (DirectionalViewPager)
│   ├── PagerPageHolder (ViewHolder per page)
│   └── PagerViewerAdapter
├── webtoon/
│   ├── WebtoonViewer
│   ├── WebtoonRecyclerView
│   ├── WebtoonAdapter
│   └── WebtoonLayoutManager
└── ReaderPageImageView (for rendering)
```

**Codex (Compose-based):**
```
ReaderScreen
├── ReaderModel (ViewModel)
├── ReaderContent
├── ReaderScaffold
├── ReaderLayout (routes to specific readers)
├── ImageBasedReaderLayout (comics: HorizontalPager/LazyColumn)
│   └── ZoomableImageView (Android View per page)
├── ComicReaderLayout (archive handling)
├── PdfReaderLayout (custom tile-based PDF)
│   ├── PdfDocumentSession (MuPDF)
│   └── PdfRenderController
└── TextReaderLayout (epub/text)
```

---

## 2. Detailed Feature Comparison

### 2.1 Paged Mode (Left-to-Right, Right-to-Left)

#### Mihon Approach
- **Location:** `viewer/pager/PagerViewers.kt` (lines 1-53)
- **Implementation:** Three separate classes (`L2RPagerViewer`, `R2LPagerViewer`, `VerticalPagerViewer`) extending `PagerViewer`
- **RTL Handling:** R2L swaps `moveToNext()` and `moveToPrevious()` methods
- **Pager:** Uses `DirectionalViewPager` (custom ViewPager2 wrapper) with `isHorizontal` flag
- **OffscreenPageLimit:** Set to 1 (only current + adjacent pages kept in memory)

```kotlin
// Mihon PagerViewers.kt:19-28
class R2LPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    override fun createPager(): Pager = Pager(activity)
    override fun moveToNext() = moveLeft()  // Swapped!
    override fun moveToPrevious() = moveRight()  // Swapped!
}
```

#### Codex Approach
- **Location:** `presentation/reader/ImageBasedReaderLayout.kt` (lines 113-125)
- **Implementation:** Single composable with `readingDirection` parameter
- **RTL Handling:** Explicit logical-to-physical page mapping functions

```kotlin
// Codex ImageBasedReaderLayout.kt:117-125
val mapLogicalToPhysicalPage = remember(isRTL, isVertical, totalPages) {
    { logicalPage: Int -> 
        if (isRTL && !isVertical && totalPages > 0) totalPages - 1 - logicalPage 
        else logicalPage 
    }
}
```

#### Analysis
| Aspect | Mihon | Codex |
|--------|-------|-------|
| RTL implementation | Class inheritance (swap methods) | Functional (page mapping) |
| Vertical mode | Separate `VerticalPagerViewer` class | Shares same composable with flag |
| Tap zones | `NavigationRegion` data class | Enum-based tap zone modes |
| Config extensibility | Subclass override pattern | Parameter/remember pattern |

**Mihon advantage:** Cleaner separation of concerns via inheritance  
**Codex advantage:** More flexible, single composable handles all modes

### 2.2 Vertical/Webtoon Mode

#### Mihon Approach
- **Location:** `viewer/webtoon/WebtoonViewer.kt` (365 lines)
- **Implementation:** RecyclerView with custom `WebtoonLayoutManager`
- **Key features:**
  - Extra layout space for preloading (`extraLayoutSpace = 500`)
  - Item prefetch disabled for memory management
  - Scroll listener for preloading previous chapters
  - Chapter transitions (prev/next) as special list items

```kotlin
// Mihon WebtoonLayoutManager.kt:16-28
class WebtoonLayoutManager(context: Context, private val extraLayoutSpace: Int) 
    : LinearLayoutManager(context) {
    init { isItemPrefetchEnabled = false }
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int = extraLayoutSpace
}
```

- **Scroll position restoration:** `scrollToPositionWithOffset()` in `moveToPage()`

#### Codex Approach
- **Location:** `presentation/reader/ImageBasedReaderLayout.kt` (lines 334-392)
- **Implementation:** `LazyColumn` with Compose state
- **Webtoon RecyclerView:** Separate `WebtoonRecyclerView` for zoom support
- **Key features:**
  - Shared zoom state across all pages (static companion object)
  - `ZoomableImageView` configured with `setVerticalMode(true)`
  - Manual scroll position tracking via `LazyListState`

```kotlin
// Codex ImageBasedReaderLayout.kt:340-350
LazyColumn(
    state = lazyListState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(...)
) {
    itemsIndexed((0 until totalPages).toList(), key = { _, page -> page }) { _, physicalPage ->
        val logicalPage = mapPhysicalToLogicalPage(physicalPage)
        // ZoomableImageView with vertical mode...
    }
}
```

#### Analysis
| Aspect | Mihon | Codex |
|--------|-------|-------|
| Layout | RecyclerView + custom LM | LazyColumn + optional WebtoonRecyclerView |
| Preloading | ExtraLayoutSpace + scroll listener | Compose prefetch (implicit) |
| Chapter transitions | List items with `ChapterTransition` | Not visible in current code |
| Zoom in webtoon | WebtoonRecyclerView (RecyclerView-based) | WebtoonRecyclerView + shared zoom |
| Memory management | `isItemPrefetchEnabled = false` | Compose lazy loading |

### 2.3 Image/Page Loading and Preloading

#### Mihon Approach
- **Page loading:** `PagerPageHolder` launches coroutines in supervisor scope
- **Status flow:** `page.statusFlow` and `page.progressFlow` for state updates
- **Preloading:** `HttpPageLoader.preloadNextPages()` with priority queue

```kotlin
// Mihon PagerPageHolder.kt:93-115
supervisorScope {
    launchIO { loader.loadPage(page) }
    page.statusFlow.collectLatest { state ->
        when (state) {
            Page.State.Queue -> setQueued()
            Page.State.LoadPage -> setLoading()
            Page.State.DownloadImage -> { setDownloading(); ... }
            Page.State.Ready -> setImage()
            is Page.State.Error -> setError(state.error)
        }
    }
}
```

```kotlin
// Mihon HttpPageLoader.kt:143-162
private fun preloadNextPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
    val pageIndex = currentPage.index
    val pages = currentPage.chapter.pages ?: return emptyList()
    if (pageIndex == pages.lastIndex) return emptyList()
    return pages.subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
        .mapNotNull { if (it.status == Page.State.Queue) PriorityPage(it, 0)... }
}
```

#### Codex Approach
- **Page loading:** `LaunchedEffect` with `Dispatchers.IO`
- **Preloading:** Manual `prefetchPages()` function with configurable window

```kotlin
// Codex ImageBasedReaderLayout.kt:127-137
suspend fun prefetchPages(currentPage: Int) {
    val physicalPage = mapLogicalToPhysicalPage(currentPage)
    for (offset in -PREFETCH_PAGES..PREFETCH_PAGES) {
        val targetPage = physicalPage + offset
        if (targetPage in 0 until totalPages && !loadedPages.containsKey(targetPage)) {
            withContext(Dispatchers.IO) {
                loadPage(mapPhysicalToLogicalPage(targetPage))
            }
        }
    }
}
```

- **Constants:** `MAX_CACHED_PAGES = 50`, `PREFETCH_PAGES = 5`

#### Analysis
| Aspect | Mihon | Codex |
|--------|-------|-------|
| Loading trigger | Flow collection | LaunchedEffect |
| Status tracking | Flow-based state | MutableState in composable |
| Preload window | 5 pages ahead | ±5 pages (±11 total) |
| Memory cache | Coil with MemoryCache | LinkedHashMap LRU (50 pages) |
| Bitmap recycling | Via Coil/Decoil | Manual `recycle()` on dispose |

### 2.4 Navigation Structure

#### Mihon Navigation
- **Tap zones:** Screen divided into regions (MENU, NEXT, PREV, LEFT, RIGHT)
- **Gesture handling:** `GestureDetectorWithLongTap` in Pager base class
- **Key events:** Volume keys, D-pad, page up/down support
- **Pan navigation:** `moveRight()`/`moveLeft()` with configurable steps

#### Codex Navigation
- **Tap zones:** 5 configurable modes (0-4)
- **Gesture handling:** Via `ZoomableImageView.GestureCallbacks`
- **Key events:** Not visible in current viewer code
- **Pan navigation:** Manual pan handling in PdfReaderLayout

### 2.5 PDF Rendering

#### Mihon
**No native PDF support.** PDFs are handled by extracting page images and rendering via `SubsamplingScaleImageView`.

#### Codex
**Custom MuPDF-based renderer** with tile-based rendering:

| Component | Location | Purpose |
|-----------|----------|---------|
| `PdfDocumentSession` | `pdf/engine/` | MuPDF document wrapper |
| `PdfRenderController` | `pdf/render/` | Rendering coordination |
| `PdfBitmapCache` | `pdf/render/` | LRU cache (12 entries) |
| `PdfPageLayout` | `pdf/model/` | Page geometry calculations |
| `PdfTileModels` | `pdf/model/` | Tile rendering requests |

**Tile rendering:** 768px tiles with 256px prefetch margin  
**Bitmap limit:** 96MiB per tile (safe limit)

---

## 3. Key Files Reference

### Mihon

| File | Lines | Purpose |
|------|-------|---------|
| `ui/reader/viewer/pager/Pager.kt` | 111 | Base ViewPager wrapper |
| `ui/reader/viewer/pager/PagerViewer.kt` | 455 | Base pager viewer, tap handling |
| `ui/reader/viewer/pager/PagerViewers.kt` | 53 | L2R/R2L/Vertical variants |
| `ui/reader/viewer/pager/PagerPageHolder.kt` | 311 | Page view holder |
| `ui/reader/viewer/pager/PagerViewerAdapter.kt` | 205 | ViewPager adapter |
| `ui/reader/webtoon/WebtoonViewer.kt` | 365 | Webtoon viewer |
| `ui/reader/webtoon/WebtoonAdapter.kt` | 200 | RecyclerView adapter |
| `ui/reader/webtoon/WebtoonLayoutManager.kt` | 52 | Custom layout manager |
| `ui/reader/webtoon/WebtoonRecyclerView.kt` | 358 | Custom RecyclerView with zoom |
| `ui/reader/ReaderActivity.kt` | 967 | Main reader activity |
| `ui/reader/ReaderViewModel.kt` | 988 | Reader state management |
| `ui/reader/loader/HttpPageLoader.kt` | 213 | Page loading with preload |

### Codex

| File | Lines | Purpose |
|------|-------|---------|
| `presentation/reader/ImageBasedReaderLayout.kt` | ~400 | Main comic pager |
| `presentation/reader/ComicReaderLayout.kt` | ~200 | Archive loading wrapper |
| `presentation/reader/PdfReaderLayout.kt` | ~750 | PDF with tile rendering |
| `presentation/reader/viewer/ZoomableImageView.kt` | ~500 | Custom zoom/pan view |
| `presentation/reader/viewer/webtoon/WebtoonRecyclerView.kt` | 356 | Webtoon zoom support |
| `ui/reader/ReaderScreen.kt` | ~400 | Main reader screen |
| `ui/reader/ReaderModel.kt` | ~500 | ViewModel |
| `pdf/engine/PdfDocumentSession.kt` | ~200 | MuPDF wrapper |
| `pdf/render/PdfRenderController.kt` | ~150 | Rendering coordinator |

---

## 4. Trade-off Analysis: Migration vs Complete Rewrite

### Option A: Migrate Mihon's Patterns to Codex (Recommended)

**What this means:**
- Keep Codex's Compose-based architecture
- Adopt Mihon's patterns for state management, preloading, and configuration
- Keep Codex's custom PDF renderer
- Possibly adopt `SubsamplingScaleImageView` or enhance `ZoomableImageView`

**Pros:**
- Preserves Compose benefits (type safety, composition, testing)
- Less risk than full rewrite
- Codex's PDF renderer is already superior to Mihon's (which has none)
- Faster implementation time
- Incremental improvement possible

**Cons:**
- Won't be identical to Mihon (Compose ≠ Views)
- Some patterns don't translate directly
- May need to maintain hybrid View/Compose in viewer layer

### Option B: Rewrite Codex Viewers to Match Mihon Structure

**What this means:**
- Replace Compose viewers with Android View-based implementation
- Use ViewPager2 and RecyclerView like Mihon
- Embed Compose only for surrounding UI (toolbars, dialogs)

**Pros:**
- Can potentially share more code with Mihon
- ViewPager2/RecyclerView are battle-tested
- `SubsamplingScaleImageView` handles edge cases well

**Cons:**
- Loses Compose benefits for the main viewer experience
- Significant rewrite with high risk
- PDF renderer integration becomes complex
- Time-consuming (weeks vs days for migration)
- Would need to maintain separate Compose and View code paths

---

## 5. Implementation Plan (Migration Approach)

### Phase 1: Architecture Unification
1. **Extract viewer interface** - Create common interface for pager behaviors
2. **Standardize page state model** - Align with Mihon's `Page.State` flow pattern
3. **Unify tap zone handling** - Extract to configuration-driven system

### Phase 2: Paged Mode Alignment
1. **Adopt Mihon's page state flow** - Replace mutable state with Flow-based state
2. **Enhance preloading** - Implement Mihon's priority-based preload queue
3. **Add chapter transitions** - Implement Mihon's transition pages as list items

### Phase 3: Webtoon Mode Alignment
1. **Adopt WebtoonLayoutManager pattern** - Add extraLayoutSpace for preloading
2. **Implement chapter preloading** - Mihon's scroll-based preload trigger
3. **Consider SubsamplingScaleImageView** - Wrapper for ZoomableImageView

### Phase 4: Memory Management
1. **Adopt Coil image pipeline** - Replace manual LinkedHashMap with Coil
2. **Standardize bitmap recycling** - Let Coil handle memory management
3. **Match Mihon's cache configuration** - MemoryCache.Builder pattern

### Phase 5: PDF Integration
1. **Keep MuPDF renderer** - Codex's advantage
2. **Adapt to new viewer architecture** - Wrap in same pager interface
3. **Enhance preload margin** - Match Mihon's chapter preload behavior

---

## 6. Recommendation

**Migration (Option A)** is recommended because:

1. **Compose is the right foundation** - Codex built its UI layer in Compose; discarding it for Views is step backward
2. **PDF is already superior** - Codex has native MuPDF support Mihon lacks
3. **Lower risk** - Migration can be incremental; rewrite is high-risk big-bang
4. **Faster delivery** - Migration weeks vs rewrite months
5. **Preserves custom work** - Codex's custom PDF renderer, ZoomableImageView are valuable

**What to migrate from Mihon:**
- Page state Flow pattern (statusFlow, progressFlow)
- Preloading strategy with configurable window
- Chapter transition handling
- WebtoonLayoutManager's extraLayoutSpace pattern
- Tap zone configuration structure
- Volume/key navigation handling

**What to keep from Codex:**
- Compose-based architecture
- Custom PDF renderer with MuPDF
- ZoomableImageView (or wrap SubsamplingScaleImageView)
- Logical/physical page mapping approach
- Custom tap zone handling

---

## 7. Decision: Migration Approach

**User confirmed: Migration (Option A)** - Adopt Mihon's patterns within Codex's Compose architecture.

**Constraints:**
- Keep Codex's Compose-based architecture
- Keep the custom MuPDF PDF renderer
- Incremental improvement preferred over big-bang rewrite

---

## 8. Detailed Implementation Plan

### Phase 1: State Management Alignment

**Goal:** Adopt Mihon's Flow-based page state pattern

| Step | Task | Files to Modify | Mihon Reference |
|------|------|----------------|------------------|
| 1.1 | Create `ReaderPageState` sealed class mirroring Mihon's `Page.State` | New file in `domain/` or `ui/reader/` | `ui/reader/model/ReaderPage.kt` |
| 1.2 | Convert `ImageBasedReaderLayout` page state to Flow | `presentation/reader/ImageBasedReaderLayout.kt` | `PagerPageHolder.kt:93-115` |
| 1.3 | Add `progressFlow` for download progress | `ImageBasedReaderLayout.kt` | `PagerPageHolder.kt:97` |
| 1.4 | Add chapter transition states | `ImageBasedReaderLayout.kt` | `PagerViewerAdapter.kt:94-145` |

**COMPLETED: Phase 1 implemented:**
- `ReaderPageState.kt` created with sealed interface matching Mihon's Page.State
- `StateHolder` class for reactive state management
- `ReaderPage` class with stateFlow and progressFlow
- `ChapterTransition` sealed class for prev/next chapter handling
- `ImageBasedReaderLayout.kt` updated with page state tracking
- Page loading now updates `pageStates[physicalPage]` with Loading/Ready/Error states
- Added `EXTRA_LAYOUT_SPACE = 500` constant for webtoon preloading
- DisposableEffect properly clears pageStates and pageProgress on dispose

**Mihon state pattern implemented:**
```kotlin
sealed interface ReaderPageState {
    data object Queued : ReaderPageState
    data object Loading : ReaderPageState
    data class Downloading(val progress: Int) : ReaderPageState
    data object Ready : ReaderPageState
    data class Error(val message: String, val throwable: Throwable? = null) : ReaderPageState
}
```

### Phase 2: Preloading Enhancement

**Goal:** Implement Mihon's priority-based preload queue

| Step | Task | Files to Modify | Mihon Reference | Status |
|------|------|----------------|------------------|--------|
| 2.1 | Create `PagePreloader` class | New file in `domain/reader/` | `HttpPageLoader.kt:143-162` | ✅ Done |
| 2.2 | Implement priority queue for preload ordering | `PagePreloader.kt` | `HttpPageLoader.kt:165-180` | ✅ Done |
| 2.3 | Connect preloader to `ImageBasedReaderLayout` | `presentation/reader/ImageBasedReaderLayout.kt` | `WebtoonViewer.kt:208-224` | ✅ Done |
| 2.4 | Add scroll-direction-aware preloading | `ImageBasedReaderLayout.kt` | `HttpPageLoader.kt:50-75` | ✅ Done |
| 2.5 | Add visible range preloading for webtoon | `ImageBasedReaderLayout.kt` | `WebtoonViewer.kt:88-113` | ✅ Done |

**Preloading constants adopted:**
- Preload window: 5 pages before, 5 pages after
- ExtraLayoutSpace for webtoon: 500px ✅ Added
- Preload trigger threshold: 5 pages from end

**COMPLETED: Phase 2 infrastructure:**
- `PagePreloader.kt` created with priority-based queue
- `preloadAround()` function for directional preloading
- `enqueuePreload()` function for forward-only preloading
- Preload constants: `PREFETCH_PAGES = 5`, `EXTRA_LAYOUT_SPACE = 500`
- Visible range preloading: `preloadVisibleRange()` function tracks first and last visible items

### Phase 3: Webtoon Architecture Alignment

**Goal:** Adopt Mihon's WebtoonLayoutManager patterns

| Step | Task | Files to Modify | Mihon Reference | Status |
|------|------|----------------|------------------|--------|
| 3.1 | Enhance `LazyColumn` with extraLayoutSpace equivalent | `presentation/reader/ImageBasedReaderLayout.kt` | `WebtoonLayoutManager.kt:20-28` | ✅ Done |
| 3.2 | Implement scroll-based chapter preloading | `ImageBasedReaderLayout.kt` webtoon section | `WebtoonViewer.kt:88-113` | ✅ Done |
| 3.3 | Add chapter transitions (Prev/Next as list items) | N/A (single-chapter architecture) | `WebtoonAdapter.kt:30-75` | N/A |
| 3.4 | Restore scroll position on chapter load | `ImageBasedReaderLayout.kt` | `WebtoonViewer.kt:257-267` | ✅ Done |

**COMPLETED: Phase 3:**
- `EXTRA_LAYOUT_SPACE = 500` added and applied to LazyColumn contentPadding
- Page states now tracked with `pageStates` map
- Chapter transition classes defined in `ReaderPageState.kt`
- Visible range preloading implemented for webtoon mode
- Scroll-based preloading via `snapshotFlow` with `firstVisibleItemIndex` tracking

**Note on Chapter Transitions:** Mihon's chapter transitions (Prev/Next) are designed for multi-chapter manga series where each chapter is a separate file. Codex treats each comic/PDF as a single-chapter book, so chapter transitions between files are not applicable. The `ChapterTransition` classes are available for future multi-chapter support if needed.

### Phase 4: Tap Zone and Navigation Refinement

**Goal:** Align tap zone handling with Mihon's more flexible system

| Step | Task | Files to Modify | Mihon Reference | Status |
|------|------|----------------|------------------|--------|
| 4.1 | Extract tap zone to configuration enum | `domain/reader/ReaderNavigation.kt` | `ViewerNavigation.kt` | ✅ Done |
| 4.2 | Add NavigationRegion data class | `domain/reader/ReaderNavigation.kt` | `PagerViewer.kt:73-103` | ✅ Done |
| 4.3 | Implement volume key navigation | Settings + infrastructure | `PagerViewer.kt:386-423` | ✅ Settings Done |
| 4.4 | Add D-pad/page up/down support | `ReaderScreen.kt` | `PagerViewer.kt:400-420` | Pending |

**COMPLETED: Phase 4 Infrastructure:**
- `ReaderNavigation.kt` created with `NavigationRegion` enum (MENU, PREV, NEXT, LEFT, RIGHT)
- `ReaderTapZone` sealed class with tap zone configurations (Default, LShaped, Kindle, Edge, RightAndLeft, Disabled)
- `TapInversion` enum for tap inversion (NONE, HORIZONTAL, VERTICAL, BOTH)
- `NavigationRegionConfig` data class for region definitions
- Settings added: `comicVolumeKeysEnabled` and `comicVolumeKeysInverted`
- Settings UI added to comic reader options
- String resources added for volume keys settings

**Explicitly Excluded (per user request):**
- D-pad/keyboard navigation support - not needed for this project

### Phase 5: PDF Integration

**Goal:** Integrate PDF into unified viewer architecture while keeping MuPDF

| Step | Task | Files to Modify | Mihon Reference | Status |
|------|------|----------------|------------------|--------|
| 5.1 | Add ReaderPageState tracking to PDF | `presentation/reader/PdfReaderLayout.kt` | `PagerPageHolder.kt` | ✅ Done |
| 5.2 | Match vertical scroll preload margin | `PdfReaderLayout.kt` | `WebtoonViewer.kt` | ✅ Done (already present) |

**COMPLETED: Phase 5:**
- Added `ReaderPageState` tracking to `PdfReaderLayout`
- PDF pages now track Loading/Ready/Error states like comics
- Preloading via viewport height margin already present in code
- MuPDF tile-based rendering preserved (superior to Mihon's approach)

**Note on PDF Architecture:** Codex's MuPDF-based tile rendering is already sophisticated and exceeds Mihon's capabilities (Mihon has no native PDF support - it extracts images from PDFs). The main improvement was adding consistent state tracking via `ReaderPageState`.

### Phase 6: Memory Management (Optional Enhancement)

**Goal:** Consider adopting Coil for image pipeline

| Step | Task | Files to Modify | Mihon Reference |
|------|------|----------------|------------------|
| 6.1 | Evaluate Coil vs current manual approach | N/A | `App.kt:189-220` |
| 6.2 | If adopted: Create `TachiyomiImageDecoder` equivalent | `data/coil/` | `TachiyomiImageDecoder.kt` |
| 6.3 | If adopted: Configure Coil with Mihon's settings | `App.kt` or DI module | `App.kt:206-218` |

**Note:** Codex's manual `LinkedHashMap` LRU approach works well. Only migrate to Coil if debugging issues arise.

---

## 9. Migration Order Recommendation

### Week 1-2: Core Infrastructure
1. Create `ReaderPageState` sealed class
2. Create `PagePreloader` class
3. Connect preloader to `ImageBasedReaderLayout`

### Week 3-4: Webtoon Enhancement
1. Add extraLayoutSpace equivalent
2. Implement scroll-based chapter preloading
3. Add chapter transitions

### Week 5-6: Navigation & Polish
1. Extract tap zone configuration
2. Add volume/key navigation
3. PDF integration refinement

### Week 7+: Testing & Iteration
1. User testing
2. Bug fixes
3. Performance tuning

---

## 10. Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `ReaderPageState.kt` | `domain/model/` or `ui/reader/` | Page state sealed class |
| `PagePreloader.kt` | `domain/` or `data/` | Priority-based preload queue |
| `NavigationRegion.kt` | `presentation/reader/` | Tap zone configuration |

## 11. Files to Modify

### High Priority (Core functionality)
- `presentation/reader/ImageBasedReaderLayout.kt`
- `presentation/reader/ComicReaderLayout.kt`
- `presentation/reader/PdfReaderLayout.kt`

### Medium Priority (Enhancements)
- `presentation/reader/ReaderLayout.kt`
- `ui/reader/ReaderScreen.kt`
- `ui/reader/ReaderModel.kt`

### Lower Priority (Polish)
- `presentation/reader/viewer/ZoomableImageView.kt`
- `presentation/reader/viewer/webtoon/WebtoonRecyclerView.kt`

---

## 12. Success Criteria

1. **Preloading:** Pages preload smoothly 5 ahead/5 behind current position
2. **Webtoon:** Chapter transitions appear correctly, scroll position restores
3. **Navigation:** Tap zones work correctly in all modes, volume keys functional
4. **PDF:** Same preload behavior as comic images
5. **Performance:** No jank or memory issues during normal reading

---

## 13. Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Compose state vs Flow complexity | Use `collectAsState()` to bridge Flow to Compose |
| Breaking existing PDF functionality | Test PDF thoroughly after each phase |
| Performance issues with preloading | Monitor memory, use soft references if needed |
| Migration taking too long | Focus on core preloading first, polish later |
