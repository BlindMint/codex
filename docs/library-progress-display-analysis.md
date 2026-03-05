# Library Progress Display & Speed Reading Analysis

Analysis of three related issues: speed reader progress rounding on library covers, progress button layout consistency, and PDF speed reading support.

---

## Issue 1: Speed Reader Progress Shows 99% on Library Covers

### Problem

When a book is fully read in the speed reader (word picker shows 100%, book info page shows 100%), the library cover progress pill shows 99%.

### Root Cause

There are two contributing factors:

**Factor A: Truncation via `.toInt()` in library covers**

The library cover progress calculation in both `LibraryItem.kt:132` and `LibraryListItem.kt:150` uses:

```kotlin
"${(book.data.speedReaderWordIndex.toFloat() / book.data.speedReaderTotalWords * 100).toInt()}%"
```

`.toInt()` truncates — so 99.9 becomes 99, not 100.

Meanwhile, the book info page (`BookInfoLayoutInfoProgress.kt:43`) uses:

```kotlin
val progress = (book.speedReaderWordIndex.toFloat() / book.speedReaderTotalWords).calculateProgress(1)
```

`calculateProgress(1)` formats to 1 decimal place: `99.9%`. This is closer to correct but still not 100%.

**Factor B: Word index is 0-based, never reaches `totalWords`**

The speed reader uses 0-based word indexing. The last word in a 1000-word book has index 999. Progress is calculated as `wordIndex / totalWords` = `999 / 1000` = `0.999`. This can never equal `1.0` because `wordIndex` maxes out at `totalWords - 1`.

This same off-by-one pattern was previously fixed for the normal reader's scroll-based progress (where `canScrollForward == false` now snaps to 100%). The speed reader needs an equivalent fix.

### Where the inconsistency occurs

| Location | Calculation | Last-word result (1000 words) |
|---|---|---|
| Library cover (grid) | `(999f / 1000 * 100).toInt()` | **99%** |
| Library cover (list) | `(999f / 1000 * 100).toInt()` | **99%** |
| Book info page | `(999f / 1000).calculateProgress(1)` | **99.9%** |
| Word picker slider | `String.format("%.1f", 999f / 1000 * 100)` | **99.9%** |

### Fix

Two changes needed:

1. **Snap to 100% when at the last word**: When `speedReaderWordIndex >= speedReaderTotalWords - 1`, the progress should be 1.0 (100%). This is the semantic fix — if you've reached the last word, you've completed the book.

2. **Use consistent formatting**: Replace the `.toInt()` truncation in library covers with the same `calculateProgress(1)` used by the book info page. This ensures all display locations show the same value.

### Files to modify

- `presentation/library/LibraryItem.kt:130-136` — grid cover speed progress calculation
- `presentation/library/LibraryListItem.kt:149-153` — list cover speed progress calculation
- `presentation/book_info/BookInfoLayoutInfoProgress.kt:41-48, 86-89` — book info progress (already better, but needs snap-to-100%)
- `presentation/reader/SpeedReadingBars.kt:70-71` — speed reader top bar progress
- `presentation/reader/SpeedReadingWordPickerSheet.kt:125-127, 401-403` — word picker progress display

---

## Issue 2: Progress Button Layout Inconsistency

### Current Behavior

**Grid layout (`LibraryItem.kt`):**
- **Before speed reader used**: Single pill at **bottom-right** with `X%`, clickable to open normal reader
- **After speed reader used**: Split pill at **bottom-center** with `⚡X% | Y%`, each half clickable to its respective reader

This creates two visual changes when a book is first opened in the speed reader:
1. **Position shift**: bottom-right → bottom-center
2. **Width change**: single value → split pill with two values and a divider

**List layout (`LibraryListItem.kt`):**
- Always shows normal reader button at bottom-right of cover
- Adds speed reader button at bottom-left when `speedReaderHasBeenOpened`
- No position shift (both positions are always the same), just an additional button appearing

### Options Considered

| Option | Pros | Cons |
|---|---|---|
| **A. Keep current** (single → split, position shifts) | No work needed | Jarring layout shift when speed reader is first used |
| **B. Always center single pill** | No position shift | Width still changes; centered single pill looks odd on small covers |
| **C. Always show split pill** (speed side empty/placeholder until used) | No layout shift at all | Wastes space; empty left half looks broken; confusing UX for books never speed-read |
| **D. Always bottom-center, single → split** (position consistent, width grows) | No position shift; clean single pill before speed reader | Minor width change, but this is expected and natural |

### Recommendation: Option D

Always position the progress pill at **bottom-center**. Before speed reader is used, show a single centered pill. After speed reader is used, the pill grows to include the split layout. The position anchor (bottom-center) stays constant, so the visual change is minimal — just the pill widening symmetrically.

This is the least disruptive change:
- Single pill at bottom-center looks natural (centered on the cover)
- When it grows to a split pill, it grows symmetrically from center — no jarring left/right jump
- No wasted space or confusing placeholder states
- The list layout already handles this well (separate buttons at fixed positions)

### Additional Change: Swap Split Pill Halves

The split pill should be `[reader% | ⚡speed%]` (normal left, speed right) to match the book info page layout where the normal reader button is on the left and the speed reader bolt button is on the right, and the progress bars show normal on top / speed below.

### Files to modify

- `presentation/library/LibraryItem.kt` — change single pill from `Alignment.BottomEnd` to `Alignment.BottomCenter`; swap split pill halves so normal reader is left, speed reader is right

---

## Issue 3: PDF Speed Reading Support

### Current State

Speed reading is currently blocked only for comics (`isComic` check), not for PDFs. The gating logic:

| Location | Check | Effect |
|---|---|---|
| `BookInfoScreen.kt:128` | `!book.isComic` | Allows PDF navigation to speed reader |
| `BookInfoLayoutButton.kt:80` | `!book.isComic` | Shows speed reader button for PDFs |
| `LibraryItem.kt:125` | `!book.data.isComic` | Shows speed progress on PDF covers |
| `SpeedReaderModel.kt:105` | `!loadedBook.isComic` | Loads words for PDFs |

So PDFs already have the speed reading navigation path available. The question is whether text extraction works well enough.

### Text Extraction Pipeline

`PdfTextParser.kt` uses Apache PDFBox (`PDFTextStripper`) to extract text from PDFs. This works well for:
- **Text-based PDFs**: Created from word processors, LaTeX, etc. — excellent extraction
- **OCR'd PDFs**: Scanned documents that have been through OCR — usually good extraction

It does **not** work for:
- **Image-only PDFs**: Scanned pages without OCR layer — no text to extract, returns empty/garbage

### Behavior When Text Extraction Fails

`SpeedReaderModel.kt:111-114` already handles this:

```kotlin
if (loadedWords.isEmpty()) {
    errorMessage.value = UIText.StringResource(R.string.error_could_not_get_text)
    isLoading.value = false
}
```

If a PDF has no extractable text, the speed reader shows an error message. This is acceptable behavior — the user sees a clear message rather than a broken reader.

### Recommendation

**No code changes needed** — PDF speed reading already works. The `isComic` gate correctly excludes only comic archives (CBR/CBZ/CB7), and PDFs pass through. The text extraction pipeline handles text-based PDFs well, and image-only PDFs fail gracefully with an error message.

If desired, the error message could be made more specific for PDFs (e.g., "This PDF does not contain extractable text") but this is a minor UX polish, not a functional issue.

### Optional Enhancement

For PDFs that are "images of books" (scanned without OCR), the only way to enable speed reading would be to add an OCR step. This is a significant feature addition (requiring Tesseract or ML Kit) and is outside the scope of this fix. The current behavior (error message) is appropriate.

---

## Summary

| # | Issue | Severity | Fix Required |
|---|-------|----------|-------------|
| 1 | Speed reader 99% on library covers | Bug | Yes — snap to 100% at last word + consistent formatting |
| 2 | Progress button layout shift | UX | Yes — change single pill to bottom-center alignment |
| 3 | PDF speed reading support | Question | No — already supported, fails gracefully for image-only PDFs |
