# Reader Analysis Document

## Overview

This document analyzes the four reader implementations in Codex:
1. **Normal Book Reader** (Text-based eBooks: EPUB, FB2, etc.)
2. **Comic Reader** (CBR, CBZ, CB7)
3. **PDF Reader**
4. **Speed Reader** (RSVP - Rapid Serial Visual Presentation)

---

## 1. Normal Book Reader (Text)

### 1.1 Loading Animation Issues

**Issue: Loading animation timing**
- **Location**: `ReaderModel.kt:145-202` (OnLoadText handler)
- **Problem**: The loading animation hides after a 300ms delay following scroll restoration. However, this relies on `snapshotFlow` emitting when `totalItemsCount` changes, which may not fire reliably if LazyColumn initialization is delayed.
- **Current Code**:
  ```kotlin
  // Line 180-182
  delay(300)
  _state.update { it.copy(isLoading = false, ...) }
  ```
- **Impact**: User may see the book content briefly before the loading animation disappears, or the animation may persist longer than expected.

**Issue: Double scroll restoration**
- **Location**: `ReaderModel.kt:1001-1026` and `ReaderScreen.kt:476-486`
- **Problem**: Scroll position restoration happens in TWO places:
  1. In `init()` via `OnLoadText` event after text loads
  2. In `ReaderScreen.kt` via `LaunchedEffect(state.value.initialScrollIndex)`
- **Risk**: Potential conflict if both execute, though the code attempts to coordinate via `initialScrollIndex = -1` sentinel value.

### 1.2 Race Conditions

**Issue: State corruption during transitions**
- **Location**: `ReaderModel.kt:86-91`, `ReaderModel.kt:1388-1398`
- **Problem**: The `isInitializing` volatile flag prevents `resetScreen()` from corrupting state during init, but this is a best-effort fix. The timing relies on:
  1. `init()` sets `isInitializing = true` on Main thread
  2. `resetScreen()` checks `isInitializing` after `yield()` on Main thread
- **Risk**: Under high system load, the race may still occur.

**Issue: Progress update race conditions**
- **Location**: `ReaderModel.kt:231-252` (OnChangeProgress)
- **Problem**: Multiple coroutines can update progress simultaneously:
  ```kotlin
  // Line 243-248 - database update happens after state update
  _state.update { ... }
  bookRepository.updateNormalReaderProgress(...) // No mutex protection here
  ```
- **Impact**: Last-write-wins for rapid scroll events.

### 1.3 Progress Tracking Issues

**Issue: Incomplete progress data on exit**
- **Location**: `ReaderModel.kt:327-406` (OnLeave)
- **Problem**: Progress is saved when leaving, but the 300ms debounce on scroll-based progress updates means recent scroll changes may be lost if the user exits quickly.
- **Mitigation**: `OnLeave` does capture current position synchronously.

**Issue: scrollOffset not always restored**
- **Location**: `ReaderModel.kt:159-177`
- **Problem**: When `scrollOffset > 0`, it's used directly. But if the screen size changes (rotation, split screen), the offset may position content incorrectly.

### 1.4 Performance Issues

**Issue: Expensive progress calculation**
- **Location**: `ReaderModel.kt:1268-1291`
- **Problem**: `calculateWordBasedProgress()` iterates through all text items on EVERY scroll event:
  ```kotlin
  val totalWords = _state.value.text.sumOf { ... } // O(n) every scroll
  ```
- **Impact**: For large books, this causes UI stuttering.

**Issue: Excessive recomposition**
- **Location**: `ReaderScreen.kt:471-473`
- **Problem**: `LaunchedEffect(listState)` with `snapshotFlow` fires on every scroll position change:
  ```kotlin
  snapshotFlow { listState.firstVisibleItemIndex ... }.debounce(300)
  ```
- **Impact**: While debounced, the lambda still runs frequently.

---

## 2. Comic Reader

### 2.1 Loading Animation Issues

**Issue: Immediate loading dismissal with jump**
- **Location**: `ComicReaderLayout.kt:289-294` and `ComicReaderLayout.kt:327-333`
- **Problem**: 
  1. `onLoadingComplete()` is called in `finally` block immediately when archive loading finishes
  2. `LaunchedEffect(totalPages)` then scrolls to initial page
  3. User sees loading animation disappear, THEN the visible scroll/jump to correct page
- **Current Code**:
  ```kotlin
  // Line 291-293
  isLoading = false
  initialLoadComplete = true
  onLoadingComplete()  // Sets isLoading = false in ReaderState
  
  // Then in lines 327-333
  LaunchedEffect(totalPages) {
      if (initialPage >= 0 && initialPage < totalPages) {
          pagerState.scrollToPage(targetPhysicalPage)  // JUMP HAPPENS HERE
      }
  }
  ```

**Issue: No loading state for page-by-page loading**
- **Location**: `ComicReaderLayout.kt:371-407` (within HorizontalPager)
- **Problem**: When navigating to unloaded pages, there's no loading indicator - just an empty box:
  ```kotlin
  } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          // Empty - no loading indicator
      }
  }
  ```

### 2.2 Race Conditions

**Issue: Multiple LaunchedEffects competing for scroll position**
- **Location**: `ComicReaderLayout.kt:327-355`
- **Problem**: Two `LaunchedEffect` blocks control scroll position:
  1. `LaunchedEffect(totalPages)` - for initial load
  2. `LaunchedEffect(currentPage, totalPages, isRTL, initialLoadComplete)` - for sync
- **Race**: The second effect checks `initialLoadComplete` but there's a window where both might execute.

**Issue: RTL direction change not atomic**
- **Location**: `ComicReaderLayout.kt:151-160`
- **Problem**: Page mapping functions use `totalPages` which may change during the calculation:
  ```kotlin
  val mapLogicalToPhysicalPage = { logicalPage: Int ->
      if (isRTL && totalPages > 0) totalPages - 1 - logicalPage else logicalPage
  }
  ```

### 2.3 Progress Tracking

**Issue: Page tracking relies on debounced flow**
- **Location**: `ComicReaderLayout.kt:210-219`
- **Problem**: 50ms debounce means rapid page changes could miss updates:
  ```kotlin
  .debounce(50)
  ```
- **Impact**: If user swipes quickly through multiple pages, intermediate pages may not be tracked.

**Issue: Progress saved on every page change**
- **Location**: `ReaderModel.kt:887-916`
- **Good**: Each page change triggers progress save.
- **Risk**: Database writes are frequent but should be acceptable.

### 2.4 Performance Issues

**Issue: Unbounded page cache**
- **Location**: `ComicReaderLayout.kt:117`
- **Problem**: `loadedPages` grows without limit:
  ```kotlin
  val loadedPages = remember { mutableMapOf<Int, Pair<ImageBitmap, Bitmap>>() }
  ```
- **Impact**: Memory usage grows with reading position; large comics could cause OOM.

**Issue: No prefetch strategy**
- **Location**: `ComicReaderLayout.kt:119-148`
- **Problem**: Only first 3 pages are preloaded:
  ```kotlin
  for (i in 0 until min(3, archive.entries.size)) { loadPage(i) }
  ```
- **Impact**: User experiences loading pauses when reading ahead.

---

## 3. PDF Reader

### 3.1 Loading Animation Issues

**Issue: Same jump issue as comic reader**
- **Location**: `PdfReaderLayout.kt:279-285` and `PdfReaderLayout.kt:240-243`
- **Problem**: Identical pattern to comics - loading completes, THEN scroll to initial page happens.

**Issue: Loading indicator disappears before render**
- **Location**: `PdfReaderLayout.kt:373-380`
- **Problem**: Same empty box issue as comics for pages that need rendering.

### 3.2 Race Conditions

**Issue: Render mutex doesn't prevent all races**
- **Location**: `PdfReaderLayout.kt:136-164`
- **Problem**: The mutex helps, but `pdfRenderer` could be null if cleanup happens during render:
  ```kotlin
  suspend fun renderPage(pageIndex: Int, renderer: PdfRenderer): ImageBitmap? {
      return renderMutex.withLock {
          // renderer could be closed between check and use
          loadedPages[pageIndex]?.first?.let { return@withLock it }
          // ...
      }
  }
  ```

### 3.3 Progress Tracking

**Issue: No progress tracking for vertical mode scroll**
- **Location**: `PdfReaderLayout.kt:179-188`
- **Problem**: Vertical mode uses `lazyListState.firstVisibleItemIndex` but this may not correlate correctly to PDF page since each item is one page.

**Issue: No progress save on PDF close**
- **Location**: `ReaderModel.kt:372-396`
- **Problem**: Comic/PDF progress IS saved in `OnLeave`, but if app crashes, the debounced scroll updates may be lost.

### 3.4 Performance Issues

**Issue: Same unbounded cache as comics**
- **Location**: `PdfReaderLayout.kt:93`
- **Problem**: Same issue - `loadedPages` map grows unbounded.

**Issue: No page rendering queue**
- **Problem**: Each page renders independently; rapid scrolling causes multiple concurrent renders.

---

## 4. Speed Reader

### 4.1 Loading Animation Issues

**Issue: Loading animation disappears BEFORE content is ready**
- **Location**: `SpeedReaderModel.kt:137` and `SpeedReadingScaffold.kt:275-287`
- **Problem**: 
  1. `SpeedReaderModel.loadBook()` sets `isLoading.value = false` when words load
  2. `SpeedReadingScaffold` shows loading until `words.isNotEmpty() && selectedWordIndex >= 0`
  3. But `selectedWordIndex` is set via `LaunchedEffect` which runs AFTER `isLoading` becomes false
  4. Result: Loading disappears, brief flash of empty/content-free state, then word appears

**Issue: 3-second countdown cannot be skipped**
- **Location**: `SpeedReadingScaffold.kt:86-97`
- **Problem**: Countdown always runs before playback starts:
  ```kotlin
  LaunchedEffect(showCountdown.value) {
      if (showCountdown.value) {
          countdownValue.intValue = 3
          while (countdownValue.intValue > 0) {
              delay(1000)  // 3 seconds minimum before playing
              countdownValue.intValue--
          }
          // ...
      }
  }
  ```
- **Impact**: User must wait 3 seconds every time they press play.

**Issue: No "resume from last position" smooth transition**
- **Problem**: When reopening a book, the speed reader shows loading, then the word at the saved position appears without any smooth transition.

### 4.2 Race Conditions

**Issue: Progress set to 0 before restoration**
- **Location**: `SpeedReaderModel.kt:80-82`
- **Problem**: Progress is explicitly set to 0 before the correct value is loaded:
  ```kotlin
  // Set initial progress to 0; will be updated after text loads
  currentProgress.floatValue = 0f
  ```
- **Impact**: Brief flash of "0%" before correct progress appears.

**Issue: Word extraction logic issues**
- **Location**: `SpeedReadingScreen.kt:57-63` (presentation layer)
- **Problem**: Words are extracted from `ReaderText.Text`:
  ```kotlin
  val words = remember(text, currentProgress) {
      val startIndex = (currentProgress * text.size).toInt()
      text.drop(startIndex)
          .filterIsInstance<ReaderText.Text>()
          .flatMap { it.line.text.split("\\s+".toRegex()) }
  }
  ```
- **Issue**: This extracts ALL words from remaining text into memory - for large books this is expensive and doesn't match how words are stored in database.

### 4.3 Progress Tracking Issues

**Issue: Progress not properly restored on re-entry**
- **Location**: `SpeedReaderModel.kt:125-135`
- **Problem**: While the code attempts to restore `speedReaderWordIndex`, there's a window where UI shows wrong position:
  ```kotlin
  currentWordIndex.intValue = initialIndex  // Set after words load
  currentProgress.floatValue = progress    // This may not match actual displayed word
  ```

**Issue: Missing periodic progress saves during playback**
- **Location**: `SpeedReadingContent.kt` (presentation layer)
- **Problem**: Progress is saved when:
  - User manually pauses
  - User picks a word
  - User exits
- **Missing**: No periodic auto-save during playback; if crash occurs mid-reading, progress could be lost.

**Issue: No progress save on app background**
- **Problem**: If user presses home during playback, progress at last manual pause is saved, but not during active playback.

### 4.4 Performance Issues

**Issue: Words list recreated on every progress change**
- **Location**: `SpeedReadingScreen.kt:57-63`
- **Problem**: The `words` derivation depends on `currentProgress`:
  ```kotlin
  val words = remember(text, currentProgress) {
      // This runs whenever currentProgress changes
  }
  ```
- **Impact**: For large books, re-deriving the word list is expensive.

**Issue: No word preloading**
- **Problem**: Speed reader reads words from the full text list; no caching of the parsed word array in database.

---

## Cross-Cutting Issues

### Loading Animation Timing Summary

| Reader | Loading Animation Issue | Severity |
|--------|------------------------|----------|
| Normal | 300ms delay exists but unreliable | Medium |
| Comic | Jumps to page AFTER loading completes | High |
| PDF | Same as comic | High |
| Speed | Disappears before content ready | High |

### Database Update Frequency

| Reader | Save Strategy | Risk of Lost Progress |
|--------|--------------|----------------------|
| Normal | 300ms debounce on scroll | Medium |
| Comic | On every page change | Low |
| PDF | On every page change | Low |
| Speed | Only on pause/exit | **High** |

### Memory Management

| Reader | Issue | Severity |
|--------|-------|----------|
| Normal | Text stored in memory | Acceptable (required for scrolling) |
| Comic | Unbounded page cache | High |
| PDF | Unbounded page cache | High |
| Speed | Full word list derived | Medium |

---

## Proposed Solutions Summary

### High Priority

1. **Fix loading animation timing for all readers**:
   - Do NOT set `isLoading = false` until scroll/scroll-to-item animation completes
   - Use `LazyListState.isAnimatingScroll` or similar to detect when scroll completes
   - Add minimum display time for loading animation (e.g., 500ms minimum)

2. **Fix speed reader progress tracking**:
   - Save progress periodically during playback (every 10-30 seconds or every N words)
   - Implement `onSaveProgress` call in SpeedReadingContent during active playback
   - Remove the 3-second countdown or make it optional

3. **Add prefetch for comic/PDF readers**:
   - Implement lookahead page loading (load N pages ahead and N pages behind)
   - Add LRU eviction for page cache to prevent OOM

### Medium Priority

4. **Improve normal reader scroll restoration**:
   - Consolidate scroll restoration logic to single location
   - Handle screen rotation properly

5. **Optimize progress calculations**:
   - Cache word counts instead of recalculating on every scroll
   - Use incremental updates rather than full recalculation

### Lower Priority

6. **Add loading indicators for lazy-loaded content**:
   - Comic/PDF pages that need rendering should show placeholder

7. **Improve speed reader word handling**:
   - Pre-parse and cache words in database
   - Don't recreate word list on every progress change

---

## Fixes Applied (Branch: fix/reader-loading-animation-and-progress-fixes)

### Completed Fixes:

1. **Normal Reader - Loading Animation** (`ReaderModel.kt`)
   - Added `isScrollRestorationComplete` state field
   - Changed from fixed 300ms delay to waiting for actual scroll completion via `snapshotFlow`
   - Loading animation now hides AFTER scroll animation completes

2. **Comic Reader - Loading Animation** (`ComicReaderLayout.kt`, `ReaderLayout.kt`, `ReaderEvent.kt`, `ReaderModel.kt`)
   - Added new `OnComicScrollRestorationComplete` event
   - Added `onScrollRestorationComplete` callback parameter
   - Loading animation now waits for scroll to saved position before hiding
   - Added LRU cache with MAX_CACHED_PAGES=50 to prevent unbounded memory growth

3. **PDF Reader - Loading Animation** (`PdfReaderLayout.kt`, `ReaderLayout.kt`)
   - Added `onScrollRestorationComplete` callback
   - Loading animation now waits for scroll animation to complete

4. **Speed Reader - Loading & Progress** (`SpeedReaderModel.kt`, `SpeedReadingScreen.kt`, `SpeedReadingContent.kt`)
   - Added `isReadyForDisplay` state to track when both words AND position are ready
   - Loading now hides only when both conditions are met
   - Added periodic progress saves during playback (every ~1000 words)
   - Made 3-second countdown skippable (by tapping during countdown)

5. **Normal Reader - Progress Calculation Optimization** (`ReaderModel.kt`)
   - Added caching for word-based progress calculation
   - Cache is invalidated when new text loads or screen resets
   - Changed from O(n) per-scroll to O(1) lookup using cumulative word counts

### Remaining Items (Not Implemented):

- Comic/PDF prefetch logic (caused build complexity issues)

### Additional Speed Reader Fixes Applied:

1. **Fixed variable shadowing bug** (`SpeedReadingScaffold.kt:207`)
   - Changed `val currentWordIndex = ...` to `val wordIndex = ...` 
   - The original code was shadowing the outer scope variable, causing incorrect progress to be saved on exit

2. **Removed dead code** (`SpeedReaderModel.kt:152-155`)
   - Removed unreachable `else if (!loadedBook.isComic) { isLoading.value = false }` block
   - This code could never execute since it was inside the outer `if (!loadedBook.isComic)` block

3. **paragraphIndex retained for quick picker**
   - Used for logical grouping in the quick picker (sentence-based grouping)
   - Quick picker shows rough word positions for progress visualization
   - Not used for actual reading (normal reader handles that)
