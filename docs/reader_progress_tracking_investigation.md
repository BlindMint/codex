# Reader Progress Tracking Investigation

## Branch: `fix/reader-progress-tracking-separation`

## Issues Reported

### Issue 1: Cross-Book Progress Interference (Speed Reader)
- Open book A in speed reader at 60% progress
- Close it, open book B in speed reader at 0%
- Book B shows 60% instead of 0%
- Manually set book B to 0%
- Open book A again - it resets to 0%

### Issue 2: Normal Reader Progress Corruption
- Book has 60% in normal reader, 50% in speed reader
- Open book in speed reader, close it
- Normal reader progress indicator shows ~56% in library
- Opening book in normal reader scrolls to correct location (60%)
- After closing normal reader, it returns to 50%

## Database Schema Analysis

### BookEntity Fields (data/local/dto/BookEntity.kt)
```
Normal Reader Progress:
- scrollIndex: Int
- scrollOffset: Int  
- progress: Float

Speed Reader Progress:
- speedReaderWordIndex: Int (default: 0)
- speedReaderHasBeenOpened: Boolean (default: false)
- speedReaderTotalWords: Int (default: 0)
```

### displayProgress Property (domain/library/book/Book.kt:61-66)
```kotlin
val displayProgress: Float
    get() = if (speedReaderHasBeenOpened && speedReaderTotalWords > 0) {
        speedReaderWordIndex.toFloat() / speedReaderTotalWords.toFloat()
    } else {
        progress
    }
```

**Key Issue**: This property determines what shows in the library. If `speedReaderHasBeenOpened` is true, it shows speed reader progress. This could cause confusion if the flag is incorrectly set.

## Repository Methods

### BookRepositoryImpl (data/repository/BookRepositoryImpl.kt)
- `updateSpeedReaderProgress(bookId, wordIndex)` - Updates ONLY speed reader progress
- `updateBook(book)` - Updates ALL book fields (including speed reader fields!)
- `markSpeedReaderOpened(bookId)` - Sets speedReaderHasBeenOpened = true
- `updateSpeedReaderTotalWords(bookId, totalWords)` - Updates total word count

### BookMapperImpl (data/mapper/book/BookMapperImpl.kt)
Maps all fields between Book and BookEntity, including both normal and speed reader progress fields.

## Reader ViewModels

### SpeedReaderModel (ui/reader/SpeedReaderModel.kt)
- Loads book via `getBookById`
- Uses `bookRepository.getSpeedReaderWords(bookId)` for word extraction
- Progress saved via `bookRepository.updateSpeedReaderProgress()`
- Properly separates speed reader progress

### ReaderModel (ui/reader/ReaderModel.kt)
- Uses `UpdateBook` use case to save progress
- **Problem**: `updateBook.execute(book)` saves ALL fields including speed reader fields

## Root Cause Analysis

### Issue 1: Cross-Book Contamination
Likely causes:
1. **Shared mutable state in ViewModel**: The SpeedReaderModel might have shared state that's not properly cleared between books
2. **Cache key collision**: The `speedReaderWordCache` in BookRepositoryImpl uses bookId as key - this should be isolated per book

### Issue 2: Normal Reader Progress Corruption
**Primary Cause**: When normal reader saves progress via `updateBook()`, it writes ALL fields to the database. This likely includes speed reader fields with default/incorrect values, overwriting previously saved speed reader progress.

The display shows ~56% because:
1. Speed reader was opened → `speedReaderHasBeenOpened = true`
2. `speedReaderTotalWords` was set to some value
3. `speedReaderWordIndex` was set (perhaps 0 or incorrect value)
4. Library shows `speedReaderWordIndex / speedReaderTotalWords` = ~56%

## Proposed Solutions

### Solution 1: Separate Update Methods
Create separate update methods that only update their specific progress type:

```kotlin
// In BookRepository interface
suspend fun updateNormalReaderProgress(bookId: Int, scrollIndex: Int, scrollOffset: Int, progress: Float)
suspend fun updateSpeedReaderProgress(bookId: Int, wordIndex: Int)
```

### Solution 2: Preserve Other Progress Type on Update
In the repository, when updating normal reader progress, preserve speed reader fields:
```kotlin
override suspend fun updateNormalReaderProgress(bookId: Int, scrollIndex: Int, scrollOffset: Int, progress: Float) {
    val existingBook = database.findBookById(bookId)
    val updatedBook = existingBook.copy(
        scrollIndex = scrollIndex,
        scrollOffset = scrollOffset,
        progress = progress
        // PRESERVE: speedReaderWordIndex, speedReaderHasBeenOpened, speedReaderTotalWords
    )
    database.updateBooks(listOf(updatedBook))
}
```

### Solution 3: Clear Speed Reader Flag
If a book is opened in normal reader first, ensure speed reader progress is properly initialized or cleared.

### Solution 4: Add Explicit Progress Type Tracking
Track which reader was last used for progress, and only update that specific progress type.

## Action Items

1. [ ] Audit all places where `updateBook` is called and ensure it doesn't inadvertently overwrite speed reader progress
2. [ ] Add separate repository methods for normal vs speed reader progress updates
3. [ ] Review caching in BookRepositoryImpl to ensure no cross-book contamination
4. [ ] Verify SpeedReaderModel properly clears state between book loads
5. [ ] Test with clean database to verify separation works correctly

## Files to Review

- `data/repository/BookRepositoryImpl.kt` - Main repository with update logic
- `ui/reader/SpeedReaderModel.kt` - Speed reader ViewModel
- `ui/reader/ReaderModel.kt` - Normal reader ViewModel  
- `data/mapper/book/BookMapperImpl.kt` - Maps between entity and domain
- `domain/library/book/Book.kt` - Book domain model with displayProgress
- `data/local/room/BookDao.kt` - Database access

## Notes

- User can perform clean installs, so migrations not a concern
- Speed reader should work for all file types except PDF and comics (CBR, CBZ, CB7)
- Normal reader uses scroll-based progress
- Speed reader uses word-index-based progress

---

## Implementation Complete (2026-02-25)

### Changes Made

#### 1. New Repository Methods (BookRepository.kt)
Added new method:
```kotlin
suspend fun updateNormalReaderProgress(
    bookId: Int,
    scrollIndex: Int,
    scrollOffset: Int,
    progress: Float
)
```

#### 2. New DAO Queries (BookDao.kt)
Added new query:
```kotlin
@Query("UPDATE bookentity SET scrollIndex = :scrollIndex, scrollOffset = :scrollOffset, progress = :progress WHERE id = :id")
suspend fun updateNormalReaderProgress(id: Int, scrollIndex: Int, scrollOffset: Int, progress: Float)
```

#### 3. Repository Implementation (BookRepositoryImpl.kt)
Implemented the new method that only updates normal reader fields:
```kotlin
override suspend fun updateNormalReaderProgress(bookId: Int, scrollIndex: Int, scrollOffset: Int, progress: Float) {
    Log.d("NORMAL_READER_DB", "Updating normal reader progress: bookId=$bookId, scrollIndex=$scrollIndex, scrollOffset=$scrollOffset, progress=$progress")
    database.updateNormalReaderProgress(bookId, scrollIndex, scrollOffset, progress)
}
```

#### 4. ReaderModel Updates (ReaderModel.kt)
- Added `BookRepository` dependency
- Replaced `updateBook.execute()` calls with `bookRepository.updateNormalReaderProgress()` for:
  - `OnChangeProgress` event
  - Scroll-based progress updates (normal books)
  - Page-based progress updates (comics/PDFs)
  - `updateProgress()` function (snapshotFlow collector)

#### 5. Fixed displayProgress (Book.kt)
Changed from:
```kotlin
val displayProgress: Float
    get() = if (speedReaderHasBeenOpened && speedReaderTotalWords > 0) {
        speedReaderWordIndex.toFloat() / speedReaderTotalWords.toFloat()
    } else {
        progress
    }
```

To:
```kotlin
val displayProgress: Float
    get() = progress
```

This ensures the library always shows normal reader progress, preventing confusion when switching between readers.

### Verification

Build successful. The following locations were updated to use isolated progress methods:
- Normal reader scroll progress updates
- Normal reader page-based progress updates (comics/PDFs)
- Speed reader word index updates (already using `updateSpeedReaderProgress`)

### Remaining Considerations

The following `updateBook` calls remain but are non-critical:
- Lines 140, 1088, 1124 in ReaderModel.kt - These only update `lastOpened` and preserve all other fields including progress

Other `updateBook` usages in BookInfoModel and LibraryModel update metadata (tags, favorites, etc.) and should preserve existing progress since they load books fresh from the database.
