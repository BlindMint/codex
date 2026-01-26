# Comic Reader Analysis: Mihon vs Codex

**Analysis Date:** January 26, 2026
**Goal:** Understand how Mihon handles comic loading, display, progress tracking, and navigation to improve Codex's comic reader

---

## Executive Summary

Mihon provides a significantly more polished comic reading experience than Codex, particularly in vertical/webtoon mode. Key differences include:

1. **Smarter Preloading:** Proactive page loading based on reading position
2. **Real-time Progress Updates:** Immediate UI and database updates on page changes
3. **Efficient RecyclerView Implementation:** Custom WebtoonLayoutManager for smooth scrolling
4. **Better State Management:** Viewer-based architecture with clear separation of concerns
5. **Comprehensive Caching:** Two-tier system (memory + disk) with intelligent eviction

---

## 1. Architecture Comparison

### Mihon Architecture

```
ReaderActivity
├── ReaderViewModel (MVVM pattern)
│   ├── State management for current chapter, pages, progress
│   ├── Preload coordination
│   └── Progress saving to database
│
├── Viewer (interface)
│   ├── PagerViewer (LTR/RTL paged modes)
│   └── WebtoonViewer (vertical/webtoon mode)
│       ├── WebtoonRecyclerView
│       ├── WebtoonLayoutManager (custom LinearLayoutManager)
│       └── WebtoonAdapter
│
└── PageLoader implementations
    ├── HttpPageLoader (online chapters)
    ├── DownloadPageLoader (downloaded chapters)
    ├── ArchivePageLoader (local archives)
    └── DirectoryPageLoader (local directories)
```

**Key Design Principles:**
- **Single Source of Truth:** `onPageSelected()` is the authoritative callback for page changes
- **Viewer Interface:** Clean separation between paged and webtoon modes
- **State Flow:** ViewModel → Activity → Viewer → PageHolders → UI

### Codex Architecture

```
ReaderScreen (Compose)
├── ReaderModel (ViewModel)
│   ├── State management (single StateFlow)
│   ├── Progress saving with debouncing
│   └── ComicImageCache integration
│
├── ComicReaderLayout (Compose component)
│   ├── Lazy loading cache (Map<Int, ImageBitmap>)
│   ├── ArchiveReader integration
│   ├── HorizontalPager for paged mode
│   └── LazyColumn for webtoon mode
│
└── Manual synchronization between PagerState and LazyListState
```

**Key Design Issues:**
- **Two Scroll States:** Both PagerState and LazyListState exist, requiring manual sync
- **Manual Preloading:** Window-based loading without intelligent prefetch
- **Debounced Saves:** 500ms delay before writing to database

---

## 2. Page Loading Mechanism

### Mihon's Approach

**File:** `eu/kanade.tachiyomi.ui.reader.loader.HttpPageLoader.kt`

```kotlin
// Priority queue for ordered page loading
private val queue = PriorityBlockingQueue<PriorityPage>()
private val preloadSize = 4  // Preload 4 pages ahead

override suspend fun getPages(): List<ReaderPage> {
    val pages = chapterCache.getPageListFromCache(chapter.chapter) ?: source.getPageList(chapter)
    return pages.mapIndexed { index, page ->
        ReaderPage(index, page.url, page.imageUrl).also { it.chapter = chapter }
    }
}

override suspend fun loadPage(page: ReaderPage) {
    // Load page through queue
    if (page.status == Page.State.Queue) {
        PriorityPage(page, 1).also { queue.offer(it) }
    }
    // Preload next pages automatically
    queuedPages += preloadNextPages(page, preloadSize)
}
```

**Key Features:**
1. **Priority Queue:** Ensures correct loading order even with concurrent requests
2. **Auto Preload:** Always loads next N pages when current page loads
3. **Cache-Aware:** Checks cache before downloading
4. **State Machine:** Queue → LoadPage → DownloadImage → Ready

### Codex's Approach

**File:** `us.blindmint.codex.presentation.reader.ComicReaderLayout.kt`

```kotlin
// Manual lazy loading with window management
val loadedPages = remember { mutableMapOf<Int, ImageBitmap>() }
val loadingPages = remember { mutableSetOf<Int>() }

suspend fun loadPage(pageIndex: Int) {
    // Manual duplicate check
    synchronized(loadingPagesLock) {
        if (loadingPages.contains(pageIndex)) return
        loadingPages.add(pageIndex)
    }

    // Check cache first
    val cachedImage = readerModel.comicImageCache.getPage(archiveId, pageIndex)
    if (cachedImage != null) {
        // Load from archive
        archiveHandle?.let { archive ->
            val entry = archive.entries[pageIndex]
            val bitmap = decodeBitmapEfficiently(...)
            loadedPages[pageIndex] = bitmap
            readerModel.comicImageCache.putPage(archiveId, pageIndex, bitmap)
        }
    }
}

// Window-based preloading
LaunchedEffect(currentPage, totalPages, comicReaderMode) {
    val pagesBehind = 10
    val pagesAhead = when (comicReaderMode) {
        "PAGED" -> 5
        "WEBTOON" -> 10
        else -> 10
    }

    val minPage = maxOf(0, currentPage - pagesBehind)
    val maxPage = minOf(totalPages - 1, currentPage + pagesAhead)

    // Load pages in window
    (minPage..maxPage).forEach { loadPage(it) }
}
```

**Key Issues:**
1. **No Ordering Guarantee:** Parallel loads don't maintain priority
2. **Manual Window Management:** No intelligent adaptive preloading
3. **Duplicate Loading:** Relies on manual set-based deduplication

---

## 3. Progress Tracking & Saving

### Mihon's Progress System

**File:** `eu/kanade.tachiyomi.ui.reader.ReaderViewModel.kt`

```kotlin
fun onPageSelected(page: ReaderPage) {
    // IMMEDIATELY update state and database
    viewModelScope.launchNonCancellable {
        updateChapterProgress(selectedChapter, page)
    }

    // Preload next chapter if near end
    val inPreloadRange = pages.size - page.number < 5
    if (inPreloadRange && allowPreload) {
        activity.requestPreloadChapter(transitionChapter)
    }
}

private suspend fun updateChapterProgress(readerChapter: ReaderChapter, page: Page) {
    val pageIndex = page.index

    // Update ViewModel state immediately
    mutableState.update {
        it.copy(currentPage = pageIndex + 1)
    }

    // Save to database immediately
    readerChapter.requestedPage = pageIndex
    readerChapter.chapter.last_page_read = pageIndex
    chapterPageIndex = pageIndex

    if (!incognitoMode && page.status !is Page.State.Error) {
        // If chapter complete, mark as read
        if (readerChapter.pages?.lastIndex == pageIndex) {
            updateChapterProgressOnComplete(readerChapter)
        }

        // Write to database immediately
        updateChapter.await(
            ChapterUpdate(
                id = readerChapter.chapter.id!!,
                read = readerChapter.chapter.read,
                lastPageRead = pageIndex.toLong()
            ),
        )
    }
}
```

**Database Write Frequency:** **Every page change** (no debouncing)

### Codex's Progress System

**File:** `us.blindmint.codex.ui.reader.ReaderModel.kt`

```kotlin
fun onEvent(event: ReaderEvent.OnComicPageChanged) {
    launch(Dispatchers.IO) {
        _state.update {
            it.copy(
                currentComicPage = event.currentPage,
                book = it.book.copy(
                    currentPage = event.currentPage,
                    lastPageRead = event.currentPage,
                    progress = if (it.totalComicPages > 0) {
                        event.currentPage.toFloat() / maxOf(1, it.totalComicPages - 1)
                    } else 0f
                )
            )
        }

        // DEBOUNCHED: Only save after 500ms delay
        scheduleComicProgressSave(event.currentPage, _state.value.totalComicPages)

        LibraryScreen.refreshListChannel.trySend(300)
        HistoryScreen.refreshListChannel.trySend(300)
    }
}

private fun scheduleComicProgressSave(page: Int, totalPages: Int) {
    comicProgressJob?.cancel()
    comicProgressJob = viewModelScope.launch(Dispatchers.IO) {
        delay(500)  // 500ms debounce!

        val currentPage = _state.value.currentComicPage

        if (currentPage == page && currentPage != lastSavedComicPage && totalPages > 0) {
            val progress = currentPage.toFloat() / maxOf(1, totalPages - 1)
            updateBook.execute(
                _state.value.book.copy(
                    currentPage = currentPage,
                    lastPageRead = currentPage,
                    progress = progress
                )
            )
            lastSavedComicPage = currentPage
        }
    }
}
```

**Database Write Frequency:** **Every page change, but with 500ms debounce**

---

## 4. UI Updates vs Database Operations

### Mihon's Approach

**Real-time UI Updates:**
```kotlin
// WebtoonViewer.kt
recycler.addOnScrollListener(
    object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            onScrolled()

            // Track first visible item for progress
            val position = layoutManager.findLastEndVisibleItemPosition()
            val item = adapter.items.getOrNull(position)
            if (item != null && currentPage != item) {
                currentPage = item
                when (item) {
                    is ReaderPage -> onPageSelected(item, allowPreload)
                    is ChapterTransition -> onTransitionSelected(item)
                }
            }
        }
    }
)
```

**Key Points:**
- **Immediate Callbacks:** `onPageSelected()` called as soon as item becomes visible
- **No Debouncing:** Progress bar updates instantly
- **Efficient RecyclerView:** Uses `findLastEndVisibleItemPosition()` for tracking

### Codex's Approach

```kotlin
// ComicReaderLayout.kt
// Debounced page change detection
LaunchedEffect(pagerState, isRTL, comicReaderMode) {
    snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
        .debounce(50)  // 50ms debounce
        .collect { (physicalPage, _) ->
            if (comicReaderMode == "PAGED" && totalPages > 0) {
                val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                onPageChanged(logicalPage)
            }
        }
}

// Webtoon mode also debounced
LaunchedEffect(lazyListState, isRTL, comicReaderMode) {
    snapshotFlow { lazyListState.firstVisibleItemIndex }
        .debounce(50)  // 50ms debounce
        .collect { physicalIndex ->
            if (comicReaderMode == "WEBTOON" && totalPages > 0) {
                val logicalPage = mapPhysicalToLogicalPage(physicalIndex)
                onPageChanged(logicalPage)
            }
        }
}
```

**Key Issues:**
- **50ms Debounce:** Introduces lag between scroll and progress update
- **Dual State Sync:** PagerState and LazyListState must be synchronized
- **Delayed Updates:** Progress doesn't reflect scroll immediately

---

## 5. LTR/RTL Handling

### Mihon's LTR/RTL Implementation

**File:** `eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer.kt`

```kotlin
abstract class PagerViewer(val activity: ReaderActivity) : Viewer {
    // Different pager implementations for L2R, R2L, vertical
    abstract fun createPager(): Pager

    private fun moveRight() {
        if (pager.currentItem != adapter.count - 1) {
            pager.setCurrentItem(pager.currentItem + 1, config.usePageTransitions)
        }
    }

    private fun moveLeft() {
        if (pager.currentItem != 0) {
            pager.setCurrentItem(pager.currentItem - 1, config.usePageTransitions)
        }
    }
}
```

**RTL Support:**
- Separate pager implementations handle RTL naturally
- Page ordering managed by adapter
- Navigation direction handled by config

### Codex's LTR/RTL Implementation

**File:** `us.blindmint.codex.presentation.reader.ComicReaderLayout.kt`

```kotlin
val isRTL = comicReadingDirection == "RTL"

// Map logical page (0 = first) to physical page
val mapLogicalToPhysicalPage = { logicalPage: Int ->
    if (isRTL && totalPages > 0) totalPages - 1 - logicalPage else logicalPage
}

// Map physical back to logical
val mapPhysicalToLogicalPage = { physicalPage: Int ->
    if (isRTL && totalPages > 0) totalPages - 1 - physicalPage else physicalPage
}

// Sync states when direction changes
LaunchedEffect(comicReadingDirection, totalPages) {
    if (totalPages > 0) {
        val targetPhysicalPage = if (comicReadingDirection == "RTL") {
            totalPages - 1 - storedLogicalPage
        } else {
            storedLogicalPage
        }

        if (comicReaderMode == "PAGED") {
            pagerState.scrollToPage(targetPhysicalPage)
        } else {
            lazyListState.scrollToItem(targetPhysicalPage)
        }
    }
}
```

**Key Issues:**
- **Manual Mapping:** Complex conversion between logical/physical pages
- **Sync Complexity:** Must maintain consistency across two scroll states
- **Initial Load Bug:** Race condition on direction changes (lines 448-466)

---

## 6. Vertical/Webtoon Mode

### Mihon's Webtoon Implementation

**File:** `eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer.kt`

```kotlin
class WebtoonViewer(val activity: ReaderActivity, val isContinuous: Boolean = true) : Viewer {

    private val recycler = WebtoonRecyclerView(activity)
    private val layoutManager = WebtoonLayoutManager(activity, scrollDistance)
    private val adapter = WebtoonAdapter(this)

    init {
        // CRITICAL: Disable item prefetch for smooth scrolling
        recycler.isItemPrefetchEnabled = false  // <-- KEY!
        recycler.itemAnimator = null
        recycler.layoutManager = layoutManager

        recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    onScrolled()

                    // Track visible items
                    val firstIndex = layoutManager.findFirstVisibleItemPosition()
                    val lastIndex = layoutManager.findLastEndVisibleItemPosition()

                    // Auto-hide menu on scroll
                    if ((dy > threshold || dy < -threshold) && activity.viewModel.state.value.menuVisible) {
                        activity.hideMenu()
                    }

                    // Preload previous chapter
                    if (dy < 0) {
                        val firstItem = adapter.items.getOrNull(firstIndex)
                        if (firstItem is ChapterTransition.Prev && firstItem.to != null) {
                            activity.requestPreloadChapter(firstItem.to)
                        }
                    }

                    // Show menu at end
                    val lastItem = adapter.items.getOrNull(lastIndex)
                    if (lastItem is ChapterTransition.Next && lastItem.to == null) {
                        activity.showMenu()
                    }
                }
            }
        )
    }

    private fun onScrolled(pos: Int? = null) {
        val position = pos ?: layoutManager.findLastEndVisibleItemPosition()
        val item = adapter.items.getOrNull(position)
        if (item != null && currentPage != item) {
            currentPage = item
            when (item) {
                is ReaderPage -> onPageSelected(item, allowPreload)
                is ChapterTransition -> onTransitionSelected(item)
            }
        }
    }
}
```

**Key Features:**
1. **WebtoonLayoutManager:** Custom LinearLayoutManager with extra layout space
2. **Item Prefetch Disabled:** Prevents premature loading and blank views
3. **Chapter Transitions:** Seamless insertion of transition pages between chapters
4. **Scroll Detection:** Uses threshold to determine intentional vs incidental scrolling

### Codex's Webtoon Implementation

**File:** `us.blindmint.codex.presentation.reader.ComicReaderLayout.kt`

```kotlin
LazyColumn(
    state = lazyListState,
    modifier = Modifier.fillMaxSize()
) {
    itemsIndexed(
        (0 until totalPages).toList(),
        key = { _, physicalPage -> physicalPage }
    ) { _, physicalPage ->
        // For RTL, we need to load logical page
        val logicalPage = mapPhysicalToLogicalPage(physicalPage)

        LaunchedEffect(logicalPage) {
            loadPage(logicalPage)
        }

        val pageImage = getPageFromMemory(logicalPage)
        if (pageImage != null) {
            // Show loading placeholder
        } else {
            Image(
                bitmap = pageImage,
                contentScale = webtoonContentScale,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(comicTapZone, showMenu) {
                        detectTapGestures { offset ->
                            // Simple tap handling
                        }
                    }
            )
        }
    }
}
```

**Key Issues:**
1. **No Custom LayoutManager:** Uses standard LazyColumn
2. **No Chapter Transitions:** Abrupt chapter boundaries
3. **Standard Compose LazyColumn:** Less efficient than custom RecyclerView
4. **Blank Views:** Pages load as placeholders, then swap to images

---

## 7. Progress Restoration

### Mihon's Restoration

**File:** `eu.kanade.tachiyomi.ui.reader.ReaderViewModel.kt`

```kotlin
// Uses SavedStateHandle for process restoration
private var chapterId = savedState.get<Long>("chapter_id") ?: -1L
private var chapterPageIndex = savedState.get<Int>("page_index") ?: -1

init {
    state.map { it.viewerChapters?.currChapter }
        .distinctUntilChanged()
        .filterNotNull()
        .onEach { currentChapter ->
            if (chapterPageIndex >= 0) {
                // Restore from SavedState
                currentChapter.requestedPage = chapterPageIndex
            } else if (!currentChapter.chapter.read) {
                // Restore from database
                currentChapter.requestedPage = currentChapter.chapter.last_page_read
            }

            chapterId = currentChapter.chapter.id!!
        }
        .launchIn(viewModelScope)
}
```

**File:** `eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer.kt`

```kotlin
override fun setChapters(chapters: ViewerChapters) {
    adapter.setChapters(chapters, forceTransition)

    if (recycler.isGone) {
        val pages = chapters.currChapter.pages ?: return
        moveToPage(pages[min(chapters.currChapter.requestedPage, pages.lastIndex)])
        recycler.isVisible = true
    }
}
```

**Key Features:**
1. **SavedStateHandle:** Persists position across process kills
2. **RequestedPage:** Viewer uses this to scroll to correct position
3. **Smart Fallback:** Uses database if no saved state

### Codex's Restoration

**File:** `us.blindmint.codex.ui.reader.ReaderModel.kt`

```kotlin
init(bookId: Int, ..., skipTextLoading: Boolean = false, reuseExistingText: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
        val book = getBookById.execute(bookId)

        if (book.isComic) {
            // Initialize with saved page from database
            _state.update {
                ReaderState(
                    book = book.copy(
                        lastOpened = lastOpened
                    ),
                    currentComicPage = book.lastPageRead.coerceIn(0, book.pageCount ?: 0),
                    showMenu = false,
                    isLoading = true
                )
            }
        }
    }
}
```

**File:** `us.blindmint.codex.presentation.reader.ComicReaderLayout.kt`

```kotlin
LaunchedEffect(totalPages) {
    if (totalPages == 0) return@LaunchedEffect

    // Only scroll on initial load (when totalPages first becomes > 0)
    if (initialPage > 0 && initialPage < totalPages) {
        val targetPhysicalPage = mapLogicalToPhysicalPage(initialPage)
        if (comicReaderMode == "PAGED") {
            pagerState.animateScrollToPage(targetPhysicalPage)
        } else {
            lazyListState.animateScrollToItem(targetPhysicalPage)
        }
        delay(300)  // Wait for animation
        hasPerformedInitialScroll = true
    }
}
```

**Key Issues:**
1. **No SavedState:** Position lost on process kill
2. **Animation Race:** Uses fixed 300ms delay that may be too short/long
3. **Mode Change Bug:** Scrolling to wrong position when direction changes (lines 448-466)

---

## 8. Caching Strategy

### Mihon's Two-Tier Cache

**File:** `eu.kanade.tachiyomi.data.cache.ChapterCache.kt`

```kotlin
class ChapterCache(
    private val context: Context,
    private val json: Json,
) {
    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, "chapter_disk_cache"),
        PARAMETER_APP_VERSION,
        PARAMETER_VALUE_COUNT,
        PARAMETER_CACHE_SIZE,  // 100 MB
    )

    fun putPageListToCache(chapter: Chapter, pages: List<Page>) {
        val cachedValue = json.encodeToString(pages)
        val key = DiskUtil.hashKeyForDisk(getKey(chapter))
        editor.newOutputStream(0).sink().buffer().use {
            it.write(cachedValue.toByteArray())
        }
        editor.commit()
    }

    fun getPageListFromCache(chapter: Chapter): List<Page>? {
        val key = DiskUtil.hashKeyForDisk(getKey(chapter))
        return diskCache.get(key).use {
            json.decodeFromString(it.getString(0))
        }
    }
}
```

**File:** `eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder.kt`

```kotlin
// Custom Coil decoder that DISABLES Coil's cache
ImageRequest.Builder(context)
    .data(data)
    .memoryCachePolicy(CachePolicy.DISABLED)  // <-- Custom cache used instead
    .diskCachePolicy(CachePolicy.DISABLED)  // <-- Images cached by page loader
    .customDecoder(true)
```

**Cache Hierarchy:**
```
ChapterCache (Disk LRU)
├── Page lists as JSON (fast chapter loads)
└── Images stored separately by page loader

HttpPageLoader
└── Downloads images to ChapterCache image files

TachiyomiImageDecoder
└── Decodes from cache, skips Coil's caching
```

### Codex's Two-Tier Cache

**File:** `us.blindmint.codex.data.local.cache.ComicImageCache.kt`

```kotlin
@Singleton
class ComicImageCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Memory cache: LRU
    private val memoryCache = LruCache<String, ImageBitmap>(
        (MAX_MEMORY_BYTES / 8).toInt()  // ~6 MB
    )

    // Disk cache: LRU
    private var diskCache: DiskLruCache? = null
    init {
        diskCache = DiskLruCache.open(cacheDir, APP_VERSION, VALUE_COUNT, MAX_DISK_BYTES)  // 200 MB
    }

    suspend fun getPage(archiveId: String, pageIndex: Int): ImageBitmap? {
        val cacheKey = generateCacheKey(archiveId, pageIndex)

        // Check memory cache
        memoryCache[cacheKey]?.let {
            return@withContext it
        }

        // Check disk cache
        val diskCacheSnapshot = diskCache?.get(cacheKey)
        if (diskCacheSnapshot != null) {
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val imageBitmap = bitmap.asImageBitmap()
            memoryCache.put(cacheKey, imageBitmap)  // Promote to memory
            return@withContext imageBitmap
        }

        return@withContext null
    }

    suspend fun putPage(archiveId: String, pageIndex: Int, imageBitmap: ImageBitmap) {
        val cacheKey = generateCacheKey(archiveId, pageIndex)
        memoryCache.put(cacheKey, imageBitmap)
        diskCache.edit(cacheKey)?.commit()  // Write to both tiers
    }
}
```

**Comparison:**

| Feature | Mihon | Codex |
|---------|--------|--------|
| Page List Cache | Yes (JSON) | No |
| Cache Hierarchy | ChapterCache → Images | Direct two-tier |
| Memory Cache | Coil (custom) | LruCache (~6 MB) |
| Disk Cache | ChapterCache (100 MB) | DiskLruCache (200 MB) |
| Cache Coordination | Decoder disables Coil cache | Manual management |
| Eviction | LRU by Chapter | LRU by image |

**Key Advantage of Mihon:**
- **Page List Caching:** Skips parsing entire chapter on reopen
- **Coordinated Caching:** Page loader and decoder work together
- **Automatic Promoting:** Images promoted from disk → memory automatically

---

## 9. Key Differences Summary

| Aspect | Mihon | Codex |
|--------|--------|--------|
| **Page Loading** | Priority queue with auto-preload | Manual window-based loading |
| **Progress Updates** | Immediate on page change | Debounced (500ms delay) |
| **Database Writes** | Every page change (immediate) | Every page change (500ms delayed) |
| **Webtoon Scroll** | Custom RecyclerView | LazyColumn (Compose) |
| **Item Prefetch** | Disabled (smooth scrolling) | Default (causes blank views) |
| **Chapter Transitions** | Seamless with transition pages | Abrupt chapter boundaries |
| **LTR/RTL** | Native pager support | Manual logical/physical mapping |
| **State Restoration** | SavedStateHandle + requestedPage | No process restoration |
| **Progress Restoration** | ScrollToPage() on init | AnimatedScrollTo + 300ms delay |
| **Cache Coordination** | Page loader + decoder sync | Manual two-tier management |
| **Page List Cache** | Yes (JSON) | No |

---

## 10. Specific Code Issues in Codex

### Issue 1: RTL/LTR State Sync Bug

**Location:** `ComicReaderLayout.kt` lines 448-466

```kotlin
// When direction changes, we scroll to maintain position
LaunchedEffect(comicReadingDirection, totalPages) {
    if (totalPages > 0) {
        val targetPhysicalPage = if (comicReadingDirection == "RTL") {
            totalPages - 1 - storedLogicalPage  // BUG: storedLogicalPage might not be updated yet!
        } else {
            storedLogicalPage
        }

        if (comicReaderMode == "PAGED") {
            pagerState.scrollToPage(targetPhysicalPage)  // Scrolls WRONG page
        } else {
            lazyListState.scrollToItem(targetPhysicalPage)
        }
    }
}

// Updates storedLogicalPage
LaunchedEffect(currentPage) {
    storedLogicalPage = currentPage
}
```

**Problem:** When `comicReadingDirection` changes, the `LaunchedEffect` fires BEFORE `storedLogicalPage` is updated, causing scroll to wrong position.

**Fix:** Combine keys or use derived state.

### Issue 2: Debounce Reduces Responsiveness

**Location:** `ReaderModel.kt` line 1351

```kotlin
private fun scheduleComicProgressSave(page: Int, totalPages: Int) {
    comicProgressJob?.cancel()
    comicProgressJob = viewModelScope.launch(Dispatchers.IO) {
        delay(500)  // Unnecessary delay

        val currentPage = _state.value.currentComicPage
        if (currentPage == page && currentPage != lastSavedComicPage) {
            updateBook.execute(...)
        }
    }
}
```

**Problem:** User scrolls rapidly, progress bar doesn't update for 500ms.

**Fix:** Save immediately, use Mutex for concurrent access.

### Issue 3: No Chapter Preloading

**Location:** `ComicReaderLayout.kt` lines 339-379

**Problem:** Codex only preloads pages around current position, never preloads next chapter.

**Fix:** Detect when near end of chapter (e.g., last 5 pages) and trigger chapter preload.

### Issue 4: LazyColumn Not Optimized for Webtoon

**Location:** `ComicReaderLayout.kt` lines 551-606

**Problem:** Standard LazyColumn doesn't optimize for webtoon use case:
- Default prefetch causes blank views
- No extra layout space for smooth scrolling
- No chapter transition pages

**Fix:** Create custom WebtoonLayoutManager or use AndroidViewInterop.

---

## 11. Critical Insights from Mihon

### 1. Single Source of Truth Pattern

Mihon's `onPageSelected()` is THE authoritative callback for page changes. All state flows through this single point:

```
Viewer detects page change → onPageSelected(page) → 
  ├─→ Update ViewModel state (immediate)
  ├─→ Update database (immediate)
  ├─→ Preload next chapter (if near end)
  └─→ Update UI (immediate)
```

**Codex Issue:** Multiple code paths update state:
- LaunchedEffect with debounce → onComicPageChanged → scheduleComicProgressSave → database
- Inconsistent state leads to bugs

### 2. Viewer Interface Abstraction

Mihon's `Viewer` interface cleanly separates concerns:

```kotlin
interface Viewer {
    fun getView(): View
    fun destroy()
    fun setChapters(chapters: ViewerChapters)
    fun moveToPage(page: ReaderPage)
    fun handleKeyEvent(event: KeyEvent): Boolean
    fun handleGenericMotionEvent(event: MotionEvent): Boolean
}
```

**Benefits:**
- Activity works with any viewer implementation
- Clean swapping between paged/webtoon modes
- Consistent event handling

### 3. Chapter Transition Pages

Mihon inserts special pages between chapters:

```kotlin
sealed class ChapterTransition {
    data class Prev(val from: ReaderChapter, val to: ReaderChapter?) : ChapterTransition()
    data class Next(val from: ReaderChapter, val to: ReaderChapter?) : ChapterTransition()
}
```

**Adapter Logic:**
- Add last 2 pages of previous chapter
- Add "Previous Chapter" transition page
- Add all pages of current chapter
- Add "Next Chapter" transition page
- Add first 2 pages of next chapter

**User Experience:** Seamless scrolling from one chapter to next with visual indicator.

### 4. RequestedPage Pattern

Mihon uses `requestedPage` to coordinate viewer scrolling:

```kotlin
// ViewModel sets it
currentChapter.requestedPage = pageIndex

// Viewer uses it
override fun setChapters(chapters: ViewerChapters) {
    val requestedPage = chapters.currChapter.requestedPage
    moveToPage(pages[min(requestedPage, pages.lastIndex)])
}

// Scroll to correct position on layout
if (recycler.isGone) {
    moveToPage(pages[min(chapters.currChapter.requestedPage, pages.lastIndex)])
    recycler.isVisible = true
}
```

**Pattern:** Clear separation of concern:
- ViewModel: Manages state
- Viewer: Handles scrolling to requested position

---

## 12. Performance Techniques

### Mihon's Optimizations

1. **Disabled Item Prefetch:**
```kotlin
class WebtoonLayoutManager(...) : LinearLayoutManager(context) {
    init {
        isItemPrefetchEnabled = false  // Critical for webtoon
    }
}
```

2. **Extra Layout Space:**
```kotlin
private val extraLayoutSpace: Int = scrollDistance
// Allows rendering pages even if not yet visible
override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
    return extraLayoutSpace
}
```

3. **DiffUtil Updates:**
```kotlin
private fun updateItems(newItems: List<Any>) {
    val result = DiffUtil.calculateDiff(Callback(items, newItems))
    items = newItems
    result.dispatchUpdatesTo(this)  // Minimal UI updates
}
```

4. **Priority Queue for Loading:**
```kotlin
private val queue = PriorityBlockingQueue<PriorityPage>()

class PriorityPage(page: ReaderPage, priority: Int) : Comparable<PriorityPage> {
    override fun compareTo(other: PriorityPage): Int {
        // Priority: 0 (current), 1 (preload), 2 (retry)
        return when {
            other.priority == 0 -> 1
            other.priority == 1 -> -1
            else -> priority.compareTo(other.priority)
        }
    }
}
```

### Codex's Missed Optimizations

1. **No DiffUtil:** LaunchedEffect causes full recomposition
2. **LazyColumn Default Prefetch:** Causes blank views
3. **No Priority Queue:** Parallel loads may complete out of order
4. **Manual Mutex:** More error-prone than queue-based approach

---

## 13. Recommended Improvements for Codex

### High Priority (Must Fix)

1. **Fix RTL/LTR Sync Bug:**
   - Combine LaunchedEffect keys: `LaunchedEffect(currentPage, comicReadingDirection)`
   - Or use `remember { derivedStateOf(...) }` for logical page

2. **Remove Progress Debounce:**
   - Save immediately in `onComicPageChanged`
   - Use Mutex to prevent concurrent writes instead of delay

3. **Add SavedStateHandle:**
   - Save `currentPage` to SavedStateHandle on page change
   - Restore from SavedStateHandle in `init()`

4. **Implement Chapter Preloading:**
   - Detect when `currentPage >= totalPages - 5`
   - Call preload mechanism for next chapter

### Medium Priority (Should Fix)

5. **Custom RecyclerView for Webtoon:**
   - Implement `WebtoonLayoutManager` equivalent
   - Disable item prefetch
   - Add extra layout space
   - Use `LazyVerticalGrid` with AndroidViewInterop

6. **Add Page List Caching:**
   - Cache page list (archive structure) as JSON
   - Skip parsing on reopen
   - Store in `ComicImageCache` or separate cache

7. **Implement Chapter Transitions:**
   - Add transition items between chapters
   - Show "Previous Chapter" / "Next Chapter" indicators
   - Preload transition pages

### Low Priority (Nice to Have)

8. **Priority Queue for Page Loading:**
   - Implement ordered loading with PriorityBlockingQueue
   - Priority: Current (0), Preload (1), Retry (2)

9. **DiffUtil for Updates:**
   - Use Compose's `derivedStateOf` where possible
   - Implement custom DiffUtil for RecyclerView

10. **Consolidate Scroll States:**
   - Use single scroll state or clean abstraction
   - Eliminate manual sync between PagerState and LazyListState

---

## 14. Implementation Roadmap

### Phase 1: Fix Critical Bugs (1-2 days) ✅ COMPLETED

**Goal:** Fix immediate user-facing issues

- [x] Fix RTL/LTR sync bug (Issue #1)
- [x] Remove 500ms progress debounce (Issue #2) - Already fixed in current codebase
- [x] Add SavedStateHandle for progress restoration
- [ ] Test all reading modes thoroughly

**Files to Modify:**
- `ComicReaderLayout.kt` ✅
- `ReaderModel.kt` ✅

**Changes Made:**
1. Fixed RTL/LTR sync bug by combining LaunchedEffect keys to prevent race condition
2. Confirmed progress saving is immediate (no debounce) - already implemented
3. Added SavedStateHandle for comic page restoration across process kills
4. Added proper nullability annotations for SavedStateHandle reads

---

### Phase 2: Improve Preloading (2-3 days)

**Goal:** Proactive page loading for smooth experience

- [ ] Implement priority queue for page loading
- [ ] Add chapter preload detection (last 5 pages)
- [ ] Implement preload mechanism for next chapter
- [ ] Add preload UI indicator

**Files to Modify:**
- `ComicReaderLayout.kt`
- `ComicImageCache.kt` (if needed)
- New: `PageLoadQueue.kt`

---

### Phase 3: Webtoon Optimization (3-5 days)

**Goal:** Smooth vertical scrolling with custom layout manager

- [ ] Implement WebtoonLayoutManager equivalent
- [ ] Create custom RecyclerView for webtoon mode
- [ ] Disable item prefetch
- [ ] Add extra layout space
- [ ] Integrate with Compose (AndroidViewInterop)

**Files to Create:**
- `presentation/reader/webtoon/WebtoonLayoutManager.kt`
- `presentation/reader/webtoon/WebtoonRecyclerView.kt`
- `presentation/reader/webtoon/WebtoonAdapter.kt`

---

### Phase 4: Chapter Transitions (2-3 days)

**Goal:** Seamless chapter boundaries

- [ ] Create ChapterTransition model
- [ ] Add transition page rendering
- [ ] Update adapter to include transition pages
- [ ] Add chapter preload on transition

**Files to Modify:**
- `ComicReaderLayout.kt`
- `ReaderModel.kt`

---

### Phase 5: Caching Improvements (2-3 days)

**Goal:** Faster chapter loads and smarter caching

- [ ] Add page list caching (JSON)
- [ ] Implement coordinated cache with decoder
- [ ] Add cache invalidation strategy
- [ ] Optimize cache key generation

**Files to Modify:**
- `ComicImageCache.kt`
- New: `ChapterListCache.kt`

---

### Phase 6: Viewer Abstraction (3-4 days)

**Goal:** Clean separation between paged and webtoon modes

- [ ] Create Viewer interface
- [ ] Create PagerViewer abstraction
- [ ] Create WebtoonViewer abstraction
- [ ] Refactor ReaderActivity/Model to use Viewer interface
- [ ] Add viewer swapping mechanism

**Files to Create:**
- `presentation/reader/viewer/Viewer.kt` (interface)
- `presentation/reader/viewer/PagerViewer.kt`
- `presentation/reader/viewer/WebtoonViewer.kt`

---

### Phase 7: Testing & Polish (2-3 days)

**Goal:** Ensure stability and smooth experience

- [ ] Test LTR/RTL switching mid-chapter
- [ ] Test webtoon scrolling performance
- [ ] Test chapter transitions
- [ ] Test progress restoration after process kill
- [ ] Test rapid scrolling and page changes
- [ ] Test preloading behavior
- [ ] Memory profiling (leaks, cache efficiency)
- [ ] Performance profiling (scroll FPS, load times)

---

## 15. Migration Strategy Notes

### Incremental Approach

**Recommended:** Do NOT attempt to rewrite everything at once

**Why:**
1. **Risk of Regressions:** Large changes introduce bugs
2. **Testing Difficulty:** Can't verify each phase independently
3. **User Impact:** Long development freezes, no incremental improvements

**Better Approach:**
1. **Phase 1:** Fix bugs first (quick wins, high impact)
2. **Phase 2-4:** Add features incrementally (each phase is testable)
3. **Phase 5-7:** Refactoring while maintaining working code
4. **Backwards Compatibility:** Keep old code path behind feature flag

### Feature Flags

```kotlin
// In ReaderState
val useNewWebtoonViewer: Boolean = false  // Feature flag
val useNewPreloading: Boolean = false  // Feature flag

// In ComicReaderLayout
if (useNewWebtoonViewer) {
    NewWebtoonViewer(...)
} else {
    LazyColumn(...)  // Old implementation
}
```

---

## 16. Key Takeaways

### What Makes Mihon Smooth

1. **Immediate Updates:** No debouncing, UI updates instantly on page change
2. **Smart Preloading:** Loads pages before user needs them
3. **Optimized RecyclerView:** Custom layout manager for use case
4. **Clean Architecture:** Viewer interface, single source of truth
5. **Coordinated Caching:** Page list cache + decoder coordination

### What Codex Lacks

1. **Debounced Progress:** 500ms delay makes UI feel laggy
2. **Manual Preloading:** Window-based vs proactive queue
3. **Standard Compose Components:** LazyColumn not optimized for webtoon
4. **Sync Issues:** Multiple scroll states cause race conditions
5. **No Process Restoration:** Position lost on app restart

### What to Copy from Mihon

**Copy Directly:**
- Priority queue logic (`PriorityBlockingQueue`)
- WebtoonLayoutManager implementation
- Chapter transition pages
- Page list caching strategy
- Viewer interface abstraction

**Adapt for Codex:**
- Keep Compose-based UI (don't switch to XML Views)
- Keep existing `ComicImageCache` (it's well-designed)
- Keep `ArchiveReader` (works well for archives)
- Adapt Mihon patterns to Compose state management

---

## 17. References

### Mihon Code References

**ReaderViewModel:** `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`
- `onPageSelected()` (line 439): Main page change handler
- `updateChapterProgress()` (line 534): Progress saving logic
- `preload()` (line 390): Chapter preloading

**WebtoonViewer:** `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonViewer.kt`
- `onPageSelected()` (line 208): Page change callback
- `onScrolled()` (line 269): Scroll tracking

**WebtoonLayoutManager:** `/home/samurai/dev/git/mihon/app/src/main/java/androidx/recyclerview/widget/WebtoonLayoutManager.kt`
- Custom LinearLayoutManager with prefetch disabled

**HttpPageLoader:** `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/loader/HttpPageLoader.kt`
- `preloadSize = 4` (line 44): Preload ahead count
- `PriorityBlockingQueue` (line 42): Ordered loading

**ChapterCache:** `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/data/cache/ChapterCache.kt`
- JSON-based page list caching

### Codex Code References

**ReaderModel:** `/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/ui/reader/ReaderModel.kt`
- `scheduleComicProgressSave()` (line 1347): Debounced progress saving

**ComicReaderLayout:** `/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/presentation/reader/ComicReaderLayout.kt`
- `loadPage()` (line 152): Manual page loading
- RTL/LTR mapping (lines 206-212): Complex page conversion

**ComicImageCache:** `/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/local/cache/ComicImageCache.kt`
- Two-tier cache implementation

---

**End of Analysis**
