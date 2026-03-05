# Performance Audit

Comprehensive performance analysis across the entire Codex app: startup, navigation, library, readers, database, parsers, and imports.

---

## Critical

### 1. `allowMainThreadQueries()` Enabled Globally

**File:** `data/di/AppModule.kt:83`

The Room database is configured with `.allowMainThreadQueries()`, which permits any database query to block the UI thread. This is the single highest-impact performance issue in the app - it means any DAO call made from a composable or main-thread coroutine will freeze the UI for the duration of the query.

**Fix:** Remove `.allowMainThreadQueries()` and audit all call sites. Any query that currently runs on the main thread must be wrapped in `withContext(Dispatchers.IO)` or moved to a ViewModel coroutine. Room will throw `IllegalStateException` at runtime for violations, making it easy to find them.

---

### 2. Four Separate Full-Table Scans for Metadata Extraction

**File:** `data/repository/BookRepositoryImpl.kt:400-446`

`getAllAuthors()`, `getAllSeries()`, `getAllTags()`, and `getAllLanguages()` each independently call `database.getAllBooks()`, loading the entire BookEntity table (all columns) into memory four times. For a 1,000-book library, this is ~4 full table scans just to extract a few string lists.

**Fix:** Combine into a single function that loads all books once and extracts all four metadata lists in one pass:

```kotlin
suspend fun getAllMetadata(): LibraryMetadata = withContext(Dispatchers.IO) {
    val allBooks = database.getAllBooks()
    val authors = mutableSetOf<String>()
    val series = mutableSetOf<String>()
    val tags = mutableSetOf<String>()
    val languages = mutableSetOf<String>()
    var hasUnknownAuthors = false

    allBooks.forEach { book ->
        authors.addAll(book.authors.filter { it.isNotBlank() })
        series.addAll(book.series.filter { it.isNotBlank() })
        tags.addAll(book.tags)
        languages.addAll(book.languages.filter { it.isNotBlank() })
        if (book.authors.isEmpty()) hasUnknownAuthors = true
    }

    LibraryMetadata(
        authors = if (hasUnknownAuthors) listOf("Unknown") + authors.sorted() else authors.sorted(),
        series = series.sorted(),
        tags = tags.sorted(),
        languages = languages.sorted()
    )
}
```

Alternatively, add dedicated SQL queries that extract distinct values directly (though the comma-delimited TypeConverter storage makes this harder).

---

### 3. Missing Database Indices on Foreign Key Columns

**Files:**
- `data/local/dto/HistoryEntity.kt` - `bookId` has no index
- `data/local/dto/BookmarkEntity.kt` - `bookId` has no index
- `data/local/dto/BookProgressHistoryEntity.kt` - `filePath` has no index

These columns are used in WHERE clauses (`getLatestHistoryForBook`, `getBookmarksByBookId`, `getBookProgressHistory`) but lack indices, forcing full table scans.

**Fix:** Add indices via a Room migration (version 27):

```kotlin
// HistoryEntity
@Entity(indices = [Index(value = ["bookId"])])

// BookmarkEntity
@Entity(indices = [Index(value = ["bookId"])])

// BookProgressHistoryEntity
@Entity(indices = [Index(value = ["filePath"])])
```

---

### 4. `hiltViewModel<MainModel>()` Called Inside Library List Items

**Files:**
- `presentation/library/LibraryItem.kt:73`
- `presentation/library/LibraryListItem.kt:144`
- `presentation/library/LibraryPager.kt:52`

Each library grid/list item calls `hiltViewModel<MainModel>()` to read settings like `libraryShowNormalProgress` and `libraryShowSpeedProgress`. While Hilt scopes these to the same instance, the `hiltViewModel()` lookup has overhead per call, and `collectAsStateWithLifecycle()` creates a new state subscription per item. For a library with 200+ books visible, this is 200+ redundant state subscriptions.

**Fix:** Read the settings once in the parent composable (e.g., `LibraryLayout` or `LibraryPager`) and pass the relevant booleans down as parameters to `LibraryItem` and `LibraryListItem`.

---

### 5. Comic Pages Loaded at Full Resolution Without Downsampling

**File:** `presentation/reader/ComicReaderLayout.kt:153`

`BitmapFactory.decodeStream(input)` loads comic pages at native resolution with no `inSampleSize`. A typical manga page is 1600x2400 (~15MB in ARGB_8888). With `MAX_CACHED_PAGES = 50` and `PREFETCH_PAGES = 5` (loading up to 11 pages around current position), peak memory for the comic reader cache alone could reach 150-750MB depending on page dimensions.

The cover thumbnail extractor at `ComicFileParser.kt:49` correctly uses `inSampleSize = 4`, but the actual page reader does not.

**Fix:** Calculate `inSampleSize` based on screen dimensions:

```kotlin
// First pass: decode bounds only
val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
BitmapFactory.decodeStream(input, null, options)

// Calculate sample size based on screen
val screenWidth = displayMetrics.widthPixels
val sampleSize = calculateInSampleSize(options, screenWidth, screenWidth * 2)

// Second pass: decode with downsampling
input.reset() // or re-open the stream
val finalOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
val bitmap = BitmapFactory.decodeStream(input, null, finalOptions)
```

---

## High

### 6. Regex Compiled on Every Invocation in Word-Count Hot Path

**File:** `ui/reader/ReaderModel.kt:1374`

```kotlin
readerText.line.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
```

This compiles a new `Regex` object for every text item during word cache building (called once per book load, but iterates all text items). Additionally, `.split()` allocates a `List<String>` and `.filter()` allocates another - all just to get a count.

**Fix:** Use a pre-compiled regex as a class-level constant and count without allocating lists:

```kotlin
companion object {
    private val WHITESPACE_REGEX = "\\s+".toRegex()
}

// Count words without intermediate list allocations
private fun countWords(text: String): Int {
    if (text.isBlank()) return 0
    var count = 0
    var inWord = false
    for (char in text) {
        if (char.isWhitespace()) {
            inWord = false
        } else if (!inWord) {
            count++
            inWord = true
        }
    }
    return count
}
```

---

### 7. Speed Reader Word Extractor Creates Multiple Intermediate Strings

**File:** `data/parser/SpeedReaderWordExtractor.kt:80-131`

`preprocessText()` performs three full passes over the text:
1. Lines 86-107: Character-by-character `buildString` per text item (converts newlines to spaces)
2. Lines 111-124: Another full pass to collapse whitespace via `buildString`
3. Line 126: `Regex("([\\p{L}])([—–])([\\p{L}])")` compiled and applied
4. Line 130: `split(Regex("(?<=\\s)|(?=\\s)"))` - another regex compilation with expensive lookaround assertions

For a 300KB book, this creates several large intermediate String objects.

**Fix:**
- Compile regexes as `companion object` constants
- Combine passes 1 and 2 into a single character-by-character loop that handles newlines and whitespace collapse simultaneously
- Replace the lookaround split with a simpler whitespace-based split: `text.split(WHITESPACE_REGEX).filter { it.isNotBlank() }`

---

### 8. Regex Compiled Per Line in Document Parser

**File:** `data/parser/DocumentParser.kt:119-127`

```kotlin
val formattedLine = line.replace(
    Regex("""\*\*\*\s*(.*?)\s*\*\*\*"""), "_**$1**_"
).replace(
    Regex("""\*\*\s*(.*?)\s*\*\*"""), "**$1**"
).replace(
    Regex("""_\s*(.*?)\s*_"""), "_$1_"
)
val imageRegex = Regex("""\[\[(.*?)\|(.*?)]]""")
```

Four `Regex` objects are compiled for every line of every document. For a book with 10,000 lines, that's 40,000 regex compilations.

**Fix:** Move all regex patterns to `companion object` constants.

---

### 9. File System Browse Uses O(n*m) Path Matching

**File:** `data/repository/FileSystemRepositoryImpl.kt:71-73`

```kotlin
existingPaths.none { existingPath ->
    existingPath.equals(path, ignoreCase = true)
}
```

For each file discovered during browsing, iterates all existing book paths. With 500 existing books and 5,000 files on storage, this is 2.5 million case-insensitive string comparisons.

**Fix:** Pre-compute a `HashSet<String>` of lowercase existing paths for O(1) lookup:

```kotlin
val existingPathsSet = existingPaths.map { it.lowercase() }.toHashSet()
// Then:
!existingPathsSet.contains(path.lowercase())
```

---

### 10. Color Preset Selection Updates Every Row Individually

**File:** `data/repository/ColorPresetRepositoryImpl.kt:35-41`

`selectColorPreset()` loads all presets, maps each to set `isSelected`, then issues a separate UPDATE for each one. For N presets, this is 1 SELECT + N UPDATEs.

**Fix:** Add two targeted DAO queries:

```kotlin
@Query("UPDATE colorpresetentity SET isSelected = 0 WHERE isSelected = 1")
suspend fun deselectAllPresets()

@Query("UPDATE colorpresetentity SET isSelected = 1 WHERE id = :id")
suspend fun selectPreset(id: Int)
```

Then `selectColorPreset` becomes just 2 queries regardless of preset count.

Similarly, `reorderColorPresets()` (lines 49-57) deletes all presets then reinserts them. Use UPDATE with order parameter instead.

---

## Medium

### 11. Library Loads All Books Into Memory on Every Refresh

**File:** `data/repository/BookRepositoryImpl.kt:77-110` via `ui/library/LibraryModel.kt:675`

`getBooks()` calls `database.getAllBooks()` which loads the entire BookEntity table with all columns. Fuzzy search filtering, category filtering, and sorting all happen in Kotlin after the full load. This is triggered on every library refresh, tab switch, and return from a reader.

**Fix (incremental):**
- Move category/favorite filtering to SQL WHERE clauses
- Add SQL ORDER BY for common sort modes (title, progress, last opened)
- For fuzzy search, consider Room FTS (Full-Text Search) if performance becomes an issue with 1000+ books
- As a quick win: cache the loaded book list and only re-query when the data actually changes (use Room's `Flow<List<BookEntity>>` to observe changes reactively)

---

### 12. DataStore Reads Use `Dispatchers.Default` Instead of `IO`

**File:** `data/repository/DataStoreRepositoryImpl.kt:48`

`getAllSettings()` spawns concurrent coroutines on `Dispatchers.Default` (CPU-bound pool) to perform I/O operations (DataStore reads). This misuses the Default dispatcher and can starve CPU-bound work.

**Fix:** Change `Dispatchers.Default` to `Dispatchers.IO`.

---

### 13. Speed Reader Text Measurements During Playback

**File:** `presentation/reader/SpeedReadingContent.kt:433-456`

During speed reader playback, three `textMeasurer.measure()` calls happen per word display:
1. Measure "before accent" text
2. Measure accent character
3. Measure full word

At 300 WPM, that's 15 text measurements per second. Text measurement involves layout computation and font metrics.

**Fix:** Cache measurement results for words that have already been measured (many common words repeat). A simple `HashMap<String, TextLayoutResult>` with LRU eviction would eliminate repeated measurements for common words.

---

### 14. PDF Parser Compiles Regex on Every Parse

**File:** `data/parser/pdf/PdfTextParser.kt:52,55`

```kotlin
text.replace("\\s+".toRegex(), " ")
text.split(pdfStripper.paragraphStart.toRegex())
```

Regex objects compiled on each PDF parse call. These should be pre-compiled constants.

---

### 15. 100MB CursorWindow Allocated Eagerly on Startup

**File:** `ui/main/MainActivity.kt:123-129`

The app uses reflection to increase the `CursorWindow` size to 100MB on every app startup. This is a workaround for large database queries returning too much data.

**Fix:** This is a symptom of loading too much data at once (issue #11). Fixing the root cause (pagination, column selection) would remove the need for this workaround. If kept, make it lazy or reduce to a more reasonable size (10-20MB).

---

### 16. AsyncCoverImage Has No Explicit Size Constraints

**File:** `presentation/core/components/common/AsyncCoverImage.kt:30-39`

The Coil `ImageRequest` has no `.size()` constraint, meaning cover images are decoded at their native resolution. If a book has a 2000x3000 cover but the grid item displays at 200x300, the full image is still decoded into memory.

**Fix:** Add size constraints to the image request:

```kotlin
ImageRequest.Builder(LocalContext.current)
    .data(uri)
    .crossfade(animationDurationMillis)
    .size(coil.size.Size.ORIGINAL) // or explicit pixel dimensions
    .build()
```

Better yet, pass the composable's size constraints and use `coil.size.Size(width, height)` to let Coil downsample during decode.

---

### 17. History Screen Loads All History Then Joins With Books

**File:** `ui/history/HistoryModel.kt:304-318`

Loads the full history table, sorts in Kotlin, then fetches books by ID list, then groups by day in Kotlin. This could be a single SQL query with JOIN and GROUP BY.

**Fix:** Add a Room query that joins history with books and returns pre-sorted results:

```kotlin
@Query("""
    SELECT h.*, b.title, b.image, b.progress, b.filePath
    FROM historyentity h
    INNER JOIN bookentity b ON h.bookId = b.id
    ORDER BY h.time DESC
""")
suspend fun getHistoryWithBooks(): List<HistoryWithBook>
```

---

## Low

### 18. Library Sorting and Tab Filtering in Composable Layer

**File:** `ui/library/LibraryScreen.kt:89-162`

Book sorting and per-tab filtering (comics, favorites, etc.) happens inside `remember` blocks in the composable. Every tab switch triggers re-filtering of the full list. This work belongs in the ViewModel where it can be pre-computed and cached.

---

### 19. `calculateWindowSizeClass()` in NavigatorTabs

**File:** `presentation/navigator/NavigatorTabs.kt:41`

`calculateWindowSizeClass()` is called on every recomposition. This performs measurement operations. It should be hoisted to the activity level and passed down as a parameter or state.

---

### 20. Color Preset Reorder Deletes All Then Reinserts

**File:** `data/repository/ColorPresetRepositoryImpl.kt:49-57`

`reorderColorPresets()` deletes the entire table then reinserts each preset. This is fragile (crash between delete and insert loses all presets) and slow (N+1 queries).

**Fix:** Use a single transaction with UPDATE statements:

```kotlin
@Transaction
suspend fun reorderColorPresets(orderedIds: List<Int>) {
    orderedIds.forEachIndexed { index, id ->
        updateColorPresetOrder(id, index)
    }
}
```

---

### 21. OPDS Response Body Read Fully Into Memory

**File:** `data/repository/OpdsRepositoryImpl.kt:68`

`response.body?.string()` reads the entire OPDS XML response into a `String` before parsing. Large OPDS catalogs (100KB+) create large string allocations. Consider streaming XML parsing with the response body's `byteStream()`.

---

### 22. EPUB Parser Reads Entire Zip Entries Into Memory

**File:** `data/parser/epub/EpubTextParser.kt:119-133`

Each chapter's zip entry is read fully with `readText()`. For very large EPUB chapters, this creates large string allocations. Streaming parsing with Jsoup's `parse(InputStream)` would reduce peak memory.

---

## Summary

| # | Issue | Severity | Category | Status |
|---|-------|----------|----------|--------|
| 1 | `allowMainThreadQueries()` enabled | Critical | Database | **Fixed** |
| 2 | 4x full table scans for metadata | Critical | Database | **Fixed** |
| 3 | Missing indices on HistoryEntity, BookmarkEntity, BookProgressHistoryEntity | Critical | Database | **Fixed** |
| 4 | `hiltViewModel<MainModel>()` in every list item | High | Compose | **Fixed** |
| 5 | Comic pages at full resolution (no downsampling) | High | Memory | Skipped (clarity) |
| 6 | Regex compiled per text item in word counting | High | CPU | **Fixed** |
| 7 | Speed reader word extractor triple-pass with regex | High | CPU | **Fixed** |
| 8 | Regex compiled per line in document parser | High | CPU | **Fixed** |
| 9 | O(n*m) path matching in file browser | High | CPU | **Fixed** |
| 10 | Color preset selection updates all rows | High | Database | **Fixed** |
| 11 | Full table load on every library refresh | Medium | Database | Noted |
| 12 | DataStore reads on wrong dispatcher | Medium | Threading | **Fixed** |
| 13 | 3 text measurements per word in speed reader | Medium | CPU | Skipped |
| 14 | PDF parser regex not pre-compiled | Medium | CPU | **Fixed** |
| 15 | 100MB CursorWindow on startup | Medium | Memory | **Fixed** (reduced to 20MB) |
| 16 | Cover images decoded at full resolution | Medium | Memory | Skipped (clarity) |
| 17 | History loads all then joins in Kotlin | Medium | Database | **Fixed** (HashMap lookup) |
| 18 | Sorting/filtering in composable layer | Low | Compose | Noted |
| 19 | `calculateWindowSizeClass()` every recomposition | Low | Compose | Skipped (minimal impact) |
| 20 | Preset reorder delete-all + reinsert | Low | Database | **Fixed** |
| 21 | OPDS response fully loaded into String | Low | Memory | Skipped (needs full body for format detection) |
| 22 | EPUB chapters fully loaded into String | Low | Memory | Skipped (chapter entries are small) |
