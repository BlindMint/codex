# Local Folder Import Process Analysis Report

**Project**: Codex eBook Reader
**Version**: 2.2.2
**Analysis Date**: January 25, 2026
**Purpose**: Comprehensive analysis of local folder import functionality with recommendations for improvement

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Implementation Overview](#current-implementation-overview)
3. [Architecture Deep Dive](#architecture-deep-dive)
4. [Issues and Improvement Opportunities](#issues-and-improvement-opportunities)
5. [Recommended Solutions](#recommended-solutions)
6. [Material 3 Progress Indicator Guidelines](#material-3-progress-indicator-guidelines)
7. [Implementation Roadmap](#implementation-roadmap)
8. [Alternative Approaches](#alternative-approaches)
9. [File Reference Index](#file-reference-index)
10. [Code Snippets and Patterns](#code-snippets-and-patterns)

---

## Executive Summary

### Current State
The Local Folder Import feature allows users to add eBooks and comics from device folders to the Codex library. The system uses Android's Storage Access Framework (SAF) for file access, supports 9 file formats, and provides basic progress indication during import.

### Key Findings
- **Strengths**: Clean architecture, proper use of coroutines, good separation of concerns
- **Critical Issues**: No true queue management, sequential file processing, inefficient two-phase scanning
- **UX Gaps**: Minimal progress feedback, no error details, progress bars disappear too quickly
- **Material 3 Compliance**: Progress indicators lack expressive features

### Recommended Priority
1. **High**: Implement queue management, parallel processing, proper cancellation
2. **Medium**: Enhanced progress UI, error reporting, import summaries
3. **Low**: Import history, incremental refresh, library indicators

### Expected Impact
- **Performance**: 3-5x faster imports with parallel processing
- **UX**: Clearer progress indication, better error handling
- **Stability**: Controlled concurrency prevents system overload

---

## Current Implementation Overview

### 1. Import Trigger and User Flow

**Primary Location**: `app/src/main/java/us/blindmint/codex/presentation/settings/browse/scan/components/BrowseScanOption.kt`

**User Journey**:
```
Settings → Browse → "Local Folders" Section
    ↓
Tap "Add Folder"
    ↓
Show Informational Dialog (first time only)
    ↓
OpenDocumentTree() Activity Result
    ↓
User selects folder via SAF
    ↓
Grant persistable URI permission
    ↓
Auto-start import immediately
    ↓
Folder appears in list with progress bar
```

**Key Code Flow**:

```kotlin
// BrowseScanOption.kt:114-141
val persistedUriIntent = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    settingsModel.onEvent(
        SettingsEvent.OnGrantPersistableUriPermission(uri = uri)
    )

    coroutineScope.launch {
        persistedUriPermissions = getPersistedUriPermissions()
    }

    // Start background import using ViewModel
    val permissionFile = DocumentFileCompat.fromUri(context, uri)
    val folderName = permissionFile?.getBasePath(context) ?: "Folder"
    val folderPath = permissionFile?.getRootPath(context) ?: uri.toString()

    importProgressViewModel.startImport(
        folderUri = uri,
        folderName = folderName,
        folderPath = folderPath,
        onComplete = {
            LibraryScreen.refreshListChannel.trySend(0) // Refresh Library
        }
    )
}
```

**Supported File Extensions**:
```kotlin
// SupportedExtensionConstants.kt:9-23
fun provideExtensions() = listOf(
    ".epub", ".pdf", ".fb2", ".txt", ".html", ".htm",
    ".md", ".cbr", ".cbz", ".cb7", ".opf", ".xml"
)
```

**Architecture Characteristics**:
- **SAF Integration**: Uses `OpenDocumentTree` contract for folder selection
- **Permission Management**: Grants persistable URI permissions for app lifecycle
- **Auto-Import**: Import starts immediately after folder selection (no manual trigger)
- **Codex Root Filtering**: Automatically filters out Codex Directory to prevent duplicates
- **First-Time Dialog**: Shows informational dialog explaining Local Folders concept

---

### 2. File Scanning and Processing

**Primary Location**: `app/src/main/java/us/blindmint/codex/domain/use_case/book/BulkImportBooksFromFolder.kt`

**Two-Phase Architecture**:

#### Phase 1: Complete Directory Scan
```kotlin
// BulkImportBooksFromFolder.kt:42
val allFiles = getAllFilesFromFolder(folderUri)
Log.i(BULK_IMPORT, "Found ${allFiles.size} total files in folder")
```

**Implementation** (`FileSystemRepositoryImpl.kt:159-192`):
```kotlin
override suspend fun getAllFilesFromFolder(folderUri: Uri): List<CachedFile> {
    val folder = CachedFileCompat.fromUri(application, folderUri, builder = ...)
    if (!folder.isDirectory) return emptyList()

    val files = mutableListOf<CachedFile>()
    folder.walk { file ->
        if (!file.isDirectory) {
            files.add(file)
        }
    }
    return files
}
```

#### Phase 2: Sequential File Processing
```kotlin
// BulkImportBooksFromFolder.kt:46-56
val supportedFiles = allFiles.filter { cachedFile ->
    val isSupported = supportedExtensions.any { ext ->
        cachedFile.name.endsWith(ext, ignoreCase = true)
    }
    val alreadyExists = existingPaths.any { existingPath ->
        existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
    }
    val canAccess = cachedFile.canAccess()
    isSupported && !alreadyExists && canAccess
}

// BulkImportBooksFromFolder.kt:60-82
supportedFiles.forEachIndexed { index, cachedFile ->
    try {
        val nullableBook = fileSystemRepository.getBookFromFile(cachedFile)
        when (nullableBook) {
            is NullableBook.NotNull -> {
                insertBook.execute(nullableBook.bookWithCover!!)
                importedCount++
                onProgress(BulkImportProgress(importedCount, supportedFiles.size, cachedFile.name))
            }
            is NullableBook.Null -> {
                Log.w(BULK_IMPORT, "Failed to parse: ${cachedFile.name}")
                onProgress(BulkImportProgress(index + 1, supportedFiles.size, cachedFile.name))
            }
        }
    } catch (e: Exception) {
        Log.e(BULK_IMPORT, "Error importing ${cachedFile.name}: ${e.message}")
        onProgress(BulkImportProgress(index + 1, supportedFiles.size, cachedFile.name))
    }
}
```

**Filtering Logic Summary**:
1. **Extension Check**: Must match supported formats (9 eBook formats + 2 comic formats)
2. **Duplicate Check**: File path must not exist in database
3. **Access Check**: `cachedFile.canAccess()` ensures file can be read
4. **Directory Check**: Skips folders (only processes files)

**File Parsing**:

**Parser Router** (`FileParserImpl.kt:33-82`):
```kotlin
override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
    if (!cachedFile.canAccess()) return null

    val fileFormat = ".${cachedFile.name.substringAfterLast(".")}".lowercase().trim()
    return when (fileFormat) {
        ".pdf" -> pdfFileParser.parse(cachedFile)
        ".epub" -> epubFileParser.parse(cachedFile)
        ".txt" -> txtFileParser.parse(cachedFile)
        ".fb2" -> fb2FileParser.parse(cachedFile)
        ".html", ".htm" -> htmlFileParser.parse(cachedFile)
        ".md" -> txtFileParser.parse(cachedFile)  // Markdown as TXT
        ".fodt" -> fodtFileParser.parse(cachedFile)
        ".cbr", ".cbz", ".cb7" -> comicFileParser.parse(cachedFile)
        else -> null
    }
}
```

**Format-Specific Parsers**:
- **PDF**: `PdfFileParser.kt` (uses pdfbox-android)
- **EPUB**: `EpubFileParser.kt` (uses jsoup, xml parsing)
- **TXT/MD**: `TxtFileParser.kt` (simple text extraction)
- **FB2**: `Fb2FileParser.kt` (kotlinx-serialization)
- **HTML**: `HtmlFileParser.kt` (jsoup)
- **FODT**: `FodtFileParser.kt` (OpenDocument format)
- **Comics**: `ComicFileParser.kt` (junrar, commons-compress)

**Database Insertion** (`InsertBook.kt:17-19`):
```kotlin
suspend fun execute(bookWithCover: BookWithCover) {
    repository.insertBook(bookWithCover)
}
```

---

### 3. Queueing and State Management

**Primary Location**: `app/src/main/java/us/blindmint/codex/ui/import_progress/ImportProgressService.kt`

**Architecture Pattern**: Singleton Service with StateFlow

```kotlin
@Singleton
class ImportProgressService @Inject constructor(
    private val bulkImportBooksFromFolder: BulkImportBooksFromFolder
) {
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val _importOperations = MutableStateFlow<List<ImportOperation>>(emptyList())
    val importOperations: StateFlow<List<ImportOperation>> = _importOperations.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()
}
```

**Import Operation Data Model**:

```kotlin
// ImportOperation.kt:12-21
data class ImportOperation(
    val id: String,                           // Unique UUID
    val folderName: String,                     // Display name
    val folderPath: String,                     // Full path
    val totalBooks: Int,                        // Total files to process
    val currentProgress: Int,                    // Files processed
    val status: ImportStatus,                   // Current state
    val errorMessage: String? = null,           // Error details (if failed)
    val currentFile: String = ""                // Currently processing file
)
```

**Import Status Enum**:
```kotlin
// ImportStatus.kt:12-19
enum class ImportStatus {
    STARTING,      // Initial state
    SCANNING,      // During directory scan
    IN_PROGRESS,   // Processing files
    COMPLETED,     // Successfully finished
    FAILED,         // Encountered error
    CANCELLED      // User cancelled
}
```

**Import Lifecycle**:

```kotlin
// ImportProgressService.kt:51-124
fun startImport(folderUri: Uri, folderName: String, folderPath: String, onComplete: (suspend () -> Unit)? = null) {
    coroutineScope.launch {
        val operationId = UUID.randomUUID().toString()
        val operation = ImportOperation(
            id = operationId,
            folderName = folderName,
            folderPath = folderPath,
            totalBooks = 0,
            currentProgress = 0,
            status = ImportStatus.STARTING,
            currentFile = ""
        )

        // Add to operations list
        _importOperations.value = _importOperations.value + operation
        _isImporting.value = true

        try {
            // Execute import with progress callbacks
            bulkImportBooksFromFolder.execute(
                folderUri = folderUri,
                onProgress = { progress ->
                    _importOperations.value = _importOperations.value.map { op ->
                        if (op.id == operationId) {
                            op.copy(
                                status = ImportStatus.IN_PROGRESS,
                                totalBooks = progress.total,
                                currentProgress = progress.current,
                                currentFile = progress.currentFile
                            )
                        } else op
                    }
                }
            )

            // Mark as completed
            _importOperations.value = _importOperations.value.map { op ->
                if (op.id == operationId) {
                    op.copy(status = ImportStatus.COMPLETED)
                } else op
            }

            // Call completion callback
            onComplete?.invoke()

            // Auto-clear after 2 seconds
            delay(2000)
            clearOperation(operationId)
        } catch (e: Exception) {
            // Mark as failed
            _importOperations.value = _importOperations.value.map { op ->
                if (op.id == operationId) {
                    op.copy(
                        status = ImportStatus.FAILED,
                        errorMessage = e.message ?: "Unknown error"
                    )
                } else op
            }

            // Auto-clear after 5 seconds
            delay(5000)
            clearOperation(operationId)
        } finally {
            _isImporting.value = false
        }
    }
}
```

**Key Behaviors**:
- **No Queue Limit**: Unlimited concurrent imports (unbounded)
- **Immediate Execution**: `startImport()` launches coroutine immediately (no queuing)
- **Auto-Clear**: 2s after success, 5s after failure
- **Cancellation Support**: `cancelImport()` sets status to CANCELLED
- **State Persistence**: Operations persist in StateFlow for UI observation

---

### 4. Progress Indication UI

**Primary Locations**:
- `BrowseScanFolderItem.kt` (inline progress under folder)
- `ImportProgressBar.kt` (reusable component)

**Current UI Structure**:

```kotlin
// BrowseScanOption.kt:267-371
@Composable
private fun BrowseScanFolderItem(
    index: Int,
    permission: UriPermission,
    context: Context,
    onRefreshClick: () -> Unit,
    onRemoveClick: () -> Unit,
    importOperations: List<ImportOperation>,
    folderUri: Uri
) {
    // Match current operation by folder path
    val currentOperation = importOperations.find { op ->
        op.folderPath == permissionFile.getRootPath(context)
    }

    Column {
        // Folder row with icon, name, path, buttons
        Row { ...folder info... }

        // Progress section (only if active import)
        if (currentOperation != null && currentOperation.totalBooks > 0) {
            StyledText(
                text = "Importing: ${currentOperation.currentFile}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            LinearProgressIndicator(
                progress = {
                    currentOperation.currentProgress.toFloat() /
                    currentOperation.totalBooks.toFloat()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

**Reusable Progress Component** (`ImportProgressBar.kt`):

```kotlin
@Composable
fun ImportProgressBar(
    operation: ImportOperation,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = if (operation.totalBooks > 0) {
        operation.currentProgress.toFloat() / operation.totalBooks.toFloat()
    } else 0f

    Column {
        // Header with folder name and count
        Row {
            Column {
                Text("Importing: ${operation.folderName}")
                Text("${operation.currentProgress}/${operation.totalBooks}")
            }

            // Cancel button (only during IN_PROGRESS)
            if (operation.status == ImportStatus.IN_PROGRESS) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "Cancel import")
                }
            }
        }

        // Current file name (truncated to 40 chars)
        if (operation.currentFile.isNotEmpty()) {
            val displayName = operation.currentFile.let { name ->
                val prefix = "Processing: "
                val maxLength = 40
                if (name.length > maxLength) {
                    prefix + name.take(maxLength - 3) + "..."
                } else {
                    prefix + name
                }
            }
            Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Progress bar
        LinearProgressIndicator(progress = { progress })

        // Status message
        when (operation.status) {
            ImportStatus.STARTING -> Text("Preparing import...")
            ImportStatus.SCANNING -> Text("Scanning folder...")
            ImportStatus.COMPLETED -> Text("Import completed successfully", color = primary)
            ImportStatus.FAILED -> Text("Import failed: ${operation.errorMessage}", color = error)
            ImportStatus.CANCELLED -> Text("Import cancelled")
        }
    }
}
```

**Progress Display Characteristics**:
- **Basic Material 3**: Uses standard `LinearProgressIndicator`
- **Determinate Only**: Shows percentage (no indeterminate scanning indicator)
- **Inline Location**: Appears directly under folder item
- **No Animation**: Progress jumps, no smoothing
- **Text Truncation**: 40-character limit on file names
- **Conditional Visibility**: Only shows when import is active

---

### 5. Library Integration

**Refresh Mechanism**:

```kotlin
// Channel-based communication
object LibraryScreen {
    val refreshListChannel: Channel<Long> = Channel(Channel.CONFLATED)
}

// Triggered after import completes
importProgressViewModel.startImport(
    folderUri = uri,
    folderName = folderName,
    folderPath = folderPath,
    onComplete = {
        LibraryScreen.refreshListChannel.trySend(0)  // Full refresh
    }
)

// Library screen subscribes to channel
LaunchedEffect(Unit) {
    LibraryScreen.refreshListChannel.receiveAsFlow().collectLatest {
        libraryModel.loadBooks()  // Reload entire library
    }
}
```

**Book Display** (`LibraryItem.kt`):

```kotlin
@Composable
fun LibraryItem(
    book: SelectableBook,
    hasSelectedItems: Boolean,
    selectBook: (select: Boolean?) -> Unit,
    navigateToBookInfo: () -> Unit,
    navigateToReader: () -> Unit,
    navigateToSpeedReading: () -> Unit = {}
) {
    Column {
        // Cover image (or placeholder icon)
        Box {
            if (book.data.coverImage != null) {
                AsyncCoverImage(uri = book.data.coverImage)
            } else {
                Icon(Icons.Default.Image, "No cover")
            }

            // Progress badge (if enabled in settings)
            if (libraryShowProgress) {
                Row {
                    StyledText("${progress}%")
                }
            }

            // Read button (if enabled)
            if (libraryShowReadButton) {
                FilledIconButton(onClick = navigateToReader) {
                    Icon(Icons.Filled.PlayArrow, "Continue reading")
                }
            }
        }

        // Title (if position is "below")
        StyledText(book.data.title)
    }
}
```

**Integration Characteristics**:
- **Silent Refresh**: Library reloads automatically with no user feedback
- **No Import Indicators**: No visual indication of newly imported books
- **Full Reload**: Loads entire book list (not incremental)
- **Grid Layout**: Books displayed in configurable grid
- **Progress Badges**: Optional percentage overlay on cover

---

## Architecture Deep Dive

### Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│  BrowseScanOption.kt (Settings UI)                          │
│  ├── Folder Picker (SAF Activity Result)                      │
│  ├── Folder List Display                                      │
│  ├── Import Progress Bars                                     │
│  └── Refresh/Remove Actions                                  │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    VIEW MODEL LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│  ImportProgressViewModel                                      │
│  ├── Exposes: importOperations (StateFlow)                   │
│  ├── Exposes: isImporting (StateFlow)                        │
│  └── Delegates to: ImportProgressService                     │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│  ImportProgressService (Singleton)                             │
│  ├── Manages: List<ImportOperation>                          │
│  ├── State: STARTING → IN_PROGRESS → COMPLETED/FAILED          │
│  ├── Progress callbacks to UI                                 │
│  └── Auto-clears completed operations                         │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    USE CASE LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│  BulkImportBooksFromFolder                                   │
│  ├── getAllFilesFromFolder() (Phase 1)                       │
│  ├── Filter supported extensions                              │
│  ├── Filter duplicates                                       │
│  ├── Process files sequentially (Phase 2)                       │
│  └── Report progress via callback                             │
└─────────────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌─────────────────────┐         ┌─────────────────────┐
│  REPOSITORY LAYER   │         │  PARSER LAYER      │
├─────────────────────┤         ├─────────────────────┤
│ FileSystemRepo      │         │ FileParserImpl     │
│ ├── walkFiles()    │         │ ├── PdfFileParser  │
│ ├── canAccess()    │         │ ├── EpubFileParser │
│ └── getBookFile()  │         │ ├── TxtFileParser  │
└─────────────────────┘         │ ├── ComicFileParser│
                               │ └── etc...         │
                               └─────────────────────┘
                                        │
                                        ▼
                              ┌─────────────────────┐
                              │  DOMAIN LAYER      │
                              ├─────────────────────┤
                              │ BookWithCover      │
                              │ ├── Book metadata   │
                              │ └── Cover image     │
                              └─────────────────────┘
                                        │
                                        ▼
                              ┌─────────────────────┐
                              │  DATA LAYER        │
                              ├─────────────────────┤
                              │ BookDao (Room)     │
                              │ ├── insertBook()    │
                              │ └── getBooks()     │
                              └─────────────────────┘
```

### Data Flow Analysis

**Import Request Flow**:
```
User Action → BrowseScanOption
    ↓
startImport() → ImportProgressViewModel
    ↓
startImport() → ImportProgressService
    ↓
Create ImportOperation (STARTING)
    ↓
Publish to _importOperations (StateFlow)
    ↓
UI observes and shows progress
```

**Progress Update Flow**:
```
BulkImportBooksFromFolder
    ↓
onProgress(BulkImportProgress) callback
    ↓
Update ImportOperation (IN_PROGRESS)
    ↓
Publish to _importOperations (StateFlow)
    ↓
UI recomposes with new progress
```

**Library Refresh Flow**:
```
Import Completion
    ↓
onComplete callback invoked
    ↓
LibraryScreen.refreshListChannel.trySend(0)
    ↓
LibraryModel receives event
    ↓
loadBooks() from repository
    ↓
UI recomposes with new book list
```

### Coroutines and Concurrency

**Current Concurrency Model**:
```kotlin
// ImportProgressService.kt:39
private val coroutineScope = CoroutineScope(SupervisorJob())

// Each import launches in its own coroutine
fun startImport(...) {
    coroutineScope.launch {  // Independent coroutine per import
        // Process entire import
    }
}
```

**Characteristics**:
- **Unbounded Concurrency**: No limit on simultaneous imports
- **SupervisorJob**: One import failure doesn't cancel others
- **Sequential File Processing**: Files processed one-by-one within import
- **No Throttling**: Can spawn unlimited concurrent operations

**Thread Dispatchers**:
- Default: Uses `Dispatchers.Main.immediate` (ViewModel coroutines)
- File I/O: Should use `Dispatchers.IO` (not verified in current code)
- Database: Room automatically uses appropriate dispatcher

---

## Issues and Improvement Opportunities

### 🔴 Critical Issues

#### 1. No True Queue Management

**Problem**:
- `ImportProgressService` maintains a list but doesn't actually queue operations
- Multiple imports can run simultaneously without limits
- Can cause database contention, UI lag, and system overload

**Evidence**:
```kotlin
// ImportProgressService.kt:51-52
fun startImport(...) {
    coroutineScope.launch {  // Immediately launches - no queue!
        bulkImportBooksFromFolder.execute(...)
    }
}
```

**Impact**:
- User adds 5 folders → 5 concurrent imports → 5 parallel file scans
- Database operations compete (multiple concurrent `insertBook`)
- UI becomes sluggish with multiple progress updates
- Memory usage spikes with multiple `getAllFilesFromFolder` operations

**Expected Behavior with Queue**:
- Max 2-3 concurrent imports (configurable)
- Others wait in queue until slot available
- FIFO ordering maintained
- Better resource utilization

---

#### 2. Sequential File Processing

**Problem**:
- Files processed one-by-one in `forEachIndexed` loop
- No parallelization within a single import
- Large imports take unnecessarily long

**Evidence**:
```kotlin
// BulkImportBooksFromFolder.kt:60-82
supportedFiles.forEachIndexed { index, cachedFile ->
    val nullableBook = fileSystemRepository.getBookFromFile(cachedFile)
    insertBook.execute(nullableBook.bookWithCover!!)
    // Next file doesn't start until this one finishes
}
```

**Performance Impact**:
- 100 files with 500ms processing each = 50 seconds
- Same 100 files with 4 workers = ~12-15 seconds
- 3-5x speed improvement possible

**Why Sequential?**:
- Simpler implementation
- Easier error handling
- Ordered processing
- Database transaction safety

**Risk with Parallel Processing**:
- Duplicate insertion race conditions (mitigated by duplicate check)
- Database lock contention (use batch inserts)
- Progress calculation complexity (interleaved completions)

---

#### 3. Two-Phase Scanning Inefficient

**Problem**:
- Files scanned twice (once for list, once for processing)
- All file URIs held in memory before processing
- Inefficient for large directories

**Evidence**:
```kotlin
// Phase 1: Scan all files into memory
val allFiles = getAllFilesFromFolder(folderUri)  // All files in memory

// Phase 2: Process that list
supportedFiles = allFiles.filter { ... }  // Filter again
supportedFiles.forEachIndexed { ... }  // Then process
```

**Memory Impact**:
- Directory with 10,000 files = 10,000 CachedFile objects in memory
- Each CachedFile holds: Uri, path, name, size, metadata
- Estimated memory: 10,000 × ~500 bytes = 5 MB minimum
- Real world: 20-50 MB with overhead

**I/O Impact**:
- Walk directory tree: Build complete file list
- Filter list: Process all objects (even filtered)
- Process files: Iterate again
- Duplicate iterations = slower

---

#### 4. No Progress During Scanning Phase

**Problem**:
- `SCANNING` status exists but has no progress updates
- Users see no feedback during potentially long directory scans
- Can't distinguish between "stuck" and "working"

**Evidence**:
```kotlin
// ImportStatus.kt:12-19
enum class ImportStatus {
    STARTING,    // Used
    SCANNING,     // Exists but never set during actual scan
    IN_PROGRESS,  // Used
    COMPLETED,    // Used
    FAILED,       // Used
    CANCELLED     // Used
}

// BulkImportBooksFromFolder.kt:42
val allFiles = getAllFilesFromFolder(folderUri)
// No progress updates during this call!
```

**UX Impact**:
- Large directory (10,000 files): 5-10 second scan with no feedback
- User thinks: "Is it frozen? Did I do something wrong?"
- No sense of progress or estimated time

**Better Approach**:
- Report files found count during scan
- Or show indeterminate progress with text "Scanning folder..."
- Or report subdirectory count

---

#### 5. Progress Bar Not Material 3 Compliant

**Problem**:
- Basic `LinearProgressIndicator` lacks Material 3 expressive features
- Missing visual polish and accessibility enhancements

**Missing Material 3 Features**:
```kotlin
// Current (basic)
LinearProgressIndicator(
    progress = { current / total.toFloat() }
)

// Material 3 Expressive (missing)
LinearProgressIndicator(
    progress = { current / total.toFloat() },
    gap = 4.dp,                      // ❌ Track gap
    drawStopIndicator = {},            // ❌ End stop dot
    modifier = Modifier.animateContentSize()  // ❌ Smooth animations
)
```

**Visual Comparison**:

```
Current:
███████████████░░░░░░  (Basic line, no separation)

Material 3 Expressive:
███████████████ │ ░░░░░░  (Gap between active/inactive)
                 ●  (End stop indicator)
```

**Accessibility Impact**:
- No semantic labeling
- No screen reader announcements
- No content descriptions

**Animation Impact**:
- Progress jumps between values (no smoothing)
- No transition animations between states
- No completion animation

---

### 🟡 Medium Priority Issues

#### 6. No Import Completion Feedback

**Problem**:
- When import completes, library simply refreshes silently
- Users don't know import finished successfully
- No summary of what was imported

**Current Behavior**:
```kotlin
// ImportProgressService.kt:89-103
bulkImportBooksFromFolder.execute(...)

// Mark as completed
_importOperations.value = _importOperations.value.map { ...status = COMPLETED }

// Call completion callback
onComplete?.invoke()  // Just refreshes library

// Auto-clear after 2 seconds (before user can react!)
delay(2000)
clearOperation(operationId)
```

**User Confusion**:
- "Did the import finish? I don't see a message."
- "How many books were imported?"
- "Did all files import successfully?"

**Recommended Feedback**:
- Snackbar: "Imported 15 books from folder"
- Toast: "Import complete" (less intrusive)
- Summary bottom sheet: Detailed breakdown

---

#### 7. Progress Bar Disappears Too Quickly

**Problem**:
- 2-second auto-clear on success is too fast
- Users miss seeing completion status
- Can't verify import completed

**Evidence**:
```kotlin
// ImportProgressService.kt:101-103
onComplete?.invoke()
delay(2000)  // Only 2 seconds!
clearOperation(operationId)
```

**User Scenario**:
1. Import starts, progress bar shows
2. User navigates to another tab while waiting
3. User returns to Settings tab
4. Progress bar already disappeared (2 second window)
5. User wonders: "Did it finish? Should I check the library?"

**Recommended Timings**:
- Success: 5-10 seconds (allows user to see completion)
- Failure: 10 seconds (allows user to read error)
- Better: Manual dismiss button + auto-dismiss timer

---

#### 8. No Import Error Details

**Problem**:
- Failed imports show generic error message
- Users can't troubleshoot specific file issues
- Don't know which files failed or why

**Current Error Display**:
```kotlin
// ImportProgressBar.kt:135-140
ImportStatus.FAILED -> {
    Text(
        text = "Import failed: ${operation.errorMessage}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}
```

**Example Scenarios**:
- **File corrupted**: "Import failed: Failed to parse file" (which file?)
- **Access denied**: "Import failed: Could not access URI" (which file?)
- **Database error**: "Import failed: SQLite constraint" (duplicate book?)

**Better Error Reporting**:
```kotlin
data class ImportError(
    val fileName: String,
    val reason: String,
    val errorType: ErrorType  // PARSE_ERROR, ACCESS_ERROR, DUPLICATE, etc.
)

// Display list of failed files
Column {
    Text("Import completed with errors")
    errors.forEach { error ->
        Text("• ${error.fileName}: ${error.reason}")
    }
}
```

---

#### 9. No Deduplication on Re-import

**Problem**:
- Re-importing same folder shows all files as "new"
- Current duplicate check may not work reliably
- Wastes time, potential duplicate entries

**Current Logic**:
```kotlin
// BulkImportBooksFromFolder.kt:50-52
val alreadyExists = existingPaths.any { existingPath ->
    existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
}
```

**Potential Issues**:
1. **URI Comparison**: File URI might change if app is reinstalled or permissions refreshed
2. **Path Inconsistency**: `/storage/emulated/0/Books` vs `/storage/ABCD-1234/Books`
3. **Case Sensitivity**: `ignoreCase = true` helps but may not cover all cases
4. **Query Parameters**: URIs might include different query parameters

**Better Deduplication**:
```kotlin
// Check multiple attributes
val isDuplicate = existingPaths.any { existing ->
    existing.path.equals(file.path, ignoreCase = true) ||
    existing.name.equals(file.name, ignoreCase = true) && existing.size == file.size
}

// Or use content hash
val contentHash = computeFileHash(file)
val hashExists = existingPaths.any { it.hash == contentHash }
```

---

#### 10. No Cancellation During Scanning

**Problem**:
- Can cancel import but only after scan completes
- Can't abort large directory scans
- User frustration when selecting wrong folder

**Evidence**:
```kotlin
// ImportProgressService.kt:129-137
fun cancelImport(operationId: String) {
    _importOperations.value = _importOperations.value.map { op ->
        if (op.id == operationId) {
            op.copy(status = ImportStatus.CANCELLED)
        } else op
    }
    // But the scanning coroutine continues running!
}
```

**Cancellation Flow**:
```
User taps cancel
    ↓
Status set to CANCELLED
    ↓
But: getAllFilesFromFolder() still running
    ↓
But: forEachIndexed still iterating
    ↓
Finally: Process finishes, then clears operation
```

**User Impact**:
- Selects folder with 10,000 files by mistake
- Taps cancel immediately
- Still has to wait 10 seconds for scan to complete
- Cancelling doesn't stop the work

**Fix Required**:
- Pass `Job` reference to use case
- Call `job.cancel()` to interrupt coroutine
- Add cooperative cancellation checks in loops

---

### 🟢 Minor Issues

#### 11. File Name Truncation Inconsistent

**Problem**:
- 40-character limit but doesn't handle long extensions well
- Truncates in middle of important info

**Current Logic**:
```kotlin
// ImportProgressBar.kt:84-92
val displayName = operation.currentFile.let { name ->
    val prefix = "Processing: "
    val maxLength = 40
    if (name.length > maxLength) {
        prefix + name.take(maxLength - 3) + "..."
    } else {
        prefix + name
    }
}
```

**Examples**:
```
File: "The_Very_Long_Name_Of_A_Book_That_Goes_On_And_On.epub"
Display: "Processing: The_Very_Long_Name_Of_A_Book_That_Go..." (42 chars - still too long!)

File: "My_Book_With_An_Extremely_Long_File_Name_That_Keep..."
Display: "Processing: My_Book_With_An_Extremely_Long_File_..." (extension lost!)
```

**Better Truncation**:
```kotlin
// Smart truncation: keep extension
val displayName = when {
    name.length <= maxLength -> "Processing: $name"
    else -> {
        val ext = name.substringAfterLast(".")
        val nameWithoutExt = name.substringBeforeLast(".")
        val availableForName = maxLength - ext.length - 4  // 4 for "..." and "."
        "Processing: ${nameWithoutExt.take(availableForName)}... .$ext"
    }
}
```

---

#### 12. No Import History

**Problem**:
- Can't see which folders were imported previously
- Users forget what they added
- No audit trail

**Current State**:
- Local folders shown in list (persisted URI permissions)
- But no record of: when imported, how many files, results

**Use Cases**:
- "When did I last import from this folder?"
- "How many files did I import last time?"
- "Did this folder fail before?"

**Recommended Solution**:
```kotlin
data class ImportHistory(
    val folderPath: String,
    val importedAt: LocalDateTime,
    val fileCount: Int,
    val successCount: Int,
    val failedCount: Int,
    val status: ImportStatus
)

@Entity
data class ImportHistoryEntity(
    @PrimaryKey val id: String,
    val folderPath: String,
    val importedAt: Long,  // Timestamp
    val fileCount: Int,
    val successCount: Int,
    val failedCount: Int,
    val status: String
)
```

---

#### 13. Refresh Button Re-imports Everything

**Problem**:
- Tapping refresh re-scans and re-imports all files
- Unnecessary I/O and database operations
- Can be slow for large folders

**Current Behavior**:
```kotlin
// BrowseScanOption.kt:153-165
onRefreshClick = {
    importProgressViewModel.startImport(
        folderUri = permission.uri,
        folderName = folderName,
        folderPath = folderPath,
        onComplete = { ... }
    )
}
```

**User Scenario**:
1. Import folder with 100 files (30 seconds)
2. Add 1 new file to folder via file manager
3. Tap "Refresh" to get the new file
4. Result: Re-imports all 101 files (wastes time)

**Recommended Solution**:
```kotlin
enum class RefreshMode {
    FULL_REIMPORT,      // Current: re-process everything
    NEW_FILES_ONLY,     // Only files added since last import
    CHECK_CHANGES       // Check for modified files too
}

// Add mode selector in UI
DropdownMenuItem(
    text = { Text("Refresh (new files only)") },
    onClick = { refreshWithMode(RefreshMode.NEW_FILES_ONLY) }
)
```

**Implementation**:
- Store last import timestamp per folder
- Filter `file.lastModified > lastImportTime`
- Provide mode selector in refresh menu

---

## Recommended Solutions

### Solution 1: Implement True Queue Management

**Architecture**:
```kotlin
/**
 * Queue-based import manager with controlled concurrency.
 * Limits simultaneous imports and maintains FIFO ordering.
 */
class ImportQueueManager @Inject constructor(
    private val bulkImportBooksFromFolder: BulkImportBooksFromFolder
) {
    companion object {
        private const val DEFAULT_MAX_CONCURRENT = 2
        private const val TAG = "ImportQueueManager"
    }

    private val queue = Channel<ImportTask>(Channel.UNLIMITED)
    private val activeImports = AtomicInteger(0)
    private val maxConcurrent = AtomicInteger(DEFAULT_MAX_CONCURRENT)

    private val _queuedCount = MutableStateFlow(0)
    val queuedCount: StateFlow<Int> = _queuedCount.asStateFlow()

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    private val importJobs = ConcurrentHashMap<String, Job>()

    data class ImportTask(
        val id: String,
        val folderUri: Uri,
        val folderName: String,
        val folderPath: String,
        val onComplete: (suspend () -> Unit)? = null
    )

    /**
     * Enqueue an import task. Returns operation ID.
     * Task will be processed when queue slot is available.
     */
    fun enqueue(task: ImportTask): String {
        Log.i(TAG, "Enqueueing import: ${task.folderName}")

        // Calculate position in queue (before adding)
        val position = queue.trySend(task).isSuccess

        _queuedCount.value = queue.size
        processQueue()

        return task.id
    }

    /**
     * Process queue, starting imports up to maxConcurrent limit.
     */
    private fun processQueue() {
        while (activeImports.get() < maxConcurrent.get() && !queue.isEmpty) {
            val task = runBlocking { queue.receive() }
            activeImports.incrementAndGet()
            _activeCount.value = activeImports.get()
            _queuedCount.value = queue.size

            Log.i(TAG, "Starting import: ${task.folderName} (active: ${activeImports.get()}, queued: ${queue.size})")

            val job = CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                try {
                    executeImport(task)
                } finally {
                    activeImports.decrementAndGet()
                    _activeCount.value = activeImports.get()
                    importJobs.remove(task.id)
                    Log.i(TAG, "Completed import: ${task.folderName}")

                    // Process next task
                    processQueue()
                }
            }

            importJobs[task.id] = job
        }
    }

    /**
     * Execute import with progress tracking.
     */
    private suspend fun executeImport(task: ImportTask) {
        // Create operation
        val operation = ImportOperation(
            id = task.id,
            folderName = task.folderName,
            folderPath = task.folderPath,
            totalBooks = 0,
            currentProgress = 0,
            status = ImportStatus.STARTING,
            currentFile = ""
        )

        _importOperations.value = _importOperations.value + operation
        _isImporting.value = true

        try {
            // Execute import
            val result = bulkImportBooksFromFolder.execute(
                folderUri = task.folderUri,
                onProgress = { progress ->
                    updateProgress(task.id, progress, ImportStatus.IN_PROGRESS)
                }
            )

            // Mark completed
            updateStatus(task.id, ImportStatus.COMPLETED)
            task.onComplete?.invoke()

            // Auto-clear after delay
            delay(5000)  // Longer delay
            clearOperation(task.id)
        } catch (e: CancellationException) {
            Log.i(TAG, "Import cancelled: ${task.folderName}")
            updateStatus(task.id, ImportStatus.CANCELLED)
            delay(3000)
            clearOperation(task.id)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${task.folderName}", e)
            updateStatus(task.id, ImportStatus.FAILED, e.message)
            delay(10000)  // Longer delay for errors
            clearOperation(task.id)
        } finally {
            _isImporting.value = _importOperations.value.any { it.status == ImportStatus.IN_PROGRESS }
        }
    }

    /**
     * Cancel an import (active or queued).
     */
    fun cancelImport(operationId: String) {
        // Cancel active import
        importJobs[operationId]?.cancel()
        importJobs.remove(operationId)

        // Update status
        _importOperations.value = _importOperations.value.map { op ->
            if (op.id == operationId) {
                op.copy(status = ImportStatus.CANCELLED)
            } else op
        }
    }

    /**
     * Set max concurrent imports.
     */
    fun setMaxConcurrent(max: Int) {
        require(max > 0) { "Max concurrent must be > 0" }
        maxConcurrent.set(max)
        processQueue()  // Restart queue with new limit
    }

    /**
     * Clear all queued operations.
     */
    fun clearQueue() {
        while (!queue.isEmpty) {
            queue.tryReceive()
        }
        _queuedCount.value = 0
    }

    // State management methods (similar to current ImportProgressService)
    private val _importOperations = MutableStateFlow<List<ImportOperation>>(emptyList())
    val importOperations: StateFlow<List<ImportOperation>> = _importOperations.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private fun updateProgress(operationId: String, progress: BulkImportProgress, status: ImportStatus) {
        _importOperations.value = _importOperations.value.map { op ->
            if (op.id == operationId) {
                op.copy(
                    status = status,
                    totalBooks = progress.total,
                    currentProgress = progress.current,
                    currentFile = progress.currentFile
                )
            } else op
        }
    }

    private fun updateStatus(operationId: String, status: ImportStatus, errorMessage: String? = null) {
        _importOperations.value = _importOperations.value.map { op ->
            if (op.id == operationId) {
                op.copy(status = status, errorMessage = errorMessage)
            } else op
        }
    }

    private fun clearOperation(operationId: String) {
        _importOperations.value = _importOperations.value.filter { it.id != operationId }
    }
}
```

**Integration**:
```kotlin
// Modify ImportProgressViewModel
@HiltViewModel
class ImportProgressViewModel @Inject constructor(
    private val importQueueManager: ImportQueueManager
) : ViewModel() {
    val importOperations = importQueueManager.importOperations
    val isImporting = importQueueManager.isImporting
    val queuedCount = importQueueManager.queuedCount
    val activeCount = importQueueManager.activeCount

    fun startImport(folderUri: Uri, folderName: String, folderPath: String, onComplete: (suspend () -> Unit)? = null) {
        val taskId = UUID.randomUUID().toString()
        importQueueManager.enqueue(
            ImportQueueManager.ImportTask(
                id = taskId,
                folderUri = folderUri,
                folderName = folderName,
                folderPath = folderPath,
                onComplete = onComplete
            )
        )
    }

    fun cancelImport(operationId: String) = importQueueManager.cancelImport(operationId)
    fun clearQueue() = importQueueManager.clearQueue()
    fun setMaxConcurrent(max: Int) = importQueueManager.setMaxConcurrent(max)
}
```

**Benefits**:
- ✅ Controlled concurrency (configurable, default: 2)
- ✅ Prevents system overload
- ✅ FIFO ordering maintained
- ✅ Memory-efficient (not all files loaded at once)
- ✅ Better resource utilization
- ✅ Queue visibility in UI

---

### Solution 2: Parallel File Processing with Controlled Concurrency

**Modified BulkImportBooksFromFolder**:
```kotlin
class BulkImportBooksFromFolder @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val bookRepository: BookRepository,
    private val insertBook: InsertBook
) {
    companion object {
        private const val BULK_IMPORT = "BULK IMPORT"
        private const val DEFAULT_CONCURRENCY = 4
    }

    suspend fun execute(
        folderUri: Uri,
        onProgress: (BulkImportProgress) -> Unit,
        maxConcurrency: Int = DEFAULT_CONCURRENCY
    ): Int {
        Log.i(BULK_IMPORT, "Starting bulk import with concurrency: $maxConcurrency")

        val supportedExtensions = provideExtensions()
        val existingPaths = bookRepository.getBooks("").map { it.filePath }

        // Get all files (can be optimized with streaming)
        val allFiles = getAllFilesFromFolder(folderUri)
        Log.i(BULK_IMPORT, "Found ${allFiles.size} total files")

        // Filter files
        val supportedFiles = allFiles.filter { cachedFile ->
            val isSupported = supportedExtensions.any { ext ->
                cachedFile.name.endsWith(ext, ignoreCase = true)
            }
            val alreadyExists = existingPaths.any { existingPath ->
                existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
            }
            val canAccess = cachedFile.canAccess()
            isSupported && !alreadyExists && canAccess
        }

        Log.i(BULK_IMPORT, "Found ${supportedFiles.size} supported files to import")

        if (supportedFiles.isEmpty()) return 0

        // Parallel processing with semaphore
        val semaphore = Semaphore(maxConcurrency)
        val progressMutex = Mutex()
        val importedCount = AtomicInteger(0)
        val errors = CopyOnWriteArrayList<Pair<String, String>>()  // (fileName, error)

        supportedFiles.mapIndexed { index, cachedFile ->
            async(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    val nullableBook = fileSystemRepository.getBookFromFile(cachedFile)
                    when (nullableBook) {
                        is NullableBook.NotNull -> {
                            insertBook.execute(nullableBook.bookWithCover!!)
                            val current = importedCount.incrementAndGet()

                            // Thread-safe progress update
                            progressMutex.withLock {
                                onProgress(BulkImportProgress(current, supportedFiles.size, cachedFile.name))
                            }
                            Log.i(BULK_IMPORT, "Imported: ${cachedFile.name} ($current/${supportedFiles.size})")
                        }
                        is NullableBook.Null -> {
                            errors.add(cachedFile.name to nullableBook.message.toString())
                            Log.w(BULK_IMPORT, "Failed to parse: ${cachedFile.name} - ${nullableBook.message}")
                        }
                    }
                } catch (e: CancellationException) {
                    Log.i(BULK_IMPORT, "Import cancelled during processing: ${cachedFile.name}")
                    throw e
                } catch (e: Exception) {
                    errors.add(cachedFile.name to e.message ?: "Unknown error")
                    Log.e(BULK_IMPORT, "Error importing ${cachedFile.name}: ${e.message}")
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()

        Log.i(BULK_IMPORT, "Bulk import completed. Imported $importedCount books.")
        Log.i(BULK_IMPORT, "Errors: ${errors.size}")

        return importedCount.get()
    }

    private suspend fun getAllFilesFromFolder(folderUri: Uri): List<CachedFile> {
        return fileSystemRepository.getAllFilesFromFolder(folderUri)
    }
}
```

**Key Improvements**:
- ✅ Configurable concurrency (default: 4)
- ✅ Semaphore limits simultaneous file processing
- ✅ Thread-safe progress updates with Mutex
- ✅ Proper exception handling and cancellation
- ✅ Error collection for reporting
- ✅ Uses Dispatchers.IO for file operations

**Performance Impact**:
```
Sequential (current):
100 files × 500ms = 50 seconds

Parallel (4 workers):
100 files × 500ms / 4 = 12.5 seconds
(75% faster)
```

**Considerations**:
- **Database Contention**: Consider batching inserts
- **Memory**: Still loads all files first (see Solution 3)
- **Progress Updates**: Out-of-order (interleaved completions)
- **Ordering**: Files complete in random order

---

### Solution 3: Stream-Based File Processing

**Replace two-phase approach with streaming**:
```kotlin
/**
 * Import files using streaming approach.
 * Processes files as they're found, reducing memory footprint.
 */
suspend fun executeStreaming(
    folderUri: Uri,
    onProgress: (BulkImportProgress) -> Unit,
    maxConcurrency: Int = 4
): Int {
    Log.i(BULK_IMPORT, "Starting streaming import with concurrency: $maxConcurrency")

    val supportedExtensions = provideExtensions()
    val existingPaths = bookRepository.getBooks("").map { it.filePath }

    val semaphore = Semaphore(maxConcurrency)
    val progressMutex = Mutex()
    val importedCount = AtomicInteger(0)
    val totalCount = AtomicInteger(0)

    // Process files as we find them
    folderUri.walkStreaming { cachedFile ->
        totalCount.incrementAndGet()

        // Filter in stream
        val shouldProcess = supportedExtensions.any { ext ->
            cachedFile.name.endsWith(ext, ignoreCase = true)
        } && existingPaths.none { existingPath ->
            existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
        } && cachedFile.canAccess()

        if (shouldProcess) {
            async(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    val nullableBook = fileSystemRepository.getBookFromFile(cachedFile)
                    when (nullableBook) {
                        is NullableBook.NotNull -> {
                            insertBook.execute(nullableBook.bookWithCover!!)
                            val current = importedCount.incrementAndGet()

                            progressMutex.withLock {
                                onProgress(BulkImportProgress(current, totalCount.get(), cachedFile.name))
                            }
                        }
                        is NullableBook.Null -> {
                            // Still report progress
                            progressMutex.withLock {
                                onProgress(BulkImportProgress(importedCount.get(), totalCount.get(), cachedFile.name))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(BULK_IMPORT, "Error: ${e.message}")
                    progressMutex.withLock {
                        onProgress(BulkImportProgress(importedCount.get(), totalCount.get(), cachedFile.name))
                    }
                } finally {
                    semaphore.release()
                }
            }
        }
    }

    // Wait for all tasks to complete
    delay(100)  // Small delay to ensure all async tasks started

    return importedCount.get()
}

// Extension function for streaming walk
suspend fun Uri.walkStreaming(
    onFile: suspend (CachedFile) -> Unit
) {
    val folder = CachedFileCompat.fromUri(application, this) ?: return

    suspend fun walkRecursive(directory: CachedFile) {
        ensureActive()  // Check for cancellation

        directory.listFiles { file ->
            if (file.isDirectory) {
                walkRecursive(file)
            } else {
                onFile(file)
            }
        }
    }

    walkRecursive(folder)
}
```

**Benefits**:
- ✅ Lower memory footprint (no full list in memory)
- ✅ Immediate progress updates (as files are found)
- ✅ Better for huge directories (10,000+ files)
- ✅ Can process while scanning
- ✅ Better cancellation support

**Memory Comparison**:
```
Two-phase (current):
10,000 files × ~500 bytes = 5 MB minimum (typically 20-50 MB)

Streaming:
Active processing files (4-8) × ~500 bytes = 2-4 KB
(99.9% less memory)
```

---

### Solution 4: Implement Proper Material 3 Progress Indicators

**Enhanced Progress Component**:
```kotlin
@Composable
fun Material3ImportProgressBar(
    operation: ImportOperation,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with folder name and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = operation.folderName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Progress count
                    Text(
                        text = "${operation.currentProgress}/${operation.totalBooks} files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Actions row
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Expand/collapse details
                    IconButton(onClick = { showDetails = !showDetails }) {
                        Icon(
                            imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showDetails) "Show less" else "Show more"
                        )
                    }

                    // Cancel button
                    if (operation.status == ImportStatus.IN_PROGRESS) {
                        IconButton(
                            onClick = onCancel,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Close, "Cancel import")
                        }
                    }
                }
            }

            // Current file with smart truncation
            if (operation.currentFile.isNotEmpty()) {
                Text(
                    text = truncateFileName(operation.currentFile, maxLength = 45),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }

            // Progress bar with Material 3 features
            when (operation.status) {
                ImportStatus.STARTING, ImportStatus.SCANNING -> {
                    // Indeterminate progress for scanning
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = when (operation.status) {
                            ImportStatus.STARTING -> "Preparing import..."
                            ImportStatus.SCANNING -> "Scanning folder..."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ImportStatus.IN_PROGRESS -> {
                    val progress = if (operation.totalBooks > 0) {
                        operation.currentProgress.toFloat() / operation.totalBooks.toFloat()
                    } else 0f

                    // Material 3 expressive linear progress
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .semantics {
                                this.progress = progress
                                contentDescription = "Importing ${operation.currentProgress} of ${operation.totalBooks} files"
                            },
                        gap = 4.dp,  // Track gap
                        drawStopIndicator = {},  // End stop dot
                        color = when (progress) {
                            in 0.0f..0.25f -> MaterialTheme.colorScheme.primary
                            in 0.25f..0.5f -> MaterialTheme.colorScheme.secondary
                            in 0.5f..0.75f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    // Percentage badge
                    Surface(
                        modifier = Modifier.align(Alignment.End),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                ImportStatus.COMPLETED -> {
                    // Success state with checkmark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import completed successfully!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Full progress bar to show completion
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                ImportStatus.FAILED -> {
                    // Error state with details
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = operation.errorMessage ?: "Import failed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Red progress bar
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.errorContainer
                    )

                    // Retry button
                    FilledTonalButton(
                        onClick = { /* Retry logic */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Import")
                    }
                }

                ImportStatus.CANCELLED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import cancelled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expandable details section
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp, start = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Folder Path:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = operation.folderPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Import ID:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = operation.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    )

                    if (operation.errorMessage != null) {
                        Text(
                            text = "Error Details:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = operation.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smart file name truncation that keeps extension.
 */
private fun truncateFileName(fileName: String, maxLength: Int): String {
    if (fileName.length <= maxLength) return fileName

    val ext = fileName.substringAfterLast(".")
    val nameWithoutExt = fileName.substringBeforeLast(".")
    val availableForName = maxLength - ext.length - 4  // 4 for "... ." and ext

    return if (availableForName > 5) {
        "${nameWithoutExt.take(availableForName)}... .$ext"
    } else {
        "${fileName.take(maxLength - 3)}..."
    }
}
```

**Features**:
- ✅ Material 3 expressive components (track gap, end stop)
- ✅ Indeterminate progress for scanning
- ✅ Color-coded states (primary for active, error for failed)
- ✅ Animated state transitions
- ✅ Expandable details
- ✅ Semantic labeling for accessibility
- ✅ Smart filename truncation (keeps extension)
- ✅ Percentage badge
- ✅ Retry button for failed imports

---

### Solution 5: Enhanced Progress UI with States

**Import Progress Component with States**:

```kotlin
@Composable
fun ImportProgressCard(
    operation: ImportOperation,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = animateFloatAsState(
        targetValue = if (operation.totalBooks > 0) {
            operation.currentProgress.toFloat() / operation.totalBooks.toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val cardColor by animateColorAsState(
        targetValue = when (operation.status) {
            ImportStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
            ImportStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
            ImportStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 300),
        label = "cardColor"
    )

    Surface(
        modifier = modifier,
        color = cardColor,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon(status = operation.status)
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = operation.folderName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getStatusText(operation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons
                when (operation.status) {
                    ImportStatus.IN_PROGRESS -> {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                    ImportStatus.FAILED -> {
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, "Retry")
                        }
                    }
                    else -> {}
                }
            }

            // Current file (only during processing)
            if (operation.status == ImportStatus.IN_PROGRESS && operation.currentFile.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = operation.currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Progress bar
            when (operation.status) {
                ImportStatus.STARTING, ImportStatus.SCANNING -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ImportStatus.IN_PROGRESS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progress.value },
                            modifier = Modifier.fillMaxWidth(),
                            gap = 4.dp
                        )

                        // Progress details row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${operation.currentProgress} of ${operation.totalBooks}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progress.value * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                ImportStatus.COMPLETED -> {
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ImportStatus.FAILED -> {
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }

            // Error message
            if (operation.status == ImportStatus.FAILED && operation.errorMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = operation.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: ImportStatus) {
    val (icon, tint) = when (status) {
        ImportStatus.STARTING -> Icons.Default.HourglassEmpty to MaterialTheme.colorScheme.onSurfaceVariant
        ImportStatus.SCANNING -> Icons.Default.Search to MaterialTheme.colorScheme.onSurfaceVariant
        ImportStatus.IN_PROGRESS -> Icons.Default.Autorenew to MaterialTheme.colorScheme.primary
        ImportStatus.COMPLETED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        ImportStatus.FAILED -> Icons.Default.Error to MaterialTheme.colorScheme.error
        ImportStatus.CANCELLED -> Icons.Default.Cancel to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

private fun getStatusText(operation: ImportOperation): String {
    return when (operation.status) {
        ImportStatus.STARTING -> "Starting import..."
        ImportStatus.SCANNING -> "Scanning folder..."
        ImportStatus.IN_PROGRESS -> "Importing files..."
        ImportStatus.COMPLETED -> "Import complete"
        ImportStatus.FAILED -> "Import failed"
        ImportStatus.CANCELLED -> "Cancelled"
    }
}
```

---

### Solution 6: Smarter Re-import Logic

**Incremental Import Modes**:
```kotlin
enum class RefreshMode {
    FULL_REIMPORT,      // Current: re-process everything
    NEW_FILES_ONLY,     // Only files added since last import
    CHECK_CHANGES       // Check for modified files too
}

data class RefreshOptions(
    val mode: RefreshMode,
    val lastImportTime: Long? = null,  // For NEW_FILES_ONLY
    val verifyHashes: Boolean = false  // For CHECK_CHANGES
)
```

**Modified Import with Refresh Mode**:
```kotlin
suspend fun execute(
    folderUri: Uri,
    onProgress: (BulkImportProgress) -> Unit,
    refreshOptions: RefreshOptions = RefreshOptions(RefreshMode.FULL_REIMPORT)
): Int {
    Log.i(BULK_IMPORT, "Starting import with mode: ${refreshOptions.mode}")

    val supportedExtensions = provideExtensions()
    val existingBooks = bookRepository.getBooks("").map { it.filePath }

    val allFiles = getAllFilesFromFolder(folderUri)
    Log.i(BULK_IMPORT, "Found ${allFiles.size} total files")

    val supportedFiles = allFiles.filter { cachedFile ->
        val isSupported = supportedExtensions.any { ext ->
            cachedFile.name.endsWith(ext, ignoreCase = true)
        }

        val canAccess = cachedFile.canAccess()

        val shouldImport = when (refreshOptions.mode) {
            RefreshMode.FULL_REIMPORT -> {
                // Import all supported files (skip duplicates)
                val alreadyExists = existingPaths.any { existingPath ->
                    existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
                }
                isSupported && !alreadyExists && canAccess
            }

            RefreshMode.NEW_FILES_ONLY -> {
                // Only import files modified after last import
                val isModified = refreshOptions.lastImportTime?.let { lastTime ->
                    cachedFile.lastModified > lastTime
                } ?: true

                val alreadyExists = existingPaths.any { existingPath ->
                    existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
                }

                isSupported && isModified && !alreadyExists && canAccess
            }

            RefreshMode.CHECK_CHANGES -> {
                // Check for changes by comparing file metadata/hash
                val existingBook = existingBooks.find { book ->
                    book.filePath.equals(cachedFile.uri.toString(), ignoreCase = true)
                }

                if (existingBook == null) {
                    // New file
                    isSupported && canAccess
                } else {
                    // Existing file - check if modified
                    if (refreshOptions.verifyHashes) {
                        val currentHash = computeFileHash(cachedFile)
                        currentHash != existingBook.fileHash
                    } else {
                        // Simple timestamp check
                        cachedFile.lastModified > (existingBook.lastModified ?: 0)
                    }
                }
            }
        }

        shouldImport
    }

    Log.i(BULK_IMPORT, "Found ${supportedFiles.size} files to import (${refreshOptions.mode})")

    // Process files...
}

/**
 * Compute SHA-256 hash of file content.
 * Expensive but reliable for change detection.
 */
private suspend fun computeFileHash(file: CachedFile): String {
    return withContext(Dispatchers.IO) {
        file.openInputStream()?.use { input ->
            MessageDigest.getInstance("SHA-256").digest(input.readBytes())
                .joinToString("") { "%02x".format(it) }
        } ?: ""
    }
}
```

**UI Integration**:
```kotlin
@Composable
fun RefreshDropdown(
    onRefresh: (RefreshMode) -> Unit,
    lastImportTime: Long? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Refresh, "Refresh folder")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Full re-import") },
                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                onClick = {
                    onRefresh(RefreshMode.FULL_REIMPORT)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text("New files only") },
                leadingIcon = { Icon(Icons.Default.NewReleases, null) },
                trailingText = {
                    lastImportTime?.let {
                        Text("Since ${formatDate(it)}")
                    }
                },
                onClick = {
                    onRefresh(RefreshMode.NEW_FILES_ONLY)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text("Check for changes") },
                leadingIcon = { Icon(Icons.Default.Sync, null) },
                onClick = {
                    onRefresh(RefreshMode.CHECK_CHANGES)
                    expanded = false
                }
            )
        }
    }
}
```

---

### Solution 7: Import Summary and Error Reporting

**Enhanced Result Model**:
```kotlin
data class ImportResult(
    val folderName: String,
    val totalFiles: Int,
    val successful: Int,
    val failed: Int,
    val skipped: Int,
    val durationMs: Long,
    val errors: List<FileError>
)

data class FileError(
    val fileName: String,
    val reason: String,
    val errorType: ErrorType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ErrorType {
    PARSE_ERROR,
    ACCESS_ERROR,
    DUPLICATE,
    DATABASE_ERROR,
    UNKNOWN
}
```

**Import Summary Bottom Sheet**:
```kotlin
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImportSummarySheet(
    result: ImportResult,
    onDismiss: () -> Unit,
    onRetryFailed: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded
    )

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import Summary",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                // Overall status
                val isSuccess = result.failed == 0
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isSuccess)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isSuccess)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = if (isSuccess) "Import Completed" else "Import Partially Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSuccess)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "${result.folderName} • ${formatDuration(result.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Total",
                        value = result.totalFiles.toString(),
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Success",
                        value = result.successful.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Failed",
                        value = result.failed.toString(),
                        icon = Icons.Default.Error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Skipped",
                        value = result.skipped.toString(),
                        icon = Icons.Default.SkipNext,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Errors section (if any)
                if (result.errors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Errors (${result.errors.size})",
                                style = MaterialTheme.typography.titleSmall
                            )

                            TextButton(onClick = onRetryFailed) {
                                Text("Retry Failed")
                            }
                        }

                        result.errors.take(10).forEach { error ->
                            ErrorItem(error)
                        }

                        if (result.errors.size > 10) {
                            Text(
                                text = "And ${result.errors.size - 10} more errors...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    FilledButton(
                        onClick = {
                            onDismiss()
                            // Navigate to library
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Library")
                    }
                }
            }
        }
    ) {
        // Main content (dimmed when sheet is open)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorItem(error: FileError) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = error.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = error.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
```

---

### Solution 8: Cancellation Support During Scanning

**Cooperative Cancellation Implementation**:
```kotlin
/**
 * Import with proper cancellation support.
 */
class BulkImportBooksFromFolder @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val bookRepository: BookRepository,
    private val insertBook: InsertBook
) {
    // Store active jobs for cancellation
    private val activeJobs = ConcurrentHashMap<String, Job>()

    suspend fun execute(
        folderUri: Uri,
        operationId: String,
        onProgress: (BulkImportProgress) -> Unit
    ): Int {
        Log.i(BULK_IMPORT, "Starting import: $operationId")

        val job = CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            try {
                doExecute(folderUri, operationId, onProgress)
            } catch (e: CancellationException) {
                Log.i(BULK_IMPORT, "Import cancelled: $operationId")
                throw e
            }
        }

        activeJobs[operationId] = job

        try {
            job.join()
            return job.getCompleted()
        } finally {
            activeJobs.remove(operationId)
        }
    }

    private suspend fun doExecute(
        folderUri: Uri,
        operationId: String,
        onProgress: (BulkImportProgress) -> Unit
    ): Int {
        val supportedExtensions = provideExtensions()
        val existingPaths = bookRepository.getBooks("").map { it.filePath }
        var importedCount = 0

        // Phase 1: Scan files with cancellation checks
        val allFiles = scanFilesWithCancellation(folderUri, operationId)
        Log.i(BULK_IMPORT, "Found ${allFiles.size} files")

        // Filter files
        val supportedFiles = allFiles.filter { cachedFile ->
            ensureActive(operationId)  // Check cancellation

            val isSupported = supportedExtensions.any { ext ->
                cachedFile.name.endsWith(ext, ignoreCase = true)
            }
            val alreadyExists = existingPaths.any { existingPath ->
                existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
            }
            val canAccess = cachedFile.canAccess()
            isSupported && !alreadyExists && canAccess
        }

        Log.i(BULK_IMPORT, "Found ${supportedFiles.size} supported files")

        // Phase 2: Process files with cancellation
        supportedFiles.forEachIndexed { index, cachedFile ->
            ensureActive(operationId)  // Check cancellation

            try {
                val nullableBook = fileSystemRepository.getBookFromFile(cachedFile)
                when (nullableBook) {
                    is NullableBook.NotNull -> {
                        insertBook.execute(nullableBook.bookWithCover!!)
                        importedCount++
                        onProgress(BulkImportProgress(importedCount, supportedFiles.size, cachedFile.name))
                    }
                    is NullableBook.Null -> {
                        Log.w(BULK_IMPORT, "Failed to parse: ${cachedFile.name}")
                        onProgress(BulkImportProgress(index + 1, supportedFiles.size, cachedFile.name))
                    }
                }
            } catch (e: Exception) {
                Log.e(BULK_IMPORT, "Error importing ${cachedFile.name}", e)
                onProgress(BulkImportProgress(index + 1, supportedFiles.size, cachedFile.name))
            }
        }

        return importedCount
    }

    /**
     * Scan files with cooperative cancellation.
     */
    private suspend fun scanFilesWithCancellation(
        folderUri: Uri,
        operationId: String
    ): List<CachedFile> {
        val files = mutableListOf<CachedFile>()

        suspend fun walkRecursive(directory: CachedFile) {
            ensureActive(operationId)  // Check before entering directory

            directory.listFiles { file ->
                ensureActive(operationId)  // Check before processing file

                if (file.isDirectory) {
                    walkRecursive(file)
                } else {
                    files.add(file)
                }
            }
        }

        val folder = CachedFileCompat.fromUri(application, folderUri)
        if (folder?.isDirectory == true) {
            walkRecursive(folder)
        }

        return files
    }

    /**
     * Check if operation is still active. Throws CancellationException if not.
     */
    private fun ensureActive(operationId: String) {
        if (activeJobs[operationId]?.isActive != true) {
            throw CancellationException("Import $operationId was cancelled")
        }
    }

    /**
     * Cancel an active import operation.
     */
    fun cancelImport(operationId: String) {
        Log.i(BULK_IMPORT, "Cancelling import: $operationId")
        activeJobs[operationId]?.cancel()
    }
}
```

**Integration with ImportProgressService**:
```kotlin
fun cancelImport(operationId: String) {
    // Cancel the actual job
    bulkImportBooksFromFolder.cancelImport(operationId)

    // Update UI state
    _importOperations.value = _importOperations.value.map { op ->
        if (op.id == operationId) {
            op.copy(status = ImportStatus.CANCELLED)
        } else op
    }

    // Clear after delay
    coroutineScope.launch {
        delay(3000)
        clearOperation(operationId)
    }
}
```

---

## Material 3 Progress Indicator Guidelines

### Component Selection Matrix

| Scenario | Recommended Component | Reason |
|----------|---------------------|---------|
| Inline under folder item | `LinearProgressIndicator` with track gap | Compact, space-efficient |
| Full-screen import dialog | `LinearProgressIndicator` (determinate) | Shows clear progress |
| Scanning phase (unknown duration) | `LinearProgressIndicator` (indeterminate) | Continuous animation |
| Multiple concurrent imports | Compact list with multiple progress bars | Show all active operations |
| Quick single-file operation | `CircularProgressIndicator` | Space-saving, recognizable |
| Success state | Checkmark icon or full progress bar | Clear completion signal |
| Error state | Red progress bar with error icon | Visual feedback |

### Material 3 Expressive Progress Components

**LinearProgressIndicator** (Current - Basic):
```kotlin
LinearProgressIndicator(
    progress = { current / total.toFloat() },
    modifier = Modifier.fillMaxWidth()
)
```

**LinearProgressIndicator** (Material 3 Expressive):
```kotlin
LinearProgressIndicator(
    progress = { current / total.toFloat() },
    modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .semantics {
            this.progress = current / total.toFloat()
            contentDescription = "Importing $current of $total files"
        },
    gap = 4.dp,  // Space between active/inactive portions
    drawStopIndicator = {},  // End stop dot
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
)
```

**Key Features**:
- **Gap**: Visual separation between progress and remaining (4.dp recommended)
- **End Stop Indicator**: Dot at the end of progress bar
- **Height**: 4-6.dp for visibility
- **Colors**: Primary for active, surface variant for track

**Indeterminate Progress**:
```kotlin
LinearProgressIndicator(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.surfaceVariant
)
```

### Accessibility Guidelines

**Semantic Labeling**:
```kotlin
LinearProgressIndicator(
    progress = { progress },
    modifier = Modifier.semantics {
        this.progress = progress
        contentDescription = "Importing $current of $total books"
        stateDescription = "${(progress * 100).toInt()} percent complete"
    }
)
```

**Content Descriptions**:
- Always provide `contentDescription`
- Include current progress: "15 of 50 files"
- Include status: "Importing...", "Completed", "Failed"
- Update dynamically as progress changes

**Screen Reader Announcements**:
```kotlin
val progressText = when (status) {
    ImportStatus.IN_PROGRESS -> "Importing file $current of $total: $fileName"
    ImportStatus.COMPLETED -> "Import complete. $total books imported."
    ImportStatus.FAILED -> "Import failed. $errorMessage"
    else -> status.name.lowercase()
}

Modifier.semantics {
    liveRegion = LiveRegionMode.Assertive  // Announce immediately
    contentDescription = progressText
}
```

### Animation Guidelines

**Progress Transitions**:
```kotlin
val animatedProgress = animateFloatAsState(
    targetValue = if (total > 0) current / total.toFloat() else 0f,
    animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    ),
    label = "progress"
)

LinearProgressIndicator(progress = { animatedProgress.value })
```

**State Transitions**:
```kotlin
val backgroundColor by animateColorAsState(
    targetValue = when (status) {
        ImportStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
        ImportStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    },
    animationSpec = tween(durationMillis = 300),
    label = "backgroundColor"
)
```

**Completion Animation**:
```kotlin
// Checkmark animation
val checkmarkScale by animateFloatAsState(
    targetValue = if (status == ImportStatus.COMPLETED) 1f else 0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    label = "checkmarkScale"
)

Icon(
    imageVector = Icons.Default.CheckCircle,
    contentDescription = null,
    modifier = Modifier.scale(checkmarkScale)
)
```

### Color Usage

**Progress States**:
```kotlin
enum class ProgressState {
    ACTIVE,      // Primary color theme
    COMPLETED,   // Success green or primary
    ERROR,       // Error color theme
    CANCELLED    // Neutral/onSurfaceVariant
}

val progressColor = when (state) {
    ProgressState.ACTIVE -> MaterialTheme.colorScheme.primary
    ProgressState.COMPLETED -> MaterialTheme.colorScheme.primary
    ProgressState.ERROR -> MaterialTheme.colorScheme.error
    ProgressState.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
}
```

**Track Colors**:
```kotlin
val trackColor = when (state) {
    ProgressState.ACTIVE -> MaterialTheme.colorScheme.surfaceContainerHighest
    ProgressState.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
    ProgressState.ERROR -> MaterialTheme.colorScheme.errorContainer
    ProgressState.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
}
```

### Typography

**Progress Labels**:
```kotlin
// Percentage badge
Text(
    text = "${(progress * 100).toInt()}%",
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.primary
)

// Count label
Text(
    text = "$current/$total",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

**Status Messages**:
```kotlin
Text(
    text = when (status) {
        ImportStatus.STARTING -> "Starting import..."
        ImportStatus.SCANNING -> "Scanning folder..."
        ImportStatus.IN_PROGRESS -> "Importing files..."
        ImportStatus.COMPLETED -> "Import complete"
        ImportStatus.FAILED -> "Import failed"
        ImportStatus.CANCELLED -> "Cancelled"
    },
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

---

## Implementation Roadmap

### Phase 1: Critical Fixes (1-2 weeks)

#### Task 1.1: Implement True Queue Management
**Priority**: 🔴 Critical
**Effort**: 2-3 days
**Impact**: Prevents system overload, better resource utilization

**Tasks**:
- [ ] Create `ImportQueueManager.kt` with Channel-based queue
- [ ] Implement max concurrency limit (default: 2)
- [ ] Add queue state tracking (queuedCount, activeCount)
- [ ] Integrate with `ImportProgressViewModel`
- [ ] Add queue visibility in UI (show "X queued")

**Testing**:
- [ ] Test with 5 concurrent folder imports
- [ ] Verify only 2 run simultaneously
- [ ] Verify FIFO ordering
- [ ] Test cancellation of queued items

**Acceptance Criteria**:
- ✅ Max 2 concurrent imports active
- ✅ Additional imports queued in order
- ✅ Queue visible in UI
- ✅ No performance degradation with many folders

---

#### Task 1.2: Parallel File Processing
**Priority**: 🔴 Critical
**Effort**: 2-3 days
**Impact**: 3-5x faster imports

**Tasks**:
- [ ] Modify `BulkImportBooksFromFolder.execute()` to use parallel processing
- [ ] Add Semaphore for concurrency control (default: 4)
- [ ] Implement thread-safe progress updates with Mutex
- [ ] Add error collection for reporting
- [ ] Ensure proper cancellation support

**Testing**:
- [ ] Import 100 files with 4 workers
- [ ] Verify 3-5x speed improvement
- [ ] Test with 1000 files (stress test)
- [ ] Verify no race conditions in database

**Acceptance Criteria**:
- ✅ 3-5x faster than sequential
- ✅ No duplicate database entries
- ✅ Proper error handling
- ✅ Memory usage within acceptable range

---

#### Task 1.3: Proper Material 3 Progress Indicators
**Priority**: 🔴 Critical
**Effort**: 1-2 days
**Impact**: Better visual feedback, Material 3 compliance

**Tasks**:
- [ ] Implement track gap (4.dp)
- [ ] Add end stop indicator
- [ ] Add semantic labeling for accessibility
- [ ] Implement smooth progress animations
- [ ] Add indeterminate progress for scanning

**Testing**:
- [ ] Verify progress bar appearance matches Material 3
- [ ] Test with screen reader (TalkBack)
- [ ] Verify animations are smooth
- [ ] Test color transitions between states

**Acceptance Criteria**:
- ✅ Progress bar matches Material 3 design
- ✅ Accessible with screen readers
- ✅ Smooth animations
- ✅ Indeterminate state works

---

#### Task 1.4: Cancellation During Scanning
**Priority**: 🔴 Critical
**Effort**: 1-2 days
**Impact**: User can cancel long-running scans

**Tasks**:
- [ ] Pass Job reference to use case
- [ ] Implement cooperative cancellation in `walk()`
- [ ] Add cancellation checks in loops
- [ ] Update UI state when cancelled
- [ ] Clean up resources on cancellation

**Testing**:
- [ ] Start import on large folder (10,000 files)
- [ ] Cancel immediately
- [ ] Verify scan stops within 1 second
- [ ] Verify UI updates to CANCELLED state

**Acceptance Criteria**:
- ✅ Cancellation stops within 1 second
- ✅ Resources cleaned up properly
- ✅ UI state reflects cancellation
- ✅ No errors after cancellation

---

### Phase 2: UX Improvements (1-2 weeks)

#### Task 2.1: Enhanced Progress UI with States
**Priority**: 🟡 Medium
**Effort**: 2-3 days
**Impact**: Clearer feedback, better user understanding

**Tasks**:
- [ ] Implement animated state transitions
- [ ] Add color-coded states
- [ ] Implement percentage badge
- [ ] Add expandable details section
- [ ] Improve filename truncation (keep extension)

**Testing**:
- [ ] Verify all states render correctly
- [ ] Test animations are smooth
- [ ] Verify details expand/collapse works
- [ ] Test filename truncation with various lengths

**Acceptance Criteria**:
- ✅ All states visually distinct
- ✅ Smooth transitions
- ✅ Expandable details work
- ✅ Filename truncation keeps extension

---

#### Task 2.2: Import Summary on Completion
**Priority**: 🟡 Medium
**Effort**: 2-3 days
**Impact**: Users know import results

**Tasks**:
- [ ] Create `ImportResult` data model
- [ ] Implement summary bottom sheet
- [ ] Add stats grid (total, success, failed, skipped)
- [ ] Show error details for failed files
- [ ] Add "View Library" button

**Testing**:
- [ ] Test with successful import
- [ ] Test with partial failures
- [ ] Test with complete failure
- [ ] Verify stats are accurate
- [ ] Test retry functionality

**Acceptance Criteria**:
- ✅ Summary shows accurate stats
- [ ] Error details displayed
- [ ] Navigation to library works
- [ ] Bottom sheet dismisses correctly

---

#### Task 2.3: Longer Auto-Clear Timer
**Priority**: 🟡 Medium
**Effort**: 0.5 day
**Impact**: Users can see completion status

**Tasks**:
- [ ] Change success auto-clear from 2s to 5s
- [ ] Change error auto-clear from 5s to 10s
- [ ] Consider adding manual dismiss button

**Testing**:
- [ ] Verify success shows for 5 seconds
- [ ] Verify error shows for 10 seconds
- [ ] Verify manual dismiss works

**Acceptance Criteria**:
- ✅ Success visible for 5+ seconds
- ✅ Errors visible for 10+ seconds
- ✅ Manual dismiss option

---

#### Task 2.4: Error Details with Retry
**Priority**: 🟡 Medium
**Effort**: 1-2 days
**Impact**: Users can troubleshoot and retry

**Tasks**:
- [ ] Capture detailed error information
- [ ] Classify error types (parse, access, duplicate)
- [ ] Display error list in summary
- [ ] Add retry button for failed files
- [ ] Implement retry logic (only failed files)

**Testing**:
- [ ] Test with various error types
- [ ] Verify retry only attempts failed files
- [ ] Verify error messages are helpful
- [ ] Test with all files failing

**Acceptance Criteria**:
- ✅ Error details displayed
- ✅ Retry only attempts failed files
- ✅ Error messages are actionable
- ✅ Retry works correctly

---

### Phase 3: Advanced Features (2-3 weeks)

#### Task 3.1: Stream-Based Processing
**Priority**: 🟢 Low (but recommended)
**Effort**: 3-4 days
**Impact**: Better memory efficiency for large directories

**Tasks**:
- [ ] Implement streaming file walker
- [ ] Process files as they're found
- [ ] Maintain progress tracking with streaming
- [ ] Ensure proper cancellation support
- [ ] Compare memory usage with old approach

**Testing**:
- [ ] Import 10,000 file directory
- [ ] Monitor memory usage
- [ ] Verify progress updates work
- [ ] Test cancellation during scan

**Acceptance Criteria**:
- ✅ Memory usage < 10% of old approach
- ✅ Progress updates still accurate
- ✅ Can handle 50,000+ files
- ✅ Cancellation works during scan

---

#### Task 3.2: Incremental Re-import Modes
**Priority**: 🟢 Low
**Effort**: 2-3 days
**Impact**: Faster refreshes, less wasted time

**Tasks**:
- [ ] Create `RefreshMode` enum
- [ ] Implement "new files only" mode
- [ ] Implement "check changes" mode
- [ ] Store last import timestamps
- [ ] Add refresh mode selector in UI

**Testing**:
- [ ] Test "new files only" with new files
- [ ] Test "new files only" with no new files
- [ ] Test "check changes" with modified files
- [ ] Verify timestamp storage works

**Acceptance Criteria**:
- ✅ New files detected and imported
- [ ] Modified files detected
- [ ] Timestamps stored correctly
- [ ] Mode selector works in UI

---

#### Task 3.3: Import History
**Priority**: 🟢 Low
**Effort**: 2-3 days
**Impact**: Better tracking and debugging

**Tasks**:
- [ ] Create `ImportHistory` entity
- [ ] Add history table to database
- [ ] Record import attempts
- [ ] Create history screen
- [ ] Add ability to view past imports

**Testing**:
- [ ] Verify import history recorded
- [ ] Test history screen displays correctly
- [ ] Verify history persists across app restarts
- [ ] Test with multiple imports

**Acceptance Criteria**:
- ✅ All imports recorded in history
- ✅ History screen displays correctly
- ✅ History persists
- ✅ Can view past import details

---

#### Task 3.4: Library Import Indicators
**Priority**: 🟢 Low
**Effort**: 1-2 days
**Impact**: Visual feedback for new books

**Tasks**:
- [ ] Add "recently imported" flag to books
- [ ] Implement import animation
- [ ] Add "new" badge for 24 hours
- [ ] Create "recently imported" filter

**Testing**:
- [ ] Verify "new" badge appears on recently imported books
- [ ] Verify badge disappears after 24 hours
- [ ] Test import animation
- [ ] Verify filter works

**Acceptance Criteria**:
- ✅ "New" badge shows on recent imports
- ✅ Badge disappears after 24 hours
- ✅ Import animation plays
- ✅ Filter shows recently imported

---

### Phase 4: Polish (1 week)

#### Task 4.1: Animations and Transitions
**Priority**: 🟢 Low
**Effort**: 2-3 days
**Impact**: Smoother, more polished feel

**Tasks**:
- [ ] Refine all animations
- [ ] Add completion celebration animation
- [ ] Smooth out state transitions
- [ ] Add loading skeletons for initial state

**Testing**:
- [ ] Verify all animations are smooth
- [ ] Test on different devices (performance)
- [ ] Verify animations don't cause jank
- [ ] Test with many concurrent imports

**Acceptance Criteria**:
- ✅ All animations smooth (60fps)
- ✅ No jank during imports
- ✅ Completion animation plays
- ✅ Loading states clear

---

#### Task 4.2: Accessibility Improvements
**Priority**: 🟢 Low
**Effort**: 1-2 days
**Impact**: Better screen reader support

**Tasks**:
- [ ] Add content descriptions to all elements
- [ ] Implement live region announcements
- [ ] Add keyboard navigation support
- [ ] Test with TalkBack

**Testing**:
- [ ] Test with TalkBack enabled
- [ ] Verify all elements announced
- [ ] Verify live regions work
- [ ] Test keyboard navigation

**Acceptance Criteria**:
- ✅ All elements accessible via TalkBack
- ✅ Live regions announce progress
- ✅ Keyboard navigation works
- ✅ Accessibility scan passes

---

#### Task 4.3: Performance Optimization
**Priority**: 🟢 Low
**Effort**: 2-3 days
**Impact**: Better performance on large libraries

**Tasks**:
- [ ] Benchmark current performance
- [ ] Optimize database queries
- [ ] Reduce memory allocations
- [ ] Implement caching for repeated imports

**Testing**:
- [ ] Run benchmarks
- [ ] Profile memory usage
- [ ] Test with 10,000+ books
- [ ] Verify no memory leaks

**Acceptance Criteria**:
- ✅ No regression in benchmarks
- ✅ Memory usage reduced by 20%+
- ✅ No memory leaks
- ✅ UI remains responsive

---

## Alternative Approaches

### WorkManager API

**Pros**:
- Built-in scheduling
- Automatic retry with exponential backoff
- Constraints support (battery, network, idle)
- Survives process death
- Works in background

**Cons**:
- Overkill for interactive imports
- Less immediate feedback
- More complex to implement
- Limited real-time progress reporting

**Use Case**: Background sync of OPDS catalogs or scheduled folder checks

**Verdict**: Keep current coroutine approach for interactive imports. Consider WorkManager for:
- Background OPDS feed updates
- Scheduled folder scanning (if feature requested)
- Retry of failed imports with constraints

---

### Flow-Based Import

**Pros**:
- Reactive and composable
- Naturally cancelable
- Backpressure handling
- Elegant API

**Cons**:
- Complex implementation
- Requires dependency on Flow library
- May confuse existing architecture
- Steeper learning curve

**Example**:
```kotlin
fun importFolder(folderUri: Uri): Flow<ImportProgress> = flow {
    emit(ImportProgress.Starting)

    getAllFilesFlow(folderUri)
        .filter { it.isSupported() }
        .map { it.process() }
        .catch { emit(ImportProgress.Error(it)) }
        .collect { file ->
            emit(ImportProgress.Processing(file))
            insertBook(file)
        }

    emit(ImportProgress.Completed)
}
```

**Verdict**: Consider for v3.0 rewrite if moving to reactive architecture. Not worth migration effort for current codebase.

---

### Incremental Database Writes

**Pros**:
- Avoids large transactions
- Memory efficient
- Can report progress as we go

**Cons**:
- Slower overall (more commits)
- More complex error handling
- Potential for partial imports

**Implementation**:
```kotlin
// Batch inserts every N files
supportedFiles.chunked(10).forEach { batch ->
    database.withTransaction {
        batch.forEach { file ->
            insertBook(file)
        }
    }
    reportProgress()
}
```

**Verdict**: Keep batch inserts for performance. Only use incremental writes for very large imports (>100 files) if memory is a concern.

---

## File Reference Index

### Core Import Files

| File Path | Purpose | Key Classes/Functions |
|-----------|---------|---------------------|
| `app/src/main/java/us/blindmint/codex/presentation/settings/browse/scan/components/BrowseScanOption.kt` | UI for local folder management | `BrowseScanOption()`, `BrowseScanFolderItem()` |
| `app/src/main/java/us/blindmint/codex/domain/use_case/book/BulkImportBooksFromFolder.kt` | Main import logic | `BulkImportBooksFromFolder.execute()` |
| `app/src/main/java/us/blindmint/codex/ui/import_progress/ImportProgressService.kt` | Import state management | `ImportProgressService`, `ImportProgressViewModel` |
| `app/src/main/java/us/blindmint/codex/presentation/import_progress/ImportProgressBar.kt` | Progress UI component | `ImportProgressBar()` |
| `app/src/main/java/us/blindmint/codex/data/repository/FileSystemRepositoryImpl.kt` | File system operations | `FileSystemRepositoryImpl.getAllFilesFromFolder()` |

### Data Models

| File Path | Purpose | Key Models |
|-----------|---------|------------|
| `app/src/main/java/us/blindmint/codex/domain/import_progress/ImportOperation.kt` | Import operation state | `ImportOperation` |
| `app/src/main/java/us/blindmint/codex/domain/import_progress/ImportStatus.kt` | Import status enum | `ImportStatus` |
| `app/src/main/java/us/blindmint/codex/domain/use_case/book/BulkImportBooksFromFolder.kt` | Progress data | `BulkImportProgress` |

### Parser Files

| File Path | Format | Key Class |
|-----------|---------|-----------|
| `app/src/main/java/us/blindmint/codex/data/parser/FileParserImpl.kt` | Router | `FileParserImpl.parse()` |
| `app/src/main/java/us/blindmint/codex/data/parser/pdf/PdfFileParser.kt` | PDF | `PdfFileParser` |
| `app/src/main/java/us/blindmint/codex/data/parser/epub/EpubFileParser.kt` | EPUB | `EpubFileParser` |
| `app/src/main/java/us/blindmint/codex/data/parser/comic/ComicFileParser.kt` | CBR/CBZ/CB7 | `ComicFileParser` |
| `app/src/main/java/us/blindmint/codex/data/parser/txt/TxtFileParser.kt` | TXT/MD | `TxtFileParser` |
| `app/src/main/java/us/blindmint/codex/data/parser/fb2/Fb2FileParser.kt` | FB2 | `Fb2FileParser` |
| `app/src/main/java/us/blindmint/codex/data/parser/html/HtmlFileParser.kt` | HTML/HTM | `HtmlFileParser` |
| `app/src/main/java/us/blindmint/codex/data/parser/fodt/FodtFileParser.kt` | FODT | `FodtFileParser` |

### Utility Files

| File Path | Purpose | Key Functions |
|-----------|---------|--------------|
| `app/src/main/java/us/blindmint/codex/presentation/core/constants/SupportedExtensionConstants.kt` | Supported formats | `provideExtensions()` |
| `app/src/main/java/us/blindmint/codex/domain/file/CachedFile.kt` | File abstraction | `CachedFile.walk()`, `CachedFile.listFiles()` |
| `app/src/main/java/us/blindmint/codex/domain/use_case/book/InsertBook.kt` | Database insertion | `InsertBook.execute()` |

### UI Components

| File Path | Purpose | Key Composables |
|-----------|---------|----------------|
| `app/src/main/java/us/blindmint/codex/presentation/library/LibraryItem.kt` | Library book display | `LibraryItem()` |
| `app/src/main/java/us/blindmint/codex/ui/library/LibraryScreen.kt` | Library screen | `LibraryScreen.Content()` |
| `app/src/main/java/us/blindmint/codex/presentation/library/LibraryContent.kt` | Library content | `LibraryContent()` |

---

## Code Snippets and Patterns

### Pattern 1: Channel-Based Communication

**Current Implementation** (Library Refresh):
```kotlin
// LibraryScreen.kt
object LibraryScreen {
    val refreshListChannel: Channel<Long> = Channel(Channel.CONFLATED)
}

// Usage
LibraryScreen.refreshListChannel.trySend(0)

// Consumption
LaunchedEffect(Unit) {
    LibraryScreen.refreshListChannel.receiveAsFlow().collectLatest {
        libraryModel.loadBooks()
    }
}
```

**Best Practices**:
- Use `Channel.CONFLATED` for event streaming (keep latest value)
- Use `trySend()` to avoid blocking
- Collect as Flow in UI with `collectLatest()`

---

### Pattern 2: StateFlow for UI State

**Current Implementation** (Import Progress):
```kotlin
// Service layer
private val _importOperations = MutableStateFlow<List<ImportOperation>>(emptyList())
val importOperations: StateFlow<List<ImportOperation>> = _importOperations.asStateFlow()

// Update
_importOperations.value = _importOperations.value.map { op ->
    if (op.id == operationId) {
        op.copy(status = newStatus)
    } else op
}

// Consumption
val importOperations by importProgressViewModel.importOperations.collectAsStateWithLifecycle()
```

**Best Practices**:
- Use `StateFlow` for UI state (not `Flow`)
- Expose as read-only `StateFlow` (`asStateFlow()`)
- Use `collectAsStateWithLifecycle()` in Compose
- Immutability: Update via `copy()` for data classes

---

### Pattern 3: Coroutines with Supervision

**Current Implementation** (Import Service):
```kotlin
@Singleton
class ImportProgressService @Inject constructor(
    private val bulkImportBooksFromFolder: BulkImportBooksFromFolder
) {
    private val coroutineScope = CoroutineScope(SupervisorJob())

    fun startImport(...) {
        coroutineScope.launch {
            // Import logic
        }
    }
}
```

**Best Practices**:
- Use `SupervisorJob` for independent tasks (one failure doesn't cancel others)
- Use `Dispatchers.IO` for file/database operations
- Always handle exceptions in coroutines
- Cancel coroutines when no longer needed

---

### Pattern 4: File System Access with SAF

**Current Implementation**:
```kotlin
// Using DocumentFileCompat library
val folder = CachedFileCompat.fromUri(context, folderUri)

// Walk directory
folder.walk { file ->
    if (!file.isDirectory) {
        // Process file
    }
}

// Check access
if (file.canAccess()) {
    // File is accessible
}
```

**Best Practices**:
- Use `CachedFile` for better performance than `DocumentFile`
- Always check `canAccess()` before processing
- Handle exceptions gracefully (permissions can be revoked)
- Use `walk()` for recursive directory traversal

---

### Pattern 5: Progress Callback Pattern

**Current Implementation**:
```kotlin
// Define callback type
typealias ProgressCallback = (BulkImportProgress) -> Unit

// Pass to function
suspend fun execute(
    folderUri: Uri,
    onProgress: ProgressCallback
): Int {
    // Call callback for updates
    onProgress(BulkImportProgress(current, total, fileName))
}

// Implementation
bulkImportBooksFromFolder.execute(folderUri) { progress ->
    // Update UI
    updateProgress(progress)
}
```

**Best Practices**:
- Use callback for synchronous progress updates
- Consider `Flow` for async progress streams
- Keep callbacks lightweight (avoid heavy operations)
- Thread-safety: Use Mutex if updating shared state

---

### Pattern 6: Error Handling in Imports

**Current Implementation**:
```kotlin
supportedFiles.forEachIndexed { index, file ->
    try {
        val book = fileSystemRepository.getBookFromFile(file)
        when (book) {
            is NullableBook.NotNull -> {
                insertBook.execute(book.bookWithCover)
                importedCount++
            }
            is NullableBook.Null -> {
                Log.w(TAG, "Failed to parse: ${file.name}")
                // Still report progress
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error importing ${file.name}", e)
        // Continue with next file
    }
}
```

**Best Practices**:
- Continue processing on errors (don't stop entire import)
- Log errors with context (file name, error message)
- Report progress even for failed files
- Collect errors for summary report

---

### Pattern 7: Cancellation Handling

**Recommended Implementation**:
```kotlin
// Store job reference
private val activeJobs = ConcurrentHashMap<String, Job>()

// Launch with job reference
fun startImport(operationId: String) {
    val job = coroutineScope.launch(Dispatchers.IO) {
        try {
            // Import logic with cancellation checks
            ensureActive(operationId)
            // Process files...
        } catch (e: CancellationException) {
            Log.i(TAG, "Import cancelled: $operationId")
        }
    }
    activeJobs[operationId] = job
}

// Cancel function
fun cancelImport(operationId: String) {
    activeJobs[operationId]?.cancel()
}

// Cooperative cancellation check
private fun ensureActive(operationId: String) {
    if (activeJobs[operationId]?.isActive != true) {
        throw CancellationException("Import cancelled")
    }
}
```

**Best Practices**:
- Store `Job` references for cancellation
- Use `Dispatchers.IO` for interruptible operations
- Check `job.isActive` periodically
- Clean up resources in `finally` block

---

### Pattern 8: Semaphore-Based Concurrency Control

**Recommended Implementation**:
```kotlin
suspend fun execute(
    maxConcurrency: Int = 4,
    onProgress: (BulkImportProgress) -> Unit
): Int {
    val semaphore = Semaphore(maxConcurrency)
    val progressMutex = Mutex()
    val importedCount = AtomicInteger(0)

    supportedFiles.map { file ->
        async(Dispatchers.IO) {
            semaphore.acquire()
            try {
                // Process file
                val current = importedCount.incrementAndGet()

                // Thread-safe progress update
                progressMutex.withLock {
                    onProgress(BulkImportProgress(current, total, file.name))
                }
            } finally {
                semaphore.release()
            }
        }
    }.awaitAll()

    return importedCount.get()
}
```

**Best Practices**:
- Use `Semaphore` to limit concurrent operations
- Use `Mutex` for thread-safe shared state updates
- Use `Dispatchers.IO` for blocking operations
- Always release semaphore in `finally` block

---

### Pattern 9: Material 3 Progress Indicators

**Current Implementation** (Basic):
```kotlin
LinearProgressIndicator(
    progress = { current / total.toFloat() },
    modifier = Modifier.fillMaxWidth()
)
```

**Material 3 Expressive**:
```kotlin
LinearProgressIndicator(
    progress = { animatedProgress.value },
    modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .semantics {
            this.progress = animatedProgress.value
            contentDescription = "Importing $current of $total"
        },
    gap = 4.dp,
    drawStopIndicator = {},
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
)

// Animate progress
val animatedProgress = animateFloatAsState(
    targetValue = current / total.toFloat(),
    animationSpec = tween(300, FastOutSlowInEasing),
    label = "progress"
)
```

**Best Practices**:
- Animate progress values for smooth transitions
- Use semantic labeling for accessibility
- Set appropriate height (4-6.dp)
- Use Material 3 colors from theme
- Add track gap for visual separation

---

### Pattern 10: Bottom Sheet for Import Summary

**Recommended Implementation**:
```kotlin
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImportSummarySheet(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded
    )

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Summary content
            }
        }
    ) {
        // Main content
    }
}
```

**Best Practices**:
- Use `ModalBottomSheetLayout` for bottom sheets
- Set initial value to `Expanded`
- Provide clear dismiss action
- Use proper padding and spacing
- Include action buttons (Close, View Library)

---

## Testing Strategy

### Unit Tests

**BulkImportBooksFromFolderTests.kt**:
```kotlin
@Test
fun `import filters out unsupported extensions`() = runTest {
    val mockFile = createMockFile("test.xyz")
    val result = useCase.execute(folderUri) { _ }

    assertThat(result).doesNotContain(mockFile)
}

@Test
fun `import skips existing files`() = runTest {
    val existingFile = createMockFile("existing.epub")
    repository.insertBook(existingFile)

    val result = useCase.execute(folderUri) { _ }

    assertThat(result).doesNotContain(existingFile)
}

@Test
fun `import reports correct progress`() = runTest {
    val progressUpdates = mutableListOf<BulkImportProgress>()

    useCase.execute(folderUri) { progress ->
        progressUpdates.add(progress)
    }

    assertThat(progressUpdates).hasSize(10)
    assertThat(progressUpdates.last().current).isEqualTo(10)
}
```

### Integration Tests

**ImportFlowTests.kt**:
```kotlin
@Test
fun `complete import flow from folder selection to library`() = runTest {
    // Select folder
    composeTestRule.onNodeWithText("Add folder").performClick()
    // Select test folder
    // Verify import starts
    composeTestRule.onNodeWithText("Importing:").assertExists()
    // Wait for completion
    // Verify library refreshes
    composeTestRule.onNodeWithText("Book 1").assertExists()
}
```

### Performance Tests

**ImportPerformanceTests.kt**:
```kotlin
@Test
fun `import 100 files completes within 15 seconds`() = runTest {
    val startTime = System.currentTimeMillis()
    useCase.execute(folderUri) { _ }
    val duration = System.currentTimeMillis() - startTime

    assertThat(duration).isLessThan(15000)  // 15 seconds
}

@Test
fun `memory usage stays below 50MB during import`() = runTest {
    val runtime = Runtime.getRuntime()
    val initialMemory = runtime.totalMemory() - runtime.freeMemory()

    useCase.execute(folderUri) { _ }

    val finalMemory = runtime.totalMemory() - runtime.freeMemory()
    val memoryIncrease = finalMemory - initialMemory

    assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024)  // 50 MB
}
```

### UI Tests

**ImportUITests.kt**:
```kotlin
@Test
fun `progress bar displays correctly during import`() {
    // Start import
    onNodeWithText("Refresh").performClick()

    // Verify progress bar appears
    onNodeWithTag("import_progress_bar").assertExists()

    // Verify progress updates
    waitForIdle()
    onNodeWithText("5/10").assertExists()
}
```

---

## Migration Guide

### Step 1: Backup Current Implementation
```bash
# Create feature branch
git checkout -b feature/import-queue-manager

# Create backup commit
git add app/src/main/java/us/blindmint/codex/ui/import_progress/
git commit -m "backup: current import implementation"
```

### Step 2: Implement Queue Manager
```kotlin
// Create new file: ImportQueueManager.kt
// Copy code from Solution 1 above

// Modify ImportProgressViewModel.kt
// Replace delegation to ImportProgressService with ImportQueueManager
```

### Step 3: Test Queue Manager
```bash
# Run tests
./gradlew test

# Manual testing
# - Add 3 folders, verify only 2 import at once
# - Cancel import, verify queue processes next
```

### Step 4: Implement Parallel Processing
```kotlin
// Modify BulkImportBooksFromFolder.kt
// Add parallel processing logic from Solution 2
```

### Step 5: Test Performance
```bash
# Benchmark before and after
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ImportPerformanceTests
```

### Step 6: Update Progress UI
```kotlin
// Replace ImportProgressBar.kt
// Use Material 3 expressive components from Solution 4
```

### Step 7: Final Testing
```bash
# Full test suite
./gradlew test connectedAndroidTest

# Manual testing checklist
# - Import single folder with 100 files
# - Import 5 folders concurrently
# - Cancel during scan
# - Cancel during processing
# - Test with corrupt files
# - Test with network issues (OPDS)
# - Verify library refreshes correctly
# - Test on slow device
# - Test with TalkBack enabled
```

---

## Glossary

| Term | Definition |
|-------|------------|
| **SAF** | Storage Access Framework - Android's unified storage access system |
| **URI Permission** | Permission granted by user to access specific folder/document |
| **Persistable URI** | URI permission that persists across app restarts |
| **CachedFile** | Custom abstraction over Android's DocumentFile with caching |
| **CoroutineScope** | Kotlin's context for launching coroutines |
| **SupervisorJob** | Job that doesn't cancel children when one fails |
| **StateFlow** | Kotlin's observable state holder that emits current and new values |
| **Channel** | Kotlin's non-blocking primitive for sending values between coroutines |
| **Semaphore** | Concurrency primitive that limits access to a resource |
| **Mutex** | Mutual exclusion lock for thread-safe access |
| **Determinate Progress** | Progress with known start and end points |
| **Indeterminate Progress** | Progress with unknown duration (spinning) |
| **Material 3** | Google's latest design system for Android |
| **Expressive Progress** | Material 3 progress with visual enhancements (gap, stop indicator) |

---

## References

### Official Documentation

- [Android Storage Access Framework](https://developer.android.com/training/data-storage)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3 Design](https://m3.material.io/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)

### Libraries Used

- [DocumentFileCompat](https://github.com/anggrayudi/Storage-Simple) - Enhanced DocumentFile wrapper
- [jsoup](https://jsoup.org/) - HTML parsing
- [pdfbox-android](https://pdfbox.apache.org/) - PDF parsing
- [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization) - FB2 parsing
- [junrar](https://github.com/junrar/junrar) - CBR extraction
- [commons-compress](https://commons.apache.org/proper/commons-compress/) - Archive handling

### Design Patterns

- **Repository Pattern** - Abstract data access
- **Use Case Pattern** - Business logic encapsulation
- **MVVM** - Model-View-ViewModel architecture
- **Observer Pattern** - StateFlow/Channel for state management
- **Dependency Injection** - Hilt for object graph management

---

## Conclusion

The Local Folder Import feature is functionally complete but has significant opportunities for improvement in the areas of:

1. **Performance**: Parallel processing can achieve 3-5x speed improvements
2. **Resource Management**: Queue management prevents system overload
3. **User Experience**: Enhanced progress feedback and error reporting
4. **Material Design**: Proper Material 3 components improve visual polish

The recommended solutions maintain clean architecture while delivering substantial benefits. Implementation priority should focus on queue management and parallel processing, as these provide the biggest performance gains with manageable complexity.

Following the phased roadmap outlined in this document will ensure systematic, incremental improvements while maintaining code quality and stability.

---

**Report Generated**: January 25, 2026
**Analyzing Agent**: Sisyphus (OpenCode)
**Version**: Codex 2.2.2
