# Import, Duplication Detection, and External File Handling Audit

**Date:** 2026-03-04  
**Scope:** Book import flow, duplicate detection, external file handling

---

## Executive Summary

The three investigated systems work together correctly for the primary use case: opening a book from an external source (Files app, email, etc.) detects existing library entries and avoids duplicates. The code uses content hashing as the primary deduplication mechanism, with file path tracking as a secondary check. There are some performance concerns for large libraries but no critical bugs.

---

## 1. External File Opening (ACTION_VIEW Intent)

**Entry point:** `MainActivity.kt:302-498` (`handleIncomingIntent` → `processFileImport`)

### Process Flow

1. User opens file from external app (Files, email, Downloads)
2. System dispatches `ACTION_VIEW` intent to Codex
3. `handleIncomingIntent()` captures the URI
4. `processFileImport()` executes:
   - Resolves display name from content URI
   - Computes **full content hash** of the file
   - Checks database for existing book by hash → opens existing if found
   - Checks imports folder for existing file by name → opens existing if found
   - Copies file to internal storage (`filesDir/imports/${timestamp}_${filename}`)
   - Parses book metadata and inserts into database
   - Navigates to reader

### What Works Well

- **Correct duplicate detection by content**: The hash-based lookup at line 394-404 correctly finds existing books in the library
- **Path normalization**: Multiple path formats checked (with/without `file://`, URL-decoded) in `BookRepositoryImpl.kt:542-548`
- **Mutex serialization**: Uses `fileImportMutex` to prevent race conditions when opening multiple files

### Issues Identified

#### Issue 1: Imports folder accumulation (Low severity)

**Location:** `MainActivity.kt:347-378`

The imports folder is never cleaned up. Files accumulate indefinitely:

```kotlin
val existingFile = importsDir.listFiles()?.find { file ->
    file.name.endsWith("_$fileName")
}
```

While the code checks for existing files by name, it never removes old files. For users who frequently open files from external sources, this could consume significant storage over months of use.

**Recommendation:** Add periodic cleanup of the imports folder on app startup, removing files not referenced in the database.

#### Issue 2: Same content, different filename = duplicate entry (Low severity)

**Location:** `MainActivity.kt:428-437`

When copying to internal storage, files are renamed with a timestamp prefix:

```kotlin
val targetFile = File(importsDir, "${System.currentTimeMillis()}_$fileName")
```

If a user opens the same book file twice but with different filenames (e.g., after renaming), the hash check passes correctly, but if they modify the file slightly or there's a timing issue, duplicate entries could result. The current implementation is generally correct but not bulletproof.

#### Issue 3: No progress feedback for large files (UX)

**Location:** `MainActivity.kt:385-392`

Large files take time to hash and copy, but users only see "Identifying book..." with no progress indication. For 100MB+ comic files, this could feel like the app is frozen.

**Recommendation:** Consider showing indeterminate progress or at least a spinner for files >10MB.

---

## 2. Bulk Import Process

**Primary locations:**
- `BulkImportBooksFromFolder.kt` - Local folder import
- `AutoImportCodexBooksUseCase.kt` - Codex Directory import
- `ImportProgressViewModel.kt` - Progress management

### Process Flow

1. Get all files from selected folder
2. Load **all existing book paths** into memory
3. Filter files: supported extension + not already imported
4. For each file:
   - Parse book metadata
   - Compute **partial hash** (first 64KB) for duplicate detection
   - Check hash against database
   - Insert if not duplicate

### What Works Well

- **Partial hash optimization**: Uses 64KB partial hash for fast duplicate detection (`ContentHasher.kt:34-46`)
- **Progress reporting**: Good UX with filename and count displayed during import
- **Graceful failure**: Individual file failures don't stop the entire import

### Issues Identified

#### Issue 1: Loading ALL books into memory (Performance - Medium)

**Location:** `BulkImportBooksFromFolder.kt:42`

```kotlin
val existingPaths = bookRepository.getBooks("").map { it.filePath }
```

This loads **every book** into memory just to get file paths. For a library of 2,000 books, this creates significant memory pressure and latency. A dedicated lightweight query returning only paths would be far more efficient.

**Same issue in:**
- `AutoImportCodexBooksUseCase.kt:64`
- Multiple metadata queries in `BookRepositoryImpl.kt`

**Recommendation:** Create a `getAllBookPaths(): List<String>` or `getAllFilePaths()` method in the DAO that only queries the filePath column.

#### Issue 2: Sequential processing only (Performance - Low)

**Location:** `BulkImportBooksFromFolder.kt:62-105`

All files are processed sequentially in a single coroutine. For large imports (500+ files), this could be slow. However, given the I/O-bound nature of file parsing, parallelization would require careful thread management and could introduce complexity. The current approach is acceptable.

#### Issue 3: No batch database transactions (Performance - Low)

Each book is inserted individually via `insertBook.execute()`. For large imports, wrapping inserts in a transaction would significantly improve performance:

```kotlin
database.runInTransaction {
    books.forEach { insertBook.execute(it) }
}
```

#### Issue 4: Progress callback updates state on every file (Performance - Low)

**Location:** `ImportProgressViewModel.kt:76-88`

Every file progress update creates a new list copy. For large imports, this generates many state emissions. Minor issue but could be optimized with debouncing.

---

## 3. Duplication Detection

**Primary locations:**
- `BookEntity.kt` - Database schema with indices
- `BookDao.kt` - Query methods
- `ContentHasher.kt` - Hash computation
- `BookRepositoryImpl.kt:529-558` - Lookup logic

### Mechanisms

| Mechanism | Implementation | Coverage |
|-----------|---------------|----------|
| Content Hash | XXHash64 on file content | Identical files |
| Partial Hash | XXHash64 on first 64KB | Fast duplicate detection during bulk import |
| File Path | Unique index | Prevents same path imported twice |
| Path Variants | Multiple lookup keys | Handles URI encoding differences |

### Database Indices

```kotlin
@Entity(
    indices = [
        Index(value = ["contentHash"]),      // Non-unique index
        Index(value = ["filePath"], unique = true),  // Unique constraint
        Index(value = ["opdsCalibreId"]),
        Index(value = ["opdsSourceId"])
    ]
)
```

### What Works Well

- **Unique filePath constraint**: Prevents importing the same file path twice at the database level
- **Content hash lookup**: Catches identical files even if moved/renamed
- **Path normalization**: Handles `file://` prefix, URL encoding variations

### Issues Identified

#### Issue 1: contentHash not unique - same content, different paths allowed (Design decision)

Two books with different file paths but identical content will both exist in the database. The partial hash check during bulk import catches this, but there's no enforcement at the database level. This is likely intentional (allows users to have "copies" if they want) but worth noting.

#### Issue 2: Empty hash not rejected (Minor)

`GetBookByContentHash` returns null for blank hashes, which is correct, but there's nothing preventing a book with empty contentHash from being inserted. Should consider adding a check.

---

## 4. Cross-Cutting Performance Concerns

### 4.1 getAllBooks() called everywhere

The pattern `bookRepository.getBooks("")` to get all books is used in many places:

| Location | Purpose |
|----------|---------|
| `BulkImportBooksFromFolder.kt:42` | Get paths for duplicate check |
| `AutoImportCodexBooksUseCase.kt:64` | Get paths for duplicate check |
| `BookRepositoryImpl.kt:401` | Get all authors |
| `BookRepositoryImpl.kt:419` | Get all series |
| `BookRepositoryImpl.kt:429` | Get all tags |
| `BookRepositoryImpl.kt:439` | Get all languages |
| `BookRepositoryImpl.kt:449` | Get all metadata |
| `BookRepositoryImpl.kt:506-508` | Get books by OPDS source |
| `BookRepositoryImpl.kt:517-519` | Get books by OPDS source ID |

**Impact:** All of these load full BookEntity objects (including covers, descriptions, etc.) when only a small subset of fields is needed. For a 1,000 book library, this could mean loading 50MB+ of data unnecessarily.

**Recommendation:** Add targeted queries:
- `getAllBookPaths(): List<String>`
- `getAllAuthors(): List<String>` (existing but inefficient)
- `getAllTags(): List<String>` (existing but inefficient)

### 4.2 No pagination in library

`LibraryModel.kt:672-688` loads all books into memory:

```kotlin
private suspend fun getBooksFromDatabase(...) {
    val allBooks = bookRepository.getBooks(query)  // All books!
    val filteredBooks = applyFilters(allBooks, ...)
    // ...
}
```

For libraries with 2,000+ books, this will cause UI lag. Consider LazyColumn with pagination or at least a limit/offset approach.

### 4.3 Fuzzy search in-memory

```kotlin
// BookRepositoryImpl.kt:85-93
val filteredBooks = if (query.isBlank()) {
    allBooks
} else {
    allBooks.filter { bookEntity ->
        val titleMatch = FuzzySearch.partialRatio(query.lowercase(), bookEntity.title.lowercase()) > 60
        // ...
    }
}
```

This loads ALL books then filters in Kotlin. For large libraries, this is slow. Consider using SQLite FTS (Full-Text Search) for better performance.

---

## 5. Recommendations Summary

### High Priority

1. **Create lightweight path-only query** - Add `getAllBookPaths(): List<String>` to reduce memory usage during import

2. **Add imports folder cleanup** - Periodically remove orphaned files from `filesDir/imports/`

### Medium Priority

3. **Add pagination to library** - Load books in batches for large libraries

4. **Wrap bulk inserts in transactions** - Improve import performance

5. **Consider SQLite FTS** - For faster fuzzy search on large libraries

### Low Priority / Nice to Have

6. **Show progress for large external file imports** - Add spinner for files >10MB

7. **Add contentHash NOT NULL constraint** - Prevent empty hashes

8. **Consider parallel file parsing** - For very large bulk imports (500+ files)

---

## Conclusion

The current implementation is **fundamentally sound** for typical use cases. The three systems (external file handling, bulk import, deduplication) work correctly together and achieve the user's requirement: opening a book from an external source should never add duplicates, and existing books should be opened instead of creating new entries.

The main areas for improvement are **performance at scale** (large libraries) and **storage cleanup** (orphaned import files). The code is well-structured with proper separation of concerns, and recent commits (v2.12.1) have addressed previous performance regressions.

For libraries under 1,000 books, the current system should perform adequately. For larger libraries, the recommendations above would provide meaningful improvements.
