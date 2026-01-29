# Use Case Classification - Phase 1.1

**Analysis Date:** 2026-01-28
**Total Use Cases:** 42 files
**Total Lines:** 2,094 lines

---

## Executive Summary

- **Thin Wrappers to Remove:** 35 files (614 lines)
- **Business Logic to Keep:** 8 files (1,480 lines)
- **Net Savings:** ~614 lines of boilerplate code

---

## Classification Methodology

**Thin Wrapper Criteria:**
- Single repository dependency
- One-line delegation to repository method
- No data transformation
- No coordination between multiple services
- < 30 lines total (including imports and license)

**Business Logic Criteria:**
- Coordination between multiple repositories/services
- Complex data transformations
- Progress tracking
- Domain-specific rules and validation
- External API integration

---

## THIN WRAPPERS (35 files) - DELETE

### Book Operations (12 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `GetBooks.kt` | 19 | `repository.getBooks(query)` | Simple get with query parameter |
| `GetBookById.kt` | 19 | `repository.getBookById(id)` | Simple get by ID |
| `GetBooksById.kt` | 19 | `repository.getBooksById(ids)` | Simple get by IDs |
| `GetBookByFilePath.kt` | 19 | `repository.getBookByFilePath(path)` | Simple lookup |
| `InsertBook.kt` | 19 | `repository.insertBook(book)` | Simple insert |
| `UpdateBook.kt` | 19 | `repository.updateBook(book)` | Simple update |
| `DeleteBooks.kt` | 19 | `repository.deleteBooks(books)` | Simple delete |
| `UpdateCoverImageOfBook.kt` | 20 | `repository.updateCoverImage(id, image)` | Simple update |
| `DeleteProgressHistoryUseCase.kt` | 19 | `repository.deleteProgressHistory(book)` | Simple delete |
| `GetText.kt` | 28 | `repository.getBookText(id)` | Simple delegation |
| `GetSpeedReaderWords.kt` | 20 | `repository.getBookText(id)` | Duplicate of GetText |
| `CanResetCover.kt` | 18 | Simple repository query | Simple boolean check |
| `ResetCoverImage.kt` | 18 | Simple repository update | Simple update |

**Book Operations Subtotal:** 12 files, ~250 lines

---

### Bookmark Operations (5 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `GetBookmarksByBookId.kt` | 20 | `repository.getBookmarksByBookId(id)` | Simple query |
| `InsertBookmark.kt` | 20 | `repository.insertBookmark(bookmark)` | Simple insert |
| `DeleteBookmark.kt` | 20 | `repository.deleteBookmark(bookmark)` | Simple delete |
| `DeleteBookmarksByBookId.kt` | 19 | `repository.deleteBookmarksByBookId(id)` | Simple delete |
| `DeleteAllBookmarks.kt` | 19 | `repository.deleteAllBookmarks()` | Simple delete all |

**Bookmark Operations Subtotal:** 5 files, ~98 lines

---

### Color Preset Operations (5 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `GetColorPresets.kt` | 19 | `repository.getColorPresets()` | Simple query |
| `UpdateColorPreset.kt` | 19 | `repository.updateColorPreset(preset)` | Simple update |
| `DeleteColorPreset.kt` | 19 | `repository.deleteColorPreset(preset)` | Simple delete |
| `ReorderColorPresets.kt` | 19 | `repository.reorderColorPresets(presets)` | Simple update |
| `SelectColorPreset.kt` | 19 | `repository.selectColorPreset(id)` | Simple update |

**Color Preset Operations Subtotal:** 5 files, ~95 lines

---

### History Operations (5 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `GetHistory.kt` | 19 | `repository.getHistory()` | Simple query |
| `GetLatestHistory.kt` | 19 | `repository.getLatestHistory()` | Simple query |
| `InsertHistory.kt` | 19 | `repository.insertHistory(history)` | Simple insert |
| `DeleteHistory.kt` | 19 | `repository.deleteHistory(history)` | Simple delete |
| `DeleteWholeHistory.kt` | 18 | `repository.deleteWholeHistory()` | Simple delete all |

**History Operations Subtotal:** 5 files, ~94 lines

---

### DataStore Operations (2 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `GetAllSettings.kt` | 19 | `repository.getAllSettings()` | Simple query |
| `SetDatastore.kt` | 19 | `repository.putDataToDataStore(key, value)` | Simple set |

**DataStore Operations Subtotal:** 2 files, ~38 lines

---

### File System Operations (2 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `GetFiles.kt` | 19 | `repository.getFiles()` | Simple query |
| `GetBookFromFile.kt` | 20 | `repository.getBookFromFile(file)` | Simple parsing |

**File System Operations Subtotal:** 2 files, ~39 lines

---

### Permission Operations (2 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `GrantPersistableUriPermission.kt` | 19 | `repository.grantPersistableUriPermission(uri)` | Simple delegation |
| `ReleasePersistableUriPermission.kt` | 19 | `repository.releasePersistableUriPermission(uri)` | Simple delegation |

**Permission Operations Subtotal:** 2 files, ~38 lines

---

### OPDS Operations (2 files)

| File | Lines | Pattern | Rationale |
|------|--------|---------|-----------|
| `RefreshAllBooksFromOpdsSource.kt` | 28 | `opdsRefreshRepository.refreshAllBooksFromSource()` | Delegates to specialized repository |
| `RefreshBookMetadataFromOpds.kt` | 28 | `opdsRefreshRepository.refreshBookMetadata()` | Delegates to specialized repository |

**Note:** These delegate to `OpdsRefreshRepository` which contains the actual business logic for non-destructive merging.

**OPDS Operations Subtotal:** 2 files, ~56 lines

---

## BUSINESS LOGIC (8 files) - KEEP

### Book Operations (2 files)

| File | Lines | Business Logic | Rationale |
|------|--------|----------------|-----------|
| `BulkImportBooksFromFolder.kt` | 90 | Folder scanning, file filtering, batch parsing, progress tracking | Complex coordination between file system and parser |
| `AutoImportCodexBooksUseCase.kt` | 253 | Codex directory management, OPF metadata merging, folder processing, progress tracking | Multi-step import process with complex business rules |

---

### Import/Export Operations (2 files)

| File | Lines | Business Logic | Rationale |
|------|--------|----------------|-----------|
| `ExportSettings.kt` | 182 | JSON serialization, settings validation, color preset handling | Complex settings export with multiple data types |
| `ImportSettings.kt` | 261 | JSON parsing, type conversion, settings validation, error handling, color preset reordering | Complex import with extensive validation and transformation |

---

### OPDS Operations (1 file)

| File | Lines | Business Logic | Rationale |
|------|--------|----------------|-----------|
| `ImportOpdsBookUseCase.kt` | 569 | OPDS feed parsing, book discovery, download management, metadata extraction, error handling | Full OPDS book import workflow with complex logic |

---

### DataStore Operations (1 file)

| File | Lines | Business Logic | Rationale |
|------|--------|----------------|-----------|
| `ChangeLanguage.kt` | 28 | App locale setting + persistence | Domain logic: setting system locale AND persisting preference |

**Note:** Although small, this performs domain logic (changing app locale) beyond simple data persistence.

---

**Business Logic Subtotal:** 8 files, ~1,480 lines

---

## ViewModel Impact Analysis

### ViewModels Using Thin Wrappers

| ViewModel | Thin Wrapper Use Cases | Direct Repository Access Needed |
|-----------|------------------------|------------------------------|
| `LibraryModel` | GetBooks, DeleteBooks, UpdateBook | BookRepository |
| `BookInfoModel` | GetBookById, UpdateBook, DeleteBooks, UpdateCoverImageOfBook, DeleteProgressHistoryUseCase | BookRepository |
| `HistoryModel` | GetHistory, GetBooksById, InsertHistory, DeleteHistory, DeleteWholeHistory | HistoryRepository, BookRepository |
| `SettingsModel` | GetColorPresets, UpdateColorPreset, DeleteColorPreset, GetAllSettings, GetBooks, DeleteBooks | ColorPresetRepository, DataStoreRepository, BookRepository |
| `ReaderModel` | GetBookById, UpdateBook, GetText, GetLatestHistory, GetBookmarksByBookId, InsertBookmark, DeleteBookmark, DeleteBookmarksByBookId | BookRepository, HistoryRepository, BookmarkRepository |
| `BrowseModel` | GetFiles, GetBookFromFile, InsertBook | FileSystemRepository, BookRepository |
| `SpeedReaderModel` | GetBookById, GetSpeedReaderWords | BookRepository |
| `MainModel` | GetAllSettings | DataStoreRepository |

**Total ViewModels to Update:** 8 ViewModels

---

## Recommended Changes by ViewModel

### 1. LibraryModel
**Current:**
```kotlin
class LibraryModel @Inject constructor(
    private val getBooks: GetBooks,
    private val bookRepository: BookRepository,  // Already injected!
    private val deleteBooks: DeleteBooks,
    private val moveBooks: UpdateBook,
    private val deleteProgressHistory: DeleteProgressHistoryUseCase
) : ViewModel() { ... }
```

**After:**
```kotlin
class LibraryModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private suspend fun getBooksFromDatabase(query: String) {
        val allBooks = bookRepository.getBooks(query)  // Direct access
        // ...
    }

    private suspend fun deleteSelectedBooks() {
        val selectedBooks = _state.value.books.filter { it.selected }.map { it.data }
        bookRepository.deleteBooks(selectedBooks)  // Direct access
        // ...
    }

    private suspend fun updateBook(book: Book) {
        bookRepository.updateBook(book)  // Direct access
        // ...
    }
}
```

**Impact:** Eliminate GetBooks, DeleteBooks, UpdateBook use cases

---

### 2. BookInfoModel
**Current:**
```kotlin
class BookInfoModel @Inject constructor(
    private val getBookById: GetBookById,
    private val updateBook: UpdateBook,
    private val updateCoverImageOfBook: UpdateCoverImageOfBook,
    private val deleteBooks: DeleteBooks,
    private val deleteProgressHistory: DeleteProgressHistoryUseCase
) : ViewModel() { ... }
```

**After:**
```kotlin
class BookInfoModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    // All repository calls directly
    bookRepository.getBookById(bookId)
    bookRepository.updateBook(book)
    bookRepository.updateCoverImage(id, image)
    bookRepository.deleteBooks(books)
    historyRepository.deleteProgressHistory(book)
}
```

**Impact:** Eliminate GetBookById, UpdateBook, UpdateCoverImageOfBook, DeleteBooks, DeleteProgressHistoryUseCase

---

### 3. HistoryModel
**Current:**
```kotlin
class HistoryModel @Inject constructor(
    private val getHistory: GetHistory,
    private val getBooksById: GetBooksById,
    private val insertHistory: InsertHistory,
    private val deleteHistory: DeleteHistory,
    private val deleteWholeHistory: DeleteWholeHistory
) : ViewModel() { ... }
```

**After:**
```kotlin
class HistoryModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val bookRepository: BookRepository
) : ViewModel() {
    // All repository calls directly
    historyRepository.getHistory()
    bookRepository.getBooksById(ids)
    historyRepository.insertHistory(history)
    historyRepository.deleteHistory(history)
    historyRepository.deleteWholeHistory()
}
```

**Impact:** Eliminate GetHistory, GetBooksById, InsertHistory, DeleteHistory, DeleteWholeHistory

---

### 4. SettingsModel
**Current:**
```kotlin
class SettingsModel @Inject constructor(
    private val getColorPresets: GetColorPresets,
    private val updateColorPreset: UpdateColorPreset,
    private val deleteColorPreset: DeleteColorPreset,
    private val getAllSettings: GetAllSettings,
    private val getAllBooks: GetBooks,
    private val deleteBooks: DeleteBooks,
    // ...
) : ViewModel() { ... }
```

**After:**
```kotlin
class SettingsModel @Inject constructor(
    private val colorPresetRepository: ColorPresetRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val bookRepository: BookRepository,
    // Keep: changeLanguage: ChangeLanguage (has business logic)
    // Keep: exportSettings: ExportSettings
    // Keep: importSettings: ImportSettings
    // Keep: importCodexBooks: AutoImportCodexBooksUseCase
) : ViewModel() {
    // Direct repository access
    colorPresetRepository.getColorPresets()
    colorPresetRepository.updateColorPreset(preset)
    colorPresetRepository.deleteColorPreset(preset)
    dataStoreRepository.getAllSettings()
    bookRepository.getBooks(query)
    bookRepository.deleteBooks(books)
}
```

**Impact:** Eliminate GetColorPresets, UpdateColorPreset, DeleteColorPreset, GetAllSettings, GetBooks, DeleteBooks

---

### 5. ReaderModel
**Current:**
```kotlin
class ReaderModel @Inject constructor(
    private val getBookById: GetBookById,
    private val updateBook: UpdateBook,
    private val getText: GetText,
    private val getLatestHistory: GetLatestHistory,
    private val getBookmarksByBookId: GetBookmarksByBookId,
    private val insertBookmark: InsertBookmark,
    private val deleteBookmark: DeleteBookmark,
    private val deleteBookmarksByBookId: DeleteBookmarksByBookId
) : ViewModel() { ... }
```

**After:**
```kotlin
class ReaderModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val historyRepository: HistoryRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    // Direct repository access
    bookRepository.getBookById(id)
    bookRepository.updateBook(book)
    bookRepository.getBookText(id)
    historyRepository.getLatestHistory()
    bookmarkRepository.getBookmarksByBookId(id)
    bookmarkRepository.insertBookmark(bookmark)
    bookmarkRepository.deleteBookmark(bookmark)
    bookmarkRepository.deleteBookmarksByBookId(id)
}
```

**Impact:** Eliminate GetBookById, UpdateBook, GetText, GetLatestHistory, GetBookmarksByBookId, InsertBookmark, DeleteBookmark, DeleteBookmarksByBookId

---

### 6. BrowseModel
**Current:**
```kotlin
class BrowseModel @Inject constructor(
    private val getFiles: GetFiles,
    private val getBookFromFile: GetBookFromFile,
    private val insertBook: InsertBook
) : ViewModel() { ... }
```

**After:**
```kotlin
class BrowseModel @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val bookRepository: BookRepository
) : ViewModel() {
    fileSystemRepository.getFiles()
    fileSystemRepository.getBookFromFile(file)
    bookRepository.insertBook(book)
}
```

**Impact:** Eliminate GetFiles, GetBookFromFile, InsertBook

---

### 7. SpeedReaderModel
**Current:**
```kotlin
class SpeedReaderModel @Inject constructor(
    private val getBookById: GetBookById,
    private val getSpeedReaderWords: GetSpeedReaderWords
) : ViewModel() { ... }
```

**After:**
```kotlin
class SpeedReaderModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {
    bookRepository.getBookById(id)
    bookRepository.getBookText(id)  // Same as getSpeedReaderWords
}
```

**Impact:** Eliminate GetBookById, GetSpeedReaderWords

---

### 8. MainModel
**Current:**
```kotlin
class MainModel @Inject constructor(
    private val getAllSettings: GetAllSettings
) : ViewModel() { ... }
```

**After:**
```kotlin
class MainModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {
    dataStoreRepository.getAllSettings()
}
```

**Impact:** Eliminate GetAllSettings

---

## Files to Delete (35 files)

### book/
- GetBooks.kt
- GetBookById.kt
- GetBooksById.kt
- GetBookByFilePath.kt
- InsertBook.kt
- UpdateBook.kt
- DeleteBooks.kt
- UpdateCoverImageOfBook.kt
- DeleteProgressHistoryUseCase.kt
- GetText.kt
- GetSpeedReaderWords.kt
- CanResetCover.kt
- ResetCoverImage.kt

### bookmark/
- GetBookmarksByBookId.kt
- InsertBookmark.kt
- DeleteBookmark.kt
- DeleteBookmarksByBookId.kt
- DeleteAllBookmarks.kt

### color_preset/
- GetColorPresets.kt
- UpdateColorPreset.kt
- DeleteColorPreset.kt
- ReorderColorPresets.kt
- SelectColorPreset.kt

### history/
- GetHistory.kt
- GetLatestHistory.kt
- InsertHistory.kt
- DeleteHistory.kt
- DeleteWholeHistory.kt

### data_store/
- GetAllSettings.kt
- SetDatastore.kt

### file_system/
- GetFiles.kt
- GetBookFromFile.kt

### permission/
- GrantPersistableUriPermission.kt
- ReleasePersistableUriPermission.kt

### opds/
- RefreshAllBooksFromOpdsSource.kt
- RefreshBookMetadataFromOpds.kt

---

## Summary of Changes

**Use Cases to Remove:** 35 files (614 lines)
**Use Cases to Keep:** 8 files (1,480 lines)
**ViewModels to Update:** 8 ViewModels
**Estimated Savings:** ~614 lines of boilerplate code
**Reduced Dependency Depth:** ViewModels → Repositories (instead of ViewModels → Use Cases → Repositories)

**Benefits:**
1. Simpler dependency injection graph
2. Reduced indirection without loss of abstraction
3. Easier to understand code flow
4. Fewer files to maintain
5. Use cases only kept where they provide actual value

---

## Next Steps

1. ✅ Audit complete
2. 🔄 Create detailed plan (this document)
3. ⏳ Update each ViewModel
4. ⏳ Delete thin wrapper files
5. ⏳ Build and test
6. ⏳ Document changes
