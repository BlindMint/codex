# Reader Progress Audit

Audit of progress-related issues across all three readers (normal, speed, comic/PDF).

## Issues Found

### 1. Library Refresh Spinner on Speed Reader Exit (FIXED)

**Severity:** UX / Medium
**File:** `SpeedReadingScreen.kt:138`

**Problem:** When exiting the speed reader, `LibraryScreen.refreshListChannel.trySend(0)` was used instead of `LibraryScreen.silentRefreshChannel.trySend(Unit)`. This caused the visible pull-to-refresh spinner to show in the library, whereas the normal reader and comic reader both used the silent refresh channel for a smooth return.

**Fix applied:** Changed to `silentRefreshChannel.trySend(Unit)` to match the normal/comic reader behavior.

---

### 2. Normal Reader `calculateProgress()` Still Has End-Snapping Issues

**Severity:** Low (currently mitigated by word-based tracking)
**File:** `ReaderModel.kt:1317-1341`

**Problem:** The old index-based `calculateProgress()` function has hard-coded returns of `0f` at index 0 and `1f` when the last visible item is the last index. This function is still used by:
- `OnGoToChapter` (line 275)
- `OnScrollToBookmark` (line 294)
- `OnRestoreCheckpoint` (line 326)

These call paths create `OnChangeProgress` events with index-based progress rather than word-based progress. However, the debounced `updateProgress()` flow will eventually overwrite these with word-based values after 300ms. The net effect is a brief flash of inaccurate progress on chapter jumps/bookmark navigations before the word-based value takes over.

**Proposed fix:** Replace calls to `calculateProgress()` in chapter/bookmark/checkpoint navigation with `calculateWordBasedProgress()`. This unifies all progress sources on the same word-based calculation.

---

### 3. Comic Reader Saves to Database on Every Page Turn

**Severity:** Low / Performance
**File:** `ReaderModel.kt:908-949` (`OnComicPageChanged`)

**Problem:** Every `OnComicPageChanged` event triggers a database write via `bookRepository.updateComicPdfProgress()` on `Dispatchers.IO`. For fast page flipping (e.g., swiping through several pages quickly), this generates a burst of sequential database writes.

This is not a correctness issue since Room handles threading safely, and the writes are on IO, but it is unnecessary I/O churn compared to the normal reader's debounced approach.

**Proposed fix:** Add debouncing similar to the normal reader (e.g., 300-500ms debounce on page change events). The exit handler (`OnLeave`) already saves the final page, so intermediate saves during rapid flipping are redundant. A simple approach:

```kotlin
// In ReaderModel, add a debounced job for comic progress:
private var comicProgressJob: Job? = null

// In OnComicPageChanged:
comicProgressJob?.cancel()
comicProgressJob = launch(Dispatchers.IO) {
    delay(500)
    bookRepository.updateComicPdfProgress(...)
}
```

The state update (in-memory) should still happen immediately for responsive UI; only the database write needs debouncing.

---

### 4. Speed Reader `onLeave()` Database Save Is Fire-and-Forget

**Severity:** Low / Data Loss Risk
**File:** `SpeedReaderModel.kt:186-190`

**Problem:** `onLeave()` launches a coroutine via `viewModelScope.launch` to save progress. Since this is called during `DisposableEffect.onDispose` (when the composable is being removed), the ViewModel may be cleared before the coroutine completes, especially on fast navigation. `viewModelScope` cancels all coroutines when the ViewModel is cleared.

The speed reader scaffold does call `onSaveProgress()` before `onExitSpeedReading()` when the user presses the back button (line 204), which is a synchronous path through the model. However, the `DisposableEffect.onDispose` (line 136) calls `onLeave()` as a fallback - and this fallback is the one at risk of being cancelled.

**Proposed fix:** Use `NonCancellable` context for the final save:

```kotlin
fun onLeave() {
    viewModelScope.launch(NonCancellable + Dispatchers.IO) {
        saveProgressToDatabase(currentProgress.floatValue)
    }
}
```

This ensures the database write completes even if the scope is being cancelled. The write is fast (single UPDATE statement) so this won't meaningfully delay cleanup.

---

### 5. Speed Reader Progress Not Saved to `progress` Field in Database

**Severity:** Medium / Display Inconsistency
**File:** `BookDao.kt:64-65`

**Problem:** The speed reader's `updateSpeedReaderProgress` query only updates `speedReaderWordIndex`:

```sql
UPDATE bookentity SET speedReaderWordIndex = :wordIndex, speedReaderHasBeenOpened = 1 WHERE id = :id
```

It does **not** update the `progress` field. Meanwhile:
- `LibraryItem.kt:68-69` (grid view) displays `book.data.progress` - this is the **normal reader** progress
- `LibraryListItem.kt:65-66` (list view) displays `book.data.displayProgress` which delegates to `book.data.progress`

The speed reader progress pill is computed on-the-fly from `speedReaderWordIndex / speedReaderTotalWords` in the library composables. This works for the dual-pill display, but means:
1. If a user **only** uses the speed reader and never opens the normal reader, the main `progress` field stays at 0 (or whatever it was last set to by the normal reader).
2. The `BookInfoLayoutButton` and `BookInfoLayoutInfoProgress` also use `book.progress` for the main progress display.

This is arguably by design (separate tracking for separate readers), but worth being aware of. If the intent is that the "primary" progress always reflects whichever reader was used most recently, this would need a unified progress field or a "last used reader" flag.

**Proposed fix (if desired):** No code change recommended unless you want a unified "last active" progress. The current dual-pill system handles this well for the library grid. However, the book info detail page could benefit from showing the speed reader progress alongside normal progress, similar to how the library covers do.

---

### 6. `OnScroll` Slider Uses Index-Based Positioning, Not Word-Based

**Severity:** Low / Inconsistency
**File:** `ReaderModel.kt:303-312`

**Problem:** The `OnScroll` event (triggered by the progress slider in the reader UI) calculates scroll position as:

```kotlin
val scrollTo = (_state.value.text.lastIndex * event.progress).roundToInt()
```

This is pure index-based, not word-based. So when dragging the slider, the position mapping is slightly different from the progress displayed (which is word-based). For books with large images, chapter headers, or spacing elements that have 0 words, the slider position won't perfectly correspond to the displayed percentage.

**Proposed fix:** Create an inverse of `calculateWordBasedProgress()` that converts a word-based progress fraction to a text item index. Use this for slider-based scrolling so the slider and the displayed progress use the same coordinate system.

---

### 7. Normal Reader Exit Saves `firstVisibleItemIndex` but Debounced Handler Saves `snappedIndex`

**Severity:** Very Low
**File:** `ReaderModel.kt:345-377` vs `ReaderModel.kt:1206-1243`

**Problem:** The debounced progress handler snaps to the nearest paragraph start (`findNearestParagraphStart`), but the exit handler saves `firstVisibleItemIndex` directly (line 357). This means:
- During reading, `scrollIndex` is a paragraph-snapped index
- On exit, `scrollIndex` is overwritten with the raw `firstVisibleItemIndex`

On next open, the book restores to the raw position rather than the snapped position. The visual difference is negligible (at most 3 items of spacing/headers), but it means the scroll position "jumps" slightly on restore compared to where the progress was last tracked.

**Proposed fix:** Apply `findNearestParagraphStart()` to the index saved in the exit handler as well, for consistency:

```kotlin
val snappedIndex = findNearestParagraphStart(firstVisibleItemIndex)
```

---

## Summary

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | Speed reader library refresh spinner | Medium | **Fixed** |
| 2 | Old `calculateProgress()` used in chapter/bookmark jumps | Low | Open |
| 3 | Comic reader DB write on every page turn | Low | Open |
| 4 | Speed reader `onLeave()` save may be cancelled | Low | Open |
| 5 | Speed reader doesn't update `progress` field | Medium | By design (note) |
| 6 | Scroll slider uses index-based vs word-based mapping | Low | Open |
| 7 | Exit saves raw index vs snapped index | Very Low | Open |
