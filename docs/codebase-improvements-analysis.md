# Codex Codebase Improvement Analysis

**Branch:** `feature/content-hash-deduplication`
**Created:** 2026-02-23
**Status:** In Progress

---

## Overview

This document tracks potential improvements identified during a comprehensive codebase review. Items are categorized by priority and type.

---

## Priority Legend

- 🔴 **High** - Critical issues, potential bugs, or significant improvements
- 🟡 **Medium** - Important but not urgent, good quality-of-life improvements
- 🟢 **Low** - Nice-to-have, minor optimizations, code cleanup

---

## Analysis Progress

- [x] File hashing/deduplication implementation review
- [x] Data layer (Room, repositories, parsers)
- [x] Domain layer (use cases, models)
- [x] UI/Presentation layer
- [ ] Performance profiling
- [ ] Security review
- [ ] Error handling patterns
- [ ] Code duplication analysis

---

## Findings

### 1. Database & Data Layer

#### 1.1 Missing Database Indexes 🔴
**File:** `data/local/dto/BookEntity.kt`

The `BookEntity` has several columns frequently used in queries but no `@Index` annotations:
- `contentHash` - Used for duplicate detection (new query in BookDao)
- `filePath` - Used in `findBookByFilePath()`
- `opdsCalibreId` - Used for Calibre sync
- `opdsSourceId` - Used for filtering by OPDS source

**Recommendation:** Add indexes for these columns to improve query performance.

```kotlin
@Entity(indices = [
    Index(value = ["contentHash"], unique = false),
    Index(value = ["filePath"], unique = true),
    Index(value = ["opdsCalibreId"]),
    Index(value = ["opdsSourceId"])
])
```

**Impact:** Significant performance improvement for duplicate detection and library filtering.

---

#### 1.2 Empty `updateCoverImageOfBook` Implementation 🔴
**File:** `data/repository/BookRepositoryImpl.kt:344`

```kotlin
override suspend fun updateCoverImageOfBook(bookWithOldCover: Book, newCoverImage: CoverImage?) {
    TODO("Implement updateCoverImageOfBook")
}
```

This is called from `resetCoverImage()` and will crash at runtime.

**Recommendation:** Implement this method or remove the feature.

---

#### 1.3 Unused `preloadRecentBooksText()` Method 🟡
**File:** `data/repository/BookRepositoryImpl.kt:436`

```kotlin
override suspend fun preloadRecentBooksText() {
    // Implementation here
}
```

Empty implementation. Either implement or remove from interface.

---

#### 1.4 LRU Cache Size Configuration 🟡
**File:** `data/repository/BookRepositoryImpl.kt:65-68`

```kotlin
private val textCache = LruCache<Int, List<ReaderText>>(1024 * 1024 * 100) // 100MB
private val speedReaderWordCache = LruCache<Int, List<SpeedReaderWord>>(1024 * 1024 * 50) // 50MB
```

**Issue:** LruCache uses **entry count**, not byte size. These are counting entries with a value of 100MB and 50MB respectively, which effectively means unlimited cache.

**Recommendation:** Either:
1. Use a proper size-based cache (like `LruCache` with `sizeOf()` override)
2. Or change to reasonable entry counts (e.g., 5-10 books)

---

#### 1.5 Inefficient Author/Series/Tag Loading 🟡
**File:** `data/repository/BookRepositoryImpl.kt:440-486`

These methods load ALL books from the database to extract distinct values:

```kotlin
override suspend fun getAllAuthors(): List<String> = withContext(Dispatchers.IO) {
    val allBooks = database.getAllBooks()  // Loads entire database!
    // ... filter and sort
}
```

**Recommendation:** Consider caching these values or using SQL `DISTINCT` with JSON functions if possible.

---

### 2. Content Hashing Implementation

#### 2.1 Hashing Implementation ✅ Good
**File:** `domain/util/ContentHasher.kt`

Uses XXHash (fast, non-cryptographic) - good choice for content deduplication.

**Good practices observed:**
- Uses streaming hash for memory efficiency
- Proper suspend function with Dispatchers.IO
- Consistent seed value

---

#### 2.2 Missing Hash for OPDS Downloads 🟡
**File:** `domain/use_case/opds/ImportOpdsBookUseCase.kt`

Books downloaded from OPDS sources may not get hashed, potentially allowing duplicates if the same book is later imported locally.

**Recommendation:** Add content hashing to OPDS download flow.

---

#### 2.3 Large File Hashing Performance 🟢
**File:** `domain/util/ContentHasher.kt`

For very large files (100MB+ PDFs), consider:
- Hash first 1MB + file size + last modified as a fast pre-filter
- Then full hash only when pre-filter matches

---

### 3. Error Handling

#### 3.1 Silent Exception Swallowing 🟡
**File:** `ui/main/MainActivity.kt:331`

```kotlin
} catch (_: Exception) { }
```

This silently swallows all exceptions. At minimum, log for debugging.

---

#### 3.2 Generic Exception Catching Pattern 🟡
**Files:** 60+ files catch generic `Exception`

While not always wrong, consider catching specific exceptions where possible:
- `IOException` for file operations
- `SQLException` for database
- `CancellationException` should never be caught silently

---

### 4. Performance Concerns

#### 4.1 Fuzzy Search on Entire Library 🟡
**File:** `data/repository/BookRepositoryImpl.kt:82-99`

```kotlin
val allBooks = database.getAllBooks()
val filteredBooks = if (query.isBlank()) {
    allBooks
} else {
    allBooks.filter { bookEntity ->
        FuzzySearch.partialRatio(...) > 60
    }
}
```

For large libraries, this could be slow. Consider:
- SQLite FTS (Full-Text Search) extension
- Pre-filtering with simple `LIKE` before fuzzy matching
- Caching frequent search results

---

#### 4.2 File Size Check in `findExistingBook` 🟡
**File:** `data/repository/BookRepositoryImpl.kt:573-591`

This method iterates through ALL books and accesses their files to check sizes. Very expensive operation.

```kotlin
val allBooks = database.getAllBooks()
for (entity in allBooks) {
    val cachedFile = getCachedFile(entity) ?: continue
    // ... check file size
}
```

**Recommendation:** Store file size in `BookEntity` during import for quick comparison.

---

### 5. Code Quality

#### 5.1 TODOs to Address 🟡
| File | Line | TODO |
|------|------|------|
| `domain/library/book/Book.kt` | 38 | `// TODO: remove when UI updated` (category field) |
| `data/local/dto/BookEntity.kt` | 34 | Same TODO |
| `data/di/AppModule.kt` | 59 | `// TODO: Implement proper database encryption migration` |
| `presentation/settings/reader/speed_reading/...` | 24-26 | Speed reading background image options |

---

#### 5.2 Database Migration Pending 🔴
**File:** `data/local/room/BookDatabase.kt`

The database is at version 24, but `MIGRATION_24_25` exists (for contentHash) while `@Database(version = 24)`.

**Issue:** If the branch has been used, users will have schema mismatch. Need to verify migration strategy.

---

#### 5.3 Deprecated API Usage 🟢
**File:** `data/parser/comic/ArchiveReader.kt:59`

```kotlin
@Suppress("DEPRECATION") // SevenZFile(File) constructor deprecated
```

Track upstream library updates for proper replacement.

---

### 6. Security & Privacy

#### 6.1 Credential Storage ✅ Good
**File:** `data/local/dto/OpdsSourceEntity.kt`

Credentials are stored encrypted (`usernameEncrypted`, `passwordEncrypted`) rather than plaintext. Good.

---

#### 6.2 Cache Directory Cleanup ✅ Good
**File:** `ui/main/MainActivity.kt:498`

```kotlin
override fun onDestroy() {
    cacheDir.deleteRecursively()
    super.onDestroy()
}
```

Properly cleans temporary files.

---

### 7. Feature Gaps / Enhancements

#### 7.1 Import Duplicate File Cleanup 🟡
**File:** `ui/main/MainActivity.kt:404-424`

When a file is imported via external intent, it's copied to `imports/` directory. If the same file is imported again (different URI, same content), another copy is created.

**Recommendation:** Clean up duplicate import files, or reuse existing file if content matches.

---

#### 7.2 Bulk Import Progress Tracking 🟢
**File:** `domain/use_case/book/BulkImportBooksFromFolder.kt`

Progress callback exists but could be enhanced:
- Show estimated time remaining
- Show skip count (duplicates, errors)
- Allow cancellation mid-import

---

### 8. Architecture Improvements

#### 8.1 Use Case Granularity 🟢
Current: 46 use case files. Consider:
- Some use cases are very thin wrappers around repository methods
- Could consolidate related operations (e.g., all bookmark operations in one use case)

---

### 9. Compose/UI Observations

#### 9.1 LaunchedEffect Overuse in Speed Reading 🟡
**File:** `presentation/reader/SpeedReadingScreen.kt`

50+ `LaunchedEffect` blocks in a single file, many triggered by individual settings changes. This could cause recomposition storms.

**Recommendation:** Consider batching related settings into a data class and using a single `LaunchedEffect` with the data class as key.

---

#### 9.2 Good State Management Patterns ✅
- Consistent use of `MutableStateFlow` with `asStateFlow()` pattern across ViewModels
- Proper use of `derivedStateOf` for computed values
- `remember` blocks used appropriately

---

### 10. Parser System

#### 10.1 Parallelism in EPUB Parser ✅ Good
**File:** `data/parser/epub/EpubTextParser.kt:36`

```kotlin
private val dispatcher = Dispatchers.IO.limitedParallelism(3)
```

Good use of limited parallelism for EPUB parsing. Prevents resource exhaustion.

---

#### 10.2 Archive Handle Resource Management 🟡
**File:** `data/parser/comic/ArchiveReader.kt`

Archive handles hold open file descriptors. Ensure proper cleanup in `close()` methods.

---

## Statistics

| Metric | Value |
|--------|-------|
| Total Kotlin Files | 724 |
| Use Cases | 46 |
| Room Entities | 6 |
| TODOs Found | 10 |
| `@Suppress`/`@SuppressLint` | 16 |
| Exception Handlers | 153 |
| StateFlows | 73 |
| LaunchedEffects | 248 |
| Derived States | 21 |

---

## Next Steps

1. [x] Review and prioritize items with user
2. [x] Create individual issues/tickets for accepted improvements
3. [x] Address critical items (database indexes, TODO implementation)
4. [ ] Performance testing on large library (1000+ books)
5. [ ] Security audit of file handling

---

## Implemented Changes (2026-02-23)

### ✅ Completed

| Item | Description |
|------|-------------|
| Database Indexes | Added indexes for `contentHash`, `filePath` (unique), `opdsCalibreId`, `opdsSourceId` |
| File Size Field | Added `fileSize` to `BookEntity` and `Book` for faster duplicate detection |
| Database Migration | Created `MIGRATION_24_25` with new columns and indexes |
| LRU Cache Fix | Changed from byte-size values to entry counts (5 books text, 10 speed reader) |
| Cover Modification | Removed `updateCoverImageOfBook`, `canResetCover`, `resetCoverImage` and all related UI |
| Silent Exception | Added logging to silent exception handler in MainActivity |
| OPDS Hashing | Added content hashing and file size to OPDS download flow |
| Unused Method | Removed unused `preloadRecentBooksText()` implementation |

### Files Modified
- `BookEntity.kt`, `Book.kt`, `BookMapperImpl.kt` - Added fileSize
- `BookDatabase.kt` - Migration 24→25 with indexes
- `BookRepository.kt`, `BookRepositoryImpl.kt` - Removed cover methods, fixed LRU, simplified findExistingBook
- `BookInfoModel.kt`, `BookInfoEvent.kt`, `BookInfoState.kt` - Removed cover events
- `BookInfoScreen.kt`, `BookInfoContent.kt`, `BookInfoBottomSheet.kt`, `BookInfoScaffold.kt`, `BookInfoLayout.kt`, `BookInfoLayoutInfo.kt`, `BookInfoLayoutInfoCover.kt` - Removed cover UI
- `MainActivity.kt` - Added fileSize to imports, added logging
- `BulkImportBooksFromFolder.kt` - Added fileSize during import
- `ImportOpdsBookUseCase.kt` - Added contentHash + fileSize

### Files Deleted
- `BookInfoChangeCoverBottomSheet.kt`
- `BookInfoChangeCoverBottomSheetItem.kt`

---

## Notes

- Codebase follows Clean Architecture well
- Good use of Kotlin coroutines throughout
- Comprehensive format support (10+ formats)
- Material You design is well-implemented
- Error logging could be more consistent

---

## Implementation Summary (2026-02-23)

### Completed Changes

| Task | Status | Impact |
|------|--------|--------|
| Database indexes | ✅ Done | Faster duplicate detection, library filtering |
| Remove cover modification | ✅ Done | Cleaner codebase, no unused TODOs |
| Fix LRU cache config | ✅ Done | Proper memory management (5/10 entries vs 100MB/50MB) |
| Add fileSize field | ✅ Done | Faster duplicate checks without I/O |
| Database migration 24→25 | ✅ Done | Proper migration path for contentHash + indexes |
| Silent exception logging | ✅ Done | Better debugging capability |
| OPDS content hashing | ✅ Done | Deduplication works for downloaded books too |
| Remove unused preloadRecentBooksText | ✅ Done | Cleaner codebase |

### Build Verification
- ✅ `./gradlew assembleDebug` - BUILD SUCCESSFUL

