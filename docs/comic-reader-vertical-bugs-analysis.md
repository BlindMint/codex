# Comic Reader Bug Analysis: Blank Pages & Slider Jump

## Overview

Two distinct bugs affecting the comic reader, primarily in vertical/webtoon mode:

1. **Blank pages**: Scrolling reveals pages that take up space but render as background color
2. **Slider jump**: Progress slider snaps back when navigating far ahead, then advances one page further with each retry

---

## Bug 1: Blank/Missing Pages in Vertical Mode

### Symptoms

- Scrolling down reveals pages that are entirely the background color
- Blank pages still occupy space in the scroll column (correct height)
- Navigating does not fix them
- Closing and re-opening sometimes fixes them or moves which pages are blank

### Root Cause: Concurrent Non-Thread-Safe Archive Reads

**File**: `ComicReaderLayout.kt` — `loadPage()` (lines 98–126)

In the vertical `LazyColumn` in `ComicReaderDisplay.kt`, every visible item fires its own `LaunchedEffect` that calls:

```kotlin
withContext(Dispatchers.IO) {
    pageImage = loadPage(logicalPage)
}
```

Since `Dispatchers.IO` is a thread pool, multiple items compose and launch concurrently. When 5–10 pages are visible, up to 10 calls to `loadPage()` can execute simultaneously on IO threads.

The `loadPage` function reads from the underlying archive handle. The archive handles have different thread-safety guarantees:

| Format | Class | Thread-Safe? |
|--------|-------|-------------|
| CBZ/ZIP | `LibArchiveHandle` → `java.util.zip.ZipFile` | ✅ Yes |
| CBR/RAR | `RarArchiveHandle` → `junrar.Archive` | ❌ No |
| CB7/7Z | `SevenZArchiveHandle` → `commons-compress SevenZFile` | ❌ No |

For CBR and CB7 files, concurrent calls to `getInputStream()` corrupt the archive's read state. The stream for one entry overlaps with another, causing `BitmapFactory.decodeStream()` to receive garbage or truncated data and return `null`.

### Secondary Problem: No-Retry on Failed Load

**File**: `ComicReaderDisplay.kt`, lines 455–460

```kotlin
LaunchedEffect(logicalPage) {
    if (pageImage == null) {          // ← one-shot guard
        withContext(Dispatchers.IO) {
            pageImage = loadPage(logicalPage)
        }
    }
}
```

The `if (pageImage == null)` guard means: if `loadPage` returns `null` (failure), `pageImage` stays `null` permanently for that item composition. The effect won't retry because `logicalPage` hasn't changed. The item is rendered as the placeholder `Box` with only `padding(vertical = 16.dp)`.

**Why it "takes up space"**: When an item is composed for the first time, the `LaunchedEffect` fires. During the brief window while the IO work executes, `pageImage == null` and only a 32dp placeholder is shown. Once the image loads, the item grows to full page height. If the load *fails*, the item stays at 32dp — but from the user's perspective these are often small enough that surrounding loaded pages "hold" the apparent region, making it look like the page is present but blank.

More likely for the "takes up space" observation: the blank is for items that *were* previously loaded (and thus measured by the LazyColumn at full height), then got re-composed after being scrolled out of viewport and back. On re-composition, `pageImage` starts `null` again (the `remember` state is reset), but the LazyColumn may estimate the item height from prior measurements while the item reloads. If the reload fails concurrently, the item renders blank at the estimated height.

### Also: Double Sort Bug in CBZ (LibArchiveHandle)

**File**: `ArchiveReader.kt`, lines 225–231

```kotlin
_entries.sortWith(Comparator { a, b ->
    NaturalOrderComparator.compare(a.entry.getPath(), b.entry.getPath())
})

// Sort entries by name for consistent ordering
_entries.sortBy { it.entry.getPath() }   // ← overwrites natural sort with lexicographic sort!
```

The natural sort (e.g., `page1 < page2 < page10`) is immediately overridden by a plain lexicographic sort (e.g., `page1 < page10 < page2`). For CBZ files, pages may load in wrong order. This is a separate but related visual defect.

---

## Bug 2: Progress Slider Jump / Stuck Advancing One Page at a Time

### Symptoms

- Dragging slider far ahead causes it to snap back to a page in the 15–25 range
- Each subsequent attempt to navigate past that point advances the stuck position by exactly one page

### Root Cause: Race Between `isDragging` State Reset and ViewModel Update

**File**: `ReaderBottomBarComicSlider.kt`, lines 56–62

```kotlin
var sliderValue by remember { mutableFloatStateOf((currentPage + 1).toFloat()) }
var isDragging by remember { mutableFloatStateOf(0f) }

LaunchedEffect(currentPage, isDragging) {
    if (isDragging == 0f) {
        sliderValue = (currentPage + 1).toFloat()
    }
}

// ...

LaunchedEffect(isDragged) {
    isDragging = if (isDragged) 1f else 0f
}
```

**Timeline of the race condition** (user at page N, drags to page M):

1. User drags to position M → `isDragged = true` → `isDragging = 1f`
2. `LaunchedEffect(currentPage, isDragging=1f)`: guard blocks update ✓
3. User releases at M → `onValueChangeFinished` fires → `onPageSelected(M-1)` queued to ViewModel on `Dispatchers.IO`
4. `isDragged` becomes `false` → `LaunchedEffect(isDragged)` → `isDragging = 0f`
5. **`LaunchedEffect(currentPage, isDragging=0f)` fires** — `currentPage` is STILL N (ViewModel hasn't processed the event yet)
6. → `sliderValue = N + 1` ← **SLIDER JUMPS BACK TO PAGE N+1**
7. ViewModel eventually processes event → `currentComicPage = M-1`
8. Recomposition: `currentPage = M-1`
9. `LaunchedEffect(currentPage=M-1, isDragging=0f)` fires → `sliderValue = M` ✓

The jump in step 6 is what the user sees. If `currentPage` at step 5 is ~page 20 (where the user was reading), the slider snaps to ~page 21.

### Why "Advances One Page Per Attempt"

After the race in step 6, there is a window where `sliderValue = N+1` is briefly visible AND the scroll infrastructure may process an intermediate `onPageChanged` event from the `LazyColumn` snapshotFlow:

When `scrollToItem(M-1)` fires from the `LaunchedEffect(currentPage=M-1)` effect, the `programmaticScrollTarget` is set to M-1. If there is any timing where `programmaticScrollTarget` resets to -1 (via the 100ms `delay`) before `firstVisibleItemIndex` reaches M-1, the snapshotFlow's condition:

```kotlin
if (scrollTarget == -1 || scrollTarget == physicalIndex) {
    onPageChanged(logicalPage)
}
```

…fires with whatever `firstVisibleItemIndex` happens to be at that instant — potentially an intermediate value during the scroll, or even the previous scroll position. Since `OnComicPageChanged` runs on `Dispatchers.IO` and both `OnComicPageSelected` and `OnComicPageChanged` are dispatched independently, a `OnComicPageChanged(~24)` event arriving *after* `OnComicPageSelected(M-1)` is processed can overwrite `currentComicPage` back to ~24.

The "one page further each time" pattern suggests that each successful navigation attempt commits the ViewModel to one page further than before, but the intermediate page change fires from a progressively later scroll position each time.

---

## Remediation

### Fix 1: Serialize Archive Reads with a Mutex

**Location**: `ComicReaderLayout.kt`

Add a `Mutex` to `ComicReaderLayout` and use it in `loadPage` to prevent concurrent archive access for non-thread-safe formats:

```kotlin
val loadMutex = remember { kotlinx.coroutines.sync.Mutex() }

suspend fun loadPage(pageIndex: Int): ImageBitmap? {
    // Fast path: already cached, no lock needed
    loadedPages[pageIndex]?.first?.let { return it }

    return loadMutex.withLock {
        // Double-check under lock (another coroutine may have loaded it)
        loadedPages[pageIndex]?.first?.let { return@withLock it }

        try {
            archiveHandle?.let { archive ->
                if (pageIndex < archive.entries.size) {
                    val entry = archive.entries[pageIndex]
                    archive.getInputStream(entry).use { input ->
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val bitmap = BitmapFactory.decodeStream(input, null, options)
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            loadedPages[pageIndex] = imageBitmap to bitmap
                            imageBitmap
                        } else null
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.w("CodexComic", "Failed to load page $pageIndex: ${e.message}", e)
            null
        }
    }
}
```

The double-check pattern means cached pages are returned without contention. Sequential archive reads are safe on all formats (ZIP, RAR, 7Z).

### Fix 2: Remove `if (pageImage == null)` Guard to Allow Retry

**Location**: `ComicReaderDisplay.kt`, vertical `LazyColumn` items (around line 455)

```kotlin
// Before:
LaunchedEffect(logicalPage) {
    if (pageImage == null) {
        withContext(Dispatchers.IO) {
            pageImage = loadPage(logicalPage)
        }
    }
}

// After:
LaunchedEffect(logicalPage) {
    withContext(Dispatchers.IO) {
        pageImage = loadPage(logicalPage)
    }
}
```

Without the guard, if `loadPage` returns a cached value, `pageImage` is just re-set to the same non-null value — harmless. If a prior load failed (null), this allows a retry when the item is re-composed. The `loadedPages` cache in `ComicReaderLayout` handles deduplication; the Mutex handles thread safety.

### Fix 3: Remove Double Sort in CBZ Loading

**Location**: `ArchiveReader.kt`, `LibArchiveHandle.loadArchive()` (lines 225–231)

Remove the redundant `_entries.sortBy { it.entry.getPath() }` line that overwrites the natural sort:

```kotlin
// Keep only:
_entries.sortWith(Comparator { a, b ->
    NaturalOrderComparator.compare(a.entry.getPath(), b.entry.getPath())
})
// Remove:
// _entries.sortBy { it.entry.getPath() }
```

### Fix 4: Eliminate the `isDragging` Race in the Slider

**Location**: `ReaderBottomBarComicSlider.kt`

Replace the `isDragging` float flag and the two-effect chain with a simpler single effect that checks the live `isDragged` state:

```kotlin
// Remove these:
// var isDragging by remember { mutableFloatStateOf(0f) }
// LaunchedEffect(currentPage, isDragging) { ... }
// LaunchedEffect(isDragged) { isDragging = if (isDragged) 1f else 0f }

// Add:
LaunchedEffect(currentPage) {
    if (!isDragged) {
        sliderValue = (currentPage + 1).toFloat()
    }
}
```

**Why this is safe**:
- `LaunchedEffect(currentPage)` only fires when `currentPage` changes
- At the moment the user releases the slider, `currentPage` has NOT changed yet (ViewModel is async), so this effect does NOT fire → no jump
- When the ViewModel updates `currentComicPage = newPage` → recomposition → `LaunchedEffect(currentPage=newPage)` fires → `isDragged = false` → `sliderValue = newPage + 1` ✓
- If the user starts dragging again before the ViewModel responds, `isDragged = true` suppresses the update ✓

---

## Summary Table

| Bug | File | Root Cause | Fix |
|-----|------|-----------|-----|
| Blank pages (CBR/CB7) | `ComicReaderLayout.kt` | Concurrent non-thread-safe archive reads | `Mutex` around `loadPage` archive section |
| No retry on failed load | `ComicReaderDisplay.kt` | `if (pageImage == null)` guards against retry | Remove the guard |
| Wrong page order (CBZ) | `ArchiveReader.kt` | Double sort: natural sort overwritten by lexicographic | Remove second `sortBy` call |
| Slider jump back | `ReaderBottomBarComicSlider.kt` | `isDragging` race resets slider before ViewModel responds | Use `LaunchedEffect(currentPage)` with `!isDragged` check |
