# OPDS Implementation Analysis Report

**Analysis Date:** January 25, 2026
**App Version:** 2.2.2
**Analysis Scope:** Complete OPDS functionality from source management to book downloading

---

## Executive Summary

Your OPDS implementation is well-structured following clean architecture principles. The codebase demonstrates good separation of concerns with distinct data, domain, and presentation layers. However, there are several areas for improvement across error handling, user experience, performance, and code maintainability.

**Key Findings:**
- ✅ Clean architecture with proper layering
- ✅ Support for both OPDS v1 (XML/Atom) and v2 (JSON)
- ✅ Authentication support for private OPDS catalogs
- ⚠️ Limited error recovery and user feedback
- ⚠️ No caching mechanism for OPDS feeds
- ⚠️ Batch downloads lack individual error handling
- ⚠️ Debug logging left in production code

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [OPDS Source Management](#opds-source-management)
3. [Discover Tab & Navigation Flow](#discover-tab--navigation-flow)
4. [Book Download Flow](#book-download-flow)
5. [Directory Structure & File Organization](#directory-structure--file-organization)
6. [Issues & Improvements](#issues--improvements)
7. [UI/UX Recommendations](#uiux-recommendations)
8. [Backend/Architecture Recommendations](#backendarchitecture-recommendations)
9. [Priority Action Items](#priority-action-items)

---

## Architecture Overview

### Layer Structure

```
presentation/
├── browse/opds/                    # OPDS catalog UI
│   ├── OpdsCatalogContent.kt        # Main catalog browser
│   ├── OpdsBookPreview.kt          # Book card component
│   ├── OpdsBookDetailsBottomSheet.kt # Book details modal
│   └── model/
│       ├── OpdsCatalogModel.kt        # ViewModel for catalog
│       └── OpdsCatalogState.kt        # State management
├── settings/browse/opds/            # Settings UI
│   ├── BrowseOpdsOption.kt          # Source management settings
│   └── OpdsCatalogPanel.kt         # Discover tab panel
└── browse/
    ├── OpdsCatalogScreen.kt          # Navigation screens
    └── OpdsAddSourceDialog.kt       # Add/edit source dialog

domain/
├── repository/
│   └── OpdsRepository.kt           # Repository interface
├── use_case/opds/
│   ├── ImportOpdsBookUseCase.kt    # Download logic
│   ├── RefreshBookMetadataFromOpds.kt
│   └── RefreshAllBooksFromOpdsSource.kt
└── opds/
    ├── OpdsEntry.kt                 # Domain model for entries
    ├── OpdsFeed.kt                  # Domain model for feeds
    └── OpdsLink.kt                  # Domain model for links

data/
├── repository/
│   ├── OpdsRepositoryImpl.kt        # Repository implementation
│   └── OpdsRefreshRepository.kt      # Metadata refresh logic
├── remote/
│   ├── OpdsApiService.kt            # Retrofit API service
│   └── dto/
│       ├── OpdsFeedDto.kt           # v1 DTOs
│       ├── OpdsEntryDto.kt
│       ├── OpdsLinkDto.kt
│       └── opds2/                    # v2 DTOs
├── local/
│   ├── dto/
│   │   └── OpdsSourceEntity.kt      # Room entity
│   └── room/
│       └── OpdsSourceDao.kt          # Database access
└── mapper/
    └── opds/
        └── OpdsMetadataMapper.kt      # Domain mapping
```

### Data Flow

```
User Action
    ↓
UI (OpdsCatalogModel)
    ↓
Use Case (ImportOpdsBookUseCase)
    ↓
Repository (OpdsRepository)
    ↓
Network Layer (Retrofit + OkHttp)
    ↓
DTO Mapping (OpdsFeedDto → OpdsFeed)
    ↓
Domain Model (OpdsEntry)
    ↓
Metadata Mapping (OpdsMetadataMapper)
    ↓
Book Model (BookWithCover)
    ↓
Database (BookDao) + File System (SAF)
```

---

## OPDS Source Management

### Current Implementation

#### Data Model

**File:** `data/local/dto/OpdsSourceEntity.kt`

```kotlin
@Entity
data class OpdsSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,      // ⚠️ Stored in plain text
    val enabled: Boolean = true,
    val lastSync: Long = 0,
    val status: OpdsSourceStatus = OpdsSourceStatus.UNKNOWN
)

enum class OpdsSourceStatus {
    UNKNOWN,          // Not tested
    CONNECTING,       // Testing connection
    CONNECTED,        // Successful connection
    AUTH_FAILED,      // 401/unauthorized
    CONNECTION_FAILED, // Network/other errors
    DISABLED          // Explicitly disabled
}
```

**Security Issue:** Passwords are stored in plain text in Room database. This should use Android Keystore.

#### Database Operations

**File:** `data/local/room/OpdsSourceDao.kt`

```kotlin
@Dao
interface OpdsSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpdsSource(opdsSource: OpdsSourceEntity): Long

    @Update
    suspend fun updateOpdsSource(opdsSource: OpdsSourceEntity)

    @Upsert
    suspend fun upsertOpdsSource(opdsSource: OpdsSourceEntity)

    @Delete
    suspend fun deleteOpdsSource(opdsSource: OpdsSourceEntity)

    @Query("SELECT * FROM OpdsSourceEntity")
    suspend fun getAllOpdsSources(): List<OpdsSourceEntity>

    @Query("DELETE FROM OpdsSourceEntity WHERE id = :id")
    suspend fun deleteOpdsSourceById(id: Int)
}
```

#### Connection Testing Logic

**File:** `ui/settings/opds/OpdsSourcesModel.kt`

The app implements intelligent URL variation testing when adding sources:

```kotlin
suspend fun testConnection(url: String, username: String?, password: String?): String {
    val urlVariations = generateUrlVariations(url)

    for (testUrl in urlVariations) {
        try {
            opdsRepository.fetchFeed(testUrl, username, password)
            return testUrl // Return working URL
        } catch (e: Exception) {
            continue // Try next variation
        }
    }
    opdsRepository.fetchFeed(url, username, password) // Original as fallback
    return url
}

private fun generateUrlVariations(originalUrl: String): List<String> {
    // Attempts: https://domain/opds, https://domain, http://domain/opds, http://domain
    // Helpful when users forget to add /opds or protocol
}
```

**Issue:** Connection errors provide minimal feedback to users. The error message is generic.

#### Settings UI

**File:** `presentation/settings/browse/opds/BrowseOpdsOption.kt`

Features:
- Add/Edit/Delete OPDS sources
- View connection status with color-coded indicators
- Test connection before adding
- Refresh metadata for all books from a source

```kotlin
// Status colors
OpdsSourceStatus.CONNECTED -> MaterialTheme.colorScheme.primary
OpdsSourceStatus.AUTH_FAILED -> MaterialTheme.colorScheme.error
OpdsSourceStatus.CONNECTION_FAILED -> MaterialTheme.colorScheme.error
```

---

## Discover Tab & Navigation Flow

### Discover Tab Implementation

**File:** `presentation/browse/opds/OpdsCatalogPanel.kt`

The Discover tab shows:
1. Empty state with "Add OPDS Source" button when no sources configured
2. List of configured OPDS sources when sources exist
3. Each source shows name, URL, and authentication status

```kotlin
if (sourcesState.sources.isEmpty()) {
    // Empty state with helpful message
    Column {
        Text("No OPDS sources configured")
        Text("Add an OPDS source to start browsing...")
        OutlinedButton({ showAddSourceDialog = true }) {
            Text("Add OPDS Source")
        }
    }
} else {
    LazyColumn {
        items(sourcesState.sources) { source ->
            OpdsSourceItem(source) { onNavigateToOpdsCatalog(OpdsRootScreen(source)) }
        }
    }
}
```

### Navigation Flow

**Navigation Screens:** `ui/browse/OpdsCatalogScreen.kt`

```kotlin
abstract class BaseOpdsCatalogScreen(
    open val source: OpdsSourceEntity,
    open val url: String? = null,      // Category URL
    open val title: String? = null       // Category title
)

@Parcelize
data class OpdsRootScreen(override val source: OpdsSourceEntity) : BaseOpdsCatalogScreen(source)

@Parcelize
data class OpdsCategoryScreen(
    override val source: OpdsSourceEntity,
    override val url: String,
    override val title: String
) : BaseOpdsCatalogScreen(source, url, title)
```

**Flow:**
```
Discover Tab
    ↓ (tap source)
OpdsRootScreen (source root feed)
    ↓ (tap category)
OpdsCategoryScreen (category feed)
    ↓ (tap subcategory)
OpdsCategoryScreen (subcategory feed)
    ↓ (tap book)
OpdsBookDetailsBottomSheet
    ↓ (download)
ImportOpdsBookUseCase
```

### Category/Book Detection Logic

**File:** `ui/browse/opds/OpdsCatalogContent.kt`

The app uses sophisticated logic to distinguish categories from books:

```kotlin
val isCategory = hasNavigationLinks && !hasAcquisitionLinks && !hasSelfReferencingLinks

val hasNavigationLinks = entry.links.any { link ->
    link.rel == "subsection" ||
    link.rel == "http://opds-spec.org/subsection" ||
    link.type?.startsWith("application/atom+xml") == true ||
    (link.rel != null && link.rel != "http://opds-spec.org/acquisition" &&
     link.rel != "http://opds-spec.org/image/thumbnail" &&
     link.rel != "self" && link.rel != "alternate")
}

val hasAcquisitionLinks = entry.links.any { it.rel == "http://opds-spec.org/acquisition" }
```

**Issue:** This logic is complex and may fail with non-standard OPDS feeds. Debug logging is left in production code.

### Feed Parsing

**OPDS v1 (XML/Atom):** `data/repository/OpdsRepositoryImpl.kt`

Uses SimpleXML with Retrofit:

```kotlin
private suspend fun parseOpdsV1(url: String, username: String?, password: String?): OpdsFeed {
    val client = createOkHttpClient(username, password)
    val service = retrofitWithAuth.create(OpdsApiService::class.java)
    val dto = service.getFeed(url)
    return mapV1ToDomain(dto)
}
```

**OPDS v2 (JSON):** Auto-detects JSON content:

```kotlin
override suspend fun fetchFeed(url: String, username: String?, password: String?): OpdsFeed {
    val response = client.newCall(request).execute()
    val contentType = response.header("Content-Type") ?: ""

    when {
        isOpdsV2ContentType(contentType) || isOpdsV2Content(body) -> {
            parseOpdsV2(body)
        }
        else -> parseOpdsV1(url, username, password)
    }
}
```

**Good:** Automatic format detection reduces user friction.

---

## Book Download Flow

### Download Process

**File:** `domain/use_case/opds/ImportOpdsBookUseCase.kt`

```kotlin
suspend operator fun invoke(
    opdsEntry: OpdsEntry,
    sourceUrl: String,
    username: String?,
    password: String?,
    onProgress: ((Float) -> Unit)?
): BookWithCover?
```

**Download Steps:**

1. **Find acquisition link** (first with `rel="http://opds-spec.org/acquisition"`)
2. **Resolve URL** (relative → absolute)
3. **Download file** via OkHttp with progress callback
4. **Determine extension** from MIME type, URL, or Content-Disposition header
5. **Parse file** using appropriate parser (EPUB directly, others via FileParser)
6. **Create book folder** under `codex/downloads/UUID_Title/`
7. **Save book file** with sanitized filename
8. **Download cover image** (if available in OPDS feed)
9. **Apply OPDS metadata** to book
10. **Generate metadata.opf** file
11. **Insert into database**

### Progress Tracking

**UI State:** `ui/browse/opds/model/OpdsCatalogState.kt`

```kotlin
data class OpdsCatalogState(
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,        // 0.0 to 1.0
    val downloadError: String? = null
)
```

**Progress Display:**

```kotlin
// Single book
LinearProgressIndicator(progress = { state.downloadProgress })
Text("Downloading book... ${(state.downloadProgress * 100).toInt()}%")

// Batch download
val currentBookIndex = (state.downloadProgress * state.selectedBooks.size).toInt()
Text("Downloading books... $currentBookIndex/${state.selectedBooks.size}")
```

### File Naming

**Sanitization Logic:**

```kotlin
private fun sanitizeFilename(filename: String): String {
    return filename
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("[\\x00-\\x1F]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(200) // Length limit
}
```

**Folder Structure:**

```
codex/
└── downloads/
    ├── {uuid}_Book Title - Author/
    │   ├── Book Title - Author.epub
    │   ├── cover.jpg
    │   └── metadata.opf
    ├── {uuid}_Another Book/
    │   ├── Another Book.pdf
    │   ├── cover.jpg
    │   └── metadata.opf
```

**UUID Extraction:**

```kotlin
private fun extractUuid(opdsEntry: OpdsEntry): String? {
    for (identifier in opdsEntry.identifiers) {
        if (identifier.startsWith("urn:uuid:", ignoreCase = true)) {
            return identifier.removePrefix("urn:uuid:").take(8)
        }
    }
    // Fallback: use entry ID if it looks like a UUID
    if (opdsEntry.id.matches(Regex("[0-9a-fA-F]{8}-.*"))) {
        return opdsEntry.id.take(8)
    }
    return null // Generates random UUID
}
```

### Batch Download

**Feature:** Users can select multiple books and download them all

```kotlin
fun downloadSelectedBooks(source: OpdsSourceEntity, onComplete: () -> Unit) {
    val selectedEntries = _state.value.feed?.entries?.filter { it.id in _state.value.selectedBooks }

    viewModelScope.launch {
        var completed = 0
        val total = selectedEntries.size

        for (entry in selectedEntries) {
            try {
                val bookWithCover = importOpdsBookUseCase(...)
                bookRepository.insertBook(bookWithCover)
            } catch (e: Exception) {
                // ⚠️ Error is logged but continues
                completed++
            }
            completed++
            _state.value = _state.value.copy(downloadProgress = completed / total)
        }
        onComplete()
    }
}
```

**Issue:** Batch downloads continue even if individual books fail. Users aren't told which books succeeded/failed.

### Cover Image Handling

**Source Priority:**
1. `rel="http://opds-spec.org/image"` (full size)
2. `rel="http://opds-spec.org/image/thumbnail"` (thumbnail)
3. Any `type="image/*"` link

**Auth:** Cover images are loaded with the same credentials as the feed.

---

## Directory Structure & File Organization

### Codex Directory Manager

**Interface:** `domain/storage/CodexDirectoryManager.kt`

```kotlin
interface CodexDirectoryManager {
    suspend fun getCodexRootUri(): Uri?
    suspend fun setCodexRootUri(uri: Uri): Boolean
    suspend fun isConfigured(): Boolean
    suspend fun getDownloadsDir(): DocumentFile?
    suspend fun createBookFolder(folderName: String): DocumentFile?
}
```

**Storage:** Uses Android Storage Access Framework (SAF) with persistable permissions

**Structure:**
```
/codex/                    # User-selected root (SAF URI)
├── downloads/              # OPDS books (per-book folders)
│   ├── {uuid}_Title/
│   │   ├── book.epub
│   │   ├── cover.jpg
│   │   └── metadata.opf
└── backups/                # Timestamped backup files
    └── codex-backup-*.json
```

### Book Entity

**File:** `domain/library/book/Book.kt`

```kotlin
@Parcelize
data class Book(
    val id: Int = 0,
    val title: String,
    val authors: List<String> = emptyList(),
    val description: String?,
    val filePath: String,
    val coverImage: Uri?,
    // ... progress, category, tags, etc.
    val source: BookSource = BookSource.LOCAL,      // LOCAL or OPDS
    val opdsSourceUrl: String? = null,             # Tracking
    val opdsSourceId: Int? = null,
    val metadataLastRefreshTime: Long? = null,       # For refresh
    val uuid: String? = null,                      # OPDS identifier
    val isbn: String? = null,                      # OPDS identifier
    // ... comic fields, etc.
)
```

---

## Issues & Improvements

### Critical Issues

#### 1. **Security: Passwords Stored in Plain Text** 🔴

**Severity:** Critical
**Location:** `OpdsSourceEntity.password`

**Issue:** OPDS source passwords are stored in plain text in Room database.

**Recommendation:**
```kotlin
// Use Android EncryptedSharedPreferences or Keystore
@Entity
data class OpdsSourceEntity(
    // ...
    val encryptedPassword: String? = null, // Encrypt before storing
    // ...
)

class PasswordEncryption {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "opds_credentials",
        masterKey
    )

    fun encryptPassword(password: String): String {
        // Use Keystore-based encryption
    }

    fun decryptPassword(encrypted: String): String {
        // Use Keystore-based decryption
    }
}
```

#### 2. **No OPDS Feed Caching** 🟠

**Severity:** Medium
**Impact:** Repeated network requests for same feeds, poor performance

**Issue:** Every navigation reloads feeds from the network. No local caching.

**Recommendation:**
```kotlin
@Entity
data class OpdsCacheEntity(
    @PrimaryKey val url: String,
    val feedJson: String,      // Serialized OpdsFeed
    val timestamp: Long,
    val ttl: Long              // Time-to-live
)

class CachedOpdsRepository @Inject constructor(
    private val opdsRepository: OpdsRepository,
    private val cacheDao: OpdsCacheDao
) : OpdsRepository {

    override suspend fun fetchFeed(url: String, username: String?, password: String?): OpdsFeed {
        val cacheKey = "${url}_${username ?: "anonymous"}"

        // Check cache first
        val cached = cacheDao.getCache(cacheKey)
        if (cached != null && System.currentTimeMillis() < cached.timestamp + cached.ttl) {
            return Json.decodeFromString<OpdsFeed>(cached.feedJson)
        }

        // Cache miss - fetch from network
        val feed = opdsRepository.fetchFeed(url, username, password)

        // Store in cache (15 minute TTL)
        cacheDao.upsertCache(
            OpdsCacheEntity(
                url = cacheKey,
                feedJson = Json.encodeToString(feed),
                timestamp = System.currentTimeMillis(),
                ttl = 15 * 60 * 1000
            )
        )

        return feed
    }
}
```

#### 3. **Debug Logging in Production Code** 🟡

**Severity:** Low
**Location:** Multiple files

**Issue:** Extensive debug logging with `android.util.Log.d("OPDS_DEBUG", ...)` is left in production builds.

**Example Locations:**
- `OpdsRepositoryImpl.kt` (lines 52, 60, 71, 77, 109, etc.)
- `OpdsCatalogModel.kt` (lines 34, 51, 62, 96, etc.)
- `ImportOpdsBookUseCase.kt` (lines 197, 204, 219, etc.)

**Recommendation:**
```kotlin
// BuildConfig approach
object OpdsLogger {
    private const val TAG = "OPDS"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message, throwable)
        }
    }
}

// Usage
OpdsLogger.d("Fetching OPDS feed from URL: $url")
```

### Medium Priority Issues

#### 4. **Batch Download Error Handling**

**Issue:** When downloading multiple books, individual failures don't stop the batch, but users have no visibility into which books succeeded/failed.

**Current Code:**
```kotlin
for (entry in selectedEntries) {
    try {
        val bookWithCover = importOpdsBookUseCase(...)
        bookRepository.insertBook(bookWithCover)
    } catch (e: Exception) {
        println("Failed to download book ${entry.title}: ${e.message}")
        // Continues to next book
    }
    completed++
}
```

**Recommendation:**
```kotlin
data class BatchDownloadResult(
    val succeeded: List<OpdsEntry>,
    val failed: List<Pair<OpdsEntry, String>>  // entry + error message
)

suspend fun downloadSelectedBooks(...): BatchDownloadResult {
    val succeeded = mutableListOf<OpdsEntry>()
    val failed = mutableListOf<Pair<OpdsEntry, String>>()

    for (entry in selectedEntries) {
        try {
            val bookWithCover = importOpdsBookUseCase(...)
            bookRepository.insertBook(bookWithCover)
            succeeded.add(entry)
        } catch (e: Exception) {
            failed.add(entry to (e.message ?: "Unknown error"))
        }
    }

    return BatchDownloadResult(succeeded, failed)
}

// Show results dialog
if (result.failed.isNotEmpty()) {
    AlertDialog {
        Text("${result.failed.size} books failed to download")
        LazyColumn {
            items(result.failed) { (entry, error) ->
                Text("${entry.title}: $error")
            }
        }
    }
}
```

#### 5. **Generic Error Messages**

**Issue:** Network errors provide minimal context to users.

**Current:**
```kotlin
} catch (e: Exception) {
    throw Exception("Download failed: HTTP ${response.code} ${response.message}")
}
```

**Recommendation:**
```kotlin
sealed class OpdsError : Exception {
    class NetworkError(val code: Int, val message: String) : OpdsError()
    class AuthenticationFailed : OpdsError()
    class InvalidFeedFormat : OpdsError()
    class DownloadTimeout : OpdsError()
    class FileWriteFailed : OpdsError()
    class CodexDirectoryNotConfigured : OpdsError()
}

class OpdsErrorHandler {
    fun getUserMessage(error: OpdsError): UIText {
        return when (error) {
            is OpdsError.NetworkError ->
                UIText.Plain("Connection failed (${error.code}: ${error.message}). Check your internet and try again.")
            is OpdsError.AuthenticationFailed ->
                UIText.Plain("Authentication failed. Check your username and password.")
            is OpdsError.DownloadTimeout ->
                UIText.Plain("Download timed out. The server may be slow or unavailable.")
            is OpdsError.CodexDirectoryNotConfigured ->
                UIText.Plain("Please configure your Codex directory first.")
            else ->
                UIText.Plain("An error occurred. Please try again.")
        }
    }
}
```

#### 6. **No Download Pause/Cancel**

**Issue:** Once a download starts (especially batch downloads), users cannot cancel it. Only option is force-killing the app.

**Recommendation:**
```kotlin
data class OpdsCatalogState(
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isDownloadPaused: Boolean = false,
    val downloadCancellationJob: Job? = null  // For cancellation
)

fun cancelDownload() {
    _state.value.downloadCancellationJob?.cancel()
    _state.value = _state.value.copy(
        isDownloading = false,
        downloadCancellationJob = null
    )
}

// UI
Button(
    onClick = { model.cancelDownload() },
    enabled = state.isDownloading
) {
    Text("Cancel Download")
}
```

#### 7. **Pagination Not Implemented for Category Feeds**

**Issue:** Book feeds with `rel="next"` links have "Load More" button, but category feeds don't paginate.

**Current:** Only book feeds check for `next` link.

**Recommendation:** Add pagination to all feeds with `next` links, not just book feeds.

---

## UI/UX Recommendations

### 1. **Add Download Queue Indicator**

**Problem:** Users cannot see ongoing downloads from other screens.

**Solution:**
```kotlin
// Global download manager
@Composable
fun DownloadQueueIndicator() {
    val activeDownloads = downloadManager.getActiveDownloads().collectAsState()

    if (activeDownloads.value.isNotEmpty()) {
        IconButton(
            onClick = { navigator.push(DownloadQueueScreen) }
        ) {
            Badge(
                content = { Text("${activeDownloads.value.size}") }
            ) {
                Icon(Icons.Default.Download, "Downloads")
            }
        }
    }
}
```

### 2. **Format Selection in Book Preview**

**Current:** OPDS books may have multiple formats (EPUB, PDF, MOBI), but UI only shows "Available formats" as read-only chips.

**Recommendation:** Allow users to select which format to download:

```kotlin
// In OpdsBookDetailsBottomSheet
val selectedFormat = remember { mutableStateOf(acquisitionLinks.first()) }

FlowRow {
    acquisitionLinks.forEach { link ->
        FilterChip(
            selected = selectedFormat.value == link,
            onClick = { selectedFormat.value = link },
            label = { Text(getFormatFromType(link.type)) }
        )
    }
}

Button(
    onClick = {
        model.downloadBook(entry, source, selectedFormat.value)
    }
) {
    Text("Download ${getFormatFromType(selectedFormat.value.type)}")
}
```

### 3. **Pull-to-Refresh for Feeds**

**Current:** No way to refresh feeds without navigating away and back.

**Recommendation:**
```kotlin
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpdsCatalogContent(...) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { model.loadFeed(source, url) }
    )

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        // Existing content
    }
}
```

### 4. **Better Empty States**

**Current:** Generic empty states.

**Recommendation:**
```kotlin
// Empty feed state
Column {
    Icon(Icons.Default.LibraryBooks, size = 64.dp)
    Text("No books found")
    if (searchQuery.isNotBlank()) {
        Text("Try a different search term or clear your search")
    } else {
        Text("This category is empty")
    }
}
```

### 5. **Book Status Indicators**

**Current:** No way to tell if a book is already in the library.

**Recommendation:**
```kotlin
@Composable
fun OpdsBookPreview(...) {
    val existingBook = remember(entry) {
        bookRepository.getBookByUuid(entry.uuid)
    }

    Box {
        // Existing content
        if (existingBook != null) {
            Icon(
                Icons.Default.CheckCircle,
                tint = Color.Green,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}
```

### 6. **Search History**

**Current:** No search history or saved searches.

**Recommendation:**
```kotlin
@Entity
data class SearchHistoryEntity(
    @PrimaryKey val id: Int = 0,
    val query: String,
    val timestamp: Long
)

@Composable
fun SearchWithHistory(...) {
    val history = searchHistoryDao.getRecentSearches().collectAsState()
    var showHistory by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = query,
        onValueChange = {
            query = it
            showHistory = it.isNotBlank() && history.isNotEmpty()
        },
        // ...
    )

    if (showHistory) {
        LazyColumn {
            items(history) { item ->
                Text(item.query, onClick = {
                    query = item.query
                    model.search(item.query, source)
                    showHistory = false
                })
            }
        }
    }
}
```

---

## Backend/Architecture Recommendations

### 1. **Use Coroutines Flow for Downloads**

**Current:** Downloads use `suspend` functions with callbacks for progress.

**Recommendation:**
```kotlin
// Repository
override fun downloadBook(
    url: String,
    username: String?,
    password: String?
): Flow<DownloadProgress> = flow {
    val client = createOkHttpClient(username, password)

    // Start download
    val response = client.newCall(request).execute()
    val body = response.body ?: throw Exception("Empty response")

    var bytesRead = 0L
    val source = body.source()
    val buffer = Buffer()

    while (true) {
        val read = source.read(buffer, 8192)
        if (read == -1L) break

        bytesRead += read

        // Emit progress
        if (body.contentLength() > 0) {
            emit(DownloadProgress.Progress(bytesRead / body.contentLength()))
        }
    }

    emit(DownloadProgress.Complete(buffer.readByteArray()))
}
```

### 2. **Repository Caching Layer**

As mentioned in Issue #2, add caching at repository level.

### 3. **Separate Download Service**

**Current:** Downloads run in ViewModel scope - killed when screen is destroyed.

**Recommendation:**
```kotlin
// Foreground service for reliable downloads
class OpdsDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startDownload(entry: OpdsEntry, source: OpdsSourceEntity) {
        scope.launch {
            // Download logic
            // Update notification
            // Show success/failure notification
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
```

### 4. **Retry Logic**

**Current:** No automatic retry on transient failures.

**Recommendation:**
```kotlin
class RetryableOpdsRepository @Inject constructor(
    private val opdsRepository: OpdsRepository
) : OpdsRepository {

    private val maxRetries = 3
    private val retryDelayMs = 1000L

    override suspend fun fetchFeed(url: String, username: String?, password: String?): OpdsFeed {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return opdsRepository.fetchFeed(url, username, password)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(retryDelayMs * (attempt + 1))  // Exponential backoff
                }
            }
        }

        throw lastException ?: Exception("Max retries exceeded")
    }
}
```

### 5. **Metadata Refresh Improvements**

**Current:** Refresh logic is good but doesn't handle partial updates.

**Recommendation:**
```kotlin
// Add incremental refresh
suspend fun refreshBookMetadata(
    book: Book,
    opdsEntryFinder: suspend (String?, String?) -> OpdsEntry?,
    force: Boolean = false
): Book? {
    // Skip refresh if within TTL (24 hours)
    if (!force && book.metadataLastRefreshTime != null) {
        val age = System.currentTimeMillis() - book.metadataLastRefreshTime
        if (age < 24 * 60 * 60 * 1000) {
            return book // Skip - still fresh
        }
    }

    // Perform refresh
    val opdsEntry = opdsEntryFinder(book.uuid, book.isbn)
    if (opdsEntry != null) {
        return mergeOpdsMetadata(book, opdsEntry)
    }
    return null
}
```

### 6. **Dependency Injection Improvements**

**Current:** Good Hilt usage.

**Recommendation:** Consider using qualifiers for different OPDS implementations:

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpdsV1

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpdsV2

@Module
@InstallIn(SingletonComponent::class)
object OpdsModule {
    @Provides
    @OpdsV1
    fun provideOpdsV1Parser(): OpdsV1Parser = OpdsV1Parser()

    @Provides
    @OpdsV2
    fun provideOpdsV2Parser(): OpdsV2Parser = OpdsV2Parser()
}
```

---

## Priority Action Items

### 🔴 Critical (Do First)

1. **Encrypt OPDS passwords** using Android Keystore
2. **Remove debug logging** from production builds

### 🟠 High Priority

3. **Implement OPDS feed caching** with 15-minute TTL
4. **Add proper error handling** with user-friendly messages
5. **Improve batch download error reporting**

### 🟡 Medium Priority

6. **Add download queue** with pause/cancel support
7. **Add pull-to-refresh** for feeds
8. **Show book status** (already in library)
9. **Implement retry logic** for transient failures
10. **Add download foreground service** for reliability

### 🟢 Low Priority

11. **Add search history** for OPDS searches
12. **Add format selection** in book preview
13. **Improve empty states** with helpful actions
14. **Add pagination** to category feeds
15. **Implement incremental metadata refresh**

---

## Code Quality Assessment

### Strengths ✅

1. **Clean Architecture:** Clear separation of data, domain, and presentation layers
2. **Repository Pattern:** Proper abstraction with interface + implementation
3. **Use Cases:** Single responsibility principle for business logic
4. **Type Safety:** Good use of sealed classes for results
5. **Material 3:** Modern UI components following Material Design guidelines
6. **Hilt DI:** Proper dependency injection setup
7. **OPDS v1 + v2:** Support for both XML and JSON feeds
8. **Authentication:** Support for username/password authentication
9. **URL Variation Testing:** Helpful for users who forget /opds

### Weaknesses ⚠️

1. **Security:** Passwords stored in plain text
2. **No Caching:** Every request hits the network
3. **Debug Logging:** Extensive logs left in production
4. **Error Recovery:** Limited retry and recovery mechanisms
5. **Batch Handling:** Poor error visibility for batch operations
6. **Missing Features:** No pause/cancel, no queue, no history
7. **Testing:** No unit tests visible for OPDS components

---

## Additional Notes

### File Format Support

The app supports downloading and parsing:
- EPUB (.epub)
- PDF (.pdf)
- FB2 (.fb2)
- HTML (.html, .htm)
- TXT (.txt)
- Comic books (.cbz, .cbr, .cb7)

### Metadata Fields Preserved

From OPDS feeds, the following metadata is mapped to books:
- Title
- Authors
- Description/Summary
- Publisher
- Publication date
- Language
- Categories/Tags
- Series name and index
- UUID/ISBN identifiers

### Network Configuration

- HTTP client: OkHttp with logging interceptor
- HTTP logging: Full BODY logging enabled (remove for production)
- Timeout: Uses OkHttp defaults
- User Agent: Not explicitly set
- Accept headers: `application/atom+xml, application/opds+json, application/xml, text/xml, */*`
- Charset: UTF-8 explicitly requested

---

## Conclusion

Your OPDS implementation provides solid functionality with a clean architecture. The core flow of adding sources, browsing catalogs, and downloading books works well. However, there are opportunities to improve:

1. **Security** - Password encryption is essential
2. **Performance** - Caching will dramatically improve perceived speed
3. **Reliability** - Retry logic and error recovery
4. **User Experience** - Better feedback, queue management, format selection

The recommended improvements are organized by priority to help you tackle the most impactful changes first. The critical security issue should be addressed immediately, while the other improvements can be implemented incrementally.

---

## Appendix: Key Files Reference

| File | Purpose | Complexity |
|------|-----------|------------|
| `OpdsRepositoryImpl.kt` | Network layer, feed parsing | High |
| `ImportOpdsBookUseCase.kt` | Download business logic | High |
| `OpdsCatalogModel.kt` | Catalog UI state | Medium |
| `OpdsCatalogContent.kt` | Catalog UI rendering | High |
| `OpdsBookDetailsBottomSheet.kt` | Book details modal | Medium |
| `OpdsSourcesModel.kt` | Source management | Medium |
| `OpdsRefreshRepository.kt` | Metadata refresh | Medium |
| `OpdsMetadataMapper.kt` | Domain mapping | Low |

---

**End of Report**
