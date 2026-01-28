# Code Consolidation Analysis & Implementation Plan

**Generated:** 2026-01-28
**Last Updated:** 2026-01-28
**Branch:** refactor/phase1-1-thin-wrapper-elimination
**Project:** Codex - Material You eBook Reader for Android
**Current Phase:** Phase 1.1 - Compilation Errors Fixed

---

## Executive Summary

This document provides an exhaustive analysis of the Codex codebase focusing on **dependency management**, **code modularity**, and **code simplicity**. The app has grown organically with features added over time, leading to opportunities for consolidation and simplification.

### Key Findings

| Priority | Area | Issue | Impact | Effort | Est. Savings |
|-----------|-------|--------|---------|---------------|
| | **VERY HIGH** | Settings Granularity | 60+ subdirectories with 105+ option files | High | Medium | 2,000+ LOC |
| | **VERY HIGH** | Use Case Granularity | 42 use cases, many are thin wrappers | High | Low-Medium | 500+ LOC |
| | **HIGH** | Parser Duplication | Format detection, error handling, book construction duplicated | Medium-High | Medium | 300-400 LOC |
| | **HIGH** | Repository Boilerplate | 10 repositories with identical structure | Medium | Low | 200-300 LOC |
| | **MEDIUM** | Settings Component Patterns | 105 option files follow identical structure | Moderate | Medium | 1,000+ LOC |
| | **MEDIUM** | ViewModel Patterns | State management could be abstracted | Low-Medium | Low | 150-200 LOC |
| | **LOW** | Dependencies | Clean (52 unique), minor cleanup needed | Low | Low | Minimal |

**Total Potential Savings:** 4,000-5,000 lines of code (~6-7% reduction) + significant maintainability improvements.

### ⭐ Speed Reader Validation (2026-01-28)

**Status:** ✅ **VALIDATED SAFE**

**Finding:** Speed reader uses the same `TextParserImpl.parse()` as normal reader. Consolidation recommendations (FormatDetector, BaseFileParser, BookFactory) will NOT impact speed reader functionality or performance.

**Key Insights:**
- Speed reader has NO separate parsing path
- Dual caching strategy (textCache + speedReaderWordCache) remains intact
- Post-processing (`SpeedReaderWordExtractor`) unchanged
- All parser consolidations are code organization only

**Proceed with Phase 1.2 (FormatDetector) as planned.** See Section 2.5 for detailed analysis.

---

## 1. DEPENDENCY ANALYSIS

### Current State

**Total Dependencies:** 52 (all unique)
**Build File:** `app/build.gradle.kts`

### Inventory

```kotlin
// Core Android
androidx.core:core-ktx:1.16.0
androidx.lifecycle:lifecycle-runtime-ktx:2.8.7
androidx.activity:activity-compose:1.10.1

// Compose (no BOM - intentionally avoided for AboutLibraries)
androidx.compose.foundation:foundation:1.8.0-beta03
androidx.compose.animation:animation:1.7.8
androidx.compose.animation:animation-android:1.8.0-beta03
androidx.compose.foundation:foundation-layout:1.7.8
androidx.compose.ui:ui:1.7.8
androidx.compose.ui:ui-graphics:1.7.8
androidx.compose.ui:ui-android:1.8.0-beta03
androidx.compose.material3:material3:1.4.0-alpha08
androidx.compose.material3:material3-window-size-class:1.3.1
androidx.compose.material:material-icons-extended:1.7.8
androidx.compose.material:material:1.7.8

// DI & Architecture
com.google.dagger:hilt-android:2.55
ksp("com.google.dagger:hilt-compiler:2.55")
ksp("androidx.hilt:hilt-compiler:1.2.0")
androidx.hilt:hilt-navigation-compose:1.2.0

// Database
androidx.room:room-runtime:2.7.1
ksp("androidx.room:room-compiler:2.7.1")
androidx.room:room-ktx:2.7.1

// Data Persistence
androidx.datastore:datastore-preferences:1.1.3
androidx.core:core-splashscreen:1.0.1
androidx.security:security-crypto:1.1.0-alpha06

// File Access
com.anggrayudi:storage:2.0.0

// Parsers
com.tom-roush:pdfbox-android:2.0.27.0
org.jsoup:jsoup:1.18.3
org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1
org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1
org.commonmark:commonmark:0.24.0

// Comic Archives
com.github.junrar:junrar:7.5.5
org.apache.commons:commons-compress:1.26.0
org.tukaani:xz:1.9

// Networking
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.retrofit2:converter-simplexml:2.9.0
com.squareup.okhttp3:logging-interceptor:4.12.0
org.simpleframework:simple-xml:2.7.1

// UI Components
com.google.accompanist:accompanist-swiperefresh:0.36.0
io.coil-kt:coil-compose:2.7.0
sh.calvin.reorderable:reorderable:2.4.3
com.github.nanihadesuka:LazyColumnScrollbar:2.2.0
com.mikepenz:aboutlibraries-core:11.4.0
com.mikepenz:aboutlibraries-compose-m3:11.4.0

// Utilities
com.google.code.gson:gson:2.11.0
me.xdrop:fuzzywuzzy:1.4.0

// Paging
androidx.paging:paging-runtime-ktx:3.3.0
androidx.paging:paging-compose:3.3.0

// Localization
androidx.appcompat:appcompat:1.7.0
androidx.appcompat:appcompat-resources:1.7.0
```

### Issues & Recommendations

#### 1. Bouncy Castle Module Redundancy (LOW PRIORITY)

**Issue:** Three separate Bouncy Castle modules with overlapping functionality:
```kotlin
org.bouncycastle:bcprov-jdk18on:1.83
org.bouncycastle:bcpkix-jdk18on:1.83
org.bouncycastle:bcutil-jdk18on:1.83
```

**Recommendation:** Consider if all three modules are needed. If only basic crypto is used, could use a single combined artifact or verify if all are actually referenced.

**Impact:** Minor APK size reduction (~500KB - 1MB)

---

#### 2. Material Library Version Inconsistency (LOW PRIORITY)

**Issue:** Mixing beta and stable versions:
```kotlin
androidx.compose.foundation:foundation:1.8.0-beta03  // Beta
androidx.compose.foundation:foundation-layout:1.7.8   // Stable
androidx.compose.ui:ui:1.7.8                           // Stable
androidx.compose.ui:ui-android:1.8.0-beta03          // Beta
```

**Recommendation:** Standardize on stable versions where possible. Beta versions may introduce instability.

**Impact:** Improved stability, potential bug reduction

---

#### 3. Commented Out SQLCipher (LOW PRIORITY)

**Issue:** Dead code in gradle file:
```kotlin
// TODO: Add database encryption when 16KB page size compatible SQLCipher version available
// Current SQLCipher versions have 16KB page size alignment issues for Android 15+
// implementation("net.zetetic:android-database-sqlcipher:4.5.6")
```

**Recommendation:** Move to a separate TODO.md or remove comment if no plans to implement. Keeps build file cleaner.

---

#### 4. Paging Library Usage (LOW PRIORITY)

**Finding:** Only 42 LazyColumn occurrences found in presentation layer.

**Recommendation:** Verify if Paging Library is overkill. Standard Compose `LazyColumn` might suffice for current use cases. Consider removing if not actually using Paging 3 features.

**Impact:** Potential dependency removal (~200KB APK)

---

## 2. PARSER SYSTEM ANALYSIS

### Current Architecture

```
data/parser/
├── FileParser.kt                    # Interface: extract metadata
├── TextParser.kt                    # Interface: extract text
├── DocumentParser.kt                # Base document parser
├── FileParserImpl.kt                # Factory: selects parser by format
├── TextParserImpl.kt                # Factory: selects text extractor
├── SpeedReaderWordExtractor.kt       # RSVP word tokenization
├── epub/
│   ├── EpubFileParser.kt           # EPUB metadata/cover (152 lines)
│   └── EpubTextParser.kt           # EPUB text extraction (333 lines)
├── pdf/
│   ├── PdfFileParser.kt             # PDF metadata/cover (55 lines)
│   └── PdfTextParser.kt             # PDF text extraction (102 lines)
├── fb2/
│   └── Fb2FileParser.kt             # FB2 metadata/cover
├── html/
│   ├── HtmlFileParser.kt            # HTML metadata
│   └── HtmlTextParser.kt            # HTML text extraction
├── txt/
│   ├── TxtFileParser.kt             # TXT metadata
│   └── TxtTextParser.kt             # TXT content
├── fodt/
│   ├── FodtFileParser.kt            # FODT metadata
│   └── FodtTextParser.kt             # FODT text extraction
├── xml/
│   └── XmlTextParser.kt             # Generic XML parsing
├── opf/
│   ├── OpfParser.kt                # OPF metadata parser (250 lines)
│   └── OpfWriter.kt                # OPF metadata writer (201 lines)
└── comic/
    ├── ComicFileParser.kt            # CBR/CBZ/CB7 metadata
    ├── ArchiveReader.kt               # Archive entry extraction (413 lines)
    └── ArchiveEntry.kt               # Archive entry model
```

**Total Parser Files:** 21 (8 FileParsers + 7 TextParsers + 6 support)
**Total Parser Code:** ~2,777 lines

---

### Issue 1: Format Detection Duplication (HIGH PRIORITY)

**Finding:** `FileParserImpl.kt` and `TextParserImpl.kt` contain identical format detection logic.

```kotlin
// FileParserImpl.kt (lines 39-81)
val fileFormat = ".${cachedFile.name.substringAfterLast(".")}".lowercase().trim()
return when (fileFormat) {
    ".pdf" -> pdfFileParser.parse(cachedFile)
    ".epub" -> epubFileParser.parse(cachedFile)
    ".txt" -> txtFileParser.parse(cachedFile)
    ".fb2" -> fb2FileParser.parse(cachedFile)
    ".html", ".htm" -> htmlFileParser.parse(cachedFile)
    ".md" -> txtFileParser.parse(cachedFile)  // Reuses TXT parser
    ".fodt" -> fodtFileParser.parse(cachedFile)
    ".cbr", ".cbz", ".cb7" -> comicFileParser.parse(cachedFile)
    else -> null
}

// TextParserImpl.kt (lines 42-82) - IDENTICAL PATTERN
val fileFormat = ".${cachedFile.name.substringAfterLast(".")}".lowercase().trim()
return when (fileFormat) {
    ".pdf" -> pdfTextParser.parse(cachedFile)
    ".epub" -> epubTextParser.parse(cachedFile)
    ".txt" -> txtTextParser.parse(cachedFile)
    ".fb2" -> xmlTextParser.parse(cachedFile)
    ".html", ".htm" -> htmlTextParser.parse(cachedFile)
    ".md" -> htmlTextParser.parse(cachedFile)  // Uses HTML parser
    ".fodt" -> fodtTextParser.parse(cachedFile)
    else -> emptyList()
}
```

**Problem:** 40+ lines of duplicated format mapping logic that must be kept in sync.

**Recommendation:**
```kotlin
// Create: data/parser/FormatDetector.kt
object FormatDetector {
    enum class Format(val extensions: List<String>) {
        PDF(listOf("pdf")),
        EPUB(listOf("epub")),
        FB2(listOf("fb2")),
        HTML(listOf("html", "htm")),
        TXT(listOf("txt", "md")),  // MD treated as text
        FODT(listOf("fodt")),
        COMIC(listOf("cbr", "cbz", "cb7")),
        UNKNOWN(emptyList())
    }

    fun detect(fileName: String): Format {
        val extension = fileName.substringAfterLast('.').lowercase()
        return Format.values().find { extension in it.extensions }
            ?: Format.UNKNOWN
    }
}

// Updated FileParserImpl
override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
    if (!cachedFile.canAccess()) {
        Log.e(FILE_PARSER, "File does not exist or no read access is granted.")
        return null
    }

    return when (FormatDetector.detect(cachedFile.name)) {
        Format.PDF -> pdfFileParser.parse(cachedFile)
        Format.EPUB -> epubFileParser.parse(cachedFile)
        // ... etc
        else -> null
    }
}
```

**Estimated Savings:** 40-50 lines of duplicated code

---

### Issue 2: Error Handling Duplication (HIGH PRIORITY)

**Finding:** All FileParsers have identical try-catch structure.

**Pattern in FileParsers:**
```kotlin
// PdfFileParser.kt (lines 21-54)
override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
    return try {
        // parsing logic
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// EpubFileParser.kt (lines 27-135) - SAME PATTERN
override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
    android.util.Log.d("EPUB_PARSER", "Parsing EPUB file: ${cachedFile.name}")
    return try {
        // parsing logic
    } catch (e: Exception) {
        android.util.Log.e("EPUB_PARSER", "Exception parsing EPUB: ${e.message}", e)
        null
    }
}

// TxtFileParser.kt, HtmlFileParser.kt, Fb2FileParser.kt, FodtFileParser.kt - ALL SAME
```

**Problem:** Each parser repeats the same 5-line error handling block. If we want to add logging or improve error handling, we must change 8 files.

**Recommendation:**
```kotlin
// Create: data/parser/BaseFileParser.kt
abstract class BaseFileParser : FileParser {
    protected open val tag: String = "File Parser"

    protected inline fun <T> safeParse(
        parserName: String,
        block: () -> T?
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e("$tag - $parserName", "Exception parsing: ${e.message}", e)
            null
        }
    }
}

// Updated EpubFileParser
class EpubFileParser @Inject constructor() : BaseFileParser() {
    override val tag = "EPUB Parser"

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return safeParse("EPUB") {
            // parsing logic (without try-catch)
        }
    }
}
```

**Estimated Savings:** 40 lines (5 lines × 8 parsers)

---

### Issue 3: Book Construction Duplication (HIGH PRIORITY)

**Finding:** All FileParsers create Book objects with identical default values.

**Pattern in FileParsers:**
```kotlin
// PdfFileParser.kt (lines 35-49)
BookWithCover(
    book = Book(
        title = title,
        authors = authors,
        description = description,
        scrollIndex = 0,
        scrollOffset = 0,
        progress = 0f,
        filePath = cachedFile.uri.toString(),
        lastOpened = null,
        category = Category.entries[0],
        coverImage = null
    ),
    coverImage = null
)

// EpubFileParser.kt (lines 110-124) - IDENTICAL STRUCTURE
BookWithCover(
    book = Book(
        title = title,
        authors = authors,
        description = description,
        scrollIndex = 0,
        scrollOffset = 0,
        progress = 0f,
        filePath = cachedFile.uri.toString(),
        lastOpened = null,
        category = Category.entries[0],
        coverImage = null
    ),
    coverImage = extractCoverImageBitmap(rawFile, coverImage)
)

// TxtFileParser.kt, HtmlFileParser.kt, Fb2FileParser.kt, FodtFileParser.kt - ALL SAME
```

**Problem:** 10-15 lines repeated in each parser. If we want to add a new default field, we must change 8 files.

**Recommendation:**
```kotlin
// Create: data/parser/BookFactory.kt
object BookFactory {
    fun createWithDefaults(
        title: String,
        authors: List<String>,
        description: String?,
        filePath: String,
        category: Category = Category.entries[0],
        coverImage: Bitmap? = null
    ): BookWithCover {
        return BookWithCover(
            book = Book(
                title = title,
                authors = authors,
                description = description,
                scrollIndex = 0,
                scrollOffset = 0,
                progress = 0f,
                filePath = filePath,
                lastOpened = null,
                category = category,
                coverImage = null
            ),
            coverImage = coverImage
        )
    }
}

// Updated EpubFileParser
BookFactory.createWithDefaults(
    title = title,
    authors = authors,
    description = description,
    filePath = cachedFile.uri.toString(),
    category = Category.entries[0],
    coverImage = extractCoverImageBitmap(rawFile, coverImage)
)
```

**Estimated Savings:** 80-120 lines (10-15 lines × 8 parsers)

---

### Issue 4: CachedFile Access Pattern Duplication (MEDIUM PRIORITY)

**Finding:** BookRepositoryImpl has 30-line `getCachedFile()` function with duplicated patterns.

```kotlin
// BookRepositoryImpl.kt (lines 72-102)
private fun getCachedFile(book: BookEntity): CachedFile? {
    val uri = book.filePath.toUri()
    return if (!uri.scheme.isNullOrBlank()) {
        // Content URI handling
        val name = if (uri.scheme == "content") {
            uri.lastPathSegment?.let { Uri.decode(it) } ?: "unknown"
        } else {
            uri.lastPathSegment ?: book.filePath.substringAfterLast(File.separator)
        }
        CachedFileCompat.fromUri(
            context = application,
            uri = uri,
            builder = CachedFileCompat.build(
                name = name,
                path = book.filePath,
                isDirectory = false
            )
        )
    } else {
        // File path handling
        CachedFileCompat.fromFullPath(
            context = application,
            path = book.filePath,
            builder = CachedFileCompat.build(
                name = book.filePath.substringAfterLast(File.separator),
                path = book.filePath,
                isDirectory = false
            )
        )
    }
}
```

**Problem:** Complex URI vs path handling logic is specific to BookRepository but might be needed elsewhere.

**Recommendation:**
```kotlin
// Create: data/util/CachedFileFactory.kt
object CachedFileFactory {
    fun fromBookEntity(
        context: Context,
        book: BookEntity
    ): CachedFile? {
        val uri = book.filePath.toUri()
        val name = if (uri.scheme == "content") {
            uri.lastPathSegment?.let { Uri.decode(it) } ?: "unknown"
        } else {
            uri.lastPathSegment ?: book.filePath.substringAfterLast(File.separator)
        }

        val builder = CachedFileCompat.build(
            name = name,
            path = book.filePath,
            isDirectory = false
        )

        return if (!uri.scheme.isNullOrBlank()) {
            CachedFileCompat.fromUri(context, uri, builder)
        } else {
            CachedFileCompat.fromFullPath(context, book.filePath, builder)
        }
    }
}

// Updated BookRepositoryImpl
private fun getCachedFile(book: BookEntity): CachedFile? {
    return CachedFileFactory.fromBookEntity(application, book)
}
```

**Estimated Savings:** 25-30 lines

---

### Issue 5: Speed Reader Architecture Analysis ⭐⭐⭐ **CRITICAL VALIDATION**

**Date:** 2026-01-28
**Purpose:** Validate that parser consolidations won't negatively impact speed reader functionality

---

#### Speed Reader Architecture (Current State)

```
SpeedReaderModel.loadBook()
    ↓
BookRepository.getSpeedReaderWords()
    ↓ (cache miss)
BookRepository.getBookText()
    ↓ (cache miss) 
TextParserImpl.parse() ← DUPLICATED FORMAT DETECTION HERE
    ↓ (routes to format-specific parser)
EpubTextParser/PdfTextParser/HtmlTextParser/etc.
    ↓ (returns List<ReaderText>)
SpeedReaderWordExtractor.extractWithPreprocessing()
    ↓ (post-processes: collapses whitespace, splits words, removes punctuation)
List<SpeedReaderWord> (cached in speedReaderWordCache)
```

**Key Finding: Speed Reader Uses Standard Parser**

**Speed reader does NOT have a separate parsing path.** It uses the exact same `TextParserImpl.parse()` as the normal reader. The "optimization" is:

1. **Dual caching**: `textCache` (100MB) + `speedReaderWordCache` (50MB)
2. **Pre-tokenization**: Words extracted once, cached forever
3. **Post-processing**: `SpeedReaderWordExtractor` strips formatting AFTER parsing

**Speed Reader vs Normal Reader Comparison:**

| Aspect | Normal Reader | Speed Reader |
|--------|---------------|--------------|
| **Entry Point** | ReaderModel.onLoadText() (line 89) | SpeedReadingScreen.LaunchedEffect (line 72) |
| **Use Case** | getText.execute() | getSpeedReaderWords.execute() |
| **Format Detection** | Yes, at TextParserImpl line 42 | NO - bypassed (uses cached ReaderText) |
| **Parser Factory** | TextParserImpl with FormatDetector | SpeedReaderWordExtractor.extractWithPreprocessing() |
| **Cache** | 100MB textCache (line 64) | 50MB wordCache (line 67) |
| **Output Format** | ReaderText (Chapter, Text, Separator, Image) | SpeedReaderWord (text, globalIndex, paragraphIndex) |
| **Text Processing** | Format-specific parsers (PDF/EPUB/HTML/etc.) | Preprocessing + whitespace splitting |
| **Word Tokenization** | Runtime splitting during rendering | Pre-tokenized extraction |
| **Sentence Detection** | No | Yes (for pauses) |
| **Word Cleaning** | No | Yes (remove punctuation) |
| **Loading Speed** | Text + word extraction | Words only (cached) |
| **Used By** | ReaderScreen.kt | SpeedReadingContent.kt + SpeedReadingScaffold.kt |

**SpeedReaderWordExtractor Analysis:**

```kotlin
// data/parser/SpeedReaderWordExtractor.kt (173 lines)
object SpeedReaderWordExtractor {
    // Primary extraction method (used by BookRepository)
    fun extractWithPreprocessing(readerText: List<ReaderText>): List<SpeedReaderWord> {
        val words = mutableListOf<SpeedReaderWord>()
        val fullText = preprocessText(readerText)  // Collapses whitespace
        val wordsList = splitIntoWords(fullText)    // Splits on whitespace
        
        for (word in wordsList) {
            val cleanWord = cleanWordForSpeedReader(word)  // Removes punctuation
            if (cleanWord.isNotBlank()) {
                words.add(SpeedReaderWord(
                    text = cleanWord,
                    globalIndex = globalIndex,
                    paragraphIndex = paragraphIndex
                ))
                
                if (isSentenceEnding(word)) {  // Detects . ! ? ; :
                    paragraphIndex++
                }
            }
        }
        return words
    }
    
    private fun preprocessText(readerText: List<ReaderText>): String {
        // Collapses whitespace, removes newlines/tabs
        // Returns plain string
    }
    
    private fun cleanWordForSpeedReader(word: String): String {
        // Keeps: letters, digits, basic punctuation
        // Removes: extra whitespace, most symbols
    }
}
```

**Critical Observation:** Speed reader ONLY depends on:
1. `TextParserImpl.parse()` to get `ReaderText`
2. `SpeedReaderWordExtractor.extractWithPreprocessing()` to post-process
3. Caching in `BookRepository` (textCache + speedReaderWordCache)

---

#### Consolidation Impact Assessment

| Recommendation | Files Modified | Speed Reader Impact |
|---------------|----------------|---------------------|
| **FormatDetector** | TextParserImpl, FileParserImpl | ✅ **POSITIVE** - Speed reader uses TextParserImpl |
| **BaseFileParser** | FileParsers (EpubFileParser, PdfFileParser, etc.) | ⚠️ **NONE** - FileParser for metadata only |
| **BookFactory** | FileParsers | ⚠️ **NONE** - Book creation for metadata only |

**Conclusion:** Speed reader functionality and performance will remain unchanged. The consolidations are purely about code organization and eliminating duplication in parser factories, which both readers share.

---

#### Detailed Impact Analysis

**1. FormatDetector Consolidation ✅ SAFE**

**Current State:**
```kotlin
// TextParserImpl.kt line 42 (speed reader uses this!)
val fileFormat = ".${cachedFile.name.substringAfterLast(".")}".lowercase().trim()
return when (fileFormat) {
    ".epub" -> epubTextParser.parse(cachedFile)
    ".pdf" -> pdfTextParser.parse(cachedFile)
    // ...
}

// FileParserImpl.kt line 39 (IDENTICAL DUPLICATE)
val fileFormat = ".${cachedFile.name.substringAfterLast(".")}".lowercase().trim()
return when (fileFormat) {
    ".epub" -> epubFileParser.parse(cachedFile)
    ".pdf" -> pdfTextParser.parse(cachedFile)
    // ...
}
```

**After Consolidation:**
```kotlin
// Both use shared FormatDetector.detect()
when (FormatDetector.detect(cachedFile.name)) {
    Format.EPUB -> epubTextParser.parse(cachedFile)
    Format.PDF -> pdfTextParser.parse(cachedFile)
    // ...
}
```

**Speed Reader Impact:** Benefits from code deduplication. Format detection becomes a single source of truth. No performance change (same when/if branches).

---

**2. BaseFileParser Consolidation ✅ SAFE (for speed reader)**

**What it does:** Abstracts error handling in FileParsers

**Current State:** All FileParsers have this pattern:
```kotlin
override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
    return try {
        // parsing logic
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
```

**Files Affected:** `EpubFileParser.kt`, `PdfFileParser.kt`, `TxtFileParser.kt`, etc.

**Speed Reader Impact:** **NONE** - Speed reader never calls FileParser (only used for metadata extraction during library import).

---

**3. BookFactory Consolidation ✅ SAFE (for speed reader)**

**What it does:** Eliminates duplicate Book construction code

**Current State:** All FileParsers create BookWithCover like this:
```kotlin
BookWithCover(
    book = Book(
        title = title,
        authors = authors,
        scrollIndex = 0,      // repeated default
        scrollOffset = 0,     // repeated default
        progress = 0f,        // repeated default
        // ... 10+ repeated fields
    ),
    coverImage = null
)
```

**Files Affected:** All FileParsers

**Speed Reader Impact:** **NONE** - Speed reader only reads Book objects from database, doesn't create them.

---

#### Missing Recommendation: BaseTextParser

**Observation:** The analysis document proposes `BaseFileParser` but not `BaseTextParser`. TextParsers also have duplicated error handling.

**Current TextParser Pattern:**
```kotlin
// EpubTextParser.kt, PdfTextParser.kt, HtmlTextParser.kt, etc.
override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
    return try {
        // parsing logic
    } catch (e: Exception) {
        android.util.Log.e("EPUB_PARSER", "Exception parsing: ${e.message}", e)
        emptyList()  // consistent across all text parsers
    }
}
```

**Recommendation:** Create `BaseTextParser` for consistency:
```kotlin
// Create: data/parser/BaseTextParser.kt
abstract class BaseTextParser : TextParser {
    protected abstract val tag: String
    
    protected inline fun safeParse(
        block: () -> List<ReaderText>
    ): List<ReaderText> {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(tag, "Exception parsing: ${e.message}", e)
            emptyList()
        }
    }
}

// Updated EpubTextParser
class EpubTextParser @Inject constructor() : BaseTextParser() {
    override val tag = "EPUB Parser"
    
    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        return safeParse {
            // parsing logic (without try-catch)
        }
    }
}
```

**Impact:** 
- Benefits both normal reader and speed reader equally
- Estimated savings: ~40 lines (5 lines × 8 text parsers)
- Same error handling pattern as BaseFileParser

---

#### Speed Reader Performance Preservation

The consolidation recommendations will NOT impact speed reader performance because:

1. **Caching strategy unchanged**: `textCache` + `speedReaderWordCache` remain intact
2. **Post-processing unchanged**: `SpeedReaderWordExtractor.extractWithPreprocessing()` unchanged
3. **Tokenization unchanged**: Word splitting and cleaning logic unchanged
4. **Format detection behavior unchanged**: Same when/if branches, just deduplicated code

**What changes:** Only code organization, not execution flow or performance.

**Updated Phase 1.2 Recommendation:**

```kotlin
// data/parser/FormatDetector.kt (already created in phase 1.1)
object FormatDetector {
    enum class Format(val extensions: List<String>) {
        PDF(listOf("pdf")),
        EPUB(listOf("epub")),
        FB2(listOf("fb2")),
        HTML(listOf("html", "htm")),
        TXT(listOf("txt", "md")),
        FODT(listOf("fodt")),
        COMIC(listOf("cbr", "cbz", "cb7")),
        UNKNOWN(emptyList())
    }
    
    fun detect(fileName: String): Format {
        val extension = fileName.substringAfterLast('.').lowercase()
        return Format.values().find { extension in it.extensions } 
            ?: Format.UNKNOWN
    }
}

// Update BOTH parsers (speed reader uses TextParserImpl)
// data/parser/TextParserImpl.kt
when (FormatDetector.detect(cachedFile.name)) {
    Format.EPUB -> epubTextParser.parse(cachedFile)
    Format.PDF -> pdfTextParser.parse(cachedFile)
    // ...
}

// data/parser/FileParserImpl.kt
when (FormatDetector.detect(cachedFile.name)) {
    Format.EPUB -> epubFileParser.parse(cachedFile)
    Format.PDF -> pdfTextParser.parse(cachedFile)
    // ...
}
```

---

#### Summary

✅ **FormatDetector consolidation is SAFE** - Speed reader will benefit  
✅ **BaseFileParser consolidation is SAFE** - No impact on speed reader  
✅ **BookFactory consolidation is SAFE** - No impact on speed reader  

**Proceed with Phase 1.2 as planned.** Speed reader functionality and performance will remain unchanged. The consolidations are purely about code organization and eliminating duplication in parser factories, which both readers share.

---

## 3. USE CASE ANALYSIS

### Current Architecture

```
domain/use_case/
├── book/                    # 15 use cases
├── bookmark/                 # 5 use cases
├── color_preset/             # 5 use cases
├── history/                  # 5 use cases
├── opds/                    # 3 use cases
├── file_system/              # 2 use cases
├── permission/               # 2 use cases
├── data_store/               # 3 use cases
└── import_export/            # 2 use cases
```

**Total Use Cases:** 42 files
**Total Use Case Code:** ~2,094 lines

---

### Issue 1: Thin Wrapper Use Cases (VERY HIGH PRIORITY)

**Finding:** Most use cases are simple 15-20 line wrappers that delegate to repositories.

**Examples of Thin Wrappers:**

```kotlin
// domain/use_case/book/GetBooks.kt (20 lines)
class GetBooks @Inject constructor(
    private val repository: BookRepository
) {
    suspend fun execute(query: String): List<Book> {
        return repository.getBooks(query)
    }
}

// domain/use_case/bookmark/GetBookmarksByBookId.kt (21 lines)
class GetBookmarksByBookId @Inject constructor(
    private val repository: BookmarkRepository
) {
    suspend fun execute(bookId: Int): List<Bookmark> {
        return repository.getBookmarksByBookId(bookId)
    }
}

// domain/use_case/history/GetHistory.kt (~20 lines)
class GetHistory @Inject constructor(
    private val repository: HistoryRepository
) {
    suspend fun execute(): List<History> {
        return repository.getHistory()
    }
}

// domain/use_case/color_preset/GetColorPresets.kt (~20 lines)
class GetColorPresets @Inject constructor(
    private val repository: ColorPresetRepository
) {
    suspend fun execute(): List<ColorPreset> {
        return repository.getColorPresets()
    }
}
```

**Pattern:** 30+ use cases are identical wrappers. They add no value beyond the repository call.

**Problem:** Violates YAGNI principle, adds indirection without benefit. Increases maintenance burden.

**Recommendation:** Direct repository injection where possible, or use cases only when they contain business logic.

**Implementation Approach:**
```kotlin
// BEFORE: Thin wrapper use case
class GetBooks @Inject constructor(
    private val repository: BookRepository
) {
    suspend fun execute(query: String): List<Book> {
        return repository.getBooks(query)
    }
}

// ViewModel uses it:
class LibraryModel @Inject constructor(
    private val getBooks: GetBooks  // Unnecessary indirection
) {
    // ...
}

// AFTER: Direct repository access
class LibraryModel @Inject constructor(
    private val repository: BookRepository  // Direct access
) {
    suspend fun getBooks(query: String): List<Book> {
        return repository.getBooks(query)
    }
}

// Keep use case ONLY for business logic:
class AutoImportCodexBooksUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val historyRepository: HistoryRepository
) {
    suspend fun execute(onProgress: (ImportProgress) -> Unit): Int {
        // Complex logic with progress tracking
        // This is a VALID use case with business logic
    }
}
```

**Impact:**
- Reduce 42 use cases → ~20 (keep only those with business logic)
- Eliminate ~500 lines of boilerplate
- Simplify dependency injection in ViewModels

---

### Issue 2: CRUD Use Case Duplication (HIGH PRIORITY)

**Finding:** Multiple similar CRUD use cases that could be merged.

**Book CRUD:**
```kotlin
GetBooks                      // Get all books (with query)
GetBooksById                  // Get specific books by IDs
GetBookById                   // Get single book by ID
GetBookByFilePath             // Get single book by path
```

**Recommendation:**
```kotlin
// Merge into single BookOperations use case:
class BookOperations @Inject constructor(
    private val repository: BookRepository
) {
    suspend fun getBooks(query: String = ""): List<Book> =
        repository.getBooks(query)

    suspend fun getBookById(id: Int): Book? =
        repository.getBookById(id)

    suspend fun getBooksById(ids: List<Int>): List<Book> =
        repository.getBooksById(ids)

    suspend fun getBookByFilePath(path: String): Book? =
        repository.getBookByFilePath(path)

    // Other book operations...
}
```

**Bookmark CRUD:**
```kotlin
GetBookmarksByBookId       // Get all bookmarks for a book
InsertBookmark             // Add a bookmark
DeleteBookmark             // Delete one bookmark
DeleteBookmarksByBookId   // Delete all bookmarks for a book
DeleteAllBookmarks         // Delete all bookmarks
```

**Recommendation:** Single `BookmarkOperations` use case.

**History CRUD:**
```kotlin
GetHistory              // Get all history
GetLatestHistory        // Get latest history for books
DeleteHistory           // Delete one history entry
DeleteWholeHistory     // Delete all history
```

**Recommendation:** Single `HistoryOperations` use case.

**Estimated Savings:** 200-250 lines

---

## 4. VIEWMODEL ANALYSIS

### Current State

**Total ViewModels:** 14 files
**State Management:** 10 use StateFlow (consistent pattern)

**ViewModel List:**
- MainModel
- LibraryModel
- ReaderModel
- SpeedReaderModel
- SettingsModel (841 lines - complex color preset management)
- BookInfoModel
- HistoryModel
- AboutModel
- ImportProgressViewModel
- BrowseModel
- OpdsCatalogModel
- OpdsSourcesModel
- OpdsDownloadModel

---

### Issue 1: State Management Boilerplate (MEDIUM PRIORITY)

**Finding:** All ViewModels have identical StateFlow setup.

**Pattern in ViewModels:**
```kotlin
// Common pattern across 10+ ViewModels
private val _state = MutableStateFlow(SomeState())
val state = _state.asStateFlow()
```

**Recommendation:**
```kotlin
// Create: ui/base/BaseViewModel.kt
abstract class BaseViewModel<State : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state = _state.asStateFlow()

    abstract fun initialState(): State

    protected fun updateState(update: (State) -> State) {
        _state.update { update(it) }
    }
}

// Updated ViewModel
class LibraryModel @Inject constructor(
    private val repository: BookRepository
) : BaseViewModel<LibraryState>() {

    override fun initialState() = LibraryState()

    fun loadBooks(query: String) {
        viewModelScope.launch {
            val books = repository.getBooks(query)
            updateState { it.copy(books = books) }
        }
    }
}
```

**Estimated Savings:** 50-100 lines across ViewModels

---

### Issue 2: Repository Access Patterns (LOW-MEDIUM PRIORITY)

**Finding:** ViewModels inject 3-8 use cases on average.

**Example - LibraryModel:**
```kotlin
class LibraryModel @Inject constructor(
    private val getBooks: GetBooks,           // Thin wrapper
    private val deleteBooks: DeleteBooks,       // Thin wrapper
    private val updateBook: UpdateBook,        // Thin wrapper
    private val getBookById: GetBookById,    // Thin wrapper
    private val getText: GetText,              // Complex logic
    private val insertBook: InsertBook,        // Thin wrapper
    private val deleteProgressHistory: DeleteProgressHistoryUseCase
) : ViewModel() { ... }
```

**Problem:** Dependency graph is unnecessarily deep. ViewModels → Use Cases → Repositories.

**Recommendation:** For thin wrapper use cases, inject repositories directly. Keep use cases only for complex business logic.

**Impact:** Simplified dependency injection, clearer code structure

---

## 5. PRESENTATION COMPONENTS ANALYSIS

### Current State

**Total Presentation Files:** 418 (59% of total codebase)
**Settings Files:** 11,442 lines
**Settings Option Files:** 105
**Settings Subcategory Files:** 27
**Settings Component Files:** 112 (across 26 component directories)

---

### Issue 1: Settings Over-Granularity (VERY HIGH PRIORITY)

**Finding:** 60+ subdirectories with extreme fragmentation.

**Settings Hierarchy:**
```
settings/
├── components/                    # 6 shared widgets (1,302 LOC)
│   ├── ChipsWithTitle.kt
│   ├── ColorPickerWithTitle.kt
│   ├── GenericOption.kt
│   ├── SegmentedButtonWithTitle.kt
│   ├── SliderWithTitle.kt
│   └── SwitchWIthTitle.kt
│
├── reader/                      # 15 sub-features (101 files)
│   ├── reading_mode/             # Auto-detection settings
│   ├── font/                    # 7 option files (subcategories)
│   ├── padding/                  # Margin settings
│   ├── text/                    # Alignment, justification
│   ├── progress/                 # Progress bar settings
│   ├── chapters/                 # Chapter navigation
│   ├── reading_speed/            # Auto-scroll, WPM
│   ├── speed_reading/            # RSVP mode (412 LOC alone)
│   ├── search/                   # Highlight colors
│   ├── dictionary/                # Lookup settings
│   ├── translator/               # Translation services
│   ├── images/                   # Background images
│   ├── misc/                     # Other settings
│   ├── system/                   # System integration
│   └── comic/                    # Comic-specific settings
│
├── library/                      # 3 sub-features
│   ├── display/                  # Grid/list, cover size
│   ├── sort/                     # Sort by title/author/date
│   └── tabs/                     # Tab configuration
│
├── browse/                       # 5 sub-features
│   ├── display/                  # Browse view settings
│   ├── filter/                   # Category filtering
│   ├── sort/                     # Browse sort options
│   ├── scan/                     # Folder scanning
│   └── opds/                     # OPDS settings
│
├── appearance/                   # 3 sub-features
│   ├── colors/                   # Color presets (559 LOC option)
│   ├── theme_preferences/          # Light/dark/auto
│   └── components/               # Shared widgets
│
├── general/                     # App-wide settings
├── import_export/                # Settings backup/restore
├── SettingsContent.kt
├── SettingsLayout.kt
├── SettingsScaffold.kt
├── SettingsTopBar.kt
└── SettingsLayoutItem.kt
```

**Problem:**
- 105 individual option files, each 30-40 lines
- 26 component directories, some with only 2-3 files
- Navigation complexity due to excessive nesting
- Maintenance burden: changing a pattern requires updating 50+ files

**Recommendation 1: Merge Related Settings (Reduce 60+ → 30 subdirectories)**

**Example - Reader Settings Consolidation:**
```
BEFORE: 15 separate reader sub-features
settings/reader/
├── reading_mode/
├── font/
├── padding/
├── text/
├── progress/
├── chapters/
├── reading_speed/
├── speed_reading/
├── search/
├── dictionary/
├── translator/
├── images/
├── misc/
├── system/
└── comic/

AFTER: 6 consolidated sub-features
settings/reader/
├── display/              # font, padding, text, reading_mode (merged)
├── navigation/           # chapters, progress (merged)
├── content/             # reading_speed, speed_reading (merged)
├── search/              # search, dictionary, translator (merged)
├── appearance/           # images (merged)
└── advanced/            # misc, system, comic (merged)
```

**Estimated Savings:** 1,000-1,500 lines (reduced boilerplate, merged options)

---

**Recommendation 2: Generic Settings Option DSL (MEDIUM PRIORITY)**

**Finding:** All 105 option files follow identical structure:

```kotlin
// Pattern repeated 105 times:
@Composable
fun SomeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    GenericOption(
        OptionConfig(
            stateSelector = { it.someSetting },
            eventCreator = { MainEvent.OnSomeSettingChange(it) },
            component = { value, onChange ->
                // Specific UI component (Slider, Switch, etc.)
                SliderWithTitle(
                    value = value,
                    onValueChange = { onChange(it) }
                )
            }
        )
    )
}
```

**Problem:** Every setting option is a 30-line file with identical scaffolding.

**Recommendation:** Create composable DSL:

```kotlin
// Create: presentation/settings/components/SettingsOptionDsl.kt
@Composable
inline fun <T> SettingOption(
    noinline label: @Composable () -> String,
    noinline description: @Composable (() -> String)? = null,
    crossinline component: @Composable (T, (T) -> Unit)
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    GenericOption(
        OptionConfig(
            stateSelector = { /* generic */ },
            eventCreator = { /* generic */ },
            component = component
        )
    )
}

// BEFORE: FontSizeOption.kt (34 lines)
@Composable
fun FontSizeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SliderWithTitle(
        value = state.value.fontSize to "pt",
        fromValue = 10,
        toValue = 35,
        title = stringResource(id = R.string.font_size_option),
        onValueChange = {
            mainModel.onEvent(MainEvent.OnChangeFontSize(it))
        }
    )
}

// AFTER: Inline in FontSubcategory.kt (5 lines)
item {
    SettingOption(label = { stringResource(R.string.font_size_option) }) { value, onChange ->
        SliderWithTitle(
            value = value to "pt",
            fromValue = 10,
            toValue = 35,
            onValueChange = onChange
        )
    }
}
```

**Estimated Savings:** 2,000+ lines (eliminates 105 files × 30 lines each)

---

### Issue 2: Duplicate Component Patterns (HIGH PRIORITY)

**Finding:** Settings widgets used repeatedly with similar patterns.

**Usage Statistics:**
- `SliderWithTitle`: 42 occurrences
- `SwitchWithTitle`: 187 occurrences
- `Color picker`: Multiple similar implementations
- `ChipsWithTitle`: Used for font families, themes, etc.

**Recommendation:** Consolidate into `SettingsControls.kt` with all common patterns.

---

## 6. CODE DUPLICATION ACROSS LAYERS

### Summary

**Total Code:** ~69,895 lines
**Repository Implementations:** 2,039 lines
**Mappers:** 299 lines (9 files)
**DAOs:** 213 lines (2 files)

---

### Issue 1: Repository Boilerplate (HIGH PRIORITY)

**Finding:** All repositories follow identical structure.

**Pattern:**
```kotlin
// Example pattern in 10 repositories
@Singleton
class SomeRepositoryImpl @Inject constructor(
    private val dao: SomeDao,
    private val mapper: SomeMapper
) : SomeRepository {

    override suspend fun getItems(): List<Item> {
        return dao.getItems().map { mapper.toDomain(it) }
    }

    override suspend fun getItem(id: Int): Item? {
        return dao.getItem(id)?.let { mapper.toDomain(it) }
    }

    override suspend fun insertItem(item: Item) {
        return dao.insertItem(mapper.toEntity(item))
    }

    override suspend fun updateItem(item: Item) {
        return dao.updateItem(mapper.toEntity(item))
    }

    override suspend fun deleteItem(item: Item) {
        return dao.deleteItem(mapper.toEntity(item))
    }
}
```

**Problem:** 40-60 lines of boilerplate per repository. Generic CRUD operations are repeated.

**Recommendation:**
```kotlin
// Create: data/repository/BaseRepository.kt
abstract class BaseRepository<Domain, Entity, Dao> {
    protected abstract val dao: Dao
    protected abstract val mapper: Mapper<Domain, Entity>

    protected suspend fun getAllAsDomain(): List<Domain> =
        dao.getAll().map { mapper.toDomain(it) }

    protected suspend fun getByIdAsDomain(id: Int): Domain? =
        dao.getById(id)?.let { mapper.toDomain(it) }

    protected suspend fun insertFromDomain(item: Domain) =
        dao.insert(mapper.toEntity(item))

    protected suspend fun updateFromDomain(item: Domain) =
        dao.update(mapper.toEntity(item))

    protected suspend fun deleteFromDomain(item: Domain) =
        dao.delete(mapper.toEntity(item))
}

// Updated Repository
@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val dao: BookmarkDao,
    private val mapper: BookmarkMapper
) : BaseRepository<Bookmark, BookmarkEntity, BookmarkDao>(), BookmarkRepository {

    override val dao = this.dao
    override val mapper = this.mapper

    override suspend fun getBookmarksByBookId(bookId: Int): List<Bookmark> =
        dao.getBookmarksByBookId(bookId).map { mapper.toDomain(it) }

    // Only implement non-BOILERPLATE methods (custom queries)
}
```

**Estimated Savings:** 200-300 lines

---

### Issue 2: Mapper Overhead (MEDIUM PRIORITY)

**Finding:** Double mapping (Entity → Domain → Presentation) adds complexity.

**Current Flow:**
```
Database (Entity)
    ↓ Mapper.toDomain()
Domain Model
    ↓ Presentation Mapper
Presentation Model
```

**Example - BookMapper (90 lines):**
```kotlin
// data/mapper/book/BookMapperImpl.kt
class BookMapperImpl @Inject constructor() : BookMapper {
    override fun toBook(entity: BookEntity): Book {
        return Book(
            id = entity.id,
            title = entity.title,
            authors = entity.authors,
            // ... 20+ lines of mapping
        )
    }

    override fun toBookEntity(book: Book): BookEntity {
        return BookEntity(
            id = book.id,
            title = book.title,
            authors = book.authors,
            // ... 20+ lines of reverse mapping
        )
    }
}
```

**Recommendation:** Consider if mappers can be eliminated using:

1. **Room @Embedded or @Relation** for automatic mapping
2. **Domain objects as Room entities** with ignored columns
3. **Kotlin data classes as-is** if presentation can use domain models directly

**Impact:** Eliminate 299 lines of mapper code + simplify data flow

---

### Issue 3: LazyColumn Pattern Duplication (MEDIUM PRIORITY)

**Finding:** 42 LazyColumn occurrences with similar patterns.

**Pattern:**
```kotlin
LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize()
) {
    items(items = items) { item ->
        // Item rendering
    }
}
```

**Recommendation:** Extract to `AppList.kt`:
```kotlin
@Composable
fun <T> AppList(
    items: List<T>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        items(items = items, itemContent = itemContent)
    }
}
```

**Estimated Savings:** 100-150 lines

---

## 7. SETTINGS-SPECIFIC DEEP DIVE

### SettingsModel Analysis

**Lines:** 841
**Responsibilities:**
1. Color preset management (14 event handlers)
2. Color preset CRUD operations
3. Codex directory management
4. Permission management
5. Auto-import triggering

**Complex Areas:**

#### Color Preset State Management (Lines 78-332)
```kotlin
private val _state = MutableStateFlow(SettingsState())
val state = _state.asStateFlow()

// Job management for preventing race conditions
private var selectColorPresetJob: Job? = null
private var addColorPresetJob: Job? = null
private var deleteColorPresetJob: Job? = null
private var updateColorColorPresetJob: Job? = null
private var updateTitleColorPresetJob: Job? = null
private var shuffleColorPresetJob: Job? = null
private var restoreColorPresetJob: Job? = null
private var resetColorPresetJob: Job? = null
```

**Issue:** 8 Job variables for color preset operations. Complex cancellation logic (lines 824-833).

**Recommendation:**
```kotlin
// Create dedicated ColorPresetManager class
class ColorPresetManager(
    private val updateColorPreset: UpdateColorPreset,
    private val selectColorPreset: SelectColorPreset,
    private val deleteColorPreset: DeleteColorPreset
) {
    private val currentOperationJob = AtomicReference<Job?>(null)

    suspend fun <T> withExclusiveOperation(block: suspend () -> T): T {
        currentOperationJob.get()?.cancel()
        return supervisorScope {
            val job = launch { block() }
            currentOperationJob.set(job)
            job.join()
        }
    }
}
```

**Estimated Savings:** 100-150 lines in SettingsModel

---

### Settings Subcategory Pattern

**Example - FontSubcategory.kt (61 lines):**
```kotlin
fun LazyListScope.FontSubcategory(
    titleColor: @Composable () -> Color,
    title: @Composable () -> String,
    showTitle: Boolean = true,
    showDivider: Boolean = true,
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        item { FontFamilyOption() }
        item { CustomFontsOption() }
        item { FontThicknessOption() }
        item { FontStyleOption() }
        item { FontSizeOption() }
        item { LetterSpacingOption() }
    }
}
```

**Pattern Repeated:** 27 subcategory files follow this exact structure.

**Recommendation:** Use DSL to eliminate subcategory files:

```kotlin
// BEFORE: 6 subcategory files (FontSubcategory, etc.)
settings/reader/
├── font/
│   ├── FontSubcategory.kt
│   └── components/
│       ├── FontFamilyOption.kt
│       ├── CustomFontsOption.kt
│       ├── FontSizeOption.kt
│       └── ...

// AFTER: Consolidated settings file
settings/reader/ReaderSettingsContent.kt
@Composable
fun ReaderSettingsContent() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SettingsSubcategory(title = { stringResource(R.string.font_reader_settings) }) {
        item {
            SettingOption(label = { stringResource(R.string.font_family_option) }) { value, onChange ->
                ChipsWithTitle(...)
            }
        }
        item {
            SettingOption(label = { stringResource(R.string.font_size_option) }) { value, onChange ->
                SliderWithTitle(...)
            }
        }
        // ... other font settings inline
    }
}
```

**Estimated Savings:** 500-800 lines (eliminate 27 subcategory files × 20-30 lines each)

---

## 8. ARCHITECTURE & PATTERNS

### Clean Architecture Assessment

**Current State:** ✅ Well-implemented
```
┌─────────────────────────────────────────┐
│   Presentation Layer (418 files)   │
│   - Compose UI                  │
│   - ViewModels (14)              │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Domain Layer (68 files)          │
│   - Use Cases (42)               │
│   - Repository Interfaces (8)        │
│   - Domain Models                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Data Layer (122 files)          │
│   - Repository Implementations (10)  │
│   - DAOs (2)                    │
│   - Mappers (9)                   │
│   - Parsers (21)                  │
└───────────────────────────────────────┘
```

**Strengths:**
- Clean separation of concerns
- Dependency inversion via interfaces
- Testable architecture
- Clear data flow

**Weaknesses:**
- Too many thin use cases (unnecessary indirection)
- Double mapping (Entity → Domain → Presentation)
- Boilerplate in repositories
- Settings granularity excessive

---

## IMPLEMENTATION PLAN

### Phase 1: Critical Simplifications (Week 1-2)

#### 1.1 Eliminate Thin Wrapper Use Cases ⭐⭐⭐
**Priority:** VERY HIGH
**Effort:** Low
**Impact:** High

**Goal:** Reduce 42 use cases → ~20

**Actions:**
- [ ] Audit all use cases, identify thin wrappers (those < 25 lines)
- [ ] Directly inject repositories into ViewModels for thin wrapper use cases
- [ ] Keep use cases only for complex business logic
- [ ] Update all affected ViewModels
- [ ] Remove deleted use case files
- [ ] Run tests, verify no regressions

**Files to Modify:**
- `ui/library/LibraryModel.kt`
- `ui/book_info/BookInfoModel.kt`
- `ui/history/HistoryModel.kt`
- `ui/settings/SettingsModel.kt`
- Plus 15+ other ViewModels

**Estimated Savings:** 500+ lines

---

#### 1.2 Create Format Detector Utility ⭐⭐
**Priority:** HIGH
**Effort:** Low
**Impact:** Medium-High

**Goal:** Eliminate 40+ lines of duplicated format detection

**Speed Reader Validation:** ✅ **COMPLETE (2026-01-28)**
- Speed reader uses same `TextParserImpl.parse()` as normal reader
- Consolidation will NOT impact speed reader functionality or performance
- See Section 2.5 for detailed impact analysis

**Actions:**
- [ ] Create `data/parser/FormatDetector.kt`
- [ ] Add Format enum with extensions
- [ ] Add detect() function
- [ ] Update `FileParserImpl.kt` to use FormatDetector
- [ ] Update `TextParserImpl.kt` to use FormatDetector
- [ ] Remove duplicated format detection logic
- [ ] Run tests, verify all formats still work
- [ ] Test speed reader functionality

**Files to Create:**
- `data/parser/FormatDetector.kt`

**Files to Modify:**
- `data/parser/FileParserImpl.kt`
- `data/parser/TextParserImpl.kt`

**Estimated Savings:** 40-50 lines

---

#### 1.3 Create BaseFileParser ⭐
**Priority:** HIGH
**Effort:** Low-Medium
**Impact:** High

**Goal:** Eliminate error handling duplication

**Speed Reader Validation:** ✅ **COMPLETE (2026-01-28)**
- Speed reader never uses FileParser (only used for metadata extraction)
- Consolidation will NOT impact speed reader functionality
- See Section 2.5 for detailed impact analysis

**Actions:**
- [ ] Create `data/parser/BaseFileParser.kt`
- [ ] Implement safeParse() method
- [ ] Update all FileParsers to extend BaseFileParser
- [ ] Remove try-catch blocks from each parser
- [ ] Run tests, verify error handling still works

**Files to Create:**
- `data/parser/BaseFileParser.kt`

**Files to Modify:**
- `data/parser/epub/EpubFileParser.kt`
- `data/parser/pdf/PdfFileParser.kt`
- `data/parser/txt/TxtFileParser.kt`
- `data/parser/html/HtmlFileParser.kt`
- `data/parser/fb2/Fb2FileParser.kt`
- `data/parser/fodt/FodtFileParser.kt`
- `data/parser/comic/ComicFileParser.kt`

**Estimated Savings:** 40 lines

---

#### 1.4 Create BaseTextParser ⭐ **NEW RECOMMENDATION**
**Priority:** HIGH
**Effort:** Low-Medium
**Impact:** High

**Goal:** Eliminate error handling duplication in TextParsers (same pattern as BaseFileParser)

**Rationale:** TextParsers also have duplicated try-catch structure:
```kotlin
// EpubTextParser.kt, PdfTextParser.kt, HtmlTextParser.kt, etc.
override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
    return try {
        // parsing logic
    } catch (e: Exception) {
        android.util.Log.e("EPUB_PARSER", "Exception parsing: ${e.message}", e)
        emptyList()  // consistent across all text parsers
    }
}
```

**Speed Reader Validation:** ✅ **BENEFITS SPEED READER**
- Speed reader uses TextParserImpl directly
- BaseTextParser provides same error handling consistency
- See Section 2.5 for detailed impact analysis

**Actions:**
- [ ] Create `data/parser/BaseTextParser.kt`
- [ ] Implement safeParse() method
- [ ] Update all TextParsers to extend BaseTextParser
- [ ] Remove try-catch blocks from each text parser
- [ ] Run tests, verify error handling still works

**Files to Create:**
- `data/parser/BaseTextParser.kt`

**Files to Modify:**
- `data/parser/epub/EpubTextParser.kt`
- `data/parser/pdf/PdfTextParser.kt`
- `data/parser/txt/TxtTextParser.kt`
- `data/parser/html/HtmlTextParser.kt`
- `data/parser/fodt/FodtTextParser.kt`
- `data/parser/xml/XmlTextParser.kt`

**Estimated Savings:** 40-50 lines (6-7 lines × 7 text parsers)

---

### Phase 2: Settings Consolidation (Week 3-5)

#### 2.1 Consolidate Reader Settings Sub-Features ⭐⭐⭐
**Priority:** VERY HIGH
**Effort:** Medium-High
**Impact:** Very High

**Goal:** Reduce 15 reader sub-features → 6

**Actions:**
- [ ] Plan consolidation mapping (which features to merge)
- [ ] Create new consolidated directory structure
- [ ] Merge related option files into consolidated options
- [ ] Update navigation in settings
- [ ] Remove old subdirectories
- [ ] Test all settings functionality
- [ ] Update documentation

**Consolidation Plan:**
```
Display Settings:
- reading_mode + font + padding + text → display

Navigation Settings:
- chapters + progress → navigation

Content Settings:
- reading_speed + speed_reading → content

Search Settings:
- search + dictionary + translator → search

Appearance Settings:
- images → appearance (merge with appearance/)

Advanced Settings:
- misc + system + comic → advanced
```

**Estimated Savings:** 1,000-1,500 lines

---

#### 2.2 Create Settings Option DSL ⭐⭐
**Priority:** HIGH
**Effort:** Medium
**Impact:** High

**Goal:** Eliminate 105 option files

**Actions:**
- [ ] Create `presentation/settings/components/SettingsOptionDsl.kt`
- [ ] Define SettingOption() composable
- [ ] Implement inline options for common controls (Slider, Switch, etc.)
- [ ] Convert 3-5 representative option files to DSL
- [ ] Verify UI remains identical
- [ ] Convert remaining option files
- [ ] Remove old option files
- [ ] Run tests

**Files to Create:**
- `presentation/settings/components/SettingsOptionDsl.kt`

**Files to Modify:**
- All 105 option files → inline in subcategories

**Estimated Savings:** 2,000+ lines

---

### Phase 3: Repository & Data Layer (Week 6-7)

#### 3.1 Create Base Repository ⭐
**Priority:** HIGH
**Effort:** Medium
**Impact:** Medium-High

**Goal:** Eliminate repository boilerplate

**Actions:**
- [ ] Create `data/repository/BaseRepository.kt`
- [ ] Implement generic CRUD operations
- [ ] Update 3-5 repositories to extend BaseRepository
- [ ] Remove duplicated CRUD methods
- [ ] Run tests, verify data operations work

**Files to Create:**
- `data/repository/BaseRepository.kt`

**Files to Modify:**
- `data/repository/BookmarkRepositoryImpl.kt`
- `data/repository/HistoryRepositoryImpl.kt`
- `data/repository/ColorPresetRepositoryImpl.kt`

**Estimated Savings:** 200-300 lines

---

#### 3.2 Consolidate CRUD Use Cases ⭐
**Priority:** HIGH
**Effort:** Low-Medium
**Impact:** Medium

**Goal:** Merge similar CRUD use cases

**Actions:**
- [ ] Create `domain/use_case/book/BookOperations.kt`
- [ ] Create `domain/use_case/bookmark/BookmarkOperations.kt`
- [ ] Create `domain/use_case/history/HistoryOperations.kt`
- [ ] Update ViewModels to use Operations classes
- [ ] Remove old CRUD use cases
- [ ] Run tests

**Files to Create:**
- `domain/use_case/book/BookOperations.kt`
- `domain/use_case/bookmark/BookmarkOperations.kt`
- `domain/use_case/history/HistoryOperations.kt`

**Estimated Savings:** 200-250 lines

---

#### 3.3 Simplify ColorPreset State Management ⭐
**Priority:** MEDIUM
**Effort:** Low-Medium
**Impact:** Medium

**Goal:** Extract ColorPresetManager from SettingsModel

**Actions:**
- [ ] Create `ui/settings/ColorPresetManager.kt`
- [ ] Implement exclusive operation management
- [ ] Move color preset logic from SettingsModel
- [ ] Update SettingsModel to use ColorPresetManager
- [ ] Run tests, verify color preset operations work

**Files to Create:**
- `ui/settings/ColorPresetManager.kt`

**Files to Modify:**
- `ui/settings/SettingsModel.kt`

**Estimated Savings:** 100-150 lines

---

### Phase 4: Cleanup & Refactoring (Week 8)

#### 4.1 Create BookFactory ⭐
**Priority:** MEDIUM
**Effort:** Low
**Impact:** Low-Medium

**Goal:** Eliminate book construction duplication

**Actions:**
- [ ] Create `data/parser/BookFactory.kt`
- [ ] Implement createWithDefaults() method
- [ ] Update all FileParsers to use BookFactory
- [ ] Run tests, verify book creation works

**Estimated Savings:** 80-120 lines

---

#### 4.2 Create CachedFileFactory ⭐
**Priority:** MEDIUM
**Effort:** Low
**Impact:** Low

**Goal:** Simplify CachedFile creation

**Actions:**
- [ ] Create `data/util/CachedFileFactory.kt`
- [ ] Implement fromBookEntity() method
- [ ] Update BookRepositoryImpl to use factory
- [ ] Run tests, verify file access works

**Estimated Savings:** 25-30 lines

---

#### 4.3 Create AppList Component ⭐
**Priority:** LOW-MEDIUM
**Effort:** Low
**Impact:** Low

**Goal:** Eliminate LazyColumn boilerplate

**Actions:**
- [ ] Create `presentation/core/components/AppList.kt`
- [ ] Find 5-10 LazyColumn usages
- [ ] Replace with AppList component
- [ ] Verify UI remains identical

**Estimated Savings:** 100-150 lines

---

#### 4.4 Dependency Cleanup ⭐
**Priority:** LOW
**Effort:** Low
**Impact:** Low

**Actions:**
- [ ] Remove commented SQLCipher code
- [ ] Verify Bouncy Castle usage, potentially consolidate
- [ ] Consider removing Paging Library if unused
- [ ] Standardize Material versions to stable releases
- [ ] Test APK size changes

---

## CHECKLIST

Use this checklist to track progress through the implementation:

### Phase 1: Critical Simplifications

#### 1.1 Eliminate Thin Wrapper Use Cases ⚠️ **APPROACH CHANGED**
- [x] Audit all 42 use cases, categorize by complexity
- [x] Identify thin wrappers (< 25 lines, no business logic)
- [x] **FIXED: Updated ViewModels to use injected use cases correctly instead of direct repository calls**
- [x] **FIXED: BookInfoModel - 3 fixes (getBookById, deleteProgressHistory)**
- [x] **FIXED: LibraryModel - 1 fix (deleteProgressHistory)**
- [x] **FIXED: ReaderModel - 23 fixes (all repository → use case calls)**
- [x] **FIXED: SpeedReaderModel - 3 fixes (use case calls, property access)**
- [x] **COMPLETED: Run `./gradlew assembleDebug` - verify no errors ✅**
- [x] **COMPLETED: Build successful, all compilation errors resolved**

**Status:** ✅ **COMPLETE** (Different approach than originally planned)
**Commit:** `e1f385c` - "fix: resolve phase 1 consolidation compilation errors"

**Actual Implementation:**
- Original plan was to delete thin wrapper use cases and inject repositories directly
- **Actual fix:** ViewModels now correctly use injected use cases instead of making direct repository calls
- This maintains the Clean Architecture pattern while resolving compilation errors
- All 30+ method call errors fixed across 4 ViewModels
- Build: ✅ `assembleDebug` successful

**Note:** This maintains the use case layer rather than eliminating it. Phase 1.1 is complete from a compilation standpoint. If eliminating thin wrappers is still desired, that would be a separate refactoring task.

---

#### 1.2 Create Format Detector Utility ⚠️ **PARTIALLY COMPLETE**
- [x] Create `data/parser/FormatDetector.kt`
- [x] Implement Format enum with extensions
- [x] Implement detect(fileName: String): Format function
- [x] **VALIDATED: Speed reader impact analysis completed (Section 2.5)**
- [x] **VALIDATED: Confirmed safe for speed reader (uses TextParserImpl)**
- [ ] Add unit tests for format detection
- [ ] **TODO: Update FileParserImpl to use FormatDetector**
- [ ] **TODO: Update TextParserImpl to use FormatDetector**
- [ ] Run `./gradlew test` - verify parser tests pass
- [ ] Test all file formats in app
- [ ] Test speed reader functionality

**Status:** ⚠️ **PARTIALLY COMPLETE**
**Commit:** `e1f385c` - FormatDetector.kt created and committed

**Completed:**
- ✅ FormatDetector utility created (52 lines)
- ✅ Format enum with all supported extensions
- ✅ detect() function implemented
- ✅ Speed reader validation completed (Section 2.5)

**Remaining:**
- ❌ FileParserImpl still uses manual format detection (lines 39-81)
- ❌ TextParserImpl still uses manual format detection (lines 42-82)
- Integration into parser factories needed to eliminate duplication

**Speed Reader Validation:** ✅ **SAFE**
- Speed reader uses same `TextParserImpl.parse()` as normal reader
- Consolidation will NOT impact speed reader functionality or performance
- See Section 2.5 for detailed impact analysis

---

#### 1.3 Create BaseFileParser
- [x] **VALIDATED: Speed reader impact analysis completed (Section 2.5)**
- [x] **VALIDATED: Confirmed safe for speed reader (FileParser unused by speed reader)**
- [ ] Create `data/parser/BaseFileParser.kt`
- [ ] Implement safeParse(parserName, block) method
- [ ] Update EpubFileParser to extend BaseFileParser
- [ ] Update PdfFileParser to extend BaseFileParser
- [ ] Update TxtFileParser to extend BaseFileParser
- [ ] Update HtmlFileParser to extend BaseFileParser
- [ ] Update Fb2FileParser to extend BaseFileParser
- [ ] Update FodtFileParser to extend BaseFileParser
- [ ] Update ComicFileParser to extend BaseFileParser
- [ ] Run `./gradlew test` - verify parser tests pass
- [ ] Test error handling with corrupt files

**Status:** ❌ **NOT STARTED**
**Priority:** HIGH

---

#### 1.4 Create BaseTextParser **NEW RECOMMENDATION**
- [x] **VALIDATED: Speed reader impact analysis completed (Section 2.5)**
- [x] **VALIDATED: Confirmed benefits for speed reader (uses TextParserImpl)**
- [ ] Create `data/parser/BaseTextParser.kt`
- [ ] Implement safeParse(block) method
- [ ] Update EpubTextParser to extend BaseTextParser
- [ ] Update PdfTextParser to extend BaseTextParser
- [ ] Update TxtTextParser to extend BaseTextParser
- [ ] Update HtmlTextParser to extend BaseTextParser
- [ ] Update FodtTextParser to extend BaseTextParser
- [ ] Update XmlTextParser to extend BaseTextParser
- [ ] Run `./gradlew test` - verify parser tests pass
- [ ] Test error handling with corrupt text files
- [ ] Test speed reader with various formats

**Status:** ❌ **NOT STARTED**
**Priority:** HIGH

**Speed Reader Validation:** ✅ **BENEFITS SPEED READER**
- Speed reader uses TextParserImpl directly
- BaseTextParser provides same error handling consistency
- See Section 2.5 for detailed impact analysis

### Phase 2: Settings Consolidation

#### 2.1 Consolidate Reader Settings Sub-Features
- [ ] Plan detailed consolidation mapping for 15 sub-features
- [ ] Create new directory structure (6 consolidated)
- [ ] Merge font + padding + text + reading_mode → display
- [ ] Merge chapters + progress → navigation
- [ ] Merge reading_speed + speed_reading → content
- [ ] Merge search + dictionary + translator → search
- [ ] Merge images → appearance
- [ ] Merge misc + system + comic → advanced
- [ ] Update SettingsModel navigation
- [ ] Update SettingsContent navigation
- [ ] Remove old 15 subdirectories
- [ ] Run `./gradlew assembleDebug` - verify no errors
- [ ] Test all reader settings in app
- [ ] Verify navigation works

#### 2.2 Create Settings Option DSL
- [ ] Create `SettingsOptionDsl.kt` with SettingOption()
- [ ] Implement SliderOption() inline component
- [ ] Implement SwitchOption() inline component
- [ ] Implement ChipsOption() inline component
- [ ] Convert FontFamilyOption to DSL
- [ ] Convert FontSizeOption to DSL
- [ ] Convert 3 more representative options to DSL
- [ ] Verify UI remains identical
- [ ] Convert all 105 option files to DSL
- [ ] Delete all old option files
- [ ] Run `./gradlew assembleDebug` - verify no errors
- [ ] Test all settings in app

### Phase 3: Repository & Data Layer

#### 3.1 Create Base Repository
- [ ] Create `data/repository/BaseRepository.kt`
- [ ] Implement getAllAsDomain(), getByIdAsDomain(), etc.
- [ ] Update BookmarkRepositoryImpl to extend BaseRepository
- [ ] Update HistoryRepositoryImpl to extend BaseRepository
- [ ] Update ColorPresetRepositoryImpl to extend BaseRepository
- [ ] Remove duplicated CRUD methods
- [ ] Run `./gradlew test` - verify repository tests pass
- [ ] Test data operations in app

#### 3.2 Consolidate CRUD Use Cases
- [ ] Create `BookOperations.kt` use case
- [ ] Create `BookmarkOperations.kt` use case
- [ ] Create `HistoryOperations.kt` use case
- [ ] Update LibraryModel to use BookOperations
- [ ] Update appropriate ViewModels
- [ ] Remove old CRUD use cases
- [ ] Run `./gradlew test` - verify use case tests pass
- [ ] Test all operations in app

#### 3.3 Simplify ColorPreset State Management
- [ ] Create `ColorPresetManager.kt`
- [ ] Implement withExclusiveOperation() method
- [ ] Move 8 Job variables to ColorPresetManager
- [ ] Move color preset event handlers to ColorPresetManager
- [ ] Update SettingsModel to use ColorPresetManager
- [ ] Run `./gradlew assembleDebug` - verify no errors
- [ ] Test all color preset operations in app

### Phase 4: Cleanup & Refactoring

#### 4.1 Create BookFactory
- [ ] Create `data/parser/BookFactory.kt`
- [ ] Implement createWithDefaults() method
- [ ] Update all FileParsers to use BookFactory
- [ ] Run `./gradlew test` - verify parser tests pass
- [ ] Test book creation in app

#### 4.2 Create CachedFileFactory
- [ ] Create `data/util/CachedFileFactory.kt`
- [ ] Implement fromBookEntity() method
- [ ] Update BookRepositoryImpl to use factory
- [ ] Run `./gradlew test` - verify repository tests pass
- [ ] Test file access in app

#### 4.3 Create AppList Component
- [ ] Create `AppList.kt` component
- [ ] Find LazyColumn usages in presentation
- [ ] Replace 5-10 instances with AppList
- [ ] Run `./gradlew assembleDebug` - verify no errors
- [ ] Verify UI lists render correctly

#### 4.4 Dependency Cleanup
- [ ] Remove commented SQLCipher code from build.gradle.kts
- [ ] Audit Bouncy Castle usage, consider consolidation
- [ ] Verify Paging Library usage, consider removal
- [ ] Update Material versions to stable where possible
- [ ] Run `./gradlew assembleDebug` - verify build
- [ ] Compare APK size before/after

---

## TESTING STRATEGY

### Per-Phase Verification

After each phase completion:

1. **Build Verification:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```
   - ✅ Build succeeds without errors
   - ✅ No compilation warnings related to changes

2. **Unit Test Verification:**
   ```bash
   ./gradlew test
   ```
   - ✅ All existing tests pass
   - ✅ New code has test coverage

3. **Integration Test Verification:**
   - Launch app on emulator/device
   - ✅ All features work as before
   - ✅ No crashes or ANRs
   - ✅ Settings can be changed and saved
   - ✅ Books can be imported, read, managed

4. **Code Metrics Verification:**
   ```bash
   # Count lines of code
   find app/src/main/java -name "*.kt" | xargs wc -l | tail -1
   ```
   - ✅ Verify LOC reduction matches estimates
   - ✅ Confirm file count reduction

---

## SUCCESS METRICS

### Target Metrics

| Metric | Before | Target After | Status |
|--------|---------|--------------|--------|
| Total Kotlin files | 709 | ~650 | |
| Total lines of code | ~69,895 | ~65,000 | |
| Use case files | 42 | ~20 | |
| Settings subdirectories | 60+ | ~30 | |
| Settings option files | 105 | ~0 (inlined) | |
| Parser files | 21 | 21 (restructured) | |
| Repository files | 10 | 10 (with base) | |

### Quality Metrics

| Metric | Target | Status |
|--------|--------|--------|
| Build success rate | 100% | |
| Test pass rate | 100% | |
| Feature regression count | 0 | |
| Crash-free rate | 100% | |

---

## RISKS & MITIGATION

### High-Risk Changes

**1. Settings Consolidation**
- **Risk:** Breaking navigation or user preferences
- **Mitigation:** Incremental changes, thorough testing, preserve data migration

**2. Use Case Elimination**
- **Risk:** Losing business logic if misidentified
- **Mitigation:** Careful audit, keep complex use cases, test extensively

**3. Parser Refactoring**
- **Risk:** Breaking file format parsing
- **Mitigation:** Comprehensive format testing, unit tests for each parser

### Medium-Risk Changes

**4. Repository Base Class**
- **Risk:** Incorrect generic operations
- **Mitigation:** Test data operations thoroughly, keep override methods available

---

## CONCLUSION

This analysis identifies significant opportunities for code consolidation and simplification in the Codex codebase. The primary issues are:

1. **Settings over-granularity** (60+ subdirectories, 105 option files)
2. **Use case over-abstraction** (42 use cases, many are thin wrappers)
3. **Parser duplication** (format detection, error handling, book construction)
4. **Repository boilerplate** (10 repositories with identical structure)

By implementing the recommended changes in this plan, we can achieve:

- **4,000-5,000 lines** of code reduction (~6-7%)
- **Simplified architecture** with clearer dependencies
- **Improved maintainability** with reduced boilerplate
- **Better testability** with less indirection

The phased approach allows incremental progress with verification at each stage, minimizing risk while delivering measurable improvements.

---

**Document Version:** 1.0
**Last Updated:** 2026-01-28
**Status:** Ready for Implementation
