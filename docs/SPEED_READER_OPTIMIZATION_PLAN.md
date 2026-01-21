# Speed Reader Performance Optimization Plan

## Overview

This document outlines the complete refactoring strategy to optimize speed reader performance by moving from repeated word extraction to a pre-calculated, cached word list architecture.

## Background & Problem Statement

### Current Architecture Issues

**Current Flow:**
```
File → Parsed to ReaderText → Cached in LRU (100MB)
    → Speed Reader UI extracts words repeatedly (7+ times)
    → Each extraction: split strings, filter, count (O(n) operations)
```

### Performance Bottlenecks Identified

| Location | Issue | Impact | Frequency |
|----------|--------|---------|------------|
| `SpeedReaderModel.kt` | Calculates `totalWords` on every load | 50-200ms | Every open |
| `SpeedReadingScreen.kt` | Recalculates `totalWords` from text | 50-100ms | Every recompose |
| `SpeedReadingContent.kt` | Extracts `words` list in `remember()` | 100-300ms | Every load |
| `SpeedReadingContent.kt` | Calculates `totalWords` again | 50-100ms | Every load |
| `SpeedReadingScaffold.kt` | Extracts `allWords` in LaunchedEffect | 50-100ms | Every open |
| `SpeedReadingScaffold.kt` | Calculates `totalWords` on exit | 50-100ms | Every exit |
| `SpeedReadingWordPickerSheet.kt` | Extracts all words when opening | 100-300ms | Word picker open |
| `SpeedReadingScaffold.kt` | `findNearestParagraphStart()` loops through text | 10-50ms | Navigation |

**Total overhead for cached book load: ~260-1050ms** (should be <20ms)

### Root Cause

- Speed reader uses `ReaderText` structure designed for visual scrolling (AnnotatedString, images, chapters)
- Word extraction happens repeatedly in UI layer instead of once during parsing
- No pre-calculated word positions for instant navigation

---

## Proposed Architecture

### Target Architecture

```
File → TextParser.parse() → ReaderText + SpeedReaderWord[]
    ├─→ LRU Cache #1: ReaderText (100MB, for normal reader)
    └─→ LRU Cache #2: SpeedReaderWord[] (50MB, for speed reader)
        └─→ Speed Reader UI uses cached words directly (O(1) access)
```

### Data Structures

#### SpeedReaderWord.kt (NEW - Already Created)
```kotlin
@Immutable
data class SpeedReaderWord(
    val text: String,           // Plain word (no formatting needed)
    val globalIndex: Int,       // Position in entire book (0 to totalWords-1)
    val paragraphIndex: Int       // Which paragraph this word belongs to
)
```

#### ReaderText (UNCHANGED - Normal Reader)
- `ReaderText.Text(AnnotatedString line)` - Rich text with formatting
- `ReaderText.Chapter(title, nested)` - Chapter metadata
- `ReaderText.Image(ImageBitmap)` - Embedded images
- `ReaderText.Separator` - Section breaks

### Key Benefits

| Benefit | Current | Optimized | Improvement |
|---------|---------|-----------|-------------|
| First open | Parse + 7x word extraction | Parse + 1x extraction | 6x faster |
| Cached open | Cache hit + 7x extraction | Cache hit (words already there) | 50-100x faster |
| Word navigation | Count words to find position | Direct array access | Instant |
| Word picker | Extract all words | Use cached words | Instant |
| Memory usage | 100MB text cache | 100MB text + 50MB words | Acceptable overhead |

---

## Implementation Plan

### Phase 1: Repository Layer (COMPLETED ✅)

**Status:** Already implemented in checkpoint commit `5a4403a`

#### Changes Made:
1. **Created `SpeedReaderWord.kt`** - Simple data structure for speed reader
2. **Created `SpeedReaderWordExtractor.kt`** - Extracts words from ReaderText once
3. **Updated `BookRepositoryImpl`**:
   - Added `speedReaderWordCache` LruCache (50MB)
   - Added `getSpeedReaderWords(bookId)` method
   - Modified `getBookText()` to also extract and cache words
4. **Updated `BookRepository` interface** - Added `getSpeedReaderWords()` method
5. **Created `GetSpeedReaderWords.kt`** - Use case for speed reader
6. **Updated `SpeedReaderModel`** - Uses `getSpeedReaderWords` instead of `getText`
7. **Added database migration** - `totalWordCount` field (MIGRATION_18_19)

#### Files Modified:
- `data/parser/SpeedReaderWordExtractor.kt` (NEW)
- `data/repository/BookRepositoryImpl.kt` (MODIFIED)
- `data/repository/BookRepository.kt` (MODIFIED)
- `domain/reader/SpeedReaderWord.kt` (NEW)
- `domain/use_case/book/GetSpeedReaderWords.kt` (NEW)
- `ui/reader/SpeedReaderModel.kt` (PARTIALLY MODIFIED)
- Database migration 18→19

#### Remaining Work:
- Fix compilation errors in `SpeedReaderModel.kt` to fully use `getSpeedReaderWords`
- Update all UI layer files to accept `List<SpeedReaderWord>` instead of `List<ReaderText>`

---

### Phase 2: ViewModel Layer Updates

**Status:** In Progress - Has compilation errors

#### Goal:
Replace `ReaderText`-based data flow with `SpeedReaderWord`-based flow in `SpeedReaderModel`.

#### Required Changes:

##### 2.1 SpeedReaderModel.kt

**Current State:**
```kotlin
val text = mutableStateOf<List<ReaderText>>(emptyList())
val isLoading = mutableStateOf(true)
```

**Target State:**
```kotlin
val words = mutableStateOf<List<SpeedReaderWord>>(emptyList())
val totalWords = mutableIntStateOf(0)  // Cached count
```

**loadBook() Function:**
```kotlin
fun loadBook(bookId: Int, activity: Activity, onError: () -> Unit) {
    viewModelScope.launch {
        val loadedBook = getBookById.execute(bookId) ?: return@launch onError()

        if (loadedBook.isComic) {
            errorMessage.value = "Comics not supported"
            isLoading.value = false
            return@launch
        }

        // Get pre-calculated words (instant from cache)
        val loadedWords = getSpeedReaderWords.execute(bookId)

        if (loadedWords.isEmpty()) {
            errorMessage.value = "Could not load text"
            isLoading.value = false
            return@launch
        }

        // Batch all state updates
        book.value = loadedBook
        words.value = loadedWords
        totalWords.intValue = loadedWords.size
        currentWordIndex.intValue = loadedBook.speedReaderWordIndex.coerceIn(0, loadedWords.size - 1)
        currentProgress.floatValue = if (loadedWords.size > 0) {
            loadedBook.speedReaderWordIndex.toFloat() / loadedWords.size
        } else 0f
        errorMessage.value = null
        isLoading.value = false
    }
}
```

**updateProgress() Function:**
```kotlin
fun updateProgress(progress: Float, wordIndex: Int, forceSave: Boolean = false) {
    viewModelScope.launch {
        // Always update UI immediately
        currentProgress.floatValue = progress
        currentWordIndex.intValue = wordIndex

        // Throttle database saves (every 50 words or on pause)
        if (forceSave || abs(wordIndex - lastDatabaseSaveWordIndex) >= 50) {
            saveProgressToDatabase(progress, wordIndex)
            lastDatabaseSaveWordIndex = wordIndex
        }
    }
}
```

---

### Phase 3: UI Layer Refactor

**Status:** Not Started - Requires extensive updates

#### 3.1 SpeedReadingScreen.kt

**Current Parameter:**
```kotlin
SpeedReadingScaffold(
    text = text,  // List<ReaderText>
    ...
    totalWords = totalWords,  // Calculated each time
    ...
)
```

**Target Parameter:**
```kotlin
SpeedReadingScaffold(
    words = words,  // List<SpeedReaderWord>
    totalWords = totalWords,  // Direct words.size (no calculation)
    ...
)
```

**Changes Required:**
1. Change parameter `text: List<ReaderText>` to `words: List<SpeedReaderWord>`
2. Remove `totalWords` calculation - use `words.size` directly
3. Update all references to `text` → `words`

#### 3.2 SpeedReadingContent.kt

**Current State:**
```kotlin
val words = remember(text) {
    text.filterIsInstance<ReaderText.Text>()
        .flatMap { it.line.text.split("\\s+".toRegex()) }
        .filter { it.isNotBlank() }
}

val totalWords = remember(text) {
    text.filterIsInstance<ReaderText.Text>()
        .sumOf { it.line.text.split("\\s+".toRegex()).filter { w -> w.isNotBlank() }.size }
}
```

**Target State:**
```kotlin
// Words already calculated, no extraction needed
val totalWords = words.size

// Current word is direct access
val currentWordText = words.getOrNull(currentWordIndex)?.text ?: ""

// Sentence end detection on plain string
val isSentenceEnd = currentWordText.endsWith(".") ||
                       currentWordText.endsWith("!") ||
                       currentWordText.endsWith("?") ||
                       currentWordText.endsWith(":")
```

**Changes Required:**
1. Remove `words` extraction in `remember()` - use parameter directly
2. Remove `totalWords` calculation - use `words.size`
3. Update all `text[item]` references to `words[item]`
4. Remove all `filterIsInstance`, `flatMap`, `split` operations
5. Update word access pattern: `it.line.text` → `it.text`

#### 3.3 SpeedReadingScaffold.kt

**Current State - LaunchedEffect:**
```kotlin
LaunchedEffect(text, initialWordIndex) {
    val allWords = text
        .filterIsInstance<ReaderText.Text>()
        .flatMap { it.line.text.split("\\s+".toRegex()) }
        .filter { it.isNotBlank() }

    val wordIndex = initialWordIndex.coerceIn(0, allWords.size - 1)
    selectedWordIndex = wordIndex
}
```

**Target State:**
```kotlin
LaunchedEffect(words, initialWordIndex) {
    // Words already calculated, just set index
    val wordIndex = initialWordIndex.coerceIn(0, words.size - 1)
    selectedWordIndex = wordIndex
}
```

**findNearestParagraphStart() Function:**
```kotlin
// Current: O(n) - loops and counts words
private fun findNearestParagraphStart(text: List<ReaderText>, words: List<String>, targetIndex: Int): Int

// Target: O(1) - direct lookup using pre-calculated paragraphIndex
private fun findNearestParagraphStart(words: List<SpeedReaderWord>, targetIndex: Int): Int {
    if (targetIndex <= 0) return targetIndex

    val targetWord = words.getOrNull(targetIndex) ?: return targetIndex
    return words.indexOfFirst { it.paragraphIndex == targetWord.paragraphIndex && it.globalIndex <= targetIndex }
}
```

**Changes Required:**
1. Change `findNearestParagraphStart()` parameter from `text: List<ReaderText>` to `words: List<SpeedReaderWord>`
2. Update implementation to use `paragraphIndex` field
3. Remove LaunchedEffect word extraction - use `words` directly
4. Update topBar exit logic - use `words.size` instead of calculation

#### 3.4 SpeedReadingWordPickerSheet.kt

**Current State:**
```kotlin
fun SpeedReadingWordPickerSheet(
    text: List<ReaderText>,  // Extracts words every time
    currentWordIndex: Int,
    totalWords: Int,  // Calculated
    ...
) {
    val allWords: List<WordPosition> = remember(text) {
        extractWords(text)  // O(n) extraction
    }
}
```

**Target State:**
```kotlin
fun SpeedReadingWordPickerSheet(
    words: List<SpeedReaderWord>,  // Pre-calculated, passed in
    currentWordIndex: Int,
    totalWords: Int,  // words.size, no calculation
    ...
) {
    // Convert SpeedReaderWord to WordPosition for display
    val allWords: List<WordPosition> = remember(words) {
        words.map { speedReaderWord ->
            WordPosition(
                word = speedReaderWord.text,
                textIndex = speedReaderWord.paragraphIndex,  // Use paragraphIndex
                wordIndexInText = 0,  // Simplified
                globalWordIndex = speedReaderWord.globalIndex
            )
        }
    }
}
```

**Changes Required:**
1. Remove `extractWords()` private function - no longer needed
2. Change parameter `text: List<ReaderText>` to `words: List<SpeedReaderWord>`
3. Convert `words` to `WordPosition` for display (not extraction)
4. Group by `paragraphIndex` instead of `textIndex`
5. Remove all `split`, `filter`, `flatMap` operations

---

### Phase 4: Cleanup & Deletion

#### 4.1 Remove Dead Code

**Files to Clean Up:**
1. `GetTextForSpeedReader.kt` - Duplicate of `GetText`, no longer needed
2. `extractWords()` function in `SpeedReadingWordPickerSheet.kt`
3. Any remaining `totalWords` calculations in UI layer
4. Old word extraction logic that's no longer used

#### 4.2 Update Imports

**Remove from UI files:**
- `import us.blindmint.codex.domain.reader.ReaderText` (where only used for extraction)

**Add to UI files:**
- `import us.blindmint.codex.domain.reader.SpeedReaderWord`

#### 4.3 Remove Unused Variables

Clean up:
- `WordPosition.textIndex` - Keep for display but simplify usage
- `WordPosition.wordIndexInText` - Set to 0, can be removed if not used
- Temporary variables from word extraction

---

### Phase 5: Testing Strategy

#### 5.1 Unit Testing

**Test Cases:**
1. Word extraction produces correct count
2. `paragraphIndex` increments correctly
3. `globalIndex` is sequential
4. Empty text returns empty list
5. Text with chapters/images is handled

#### 5.2 Integration Testing

**Test Scenarios:**
1. Fresh book load (not cached)
2. Cached book load (second+ open)
3. Word navigation (prev/next)
4. Paragraph navigation
5. Word picker opens instantly
6. Progress save/restore
7. App restart with saved progress

#### 5.3 Performance Testing

**Metrics to Capture:**
- First load time (cold cache)
- Cached load time (warm cache)
- Word navigation latency
- Word picker open time
- Memory usage (before/after)

**Target Performance:**
- Cached book load: <20ms (currently ~260-1050ms)
- Fresh book load: <300ms (currently ~500-1200ms)
- Word navigation: <5ms (currently ~10-50ms)

#### 5.4 Edge Cases

**Test Cases:**
1. Empty books (0 words)
2. Very large books (500,000+ words)
3. Books with special characters
4. Books with only images/chapters
5. Database migration (v18→v19)
6. Memory pressure (10+ books in cache)

---

## Implementation Checklist

### Phase 1: Repository ✅
- [x] Create `SpeedReaderWord.kt` data class
- [x] Create `SpeedReaderWordExtractor.kt`
- [x] Add `speedReaderWordCache` to `BookRepositoryImpl`
- [x] Implement `getSpeedReaderWords()` method
- [x] Update `BookRepository` interface
- [x] Create `GetSpeedReaderWords.kt` use case
- [x] Update `getBookText()` to also cache words
- [x] Add MIGRATION_18_19 for `totalWordCount`
- [ ] Fix `SpeedReaderModel.kt` compilation errors
- [ ] Test word extraction and caching

### Phase 2: ViewModel 🚧
- [ ] Update `SpeedReaderModel` state (`words` instead of `text`)
- [ ] Update `loadBook()` to use `getSpeedReaderWords()`
- [ ] Update `updateProgress()` to work with words
- [ ] Remove `totalWords` calculations
- [ ] Test progress save/restore

### Phase 3: UI Layer 📋
- [ ] Update `SpeedReadingScreen.kt` parameter
- [ ] Update `SpeedReadingContent.kt`
  - [ ] Remove word extraction in `remember()`
  - [ ] Remove `totalWords` calculation
  - [ ] Update all text references to words
  - [ ] Update word access patterns
- [ ] Update `SpeedReadingScaffold.kt`
  - [ ] Update `findNearestParagraphStart()` signature
  - [ ] Update `findNearestParagraphStart()` implementation
  - [ ] Remove LaunchedEffect extraction
  - [ ] Update exit logic
- [ ] Update `SpeedReadingWordPickerSheet.kt`
  - [ ] Remove `extractWords()` function
  - [ ] Update parameter signature
  - [ ] Convert words to WordPosition
  - [ ] Update grouping logic

### Phase 4: Cleanup 📋
- [ ] Delete `GetTextForSpeedReader.kt`
- [ ] Remove dead word extraction code
- [ ] Clean up imports
- [ ] Remove unused variables

### Phase 5: Testing 📋
- [ ] Unit tests for `SpeedReaderWordExtractor`
- [ ] Unit tests for repository caching
- [ ] Integration tests for ViewModel
- [ ] Performance profiling
- [ ] Edge case testing
- [ ] Memory profiling

---

## Risk Mitigation

### Risks Identified

| Risk | Severity | Mitigation |
|-------|----------|------------|
| Large refactor introduces bugs | High | Incremental implementation, thorough testing, keep checkpoint available |
| Performance regression | Medium | Performance benchmarks before/after, can revert to checkpoint |
| Memory overhead from dual cache | Low | Monitor memory usage, adjust cache sizes if needed |
| Breaking changes to speed reader features | Medium | Test all features: navigation, word picker, settings, progress |
| Database migration issues | Low | Test migration on development database, can rollback to v18 |

### Rollback Plan

1. **Git checkpoint exists** - Commit `5a4403a` is a safe restore point
2. **Feature flags** - Can add flag to toggle between old/new implementation during testing
3. **Incremental rollout** - Can deploy to subset of users first (if needed)

---

## Future Enhancements

After core optimization is complete, consider:

1. **Pagination for Word Picker** - Show 100 words at a time with lazy loading
2. **Prefetch adjacent words** - Load words around current position for faster navigation
3. **Paragraph jump feature** - Use `paragraphIndex` for "jump to paragraph"
4. **Word count display** - Show total word count in book info (using cached value)
5. **Search optimization** - Use pre-calculated word list for instant search

---

## Notes

### Design Decisions

1. **Why separate cache?** - Different access patterns, prevents memory bloat
2. **Why LruCache sizes?** - 100MB for text (3-5 books), 50MB for words (10-15 books)
3. **Why not modify TextParser?** - Keep normal reader unchanged, separation of concerns
4. **Why SpeedReaderWord immutable?** - Compose optimization, prevents accidental mutation

### Key Files

| File | Purpose | Changes |
|------|---------|----------|
| `SpeedReaderWord.kt` | Data structure | NEW |
| `SpeedReaderWordExtractor.kt` | Word extraction | NEW |
| `BookRepositoryImpl.kt` | Caching layer | MODIFIED |
| `BookRepository.kt` | Interface | MODIFIED |
| `GetSpeedReaderWords.kt` | Use case | NEW |
| `SpeedReaderModel.kt` | ViewModel | MODIFIED |
| `SpeedReadingScreen.kt` | UI entry point | MODIFIED |
| `SpeedReadingContent.kt` | Main reader component | MODIFIED |
| `SpeedReadingScaffold.kt` | Scaffold wrapper | MODIFIED |
| `SpeedReadingWordPickerSheet.kt` | Word picker | MODIFIED |
| `BookDatabase.kt` | Database schema | MODIFIED |

---

**Document Version:** 1.0
**Last Updated:** 2026-01-21
**Status:** Phase 1 Complete (checkpoint commit 5a4403a), Phase 2-5 Pending
