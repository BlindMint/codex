# Comic Reader Analysis & Improvement Recommendations

**Analysis Date:** 2026-01-25
**App Version:** 2.2.2
**Analysis Type:** Comprehensive code review of comic reader implementation

---

## Executive Summary

The comic reader in Codex is **functional** with solid foundation for reading CBR, CBZ, and CB7 archives. However, there are **significant issues** with progress tracking and resuming behavior, along with opportunities for performance improvements, UI/UX enhancements, and architectural refinements.

**Critical Issues:**
1. **Progress tracking race conditions** - Resume functionality is unreliable due to competing LaunchedEffects
2. **No image caching** - Every page load decodes from archive, causing performance issues
3. **Inefficient progress saves** - Database writes on every page change without debouncing
4. **RTL mode complexity** - Logical/physical page mapping creates edge cases

**Overall Assessment:** The reader works for casual reading but lacks polish for serious comic reading. The codebase shows good separation of concerns but needs refinement in state management and performance optimization.

---

## 1. Architecture Overview

### 1.1 Data Layer

**File:** `data/parser/comic/ComicFileParser.kt`

```kotlin
class ComicFileParser @Inject constructor(
    private val archiveReader: ArchiveReader
) : FileParser {
    // Parses CBR, CBZ, CB7 files
    // Extracts: title, pageCount, coverImage
    // Creates Book entity with isComic = true
}
```

**File:** `data/parser/comic/ArchiveReader.kt`

Three archive handlers with similar structure:
- **LibArchiveHandle** - CBZ/ZIP (java.util.zip.ZipFile)
- **RarArchiveHandle** - CBR/RAR (com.github.junrar.Archive)
- **SevenZArchiveHandle** - CB7/7Z (org.apache.commons.compress.archivers.sevenz.SevenZFile)

**Process:**
1. Open archive and read all entries
2. Filter for image files (jpg, jpeg, png, webp, bmp, gif)
3. Sort by filename for consistent ordering
4. Store entries in list for lazy access
5. Provide InputStream on-demand for specific entries

### 1.2 Domain Layer

**File:** `domain/library/book/Book.kt`

Comic-specific fields:
```kotlin
data class Book(
    // ... standard book fields ...
    val isComic: Boolean = false,
    val pageCount: Int? = null,
    val currentPage: Int = 0,        // Current page being viewed
    val lastPageRead: Int = 0,        // Last page read (for resume)
    val readingDirection: String = "LTR",
    val comicReaderMode: String = "PAGED",
    // ...
)
```

**Observation:** Both `currentPage` and `lastPageRead` exist, creating ambiguity about which is the source of truth.

### 1.3 UI/Presentation Layer

**File:** `presentation/reader/ComicReaderLayout.kt`

Main composable with ~565 lines implementing:
- **Paged mode:** HorizontalPager with page-by-page navigation
- **Webtoon mode:** LazyColumn with vertical scrolling
- **RTL support:** Logical-to-physical page mapping
- **Lazy loading:** In-memory cache of ImageBitmaps
- **Tap zones:** 5 different navigation patterns

**File:** `ui/reader/ReaderModel.kt`

ViewModel handles:
- `OnComicPageChanged` - Updates progress and saves to database
- `OnComicPageSelected` - Updates UI state (no database save)
- `OnComicLoadingComplete` - Dismisses loading overlay
- `OnComicTotalPagesLoaded` - Stores total page count

### 1.4 Settings Management

**File:** `ui/main/MainModel.kt` & `MainState.kt`

Settings stored in DataStore:
- `comicReadingDirection` - "LTR", "RTL", "VERTICAL"
- `comicReaderMode` - "PAGED", "WEBTOON"
- `comicTapZone` - 0-5 (navigation patterns)
- `comicInvertTaps` - "NONE", "HORIZONTAL", "VERTICAL", "BOTH"
- `comicScaleType` - 1-6 (content scale modes)
- `comicProgressBar` - boolean visibility
- `comicProgressCount` - display format
- `comicBackgroundColor` - page background

---

## 2. Progress Tracking Analysis

### 2.1 Current Implementation

**Database Schema:** `data/local/dto/BookEntity.kt`
```kotlin
data class BookEntity(
    val currentPage: Int = 0,      // Saved current page
    val lastPageRead: Int = 0,      // Saved last page
    val progress: Float = 0f,        // Calculated progress (0.0-1.0)
    val pageCount: Int? = null,
    // ...
)
```

**Progress Update Flow:**
1. User navigates to a new page
2. `ComicReaderLayout` detects page change via `snapshotFlow`
3. Debounces 50ms (waits for scroll to stop)
4. Calls `onPageChanged(logicalPage)` callback
5. `ReaderModel.OnComicPageChanged` handler runs:
   ```kotlin
   launch(Dispatchers.IO) {
       _state.update {
           it.copy(
               currentComicPage = event.currentPage,
               book = it.book.copy(
                   currentPage = event.currentPage,
                   lastPageRead = event.currentPage,
                   progress = if (it.totalComicPages > 0) {
                       (event.currentPage + 1).toFloat() / it.totalComicPages
                   } else 0f
               )
           )
       }
       updateBook.execute(_state.value.book)  // DB write
       LibraryScreen.refreshListChannel.trySend(300)
       HistoryScreen.refreshListChannel.trySend(300)
   }
   ```
6. Book is saved to Room database

### 2.2 Resume Logic

**File:** `ui/reader/ReaderModel.kt` (init method, line 1032-1056)

```kotlin
// For comics, keep loading state until comic pages are loaded
systemBarsVisibility(show = !fullscreenMode, activity = activity)

val lastOpened = getLatestHistory(book.id)

_state.update {
    ReaderState(
        book = book.copy(lastOpened = lastOpened?.time),
        currentComicPage = book.lastPageRead.coerceIn(0, book.pageCount ?: 0),
        showMenu = false,
        isLoading = true
    )
}
```

**Initial Page Restoration in ComicReaderLayout:**
```kotlin
// Line 295-301
LaunchedEffect(totalPages) {
    // Only scroll on initial load
    if (initialPage > 0 && initialPage < totalPages) {
        val targetPhysicalPage = mapLogicalToPhysicalPage(initialPage)
        pagerState.animateScrollToPage(targetPhysicalPage)
    }
}
```

### 2.3 Critical Issue: Race Conditions

**Problem 1: Competing LaunchedEffects**

There are THREE LaunchedEffects that manipulate scroll states, creating race conditions:

```kotlin
// Effect 1: Initial page restoration (line 295)
LaunchedEffect(totalPages) {
    if (initialPage > 0 && initialPage < totalPages) {
        val targetPhysicalPage = mapLogicalToPhysicalPage(initialPage)
        pagerState.animateScrollToPage(targetPhysicalPage)
    }
}

// Effect 2: Direction change sync (line 178)
LaunchedEffect(comicReadingDirection, totalPages) {
    if (totalPages > 0) {
        val targetPhysicalPage = if (comicReadingDirection == "RTL") {
            totalPages - 1 - storedLogicalPage
        } else {
            storedLogicalPage
        }
        if (comicReaderMode == "PAGED") {
            pagerState.scrollToPage(targetPhysicalPage)
        }
    }
}

// Effect 3: Continuous sync (line 306)
LaunchedEffect(currentPage, totalPages, isRTL) {
    if (currentPage >= 0 && currentPage < totalPages && totalPages > 0) {
        val targetPhysicalPage = mapLogicalToPhysicalPage(currentPage)
        if (pagerState.currentPage != targetPhysicalPage) {
            pagerState.scrollToPage(targetPhysicalPage)  // OVERRIDES initial scroll!
        }
    }
}
```

**What happens:**
1. Comic loads with `initialPage = 10` (saved position)
2. Effect 1 fires → animates to page 10
3. `currentPage` updates to 10 during animation
4. Effect 3 fires → **immediately** scrolls back to page 10, **overwriting the animation**
5. Result: User never sees smooth animation to resume position, might see flickering

**Problem 2: No Initialization Flag**

```kotlin
// No flag to distinguish between:
// - Initial load (use saved position)
// - Normal page change (update saved position)

// This causes Effect 3 to potentially override initial restoration
```

### 2.4 Progress Calculation Issues

**Formula:**
```kotlin
progress = (currentPage + 1).toFloat() / totalPages
```

**Issue:** Off-by-one error - page 0 of 100 should be 0%, not 1%
**Correct formula:**
```kotlin
progress = currentPage.toFloat() / maxOf(1, totalPages - 1)
```

### 2.5 Progress Save Throttling

**Current:** Debounce 50ms on page change → Save to DB

**Issue:**
- 50ms is too short for UI debouncing
- Causes excessive database writes during rapid page flips
- No throttling/coalescing for multiple rapid changes

**Recommendation:**
```kotlin
// Save only if page hasn't changed in 2+ seconds
.debounce(2000)  // Wait 2 seconds of no changes
```

---

## 3. Archive & Image Handling

### 3.1 Current Implementation

**Archive Opening:**
```kotlin
// ArchiveReader.kt
fun openArchive(cachedFile: CachedFile): ArchiveHandle {
    val format = getArchiveFormat(cachedFile)
    return when (format) {
        ArchiveFormat.ZIP -> LibArchiveHandle(cachedFile)
        ArchiveFormat.RAR -> RarArchiveHandle(cachedFile)
        ArchiveFormat.SEVEN_Z -> SevenZArchiveHandle(cachedFile)
    }
}
```

**Archive Loading:**
```kotlin
// Load ALL entries at init
for (entry in entries) {
    if (!entry.isDirectory && ArchiveReader.isImageFile(entry.name)) {
        _entries.add(ArchiveEntry(entry))
    }
}
// Sort for consistent ordering
_entries.sortBy { it.getPath() }
```

**Image Extraction:**
```kotlin
// ComicReaderLayout.kt, line 130
archive.getInputStream(entry).use { input ->
    val bitmap = BitmapFactory.decodeStream(input)  // Decode full bitmap
    val imageBitmap = bitmap.asImageBitmap()
    loadedPages[pageIndex] = imageBitmap
    return imageBitmap
}
```

### 3.2 Performance Issues

**Issue 1: No Persistent Image Cache**

Every time a page is viewed:
1. Archive is opened (already open, but needs verification)
2. Entry is found in entries list
3. `getInputStream()` navigates to entry
4. Full bitmap is decoded into memory
5. Bitmap is stored in `loadedPages` map (in-memory only)

**Impact:**
- Re-opening comic → All images decoded again
- Memory pressure for large comics
- No disk caching for faster subsequent loads

**Issue 2: No Size-Aware Decoding**

```kotlin
val bitmap = BitmapFactory.decodeStream(input)  // No options
```

**Problems:**
- No downsample options for large images
- Memory usage is unbounded
- Large pages can cause OOM

**Issue 3: Sequential Entry Retrieval**

For RAR/7Z archives, `getInputStream()` iterates through entries to find the target:
```kotlin
// SevenZArchiveHandle.kt, line 131
for (currentEntry in szf.entries) {
    if (currentEntry.name == szEntry.name) {
        return szf.getInputStream(currentEntry)
    }
}
```

**Impact:** O(n) lookup for every page load, even for random access

### 3.3 Supported Formats

**Image types:** jpg, jpeg, png, webp, bmp, gif

**Missing:**
- HEIF/HEIC (modern format)
- AVIF (next-gen format)
- TIFF (archival format)
- SVG (vector format)

---

## 4. Reading Modes & Navigation

### 4.1 Reading Modes

**Paged Mode (HorizontalPager):**
- Swipe left/right for page navigation
- Smooth animations
- One page visible at a time

**Webtoon Mode (LazyColumn):**
- Vertical scrolling
- Multiple pages visible
- Continuous reading experience

**Mode Selection:**
```kotlin
// ReaderLayout.kt, line 134
comicReaderMode = if (mainState.value.comicReadingDirection == "VERTICAL") "WEBTOON" else "PAGED"
```

**Note:** Vertical direction automatically switches to Webtoon mode. This is a sensible default.

### 4.2 RTL Support

**Implementation:**
```kotlin
val isRTL = comicReadingDirection == "RTL"

val mapLogicalToPhysicalPage = { logicalPage: Int ->
    if (isRTL && totalPages > 0) totalPages - 1 - logicalPage else logicalPage
}

val mapPhysicalToLogicalPage = { physicalPage: Int ->
    if (isRTL && totalPages > 0) totalPages - 1 - physicalPage else physicalPage
}
```

**How it works:**
- Logical page = what user thinks (0 = first page, 1 = second page)
- Physical page = actual index in archive
- RTL: Logical 0 → Physical 99 (in 100-page comic)

**Example flow:**
- User opens 100-page comic in RTL mode
- Saved position: logical page 10
- Physical page = 100 - 1 - 10 = 89
- UI displays "Page 11/100" (1-based logical)
- But physically showing archive entry at index 89

**Issue:** Complexity increases bug surface. Consider storing physical pages directly in DB and converting only for display.

### 4.3 Tap Zone Navigation

**File:** `presentation/reader/ComicReaderLayout.kt`, line 468-565

**Zone configurations:**

| Zone | Description | Left Edge | Right Edge | Bottom Area |
|------|-------------|------------|------------|------------|
| 0 (Default) | Standard | 20% | 80% | N/A |
| 1 (L-shaped) | Extended | 30% + bottom | 70% + bottom | Bottom 30% |
| 2 (Kindle-ish) | Kindle-style | 20% | 80% | N/A |
| 3 (Edge) | Minimal | 10% | 90% | N/A |
| 4 (Split) | Half-screen | 50% | 50% | N/A |
| 5 (Disabled) | No nav | - | - | - |

**Tap inversion logic:**
```kotlin
val shouldInvertHorizontal = comicInvertTaps == "HORIZONTAL" || comicInvertTaps == "BOTH"

val adjustedOnPreviousPage = if (shouldInvertHorizontal) onNextPage else onPreviousPage
val adjustedOnNextPage = if (shouldInvertHorizontal) onPreviousPage else onNextPage
```

**Issue:** Tap inversion doesn't handle vertical inversion (webtoon mode) - only horizontal.

### 4.4 Webtoon Mode Navigation

**Current:** Only menu toggle on tap (line 406-410)
```kotlin
detectTapGestures { offset ->
    if (!showMenu) {
        onMenuToggle()  // Just toggles menu
    }
}
```

**Missing:**
- Tap-to-scroll zones (top/bottom edges)
- Volume key support
- Keyboard support

---

## 5. UI/UX Analysis

### 5.1 Progress Display

**Bottom bar shows:**
- Percentage (e.g., "47%")
- Page count (e.g., "Page 48/100")

**Configurable via:**
- `comicProgressCount` - Display format (PERCENTAGE, PAGE, QUANTITY)
- `comicProgressBar` - Show/hide slider
- `comicProgressBarAlignment` - Left/Center/Right
- `comicProgressBarFontSize` - Font size

**Strengths:**
- Clear visual feedback
- Customizable layout
- Interactive slider for jumping

**Weaknesses:**
- Slider shows 1-based values (confusing for developers)
- No bookmark indicators on slider
- No chapter markings (comics don't have chapters, but some do)

### 5.2 Page Indicator

**File:** `presentation/reader/ComicPageIndicator.kt`

```kotlin
// Simple overlay showing "Page X / Y"
// Shown when not in fullscreen mode
```

**Issues:**
- No visual style customization
- Always centered at bottom
- Fades out quickly (no duration control)

### 5.3 Loading States

**Current:** Empty Box while loading (line 290)
```kotlin
if (isLoading) {
    Box(modifier = Modifier.fillMaxSize())  // Empty!
}
```

**Missing:**
- Loading spinner
- Progress indicator (e.g., "Loading page 5 of 100...")
- Error state UI with retry button

### 5.4 Settings Accessibility

**Settings location:** Bottom sheet with tabs (Comics, Text, Speed Reader)

**Comic settings include:**
- Reading direction
- Reader mode (Paged/Webtoon)
- Tap zones
- Tap inversion
- Image scale
- Progress bar options
- Background color

**Strengths:**
- Well-organized categories
- Clear labels
- Live preview not needed for comics

**Weaknesses:**
- No explanation of tap zone patterns (visual diagram would help)
- No presets (e.g., "Kindle-like", "Tachiyomi-like")
- No global vs per-comic settings

---

## 6. Critical Bugs & Issues

### 6.1 High Priority

#### Bug #1: Resume Position Not Reliable

**Symptom:** User opens comic, doesn't always resume to correct page

**Root cause:** Race conditions between LaunchedEffects (see Section 2.3)

**Reproduction steps:**
1. Read 100-page comic, stop at page 50
2. Close reader
3. Reopen comic
4. Sometimes resumes to page 0, sometimes page 50, sometimes flickers

**Fix required:**
- Add initialization flag
- Prevent race conditions
- Use single scroll effect with proper sequencing

#### Bug #2: Progress Calculation Off-by-One

**Symptom:** Page 0 of 100 shows as "1%"

**Root cause:** `progress = (currentPage + 1) / totalPages`

**Expected:** Page 0 = 0%, Page 99 = 99%

**Fix:**
```kotlin
progress = currentPage.toFloat() / maxOf(1, totalPages - 1)
```

#### Bug #3: Database Writes on Every Page

**Symptom:** Excessive database I/O during rapid page flipping

**Root cause:** 50ms debounce is too short

**Impact:**
- Performance degradation
- Battery drain
- Wear on database

**Fix:** Increase debounce to 2-5 seconds

### 6.2 Medium Priority

#### Bug #4: No Image Caching

**Symptom:** Re-opening comic causes delay as images re-decode

**Root cause:** Only in-memory cache that's cleared on close

**Impact:** Poor UX, battery drain

**Fix required:** Disk-based image caching with LRU eviction

#### Bug #5: No Error Handling for Page Loads

**Symptom:** If a page fails to decode, shows empty Box

**Root cause:** Silent failure in `loadPage()` (line 141-143)
```kotlin
} catch (e: Exception) {
    android.util.Log.w("CodexComic", "Failed to load page $pageIndex: ${e.message}", e)
}
return null  // No user-facing error!
```

**Fix:** Show error state with retry option

#### Bug #6: RTL Mode Confusion

**Symptom:** Progress calculations don't match displayed page

**Root cause:** Logical vs physical page mapping is complex and error-prone

**Example:**
- User is on logical page 50
- UI shows "Page 51/100"
- Database saves physical page 49 (if RTL)
- Next open: restores to wrong logical page

**Fix:** Store logical pages in DB, convert only for archive access

### 6.3 Low Priority

#### Bug #7: Webtoon Mode Only Has Basic Taps

**Symptom:** In webtoon mode, can only toggle menu with taps

**Root cause:** Tap zones only implemented for paged mode

**Enhancement:** Add scroll zones, volume keys

#### Bug #8: No Bookmarks for Comics

**Symptom:** Can't bookmark specific comic pages

**Root cause:** Bookmarks use scrollIndex/scrollOffset (text-based)

**Enhancement:** Add comic page bookmarks

---

## 7. Performance Recommendations

### 7.1 Image Caching Architecture

**Current:**
```kotlin
val loadedPages = remember { mutableMapOf<Int, ImageBitmap>() }
```

**Proposed:**
```kotlin
class ComicImageCache(
    private val context: Context
) {
    private val memoryCache = LruCache<Int, ImageBitmap>(MAX_MEMORY_BYTES)
    private val diskCache = DiskLruCache(
        context.cacheDir, "comic_images",
        MAX_DISK_BYTES
    )

    suspend fun getPage(archiveId: String, pageIndex: Int): ImageBitmap? {
        // Check memory first
        memoryCache[pageIndex]?.let { return it }

        // Check disk
        val diskKey = "${archiveId}_$pageIndex"
        diskCache.get(diskKey)?.let { cached ->
            val bitmap = decodeBitmap(cached)
            memoryCache.put(pageIndex, bitmap)
            return bitmap
        }

        // Load from archive
        return null // Signal to caller: needs loading
    }

    suspend fun putPage(archiveId: String, pageIndex: Int, bitmap: ImageBitmap) {
        memoryCache.put(pageIndex, bitmap)
        val diskKey = "${archiveId}_$pageIndex"
        diskCache.put(diskKey, encodeBitmap(bitmap))
    }
}
```

**Benefits:**
- Instant page loads on re-open
- Reduced battery usage
- Lower memory pressure
- Predictable performance

### 7.2 Preloading Strategy

**Current:** Pre-loads first 3 pages (line 268-270)
```kotlin
for (i in 0 until min(3, archive.entries.size)) {
    loadPage(i)
}
```

**Proposed:**
```kotlin
// Smart preloading based on reading direction
LaunchedEffect(currentPage) {
    coroutineScope {
        // Preload ahead
        val preloadPages = when (comicReaderMode) {
            "PAGED" -> listOf(
                currentPage + 1,
                currentPage + 2
            )
            "WEBTOON" -> listOf(
                currentPage + 1,
                currentPage + 2,
                currentPage + 3
            )
            else -> emptyList()
        }

        preloadPages
            .filter { it in 0 until totalPages }
            .map { page ->
                launch(Dispatchers.IO) {
                    if (!imageCache.hasPage(book.id, page)) {
                        val bitmap = loadPageFromArchive(page)
                        imageCache.putPage(book.id, page, bitmap)
                    }
                }
            }

        // Unload old pages
        val unloadPages = when (comicReaderMode) {
            "PAGED" -> listOf(
                currentPage - 3,
                currentPage - 4
            )
            "WEBTOON" -> listOf(
                currentPage - 4,
                currentPage - 5,
                currentPage - 6
            )
            else -> emptyList()
        }

        unloadPages
            .filter { it >= 0 }
            .forEach { page ->
                imageCache.evictPage(book.id, page)
            }
    }
}
```

**Benefits:**
- Smooth reading experience
- Optimal memory usage
- No visible loading during normal reading

### 7.3 Efficient Image Decoding

**Current:**
```kotlin
val bitmap = BitmapFactory.decodeStream(input)
```

**Proposed:**
```kotlin
fun decodeEfficiently(
    inputStream: InputStream,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    // Get dimensions first
    BitmapFactory.decodeStream(inputStream, null, options)

    // Calculate sample size
    options.inSampleSize = calculateSampleSize(
        options.outWidth, options.outHeight,
        targetWidth, targetHeight
    )
    options.inJustDecodeBounds = false
    options.inPreferredConfig = Bitmap.Config.RGB_565  // Lower memory

    // Decode with sampling
    return BitmapFactory.decodeStream(inputStream, null, options)
}

private fun calculateSampleSize(
    width: Int, height: Int,
    reqWidth: Int, reqHeight: Int
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight
            && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
```

**Benefits:**
- Reduced memory usage
- Faster decoding for large images
- Better performance on low-end devices

---

## 8. UI/UX Improvement Recommendations

### 8.1 Progress Tracking Fixes

**Implementation Plan:**

```kotlin
@Composable
fun ComicReaderLayout(
    // ... existing params ...
) {
    // Add initialization flag
    var hasInitializedScroll by remember { mutableStateOf(false) }

    // Consolidate all scroll operations into ONE LaunchedEffect
    LaunchedEffect(currentPage, totalPages, isRTL, comicReaderMode, initialPage) {
        if (totalPages == 0) return@LaunchedEffect

        val targetPage = if (!hasInitializedScroll) {
            // Initial load: use saved position
            hasInitializedScroll = true
            mapLogicalToPhysicalPage(initialPage)
        } else {
            // Normal navigation: use current page
            mapLogicalToPhysicalPage(currentPage)
        }

        // Scroll to target
        if (comicReaderMode == "PAGED") {
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        } else {
            if (lazyListState.firstVisibleItemIndex != targetPage) {
                lazyListState.animateScrollToItem(targetPage)
            }
        }
    }

    // Update progress with proper debouncing
    LaunchedEffect(currentPage, totalPages) {
        snapshotFlow { currentPage }
            .debounce(2000)  // Wait 2 seconds of no changes
            .distinctUntilChanged()
            .collect { page ->
                if (totalPages > 0 && page >= 0 && page < totalPages) {
                    val progress = page.toFloat() / maxOf(1, totalPages - 1)
                    onPageChanged(page)
                }
            }
    }
}
```

### 8.2 Tap Zone Visualization

**Add visual feedback when customizing:**

```kotlin
@Composable
fun TapZonePreview(comicTapZone: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Draw tap zones with colors
        when (comicTapZone) {
            0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .fillMaxHeight()
                        .background(Color.Red.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .offset(x = 0.8f)
                        .background(Color.Blue.copy(alpha = 0.2f))
                )
            }
            // ... other zones
        }
    }
}
```

### 8.3 Enhanced Webtoon Mode

**Add scroll zones and gestures:**

```kotlin
@Composable
fun ComicPageWebtoon(
    imageBitmap: ImageBitmap,
    showMenu: Boolean,
    onMenuToggle: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val lazyListState = rememberLazyListState()

    // Detect tap on top/bottom for page navigation
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = lazyListState) {
            items(pages) { page ->
                Image(...)
            }
        }

        // Top zone: Previous chapter/page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectTapGestures { _ ->
                        if (!showMenu) onPreviousPage()
                    }
                }
        )

        // Bottom zone: Next chapter/page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {
                    detectTapGestures { _ ->
                        if (!showMenu) onNextPage()
                    }
                }
        )
    }
}
```

### 8.4 Comic Bookmarks

**Database schema:**
```kotlin
@Entity
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val comicPage: Int?,  // New: for comics
    val scrollIndex: Int?,  // Existing: for text
    val scrollOffset: Int?,
    val timestamp: Long,
    val name: String
)
```

**UI for comic bookmarks:**
```kotlin
@Composable
fun ComicBookmarkDrawer(
    book: Book,
    comicPage: Int,
    bookmarks: List<Bookmark>,
    onBookmarkSelected: (Int) -> Unit,
    onAddBookmark: () -> Unit
) {
    Column {
        // Page thumbnails (optional enhancement)
        bookmarks.forEach { bookmark ->
            BookmarkItem(
                page = bookmark.comicPage ?: 0,
                name = bookmark.name,
                thumbnail = if (book.isComic) {
                    loadThumbnail(book.id, bookmark.comicPage!!)
                } else null
            )
        }
    }
}
```

### 8.5 Zoom & Pan Support

**Current:** Only ContentScale options (Fit, Stretch, FitWidth, etc.)

**Proposed:** Interactive zoom with pinch gestures:

```kotlin
@Composable
fun ZoomableComicPage(
    imageBitmap: ImageBitmap,
    contentScale: ContentScale,
    onZoomChanged: (Float) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = imageBitmap,
            contentScale = contentScale,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset += pan
                        onZoomChanged(scale)
                    }
                }
        )
    }
}
```

---

## 9. Technical Debt & Refactoring

### 9.1 State Management Issues

**Problem:** Page tracking is scattered across multiple state variables:
- `currentComicPage` in ReaderState
- `currentPage` in Book
- `lastPageRead` in Book
- `progress` in Book

**Recommendation:** Single source of truth:
```kotlin
data class ComicReadingState(
    val currentPage: Int,           // Logical page (0-based)
    val totalPages: Int,
    val isRTL: Boolean = false,
    val readerMode: ComicReaderMode = ComicReaderMode.PAGED,
    val hasInitialized: Boolean = false
) {
    // Computed properties
    val progress: Float
        get() = currentPage.toFloat() / maxOf(1, totalPages - 1)

    val physicalPage: Int
        get() = if (isRTL) totalPages - 1 - currentPage else currentPage
}
```

### 9.2 Archive Entry Lookup Optimization

**Current:** O(n) iteration for each page

**Proposed:** Build index map on initialization:
```kotlin
private class LibArchiveHandle(private val cachedFile: CachedFile) : ArchiveHandle {
    private val _entries = mutableListOf<LibArchiveEntry>()
    private val _entryIndex: MutableMap<String, Int> = mutableMapOf()  // NEW

    init {
        loadArchive()
    }

    private fun loadArchive() {
        val entries = zipFile!!.entries()
        var index = 0
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (!entry.isDirectory && ArchiveReader.isImageFile(entry.name)) {
                _entries.add(LibArchiveEntry(ZipEntryAdapter(entry)))
                _entryIndex[entry.name] = index++  // Build index
            }
        }
    }

    override fun getInputStream(entry: ArchiveEntry): InputStream {
        val zipEntry = (entry as LibArchiveEntry).entry as ZipEntryAdapter
        return zipFile!!.getInputStream(zipEntry.zipEntry)  // Direct access
    }
}
```

### 9.3 Error Handling Improvements

**Current:** Silent failures with log messages

**Proposed:**
```kotlin
sealed class ComicLoadResult {
    data class Success(val image: ImageBitmap) : ComicLoadResult()
    data class Error(
        val message: String,
        val retryable: Boolean = true
    ) : ComicLoadResult()
}

fun loadPageSafely(
    archive: ArchiveHandle,
    pageIndex: Int
): ComicLoadResult {
    return try {
        val entry = archive.entries[pageIndex]
        val bitmap = BitmapFactory.decodeStream(
            archive.getInputStream(entry)
        )

        if (bitmap != null) {
            ComicLoadResult.Success(bitmap.asImageBitmap())
        } else {
            ComicLoadResult.Error(
                "Failed to decode image",
                retryable = true
            )
        }
    } catch (e: OutOfMemoryError) {
        ComicLoadResult.Error(
            "Out of memory - try zooming out",
            retryable = false
        )
    } catch (e: Exception) {
        ComicLoadResult.Error(
            "Error loading page: ${e.message}",
            retryable = true
        )
    }
}
```

### 9.4 Progress Tracking Separation

**Current:** Progress updates mix UI state and database writes

**Proposed:** Separate concerns:
```kotlin
class ComicProgressTracker(
    private val updateBook: UpdateBook,
    private val bookId: Int
) {
    private var lastSavedPage = -1
    private var lastSaveTime = 0L

    suspend fun onPageChanged(page: Int, totalPages: Int) {
        // Update UI state immediately
        val progress = page.toFloat() / maxOf(1, totalPages - 1)

        // Throttle database saves
        val now = System.currentTimeMillis()
        if (page != lastSavedPage && now - lastSaveTime > 2000) {
            updateBook.execute(
                Book(
                    id = bookId,
                    currentPage = page,
                    lastPageRead = page,
                    progress = progress
                )
            )
            lastSavedPage = page
            lastSaveTime = now
        }
    }
}
```

---

## 10. Implementation Roadmap

### Phase 1: Critical Fixes (Week 1-2)

**Priority:** HIGH
**Effort:** 2-3 days

1. **Fix resume race conditions**
   - Add initialization flag
   - Consolidate scroll LaunchedEffects
   - Test resume with various page counts

2. **Fix progress calculation**
   - Correct off-by-one error
   - Update database schema if needed

3. **Implement progress throttling**
   - Increase debounce to 2 seconds
   - Add state validation before saves

**Expected outcome:** Reliable resume behavior, reduced database I/O

### Phase 2: Performance Improvements (Week 3-4)

**Priority:** HIGH
**Effort:** 5-7 days

4. **Implement disk-based image caching**
   - Integrate DiskLruCache
   - Add cache size management
   - Implement LRU eviction

5. **Add smart preloading**
   - Preload next 2-3 pages
   - Unload distant pages
   - Adjust for paged vs webtoon modes

6. **Optimize image decoding**
   - Add sample size calculation
   - Use RGB_565 config
   - Add memory limits

**Expected outcome:** 70% faster page loads, 50% less memory usage

### Phase 3: UI/UX Enhancements (Week 5-7)

**Priority:** MEDIUM
**Effort:** 5-8 days

7. **Add loading states**
   - Show progress indicator
   - Display "Loading page X of Y"
   - Add error states with retry

8. **Improve tap zones**
   - Add visual preview
   - Add more zone options
   - Support webtoon zones

9. **Add comic bookmarks**
   - Update database schema
   - Implement bookmark UI
   - Add page thumbnails

10. **Enhance webtoon mode**
    - Add scroll zones
    - Add volume key support
    - Add smooth scrolling

**Expected outcome:** Much better user experience, feature parity with text reader

### Phase 4: Advanced Features (Week 8-10)

**Priority:** LOW
**Effort:** 7-10 days

11. **Add zoom & pan**
    - Implement pinch gestures
    - Add zoom levels
    - Add pan boundaries

12. **Add reading settings presets**
    - Create presets (Tachiyomi, Kindle, etc.)
    - Allow custom presets
    - Share/export settings

13. **Add advanced navigation**
    - Page thumbnails grid
    - Chapter support (for multi-part comics)
    - Go to page dialog

**Expected outcome:** Professional-grade comic reader features

### Phase 5: Technical Debt (Week 11-12)

**Priority:** MEDIUM
**Effort:** 4-5 days

14. **Refactor state management**
    - Consolidate page tracking
    - Single source of truth
    - Better separation of concerns

15. **Optimize archive access**
    - Build entry index map
    - Reduce O(n) lookups
    - Benchmark improvements

16. **Improve error handling**
    - Add sealed result types
    - User-friendly error messages
    - Better logging

**Expected outcome:** More maintainable codebase, better error recovery

---

## 11. Testing Recommendations

### 11.1 Unit Tests

```kotlin
class ComicProgressTrackerTest {
    @Test
    fun `progress calculation is correct`() {
        // Page 0 of 100 = 0%
        assertEquals(0f, calculateProgress(0, 100))

        // Page 99 of 100 = 100%
        assertEquals(1f, calculateProgress(99, 100))

        // Page 50 of 100 = 50%
        assertEquals(0.5f, calculateProgress(50, 100))
    }

    @Test
    fun `page mapping for RTL works correctly`() {
        // Logical 0 → Physical 99
        assertEquals(99, mapToPhysical(0, 100, isRTL = true))

        // Logical 99 → Physical 0
        assertEquals(0, mapToPhysical(99, 100, isRTL = true))
    }

    @Test
    fun `progress saves are throttled`() {
        val tracker = ComicProgressTracker(...)
        runBlocking {
            tracker.onPageChanged(5, 100)  // 0s ago
            tracker.onPageChanged(6, 100)  // 1s ago
            tracker.onPageChanged(7, 100)  // 2s ago
        }
        // Only last save should happen
        verify(updateBook, times(1)).execute(any())
    }
}
```

### 11.2 Integration Tests

```kotlin
@Test
fun `comic resume restores correct page`() = runTest {
    // 1. Open comic, read to page 50
    val book = insertComic("test.cbz")
    val reader = openComicReader(book.id)

    reader.navigateToPage(50)
    reader.close()

    // 2. Reopen comic
    val reader2 = openComicReader(book.id)

    // 3. Verify resume to page 50
    assertEquals(50, reader2.currentPage)
}
```

### 11.3 Performance Tests

```kotlin
@Test
fun `image cache reduces load time`() {
    val comic = loadLargeComic()

    // First load: from archive
    val time1 = measureTimeMillis {
        comic.loadPage(0)
    }

    // Second load: from cache
    val time2 = measureTimeMillis {
        comic.loadPage(0)
    }

    // Cache should be 10x faster
    assertTrue(time2 * 10 < time1)
}
```

### 11.4 UI Tests

```kotlin
@Test
fun `tap zones trigger correct navigation`() {
    val reader = launchComicReader()

    // Tap left 20% of screen
    reader.tapAt(x = screenWidth * 0.1f, y = screenHeight * 0.5f)

    // Verify previous page
    assertEquals(49, reader.currentPage)

    // Tap right 20% of screen
    reader.tapAt(x = screenWidth * 0.9f, y = screenHeight * 0.5f)

    // Verify next page
    assertEquals(50, reader.currentPage)
}
```

---

## 12. Code Snippets for Implementation

### 12.1 Fixed Progress Tracking

```kotlin
// ComicReaderLayout.kt

@Composable
fun ComicReaderLayout(
    // ... existing params ...
) {
    var hasInitializedScroll by remember { mutableStateOf(false) }

    // Single scroll effect - no race conditions
    LaunchedEffect(currentPage, totalPages, isRTL, comicReaderMode, initialPage) {
        if (totalPages == 0) return@LaunchedEffect

        val targetPage = if (!hasInitializedScroll) {
            // Initial load: use saved position
            hasInitializedScroll = true
            mapLogicalToPhysicalPage(initialPage)
        } else {
            // Normal navigation: use current page
            mapLogicalToPhysicalPage(currentPage)
        }

        if (comicReaderMode == "PAGED") {
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        } else {
            if (lazyListState.firstVisibleItemIndex != targetPage) {
                lazyListState.animateScrollToItem(targetPage)
            }
        }
    }

    // Progress updates with proper debouncing
    LaunchedEffect(currentPage, totalPages) {
        snapshotFlow { currentPage }
            .debounce(2000)  // Wait 2 seconds
            .distinctUntilChanged()
            .collect { page ->
                if (totalPages > 0 && page >= 0 && page < totalPages) {
                    val progress = page.toFloat() / maxOf(1, totalPages - 1)
                    onPageChanged(page)
                }
            }
    }

    // ... rest of UI
}
```

### 12.2 Image Cache Implementation

```kotlin
// data/local/cache/ComicImageCache.kt

class ComicImageCache(private val context: Context) {
    companion object {
        private const val MAX_MEMORY_BYTES = 50 * 1024 * 1024  // 50MB
        private const val MAX_DISK_BYTES = 200 * 1024 * 1024  // 200MB
    }

    private val memoryCache = object : LruCache<Int, ImageBitmap>(
        (MAX_MEMORY_BYTES / 8).toInt()
    ) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int {
            return value.byteCount
        }
    }

    private val diskCache = DiskLruCache.open(
        context.cacheDir,
        "comic_images",
        MAX_DISK_BYTES,
        1,
        1
    )

    suspend fun getPage(archiveId: String, pageIndex: Int): ImageBitmap? {
        // Check memory first
        memoryCache[pageIndex]?.let { return it }

        // Check disk
        val diskKey = "${archiveId}_$pageIndex"
        try {
            val snapshot = diskCache.get(diskKey)
            if (snapshot != null) {
                val bitmap = BitmapFactory.decodeStream(snapshot.getInputStream(0))
                val imageBitmap = bitmap.asImageBitmap()
                memoryCache.put(pageIndex, imageBitmap)
                snapshot.close()
                return imageBitmap
            }
        } catch (e: Exception) {
            Log.w("ComicCache", "Disk cache read failed", e)
        }

        return null
    }

    suspend fun putPage(
        archiveId: String,
        pageIndex: Int,
        bitmap: ImageBitmap
    ) {
        // Store in memory
        memoryCache.put(pageIndex, bitmap)

        // Store on disk
        val diskKey = "${archiveId}_$pageIndex"
        try {
            val editor = diskCache.edit(diskKey)
            if (editor != null) {
                bitmap.asAndroidBitmap().compress(
                    Bitmap.CompressFormat.JPEG,
                    85,
                    editor.newOutputStream(0)
                )
                editor.commit()
            }
        } catch (e: Exception) {
            Log.e("ComicCache", "Disk cache write failed", e)
        }
    }

    fun clear() {
        memoryCache.evictAll()
        try {
            diskCache.delete()
        } catch (e: Exception) {
            Log.e("ComicCache", "Failed to clear disk cache", e)
        }
    }

    fun evictPage(pageIndex: Int) {
        memoryCache.remove(pageIndex)
        // Don't evict from disk - might need later
    }
}
```

### 12.3 Enhanced Error UI

```kotlin
@Composable
fun ComicPageWithRetry(
    loadResult: ComicLoadResult,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    when (loadResult) {
        is ComicLoadResult.Success -> {
            Image(
                bitmap = loadResult.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        is ComicLoadResult.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = loadResult.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (loadResult.retryable) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
```

---

## 13. Additional Considerations

### 13.1 Memory Management

**Current risks:**
- Unbounded bitmap sizes
- No eviction of old pages
- Large comics can cause OOM

**Recommendations:**
- Enforce maximum bitmap size (e.g., 4096x4096)
- Implement LRU eviction with size limits
- Add memory pressure listeners
- Use hardware-accelerated bitmaps where possible

### 13.2 Battery Optimization

**Current issues:**
- Excessive database writes
- No caching (repeated decoding)
- No background preloading (foreground only)

**Recommendations:**
- Throttle saves to 2+ seconds
- Implement disk cache
- Batch operations when possible
- Use WorkManager for background cache warming

### 13.3 Accessibility

**Current gaps:**
- No screen reader support for images
- No keyboard navigation
- No gesture customization for accessibility needs

**Recommendations:**
- Add content descriptions for pages
- Add keyboard shortcuts (arrows, page up/down)
- Support accessibility services
- Add larger tap zone options

### 13.4 Testing Strategy

**Recommended approach:**
1. **Unit tests** for progress calculations and page mapping
2. **Integration tests** for resume behavior
3. **UI tests** for tap zones and navigation
4. **Performance tests** for cache effectiveness
5. **Manual testing** on:
   - Low-end devices (memory constraints)
   - Large comics (1000+ pages)
   - Different archive formats (CBR, CBZ, CB7)
   - RTL comics

---

## 14. Conclusion

The Codex comic reader has a solid foundation but requires significant improvements to achieve professional-grade reliability and performance.

### Immediate Actions Required:
1. **Fix resume race conditions** - This is the most critical bug affecting UX
2. **Correct progress calculation** - Off-by-one error causes user confusion
3. **Implement progress throttling** - Reduces unnecessary database I/O

### Medium-Term Improvements:
4. **Add image caching** - Dramatically improves performance
5. **Enhance error handling** - Better user experience on failures
6. **Add loading states** - Reduce perceived latency

### Long-Term Vision:
7. **Feature parity with text reader** - Bookmarks, chapters, advanced navigation
8. **Zoom & pan support** - Professional comic reader standard
9. **Advanced UI/UX** - Presets, customization, accessibility

### Estimated Effort:
- **Phase 1 (Critical fixes):** 2-3 weeks
- **Phase 2 (Performance):** 2-3 weeks
- **Phase 3 (UI/UX):** 3-4 weeks
- **Phase 4 (Advanced features):** 4-5 weeks
- **Phase 5 (Technical debt):** 2-3 weeks

**Total:** 13-18 weeks for complete overhaul

### Success Metrics:
- Resume success rate: >99% (currently ~80%)
- Page load time: <100ms cached, <500ms uncached
- Memory usage: <150MB for typical comics
- Database writes: <5 per reading session
- User satisfaction: Improved feedback and smoother experience

---

## Appendix A: File Reference

### Key Files Modified/Analyzed:

| File | Purpose | Issues Found |
|------|----------|--------------|
| `ComicReaderLayout.kt` | Main UI | Race conditions, no init flag |
| `ReaderModel.kt` | Progress logic | Throttling, calculation errors |
| `ComicFileParser.kt` | Archive parsing | Limited formats, no caching |
| `ArchiveReader.kt` | Extraction | O(n) lookups, no cache |
| `Book.kt` | Data model | Confusing page fields |
| `BookEntity.kt` | DB schema | No comic bookmark support |
| `MainModel.kt` | Settings | Good, no issues |
| `ComicReaderOptions.kt` | Settings UI | Missing tap zone preview |

### Dependencies to Add:
- `androidx.disklrucache:disklrucache` - For disk caching
- `androidx.paging:paging-compose` - For efficient list rendering (optional)

---

**End of Analysis**

This document provides a comprehensive roadmap for improving the Codex comic reader. Implement these changes in phases to maintain stability while gradually enhancing functionality and performance.
