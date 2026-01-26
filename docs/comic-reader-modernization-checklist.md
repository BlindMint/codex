# Comic Reader Modernization - Implementation Checklist

**Created:** January 26, 2026
**Goal:** Modernize Codex comic reader based on Mihon's architecture and techniques

---

## Quick Start Guide

**High Impact Fixes (Start Here - Do These First):**
- [ ] Fix RTL/LTR sync bug causing wrong page on direction change
- [ ] Remove 500ms progress debounce (save immediately)
- [ ] Add SavedStateHandle for progress restoration
- [ ] Add chapter preload detection (last 5 pages)

---

## Phase 1: Fix Critical Bugs (1-2 days)

### 1.1 Fix RTL/LTR State Sync Bug

**Priority:** CRITICAL - Users experience wrong page when switching reading direction

**Problem:** In `ComicReaderLayout.kt`, when `comicReadingDirection` changes:
- `LaunchedEffect(comicReadingDirection, totalPages)` fires
- It reads `storedLogicalPage` (which hasn't been updated yet!)
- Scrolls to wrong position

**Location:** `ComicReaderLayout.kt` lines 448-466

**Solution Options:**

**Option A (Recommended):** Use derived state
```kotlin
val currentPage = remember(currentPage, comicReadingDirection, totalPages) {
    // Calculate logical page considering direction
    if (comicReadingDirection == "RTL" && totalPages > 0) {
        totalPages - 1 - currentPage
    } else {
        currentPage
    }
}
```

**Option B:** Combine LaunchedEffect keys
```kotlin
LaunchedEffect(currentPage, comicReadingDirection) {
    // Both variables available in effect
}
```

**Option C:** Move update before effect
```kotlin
LaunchedEffect(currentPage) {
    storedLogicalPage = currentPage
}
LaunchedEffect(comicReadingDirection) {
    // storedLogicalPage already updated
}
```

**Implementation Steps:**
1. Choose solution approach
2. Modify `ComicReaderLayout.kt` lines 448-466
3. Test direction changes while mid-chapter (not at page 0)
4. Test direction changes at chapter boundaries
5. Test rapid direction toggling

---

### 1.2 Remove Progress Debounce

**Priority:** CRITICAL - UI feels laggy during scrolling

**Problem:** 500ms delay before saving progress makes UI unresponsive

**Location:** `ReaderModel.kt` line 1351

**Current Code:**
```kotlin
private fun scheduleComicProgressSave(page: Int, totalPages: Int) {
    comicProgressJob?.cancel()
    comicProgressJob = viewModelScope.launch(Dispatchers.IO) {
        delay(500)  // <-- REMOVE THIS DELAY

        val currentPage = _state.value.currentComicPage
        if (currentPage == page && currentPage != lastSavedComicPage) {
            updateBook.execute(...)
            lastSavedComicPage = currentPage
        }
    }
}
```

**Solution:** Save immediately, use Mutex for concurrency control

```kotlin
private val progressMutex = Mutex()

private suspend fun saveProgressImmediately(page: Int, totalPages: Int) {
    progressMutex.withLock {
        val currentPage = _state.value.currentComicPage
        if (currentPage != lastSavedComicPage && totalPages > 0) {
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

**Implementation Steps:**
1. Add Mutex to ReaderModel
2. Replace `scheduleComicProgressSave` with `saveProgressImmediately`
3. Remove 500ms delay
4. Test rapid scrolling - progress should update instantly
5. Test concurrent page changes - no double saves

---

### 1.3 Add SavedStateHandle

**Priority:** HIGH - Position lost on app restart

**Problem:** Codex loses current page on process kill

**Location:** Multiple files need modification

**Solution:**

**In ReaderViewModel (Mihon-style):**
```kotlin
private val savedStateHandle: SavedStateHandle = SavedStateHandle()

private var chapterPageIndex = savedStateHandle.get<Int>("page_index") ?: -1
    set(value) {
        savedStateHandle["page_index"] = value
        field = value
    }
```

**In ReaderActivity:**
```kotlin
class ReaderActivity : BaseActivity() {
    private lateinit var binding: ReaderActivityBinding

    val viewModel by viewModels<ReaderViewModel>()

    // Pass savedStateHandle to ViewModel
    private val viewModel: ReaderViewModel by viewModels(
        extrasProducer = SavedStateHandleSupport.create(it.intent)
    )
}
```

**In ReaderModel (Codex-style):**
```kotlin
@HiltViewModel
class ReaderModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    ...
) : ViewModel() {
    // Save to handle on every page change
    private fun savePageToHandle(page: Int) {
        savedStateHandle["page_index"] = page
    }

    // Restore in init
    private fun restorePageFromHandle(): Int {
        return savedStateHandle.get<Int>("page_index") ?: 0
    }
}
```

**Implementation Steps:**
1. Add SavedStateHandle dependency to ReaderModel
2. Update composable creation in ReaderScreen to pass savedStateHandle
3. Save page to handle on every page change
4. Restore page from handle in init
5. Test: Kill app while reading → reopen → same position

---

### 1.4 Add Chapter Preload Detection

**Priority:** HIGH - Smooth chapter transitions

**Problem:** No preloading of next chapter causes delay when reaching chapter end

**Location:** `ComicReaderLayout.kt` - Add new preload logic

**Implementation:**
```kotlin
// Detect when near end of chapter
val isNearChapterEnd by remember(currentPage, totalPages) {
    currentPage >= totalPages - 5
}

// Trigger preload when detected
LaunchedEffect(isNearChapterEnd, currentChapterId) {
    if (isNearChapterEnd && hasNextChapter) {
        viewModel.preloadNextChapter()
    }
}
```

**Alternative:** Integrate into existing window preloading logic:
```kotlin
LaunchedEffect(currentPage, totalPages, comicReaderMode) {
    val pagesAhead = when (comicReaderMode) {
        "PAGED" -> 5
        "WEBTOON" -> 10
    }

    val minPage = maxOf(0, currentPage - 10) // Always preload behind
    val maxPage = minOf(totalPages - 1, currentPage + pagesAhead)

    // If near end and next chapter exists, preload it
    if (maxPage >= totalPages - 5 && hasNextChapter) {
        viewModel.preloadNextChapter()
    }

    // Load pages in window
    (minPage..maxPage).forEach { pageIndex ->
        if (!loadedPages.contains(pageIndex)) {
            loadPage(pageIndex)
        }
    }
}
```

**Implementation Steps:**
1. Add `hasNextChapter` parameter to ComicReaderLayout
2. Add `preloadNextChapter()` method to ReaderModel
3. Detect chapter end condition (last 5 pages)
4. Test: Read to page 45 of 50 → next chapter preloads
5. Test: Read last page → exit → reopen → should be preloaded

---

## Phase 2: Improve Preloading (2-3 days)

### 2.1 Implement Priority Queue for Page Loading

**Priority:** HIGH - Better page loading order and management

**Problem:** Manual set-based loading has no priority control

**Solution:** Implement priority queue similar to Mihon

**File to Create:** `data/loader/PageLoadQueue.kt`

```kotlin
/**
 * Priority levels for page loading
 * Priority 0: Current page (user is viewing)
 * Priority 1: Preload (next pages in current chapter)
 * Priority 2: Retry (failed page)
 */
enum class LoadPriority(val value: Int) {
    CURRENT(0),
    PRELOAD(1),
    RETRY(2)
}

/**
 * Priority-based page loading queue
 * Ensures correct loading order with concurrent requests
 */
class PageLoadQueue {
    private val queue = PriorityBlockingQueue<QueuedPage>()
    private val loadingPages = mutableSetOf<Int>()

    data class QueuedPage(
        val pageIndex: Int,
        val priority: LoadPriority,
        val loadTime: Long = System.currentTimeMillis()
    ) : Comparable<QueuedPage> {
        override fun compareTo(other: QueuedPage): Int {
            val priorityComparison = this.priority.compareTo(other.priority)
            return if (priorityComparison != 0) priorityComparison else this.loadTime.compareTo(other.loadTime)
        }
    }

    fun enqueue(pageIndex: Int, priority: LoadPriority) {
        if (loadingPages.contains(pageIndex)) {
            return // Already loading
        }

        val page = QueuedPage(pageIndex, priority)
        queue.offer(page)
    }

    fun take(): QueuedPage? {
        val page = queue.take()
        loadingPages.add(page.pageIndex)
        return page
    }

    fun markComplete(pageIndex: Int) {
        loadingPages.remove(pageIndex)
    }

    fun clear() {
        queue.clear()
        loadingPages.clear()
    }
}
```

**Integration into ComicReaderLayout:**
```kotlin
val pageLoadQueue = remember { PageLoadQueue() }

suspend fun loadPage(pageIndex: Int) {
    // Check if already loading
    if (pageLoadQueue.isLoading(pageIndex)) {
        return
    }

    // Determine priority
    val priority = when {
        pageIndex == currentPage -> LoadPriority.CURRENT
        pageIndex in preloadWindow -> LoadPriority.PRELOAD
        else -> LoadPriority.RETRY
    }

    pageLoadQueue.enqueue(pageIndex, priority)
}
```

**Implementation Steps:**
1. Create `PageLoadQueue.kt` file
2. Add queue instance to ComicReaderLayout (remember)
3. Modify `loadPage()` to use queue instead of direct loading
4. Add `isLoading()` check to queue
5. Add `markComplete()` call when page loads
6. Test: Rapid scrolling - pages should load in correct order
7. Test: Same page requested multiple times - should only load once

---

### 2.2 Adaptive Preloading Window

**Priority:** MEDIUM - Better memory management

**Problem:** Fixed window size (10 pages behind, 5 ahead) not optimal

**Solution:** Adjust window based on available memory and reading speed

**Implementation:**
```kotlin
// In ComicReaderLayout
data class PreloadConfig(
    val pagesBehind: Int = 10,
    val pagesAheadPaged: Int = 5,
    val pagesAheadWebtoon: Int = 10
)

val preloadConfig by remember { PreloadConfig() }

LaunchedEffect(currentPage, totalPages, comicReaderMode) {
    val (pagesBehind, pagesAhead) = when (comicReaderMode) {
        "PAGED" -> preloadConfig.pagesBehind to preloadConfig.pagesAheadPaged
        "WEBTOON" -> preloadConfig.pagesBehind to preloadConfig.pagesAheadWebtoon
        else -> preloadConfig.pagesBehind to 10
    }

    val minPage = maxOf(0, currentPage - pagesBehind)
    val maxPage = minOf(totalPages - 1, currentPage + pagesAhead)

    // Only preload if not already loading
    val pagesToLoad = (minPage..maxPage).filter { pageIndex ->
        !loadedPages.containsKey(pageIndex) && !pageLoadQueue.isLoading(pageIndex)
    }

    pagesToLoad.forEach { pageIndex ->
        loadPage(pageIndex)
    }
}
```

**Implementation Steps:**
1. Create `PreloadConfig` data class
2. Implement adaptive window sizing
3. Add memory pressure detection (optional: get available memory)
4. Test: Large chapters vs small chapters
5. Test: Rapid scrolling vs slow reading

---

### 2.3 Next Chapter Preloading

**Priority:** HIGH - Seamless chapter transitions

**Problem:** No automatic preloading of next chapter

**Solution:** Trigger preload when approaching chapter end

**In ReaderModel.kt:**
```kotlin
// Add next chapter preloading
private var nextChapter: ReaderChapter? = null
private var isPreloadingNextChapter = false

fun preloadNextChapter() {
    viewModelScope.launch {
        isPreloadingNextChapter = true
        // Load next chapter
        nextChapter = loadChapter(getNextChapterId())
        isPreloadingNextChapter = false
    }
}
```

**In ComicReaderLayout.kt:**
```kotlin
// Detect when should preload next chapter
val shouldPreloadNext by remember(currentPage, totalPages, isPreloadingNextChapter) {
    currentPage >= totalPages - 5 && !isPreloadingNextChapter
}

LaunchedEffect(shouldPreloadNext) {
    viewModel.preloadNextChapter()
}
```

**Implementation Steps:**
1. Add `preloadNextChapter()` to ReaderModel
2. Add chapter loading infrastructure if needed
3. Add `isPreloadingNextChapter` state tracking
4. Detect preload condition in ComicReaderLayout
5. Test: Read to last page → next chapter loads automatically
6. Test: Navigate away from last page → next chapter not preloaded

---

## Phase 3: Webtoon Optimization (3-5 days)

### 3.1 Implement WebtoonLayoutManager

**Priority:** HIGH - Critical for smooth vertical scrolling

**Problem:** LazyColumn not optimized for webtoon use case

**Solution:** Create custom LinearLayoutManager

**File to Create:** `presentation/reader/webtoon/WebtoonLayoutManager.kt`

```kotlin
/**
 * Custom LayoutManager for webtoon (vertical) mode
 * Based on Mihon's WebtoonLayoutManager implementation
 */
class WebtoonLayoutManager(
    context: Context,
    private val extraLayoutSpace: Int = 0
) : LinearLayoutManager(context) {

    init {
        // CRITICAL: Disable item prefetch for smooth scrolling
        isItemPrefetchEnabled = false

        // Increase cache size to reduce rebinds
        initialPrefetchItemCount = 4
    }

    /**
     * Returns position of last item whose end side is visible on screen
     * Used for accurate progress tracking in vertical scrolling
     */
    fun findLastEndVisibleItemPosition(): Int {
        ensureLayoutState()
        val callback = if (orientation == HORIZONTAL) {
            mHorizontalBoundCheck
        } else {
            mVerticalBoundCheck
        }.mCallback

        val start = callback.parentStart
        val end = callback.parentEnd

        // Search from bottom to find last visible item
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i) ?: continue
            val childStart = callback.getChildStart(child)
            val childEnd = callback.getChildEnd(child)

            // Item is visible if its end is within visible range
            if (childEnd <= end || childStart < start) {
                return getPosition(child)
            }
        }

        return NO_POSITION
    }

    /**
     * Override to provide extra layout space
     * This allows pages to be rendered even if not yet visible,
     * reducing blank view issues
     */
    @Deprecated("Deprecated in Java")
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        return extraLayoutSpace
    }
}
```

**Integration:** Use with `LazyVerticalGrid` via AndroidViewInterop

**Implementation Steps:**
1. Create `WebtoonLayoutManager.kt` file
2. Add `AndroidView` binding to use native RecyclerView
3. Disable item prefetch
4. Test: Smooth scrolling without blank views
5. Test: Progress tracking accuracy

---

### 3.2 Create WebtoonAdapter

**Priority:** HIGH - Efficient page updates

**Problem:** Need RecyclerView adapter with DiffUtil

**File to Create:** `presentation/reader/webtoon/WebtoonAdapter.kt`

```kotlin
/**
 * RecyclerView adapter for webtoon mode
 * Supports chapter transitions and efficient updates
 */
class WebtoonAdapter(
    private val viewer: WebtoonViewerInterface,  // Adapt to Compose context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<Any> = emptyList()
    var currentChapter: ReaderChapter? = null

    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        val newItems = mutableListOf<Any>()

        // Add previous chapter transition and last 2 pages
        if (chapters.prevChapter != null) {
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }

        // Add current chapter pages
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and first 2 pages
        if (chapters.nextChapter != null) {
            newItems.add(ChapterTransition.Next(chapters.currChapter, chapters.nextChapter))
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        updateItems(newItems)
    }

    private fun updateItems(newItems: List<Any>) {
        val result = DiffUtil.calculateDiff(Callback(items, newItems))
        items = newItems
        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ReaderPage -> PAGE_VIEW
            is ChapterTransition -> TRANSITION_VIEW
            else -> error("Unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            PAGE_VIEW -> WebtoonPageHolder(parent, viewer)
            TRANSITION_VIEW -> WebtoonTransitionHolder(parent, viewer)
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is WebtoonPageHolder -> holder.bind(item as ReaderPage)
            is WebtoonTransitionHolder -> holder.bind(item as ChapterTransition)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is WebtoonPageHolder -> holder.recycle()
            is WebtoonTransitionHolder -> holder.recycle()
        }
    }

    inner class Callback(
        private val oldItems: List<Any>,
        private val newItems: List<Any>,
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldItems[oldPos] == newItems[newPos]
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return true  // Contents always same for our use case
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size
    }

    companion object {
        private const val PAGE_VIEW = 0
        private const val TRANSITION_VIEW = 1
    }
}
```

**Implementation Steps:**
1. Create adapter with DiffUtil support
2. Create page holder with efficient loading
3. Add chapter transition support
4. Test: Chapter transitions are smooth
5. Test: Page updates trigger minimal recompositions

---

### 3.3 Integrate Webtoon Components

**Priority:** MEDIUM - Replace LazyColumn with RecyclerView

**Solution:** Use AndroidViewInterop to integrate native RecyclerView

**Implementation in ComicReaderLayout.kt:**
```kotlin
@Composable
fun ComicReaderLayout(...) {
    val isWebtoon = comicReaderMode == "WEBTOON"

    if (isWebtoon) {
        AndroidView(
            factory = { context ->
                WebtoonRecyclerView(context).apply {
                    layoutManager = WebtoonLayoutManager(context, extraLayoutSpace = 100.dp.toPx())
                    adapter = webtoonAdapter
                    itemAnimator = null  // Disable for smoothness

                    // Scroll listener to track current page
                    addOnScrollListener(
                        object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                val layoutManager = layoutManager as? WebtoonLayoutManager
                                val lastVisibleIndex = layoutManager?.findLastEndVisibleItemPosition() ?: return

                                // Map to logical page
                                val logicalPage = if (isRTL) {
                                    totalPages - 1 - lastVisibleIndex
                                } else {
                                    lastVisibleIndex
                                }

                                if (logicalPage != currentPage) {
                                    currentPage = logicalPage
                                    onPageChanged(logicalPage)
                                }
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            // Empty - actual content in RecyclerView
        }
    } else {
        // Existing Pager implementation
        HorizontalPager(...)
    }
}
```

**Implementation Steps:**
1. Create `WebtoonRecyclerView` wrapper
2. Add AndroidView binding to Compose
3. Integrate scroll listener
4. Test: Scroll performance vs LazyColumn
5. Test: Page change detection

---

## Phase 4: Chapter Transitions (2-3 days)

### 4.1 Create Chapter Transition Model

**Priority:** MEDIUM - Visual chapter boundaries

**Solution:** Create transition page concept like Mihon

**File to Create:** `domain/reader/ChapterTransition.kt`

```kotlin
/**
 * Represents a transition page between chapters
 * Allows users to see where one chapter ends and next begins
 */
sealed class ChapterTransition {
    /**
     * Transition from current chapter to previous chapter
     */
    data class Prev(
        val from: ReaderChapter,
        val to: ReaderChapter?
    ) : ChapterTransition()

    /**
     * Transition from current chapter to next chapter
     */
    data class Next(
        val from: ReaderChapter,
        val to: ReaderChapter?
    ) : ChapterTransition()

    /**
     * Label shown in transition pages
     */
    object Label {
        const val PREVIOUS_CHAPTER = "Previous Chapter"
        const val NEXT_CHAPTER = "Next Chapter"
    }
}
```

---

### 4.2 Create Transition Page Component

**File to Create:** `presentation/reader/webtoon/ChapterTransitionView.kt`

```kotlin
@Composable
fun ChapterTransitionView(
    transition: ChapterTransition,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (transition) {
            is ChapterTransition.Prev -> {
                Text(
                    text = ChapterTransition.Label.PREVIOUS_CHAPTER,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is ChapterTransition.Next -> {
                Text(
                    text = ChapterTransition.Label.NEXT_CHAPTER,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

---

### 4.3 Update Adapters for Transitions

**Priority:** MEDIUM - Integrate transition pages

**Modification to WebtoonAdapter:**
```kotlin
fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
    val newItems = mutableListOf<Any>()

    // Calculate gaps between chapters
    val prevHasGap = calculateChapterGap(chapters.currChapter, chapters.prevChapter) > 0
    val nextHasGap = calculateChapterGap(chapters.nextChapter, chapters.currChapter) > 0

    // Add previous chapter pages and transition
    if (chapters.prevChapter != null) {
        val prevPages = chapters.prevChapter.pages
        if (prevPages != null) {
            newItems.addAll(prevPages.takeLast(2))
        }

        // Always add transition if there's a gap
        if (prevHasGap || forceTransition) {
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }
    }

    // Add current chapter pages
    val currPages = chapters.currChapter.pages
    if (currPages != null) {
        newItems.addAll(currPages)
    }

    currentChapter = chapters.currChapter

    // Add next chapter transition and pages
    if (nextHasGap || forceTransition || chapters.nextChapter?.state != ReaderChapter.State.Loaded) {
        newItems.add(ChapterTransition.Next(chapters.currChapter, chapters.nextChapter))
    }

    if (chapters.nextChapter != null) {
        val nextPages = chapters.nextChapter.pages
        if (nextPages != null) {
            newItems.addAll(nextPages.take(2))
        }
    }

    updateItems(newItems)
}
```

**Implementation Steps:**
1. Add chapter gap calculation
2. Update adapter to include transition pages based on gaps
3. Create transition view component
4. Test: Scroll from page 45 → 46 → "Next Chapter" appears
5. Test: Scroll backward → "Previous Chapter" appears
6. Test: Tap on transition → continue to next chapter

---

## Phase 5: Caching Improvements (2-3 days)

### 5.1 Add Page List Caching

**Priority:** MEDIUM - Faster chapter loads

**Problem:** Archive parsing happens every time chapter is opened

**Solution:** Cache page list as JSON

**File to Modify:** `data/local/cache/ChapterListCache.kt` (new file)

```kotlin
import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import us.blindmint.codex.domain.reader.ReaderChapter

/**
 * Caches chapter page lists for faster chapter loads
 * Similar to Mihon's ChapterCache
 */
@Singleton
class ChapterListCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json
    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, "chapter_lists"),
        APP_VERSION,
        VALUE_COUNT,
        CACHE_SIZE,
    )

    companion object {
        private const val APP_VERSION = 1
        private const val VALUE_COUNT = 1
        private const val CACHE_SIZE = 100L * 1024 * 1024  // 100 MB
    }

    /**
     * Save page list for a chapter
     */
    suspend fun putPageList(chapterId: String, pages: List<ReaderPage>) {
        val cacheKey = "pages_$chapterId"
        val jsonPages = json.encodeToString(pages)

        try {
            val editor = diskCache.edit(cacheKey) ?: return
            editor.newOutputStream(0).sink().buffer().use {
                it.write(jsonPages.toByteArray())
            }
            editor.commit()
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Get cached page list for a chapter
     */
    suspend fun getPageList(chapterId: String): List<ReaderPage>? {
        val cacheKey = "pages_$chapterId"
        val snapshot = diskCache.get(cacheKey) ?: return null

        return try {
            val jsonPages = snapshot.getString(0)
            json.decodeFromString<List<ReaderPage>>(jsonPages)
        } catch (e: Exception) {
            null
        } finally {
            snapshot.close()
        }
    }

    /**
     * Clear cache for a specific chapter
     */
    suspend fun clearChapter(chapterId: String) {
        val cacheKey = "pages_$chapterId"
        diskCache.remove(cacheKey)
    }
}
```

**Integration into ReaderModel:**
```kotlin
@HiltViewModel
class ReaderModel @Inject constructor(
    private val chapterListCache: ChapterListCache,
    ...
) : ViewModel() {

    suspend fun loadChapter(chapterId: String): List<ReaderPage> {
        // Try cache first
        val cachedPages = chapterListCache.getPageList(chapterId)
        if (cachedPages != null) {
            return cachedPages
        }

        // Parse from archive (existing logic)
        val pages = parseArchive(chapterId)

        // Cache for next time
        chapterListCache.putPageList(chapterId, pages)

        return pages
    }
}
```

**Implementation Steps:**
1. Create `ChapterListCache.kt` file
2. Add to Hilt modules
3. Update `loadChapter` in ReaderModel to use cache
4. Test: Chapter reload - should be instant from cache
5. Test: Clear cache when archive changes

---

### 5.2 Coordinate Cache with Decoder

**Priority:** LOW - Optimize memory usage

**Problem:** ComicImageCache doesn't coordinate with page loading

**Solution:** Check cache before loading, promote to memory on load

**Modification to ComicReaderLayout.kt:**
```kotlin
suspend fun loadPage(pageIndex: Int) {
    val cacheKey = generateCacheKey(archiveId, pageIndex)

    // Check if already in memory
    val memoryBitmap = loadedPages[pageIndex]
    if (memoryBitmap != null) {
        return // Already loaded
    }

    // Check disk cache first (fast path)
    val diskBitmap = readerModel.comicImageCache.getPage(archiveId, pageIndex)
    if (diskBitmap != null) {
        loadedPages[pageIndex] = diskBitmap
        return
    }

    // Load from archive (slow path)
    loadFromArchive(pageIndex)
}
```

**Implementation Steps:**
1. Modify `loadPage` in ComicReaderLayout
2. Add `getPage` check to ComicImageCache flow
3. Test: Page reload from cache (fast)
4. Test: Page load from archive (slow)

---

## Phase 6: Viewer Abstraction (3-4 days)

### 6.1 Create Viewer Interface

**Priority:** MEDIUM - Clean architecture

**File to Create:** `presentation/reader/viewer/Viewer.kt` (new file)

```kotlin
/**
 * Viewer interface for different comic reading modes
 * Provides clean abstraction between paged and webtoon implementations
 */
interface Viewer {
    /**
     * Returns the root view for this viewer
     */
    fun getView(): View

    /**
     * Destroys this viewer and cleans up resources
     */
    fun destroy()

    /**
     * Sets the active chapters for this viewer
     */
    fun setChapters(chapters: ViewerChapters)

    /**
     * Moves to the specified page
     */
    fun moveToPage(page: ReaderPage)

    /**
     * Handles key events for this viewer
     */
    fun handleKeyEvent(event: KeyEvent): Boolean

    /**
     * Handles generic motion events
     */
    fun handleGenericMotionEvent(event: MotionEvent): Boolean
}
```

---

### 6.2 Create PagerViewer

**Priority:** MEDIUM - Improve paged mode architecture

**File to Create:** `presentation/reader/viewer/PagerViewer.kt` (new file)

```kotlin
/**
 * Pager-based viewer for LTR/RTL paged modes
 * Adapts Mihon's PagerViewer implementation to Compose
 */
abstract class PagerViewer(
    private val activity: ComponentActivity,
    private val viewer: ViewerInterface  // Adapt to Compose context
) : Viewer {

    protected val scope = MainScope()
    protected val config = PagerConfig(scope)

    /**
     * ViewPager2 implementation (native Android)
     * Use AndroidViewInterop to integrate with Compose
     */
    private val pager = remember { ViewPager2(context) }
    private var currentPage: Any? = null
    private var isIdle = true

    protected val adapter = PagerViewerAdapter(viewer, config)

    init {
        pager.offscreenPageLimit = 1
        pager.id = View.generateViewId()

        // Page change listener
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback {
            override fun onPageSelected(position: Int) {
                onPageChange(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                isIdle = state == ViewPager2.SCROLL_STATE_IDLE

                // Apply pending chapters when idle
                if (isIdle && pendingChapters != null) {
                    setChaptersInternal(pendingChapters!!)
                    pendingChapters = null
                }
            }
        })
    }

    /**
     * Called when page changes
     */
    private fun onPageChange(position: Int) {
        val page = adapter.items.getOrNull(position)
        if (page != null && currentPage != page) {
            currentPage = page
            viewer.onPageSelected(page)
        }
    }

    /**
     * Sets chapters, handling idle state
     */
    private var pendingChapters: ViewerChapters? = null

    override fun setChapters(chapters: ViewerChapters) {
        if (isIdle) {
            setChaptersInternal(chapters)
        } else {
            pendingChapters = chapters
        }
    }

    private fun setChaptersInternal(chapters: ViewerChapters) {
        adapter.setChapters(chapters)

        // Move to requested page
        val requestedPage = chapters.currChapter.requestedPage
        val pages = chapters.currChapter.pages ?: return
        if (requestedPage in pages.indices) {
            pager.setCurrentItem(requestedPage, false)
        }
    }

    override fun moveToPage(page: ReaderPage) {
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            pager.setCurrentItem(position, true)
        }
    }

    override fun getView(): View {
        return pager
    }

    override fun destroy() {
        scope.cancel()
    }

    // Abstract methods for different pager types
    abstract fun createPager(): ViewPager2
}
```

**Implementation Steps:**
1. Create Viewer interface
2. Create PagerViewer base class
3. Implement ViewPager2 integration
4. Add page change handling
5. Test: LTR and RTL modes
6. Test: Page transitions

---

### 6.3 Update ComicReaderLayout

**Priority:** MEDIUM - Use viewer abstraction

**Modification:**
```kotlin
@Composable
fun ComicReaderLayout(
    ...
    viewer: Viewer?,  // From ReaderModel state
) {
    if (viewer != null && viewer is PagerViewer) {
        AndroidView(
            factory = { context ->
                viewer.getView()  // Get native View
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**Implementation Steps:**
1. Add viewer state to ReaderModel
2. Update ComicReaderLayout to accept viewer parameter
3. Test viewer switching
4. Test: Maintain position across viewer changes

---

## Phase 7: Testing & Polish (2-3 days)

### Testing Checklist

**Phase 1 Fixes:**
- [ ] RTL/LTR sync fixed - test mid-chapter direction changes
- [ ] Progress updates instantly - test rapid scrolling
- [ ] SavedStateHandle works - test app kill/restore cycle
- [ ] Chapter preloads - test approaching chapter end

**Phase 2 Preloading:**
- [ ] Pages load in correct order - test rapid page jumps
- [ ] No duplicate page loads - test concurrency
- [ ] Adaptive window - test different chapter sizes
- [ ] Next chapter preloads - test chapter transitions

**Phase 3 Webtoon:**
- [ ] Smooth scrolling - test fast vertical scrolls
- [ ] No blank views - test continuous reading
- [ ] Progress accurate - test page change detection
- [ ] Chapter transitions - test moving between chapters

**Phase 4 Caching:**
- [ ] Page list cache - test chapter reload speed
- [ ] Memory management - test for leaks
- [ ] Disk cache works - test cache hits

**Phase 5 Architecture:**
- [ ] Viewer abstraction works - test switching modes
- [ ] Clean state flow - test event handling
- [ ] No race conditions - test concurrent operations

**Performance Testing:**
- [ ] Measure scroll FPS - target 60fps
- [ ] Measure page load time - target < 200ms from cache
- [ ] Measure memory usage - target < 150MB for images
- [ ] Test with large archives (100+ pages)

**User Experience Testing:**
- [ ] Test reading different comic formats (CBR, CBZ)
- [ ] Test on different devices (low-end, high-end)
- [ ] Test with system dark/light themes
- [ ] Test with accessibility settings enabled

---

## Quick Reference: Mihon File Locations

For detailed implementation reference, consult these Mihon files:

**Core Architecture:**
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt`

**Viewers:**
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/Viewer.kt` (interface)
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt`
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonViewer.kt`
- `/home/samurai/dev/git/mihon/app/src/main/java/androidx/recyclerview/widget/WebtoonLayoutManager.kt`

**Adapters:**
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewerAdapter.kt`
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonAdapter.kt`

**Page Loading:**
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/loader/HttpPageLoader.kt`
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/ui/reader/loader/PageLoader.kt`

**Caching:**
- `/home/samurai/dev/git/mihon/app/src/main/java/eu/kanade/tachiyomi/data/cache/ChapterCache.kt`

---

## Success Criteria

**Phase 1 Complete When:**
- [ ] RTL/LTR sync bug fixed and tested
- [ ] Progress debounce removed (immediate updates)
- [ ] SavedStateHandle integrated and working
- [ ] Chapter preload detection implemented
- [ ] All fixes tested and stable

**Phase 2 Complete When:**
- [ ] Priority queue implemented
- [ ] Adaptive preloading working
- [ ] Next chapter preloading functional
- [ ] All preloading features tested

**Phase 3 Complete When:**
- [ ] WebtoonLayoutManager created and integrated
- [ ] WebtoonAdapter with DiffUtil working
- [ ] AndroidView integration complete
- [ ] Smooth scrolling achieved (60fps)
- [ ] No blank views during scroll

**Phase 4 Complete When:**
- [ ] Chapter transition model created
- [ ] Transition views implemented
- [ ] Transitions work in both modes
- [ ] Seamless chapter transitions

**Phase 5 Complete When:**
- [ ] Page list cache implemented
- [ ] Cache coordination with decoder
- [ ] Cache hit/miss ratio improved
- [ ] Memory usage optimized

**Phase 6 Complete When:**
- [ ] Viewer interface created
- [ ] PagerViewer implemented
- [ ] ComicReaderLayout uses viewer abstraction
- [ ] Clean mode switching

**Phase 7 Complete When:**
- [ ] All test scenarios executed
- [ ] Performance targets met
- [ ] No critical bugs remaining
- [ ] User feedback collected (if testing with users)
- [ ] Ready for production release

---

## Notes

### Before Starting

1. **Create Feature Branch:**
   ```bash
   git checkout -b feature/comic-reader-modernization
   ```

2. **Read This Document Entirely:** Ensure you understand the full scope

3. **Work Incrementally:** Complete each phase before moving to the next

4. **Test Each Phase:** Don't wait until the end to test

5. **Keep Old Code Functional:** Use feature flags to switch between old/new implementations

### During Development

1. **Add Debug Logging:** Especially for RTL/LTR sync and progress updates
2. **Profile Memory:** Check for leaks after major changes
3. **Measure Performance:** Use Android Profiler for FPS and load times
4. **Test Edge Cases:** Empty archives, single-page comics, corrupted files

### Code Style

1. **Follow Mihon Patterns:** Naming, structure, organization
2. **Document Changes:** Comment why changes were made
3. **Keep Functions Small:** Prefer many small functions over large ones
4. **Use Kotlin Features:** Coroutines, flows, sealed classes

### When Submitting PR

1. **Reference This Document:** "See docs/comic-reader-modernization-checklist.md"
2. **Test Across Devices:** At least two different Android versions
3. **Performance Comparison:** Before vs after metrics
4. **Include Screenshots:** If visual changes were made

---

**End of Checklist**
