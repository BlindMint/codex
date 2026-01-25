# Book Deletion Issue - Investigation Report

## Problem Summary

When books are deleted from the library (via long-press > select all > delete), they temporarily disappear from the UI but reappear after the app is closed and reopened. Attempting to open these books shows "file not found" errors because the actual files were deleted.

## Root Cause Analysis

### 1. **Stub Implementation** - PRIMARY ISSUE
File: `app/src/main/java/us/blindmint/codex/data/repository/BookRepositoryImpl.kt` (lines 377-379)

```kotlin
override suspend fun deleteBooks(books: List<Book>) {
    // Stub implementation
}
```

The `deleteBooks()` method in `BookRepositoryImpl` is a stub that does nothing!

### 2. **Deletion Flow**
When user deletes books:
1. `DeleteBooks.execute()` is called (use case)
2. Calls `bookRepository.deleteBooks(books)` → **DOES NOTHING** (stub)
3. Calls `historyRepository.deleteBookHistory(it.id)` → deletes from HistoryEntity
4. UI state is updated to filter out deleted books → **shows 0 books** (temporary)
5. Books remain in BookEntity table in database

### 3. **App Restart Flow**
When app restarts:
1. `getBooksFromDatabase()` is called
2. Loads ALL books from BookEntity table
3. **All 400 books appear again** because they were never deleted
4. Files are missing → "file not found" errors

### 4. **Progress History Feature**
The app has a `BookProgressHistoryEntity` table designed to:
- Save progress when books are removed
- Restore progress when same book (by filePath) is re-added
- Handle cases where books are moved, renamed, or re-added

**Current implementation:**
- Progress is saved to BookProgressHistoryEntity only when `insertBook()` finds existing history
- Progress is NOT saved when books are deleted
- No auto-restoration mechanism exists (books don't reappear from progress history)

## Database Schema

### Tables Involved in Book Deletion

1. **BookEntity** - Main book records
   - ✅ Has delete method: `database.deleteBooks(List<BookEntity>)`
   - ❌ Not called by repository (stub implementation)

2. **HistoryEntity** - Reading history (when book was opened)
   - ✅ Has delete method: `database.deleteBookHistory(bookId)`
   - ✅ Already called by DeleteBooks use case

3. **BookmarkEntity** - User bookmarks
   - ✅ Has delete method: `database.deleteBookmarksByBookId(bookId)`
   - ❌ NOT called during book deletion

4. **BookProgressHistoryEntity** - Persistent progress history
   - ✅ Has delete method: `database.deleteBookProgressHistory(filePath)`
   - ❓ Decision needed: Should progress be kept or deleted?

5. **Cover Images** - Files in app's internal storage
   - ❌ NO deletion mechanism exists
   - Stored in: `application.filesDir/covers/`

## Recommendations

### Option 1: Complete Deletion (Recommended)

**What happens:**
- Delete books from BookEntity
- Delete from HistoryEntity (already done)
- Delete from BookmarkEntity
- Delete from BookProgressHistoryEntity
- Delete cover image files

**Pros:**
- Clean deletion, no orphaned data
- Prevents data leaks
- Simpler mental model for users
- Consistent with user's explicit "delete" action

**Cons:**
- Progress lost if book is re-added later
- Requires re-importing from scratch if book was moved/renamed

**User Impact:**
- ✅ Deleted books stay deleted
- ✅ No "file not found" errors
- ⚠️ Progress tracking works only if books are NOT deleted (e.g., if files are moved externally)

### Option 2: Preserve Progress History

**What happens:**
- Delete books from BookEntity
- Delete from HistoryEntity (already done)
- Delete from BookmarkEntity
- **KEEP** BookProgressHistoryEntity
- Delete cover image files

**Pros:**
- Progress preserved for re-imported books
- Maintains "progress memory" feature
- Better UX for users who move/rename files

**Cons:**
- Orphaned BookProgressHistoryEntity entries accumulate
- Progress from explicitly deleted books is preserved
- More complex data model

**User Impact:**
- ✅ Deleted books stay deleted
- ✅ Progress remembered if book is re-added
- ⚠️ Need cleanup for old progress history entries

### Option 3: Preserve All History

**What happens:**
- Delete books from BookEntity
- **KEEP** HistoryEntity
- **KEEP** BookmarkEntity
- **KEEP** BookProgressHistoryEntity
- Delete cover image files

**Pros:**
- Maximum data preservation
- Full history restoration possible

**Cons:**
- Massive data leak
- Privacy concern (reading history preserved after deletion)
- Complex implementation

**User Impact:**
- ✅ Deleted books stay deleted
- ❌ Privacy violation (reading history kept)
- ❌ Orphaned data accumulates

## Implementation Plan (Option 1 - Recommended)

### 1. Implement `deleteBooks()` in BookRepositoryImpl

```kotlin
override suspend fun deleteBooks(books: List<Book>) {
    Log.i(DELETE_BOOKS, "Deleting ${books.size} books")

    books.forEach { book ->
        // 1. Get book entity to access cover image path
        val bookEntity = database.findBookById(book.id) ?: return@forEach

        // 2. Delete bookmarks for this book
        database.deleteBookmarksByBookId(book.id)

        // 3. Delete progress history for this book
        database.deleteBookProgressHistory(book.filePath)

        // 4. Delete cover image file if exists
        bookEntity.image?.let { imagePath ->
            val coverFile = File(application.filesDir, "covers/$imagePath")
            if (coverFile.exists()) {
                coverFile.delete()
                Log.i(DELETE_BOOKS, "Deleted cover image: $imagePath")
            }
        }

        // 5. Delete book from database (last step)
        database.deleteBooks(listOf(bookEntity))
        Log.i(DELETE_BOOKS, "Deleted book: ${book.title}")
    }

    Log.i(DELETE_BOOKS, "Successfully deleted ${books.size} books")
}
```

### 2. Update DeleteBooks Use Case (Remove Redundant Call)

Currently the use case calls `historyRepository.deleteBookHistory(it.id)`, but this should be handled within `deleteBooks()` to ensure proper transaction ordering.

### 3. Add BookmarkRepository Dependency

Inject `BookmarkRepository` into `BookRepositoryImpl` or use `BookDao` directly to delete bookmarks.

### 4. Testing Checklist

- [ ] Delete single book → verify it's gone after restart
- [ ] Delete multiple books → verify all are gone after restart
- [ ] Delete book with bookmarks → verify bookmarks are deleted
- [ ] Delete book with custom cover → verify cover file is deleted
- [ ] Delete book → verify history is deleted
- [ ] Re-add deleted book → verify it starts fresh (no old progress)
- [ ] Move book file, re-add → verify progress is NOT restored (progress was deleted)

### 5. Progress History Cleanup

Add periodic cleanup of old BookProgressHistoryEntity entries (already exists in code but not called):

```kotlin
// Call this periodically (e.g., in MainViewModel init or in a scheduled job)
suspend fun cleanupOldProgressHistory() {
    val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
    val deletedCount = database.deleteOldProgressHistory(ninetyDaysAgo)
    Log.i("CLEANUP", "Cleaned up $deletedCount old progress history entries")
}
```

## Decision Required

**Please confirm which option you prefer:**

1. **Option 1 (Recommended)**: Complete deletion including progress history
2. **Option 2**: Keep progress history for re-imported books
3. **Option 3**: Keep all history (not recommended due to privacy concerns)

Also, please clarify:
- When a user deletes a book, should the progress be preserved for future re-import?
- Should bookmarks be deleted or preserved?
- Is the "progress history" feature intended for accidental removals or only for file moves/renames?
