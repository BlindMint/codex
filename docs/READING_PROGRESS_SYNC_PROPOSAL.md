# Reading Progress Sync Proposal for Codex

**Created:** 2026-02-20  
**Branch:** `feature/reading-progress-sync-proposal`  
**Status:** Draft - Awaiting Approval

---

## Executive Summary

This document outlines multiple approaches for implementing reading progress synchronization between devices in Codex. Options range from minimal-effort file-based solutions to comprehensive cloud sync with user accounts.

---

## Table of Contents

1. [Current Architecture Analysis](#1-current-architecture-analysis)
2. [Data to Synchronize](#2-data-to-synchronize)
3. [Book Matching Strategy](#3-book-matching-strategy)
4. [Implementation Options](#4-implementation-options)
   - [Option A: Manual Export/Import](#option-a-manual-exportimport)
   - [Option B: Syncthing Integration](#option-b-syncthing-integration)
   - [Option C: Google Drive Sync](#option-c-google-drive-sync)
   - [Option D: Dropbox Sync](#option-d-dropbox-sync)
   - [Option E: Self-Hosted REST API](#option-e-self-hosted-rest-api)
   - [Option F: Firebase Firestore](#option-f-firebase-firestore)
   - [Option G: Custom Backend Service](#option-g-custom-backend-service)
   - [Option H: Cloud-Synced Folder (Passive Sync)](#option-h-cloud-synced-folder-passive-sync)
5. [Recommended Approaches](#5-recommended-approaches)
6. [Complete Architecture Rework Options](#6-complete-architecture-rework-options)
7. [Security & Privacy Considerations](#7-security--privacy-considerations)
8. [Implementation Timeline Estimates](#8-implementation-timeline-estimates)

---

## 1. Current Architecture Analysis

### Existing Data Layer

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Room Database** | v2.7.1 | Local book metadata, progress, bookmarks |
| **DataStore** | Preferences | User settings |
| **Retrofit** | v2.9.0 | OPDS catalog browsing (already available) |
| **Hilt** | v2.55 | Dependency injection |

### Progress Tracking Entities

```
BookEntity
├── scrollIndex: Int          (chapter/section index)
├── scrollOffset: Int         (position within section)
├── progress: Float           (0.0 - 1.0 percentage)
├── speedReaderWordIndex: Int (speed reading position)
├── currentPage: Int          (comic/PDF page)
└── lastPageRead: Int         (last viewed page)

BookmarkEntity
├── bookId: Int
├── scrollIndex: Int
├── scrollOffset: Int
├── timestamp: Long
├── selectedText: String
├── customName: String
└── pageNumber: Int

HistoryEntity
├── bookId: Int
└── time: Long               (last opened timestamp)

BookProgressHistoryEntity
├── filePath: String          (book identifier)
├── scrollIndex: Int
├── scrollOffset: Int
├── progress: Float
└── lastModified: Long
```

### Current Strengths for Sync Implementation

- **Clean Architecture**: Clear separation between data/domain/presentation layers
- **Repository Pattern**: Already abstracted - easy to add sync logic
- **Hilt DI**: Can inject sync services cleanly
- **Retrofit Present**: Network infrastructure partially available
- **UUID/ISBN Fields**: Books have unique identifiers for matching

### Current Limitations

- **No User Authentication**: No account system exists
- **File-Path Dependent**: Progress keyed by local file paths (won't match across devices)
- **No Cloud Infrastructure**: All data is local-only
- **No Sync Conflict Resolution**: No last-write-wins or merge logic

---

## 2. Data to Synchronize

### Essential Sync Data (Small Payload)

| Data | Size Estimate | Priority |
|------|---------------|----------|
| Book progress (scrollIndex, scrollOffset, progress) | ~24 bytes/book | **Critical** |
| Comic progress (currentPage, lastPageRead) | ~8 bytes/book | **Critical** |
| Speed reader progress (wordIndex) | ~4 bytes/book | High |
| Last opened timestamp | ~8 bytes/book | High |
| **Total per book** | **~44 bytes** | |

### Extended Sync Data (Optional)

| Data | Size Estimate | Priority |
|------|---------------|----------|
| Bookmarks | ~50-200 bytes/bookmark | Medium |
| Reading history | ~16 bytes/entry | Medium |
| User settings (DataStore) | ~5-10 KB total | Low |
| Custom color presets | ~200 bytes/preset | Low |

### Data to NOT Sync

- Book files themselves (too large, user manages)
- Cover images (regenerable from files)
- Book metadata (can be re-parsed from files)

---

## 3. Book Matching Strategy

The core challenge: How do we know that "Book X on Device A" is the same as "Book X on Device B"?

### Matching Hierarchy (Priority Order)

```kotlin
fun matchBooks(local: Book, remote: SyncData): MatchConfidence {
    // 1. UUID match (most reliable - from EPUB metadata)
    if (local.uuid != null && remote.uuid == local.uuid) {
        return MatchConfidence.EXACT
    }
    
    // 2. ISBN match (reliable for published books)
    if (local.isbn != null && remote.isbn == local.isbn) {
        return MatchConfidence.EXACT
    }
    
    // 3. Calibre ID match (for Calibre-managed libraries)
    if (local.opdsCalibreId != null && remote.calibreId == local.opdsCalibreId) {
        return MatchConfidence.EXACT
    }
    
    // 4. Title + Author fuzzy match (fallback)
    val titleMatch = fuzzyMatch(local.title, remote.title) > 0.9
    val authorMatch = local.authors.any { a -> 
        remote.authors.any { ra -> fuzzyMatch(a, ra) > 0.85 }
    }
    if (titleMatch && authorMatch) {
        return MatchConfidence.LIKELY
    }
    
    // 5. File hash match (for identical files)
    // Requires computing hash on first sync - expensive but definitive
    
    return MatchConfidence.NONE
}
```

### Proposed Sync Identifier

Add a new `syncId` field that combines multiple identifiers:

```kotlin
data class BookSyncIdentifier(
    val uuid: String?,
    val isbn: String?,
    val title: String,
    val authors: List<String>,
    val fileHash: String?, // SHA-256 of first 1MB (computed lazily)
    val fileSize: Long,
    val calibreId: String?
) {
    fun toUniqueId(): String {
        // Create deterministic ID from available identifiers
        return uuid ?: isbn ?: calibreId ?: generateHashFromMetadata()
    }
}
```

---

## 4. Implementation Options

### Option A: Manual Export/Import

**Description:** Users manually export sync data to a file, transfer it to another device, and import.

**Architecture:**

```
┌─────────────────┐
│  Device A       │
│  ┌───────────┐  │     ┌──────────────┐
│  │  Codex    │  │────▶│ sync.json    │
│  │  Database │  │     │ (encrypted)  │
│  └───────────┘  │     └──────────────┘
└─────────────────┘            │
                               ▼
┌─────────────────┐     ┌──────────────┐
│  Device B       │◀────│ User transfers│
│  ┌───────────┐  │     │ via cloud/USB │
│  │  Codex    │  │     └──────────────┘
│  │  Database │  │
│  └───────────┘  │
└─────────────────┘
```

**Implementation:**

```kotlin
// New domain model
data class SyncExport(
    val version: Int = 1,
    val exportDate: Long,
    val deviceId: String,
    val progress: List<BookProgressSync>,
    val bookmarks: List<BookmarkSync>,
    val settings: Map<String, Any>?
)

data class BookProgressSync(
    val identifier: BookSyncIdentifier,
    val scrollIndex: Int,
    val scrollOffset: Int,
    val progress: Float,
    val currentPage: Int,
    val speedReaderWordIndex: Int,
    val lastModified: Long
)
```

**New Files Required:**
- `domain/model/SyncExport.kt`
- `domain/model/BookSyncIdentifier.kt`
- `domain/use_case/sync/ExportSyncData.kt`
- `domain/use_case/sync/ImportSyncData.kt`
- `domain/repository/SyncRepository.kt`
- `data/repository/SyncRepositoryImpl.kt`
- `data/sync/SyncDataMapper.kt`
- `presentation/settings/sync/SyncSettingsScreen.kt`

**Pros:**
- Zero infrastructure cost
- Works offline
- Maximum privacy (data never leaves user's control)
- Simple implementation
- Fits existing architecture perfectly

**Cons:**
- Manual process (not automatic)
- No real-time sync
- User must remember to export/import
- Potential for data conflicts if both devices modified

**Effort:** ~1-2 weeks

---

### Option B: Syncthing Integration

**Description:** Leverage Syncthing (open-source P2P sync) to automatically sync a small database file between devices.

**Architecture:**

```
┌─────────────────────────────────────────────────────┐
│                    Syncthing                         │
│  ┌─────────────────────────────────────────────┐    │
│  │  P2P encrypted sync folder                   │    │
│  │  /Codex/sync/                                │    │
│  │  ├── progress.db (Room DB copy)              │    │
│  │  └── bookmarks.db                            │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
         ▲                          ▲
         │                          │
    ┌────┴────┐                ┌────┴────┐
    │ Device A│                │ Device B│
    │ Codex   │                │ Codex   │
    └─────────┘                └─────────┘
```

**Implementation Approaches:**

1. **Intent-based (Simple):** Just open Syncthing app, user configures sync folder
2. **REST API (Advanced):** Use Syncthing's local REST API to configure/monitor

```kotlin
// Syncthing API client
interface SyncthingApi {
    @GET("/rest/db/status")
    suspend fun getFolderStatus(folder: String): FolderStatus
    
    @POST("/rest/db/scan")
    suspend fun requestScan(folder: String)
}

// In Codex
class SyncthingSyncManager(
    private val syncthingApi: SyncthingApi,
    private val progressRepository: ProgressRepository
) {
    suspend fun syncWithSyncthing() {
        // 1. Export current progress to sync folder
        exportProgressToSyncFolder()
        
        // 2. Request Syncthing scan
        syncthingApi.requestScan("codex-sync")
        
        // 3. Wait for sync completion
        waitForSyncCompletion()
        
        // 4. Import merged progress
        importProgressFromSyncFolder()
    }
}
```

**Pros:**
- Automatic background sync
- P2P (no central server)
- Open source
- End-to-end encryption
- Works on local network or over internet
- Handles conflicts

**Cons:**
- Requires Syncthing installed
- Requires user setup
- Android background restrictions may affect reliability
- Need to handle file-based database carefully

**Effort:** ~2-3 weeks

---

### Option C: Google Drive Sync

**Description:** Use Google Drive API to store and sync progress data in the user's Drive.

**Architecture:**

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│  Device A   │         │  Google Drive    │         │  Device B   │
│  Codex      │◀───────▶│  App Data Folder │◀───────▶│  Codex      │
│             │         │  /codex_sync.json│         │             │
└─────────────┘         └──────────────────┘         └─────────────┘
                               │
                        Google Sign-In
                        OAuth 2.0
```

**Dependencies:**
```kotlin
// build.gradle.kts additions
implementation("com.google.apis:google-api-services-drive:v3-rev20231211-2.0.0")
implementation("com.google.auth:google-auth-library-oauth2-http:1.20.0")
// Or use Google Play Services
implementation("com.google.android.gms:play-services-auth:20.7.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20231211-2.0.0")
```

**Implementation:**

```kotlin
// New module structure
data/sync/
├── google_drive/
│   ├── GoogleDriveService.kt
│   ├── GoogleDriveSyncRepository.kt
│   └── GoogleSignInHelper.kt

domain/sync/
├── SyncRepository.kt
├── SyncConflictResolver.kt
└── use_case/
    ├── SyncWithCloud.kt
    ├── ResolveSyncConflict.kt
    └── GetSyncStatus.kt
```

```kotlin
class GoogleDriveSyncRepository @Inject constructor(
    private val driveService: Drive,
    private val bookRepository: BookRepository,
    private val gson: Gson
) : SyncRepository {
    
    companion object {
        private const val SYNC_FILE_NAME = "codex_progress_sync.json"
        private const val APP_DATA_FOLDER = "appDataFolder"
    }
    
    override suspend fun uploadProgress(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val progress = collectAllProgress()
            val json = gson.toJson(progress)
            
            val metadata = File().apply {
                name = SYNC_FILE_NAME
                parents = listOf(APP_DATA_FOLDER)
            }
            
            val content = json.toByteArray()
            driveService.files()
                .update(findOrCreateSyncFile(), metadata, ByteArrayContent("application/json", content))
                .execute()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun downloadProgress(): Result<SyncData> = withContext(Dispatchers.IO) {
        try {
            val fileId = findSyncFile() ?: return@withContext Result.failure(SyncException.NoData)
            val content = driveService.files().get(fileId).executeMediaAsInputStream()
            val json = content.readBytes().toString(Charsets.UTF_8)
            val syncData = gson.fromJson(json, SyncData::class.java)
            Result.success(syncData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**New Files Required:**
- `data/sync/google_drive/GoogleDriveService.kt`
- `data/sync/google_drive/GoogleDriveSyncRepository.kt`
- `data/sync/common/SyncConflictResolver.kt`
- `domain/repository/SyncRepository.kt`
- `domain/sync/SyncData.kt`
- `domain/sync/SyncConflict.kt`
- `domain/use_case/sync/*.kt`
- `presentation/settings/sync/GoogleDriveSyncSettings.kt`
- `presentation/settings/sync/SyncStatusScreen.kt`

**Pros:**
- Automatic sync
- Google account (most users have one)
- Reliable infrastructure
- Google handles auth/security
- Works in background via WorkManager

**Cons:**
- Google dependency (privacy concern for some users)
- Requires Google account
- API quotas/limits
- Need to handle auth token refresh
- Play Services dependency

**Effort:** ~3-4 weeks

---

### Option D: Dropbox Sync

**Description:** Similar to Google Drive but using Dropbox API.

**Architecture:**
```
Same as Google Drive but using Dropbox infrastructure
```

**Dependencies:**
```kotlin
implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")
```

**Implementation:**
```kotlin
class DropboxSyncRepository @Inject constructor(
    private val dropboxClient: DbxClientV2,
    private val bookRepository: BookRepository,
    private val gson: Gson
) : SyncRepository {
    
    companion object {
        private const val SYNC_PATH = "/codex_sync.json"
    }
    
    override suspend fun uploadProgress(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val progress = collectAllProgress()
            val json = gson.toJson(progress)
            val content = json.toByteArray()
            
            dropboxClient.files()
                .uploadBuilder(SYNC_PATH)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(ByteArrayInputStream(content))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Pros:**
- Dropbox is widely trusted
- Clean API
- No Play Services dependency
- Cross-platform (could work on iOS in future)

**Cons:**
- Requires Dropbox account
- Free tier storage limits (though our data is tiny)
- API rate limits
- OAuth handling complexity

**Effort:** ~2-3 weeks

---

### Option E: Self-Hosted REST API

**Description:** Provide a simple REST API that users can self-host (or you host) for sync.

**Architecture:**

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│  Device A   │         │  Self-Hosted     │         │  Device B   │
│  Codex      │◀───────▶│  REST API        │◀───────▶│  Codex      │
│             │  HTTPS  │  (Node/Go/Rust)  │  HTTPS  │             │
└─────────────┘         └──────────────────┘         └─────────────┘
                               │
                        ┌──────┴──────┐
                        │  Database   │
                        │  (SQLite/   │
                        │   Postgres) │
                        └─────────────┘
```

**Server Implementation (Example - Go):**

```go
// Simple sync server
type SyncServer struct {
    db *sql.DB
}

type ProgressData struct {
    DeviceID    string    `json:"device_id"`
    BookID      string    `json:"book_id"`
    Progress    float64   `json:"progress"`
    ScrollIndex int       `json:"scroll_index"`
    ScrollOffset int      `json:"scroll_offset"`
    Modified    time.Time `json:"modified"`
}

// POST /sync/upload
func (s *SyncServer) UploadProgress(w http.ResponseWriter, r *http.Request) {
    var data []ProgressData
    json.NewDecoder(r.Body).Decode(&data)
    
    for _, p := range data {
        s.db.Exec(`
            INSERT INTO progress (book_id, device_id, progress, scroll_index, scroll_offset, modified)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(book_id) DO UPDATE SET
                progress = CASE WHEN excluded.modified > modified THEN excluded.progress ELSE progress END,
                modified = MAX(excluded.modified, modified)
        `, p.BookID, p.DeviceID, p.Progress, p.ScrollIndex, p.ScrollOffset, p.Modified)
    }
}

// GET /sync/download?since=<timestamp>
func (s *SyncServer) DownloadProgress(w http.ResponseWriter, r *http.Request) {
    since := r.URL.Query().Get("since")
    rows, _ := s.db.Query(`
        SELECT book_id, progress, scroll_index, scroll_offset, modified
        FROM progress WHERE modified > ?
    `, since)
    // Return JSON
}
```

**Android Implementation:**

```kotlin
interface SyncApiService {
    @POST("sync/upload")
    suspend fun uploadProgress(@Body data: SyncUploadRequest): Response<Unit>
    
    @GET("sync/download")
    suspend fun downloadProgress(@Query("since") since: Long): Response<SyncDownloadResponse>
}

class SelfHostedSyncRepository @Inject constructor(
    private val api: SyncApiService,
    private val datastore: DataStore<Preferences>,
    private val bookRepository: BookRepository
) : SyncRepository {
    
    override suspend fun sync(): Result<SyncResult> {
        val lastSync = getLastSyncTimestamp()
        
        // Upload local changes
        val localChanges = getLocalChangesSince(lastSync)
        api.uploadProgress(SyncUploadRequest(localChanges))
        
        // Download remote changes
        val remoteChanges = api.downloadProgress(lastSync)
        applyRemoteChanges(remoteChanges.body()!!)
        
        updateLastSyncTimestamp()
        return Result.success(SyncResult(localChanges.size, remoteChanges.body()!!.changes.size))
    }
}
```

**Deployment Options:**
1. User self-hosts (Docker image provided)
2. You provide a hosted version
3. Community-hosted instances

**Pros:**
- Full control over data
- Can be self-hosted
- No third-party dependencies
- Could be open-sourced
- Scales well

**Cons:**
- Requires hosting (cost/complexity)
- Need authentication system
- Need to handle server availability
- More moving parts

**Effort:** ~4-6 weeks (including server)

---

### Option F: Firebase Firestore

**Description:** Use Firebase Firestore for real-time sync with offline support.

**Architecture:**

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│  Device A   │         │  Firebase        │         │  Device B   │
│  Codex      │◀───────▶│  Firestore       │◀───────▶│  Codex      │
│             │         │  Real-time sync  │         │             │
└─────────────┘         └──────────────────┘         └─────────────┘
                               │
                        ┌──────┴──────┐
                        │ Firebase    │
                        │ Auth        │
                        │ (Anonymous  │
                        │  or Google) │
                        └─────────────┘
```

**Dependencies:**
```kotlin
// build.gradle.kts additions
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-auth-ktx")
// Optional: Google Sign-In
implementation("com.google.android.gms:play-services-auth:20.7.0")
```

**Implementation:**

```kotlin
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val bookRepository: BookRepository
) : SyncRepository {
    
    private val userId: String
        get() = auth.currentUser?.uid ?: throw SyncException.NotAuthenticated
    
    private fun progressCollection() = firestore
        .collection("users")
        .document(userId)
        .collection("progress")
    
    override suspend fun syncBookProgress(book: Book, progress: BookProgress) {
        val docId = generateBookSyncId(book)
        progressCollection()
            .document(docId)
            .set(
                mapOf(
                    "progress" to progress.progress,
                    "scrollIndex" to progress.scrollIndex,
                    "scrollOffset" to progress.scrollOffset,
                    "currentPage" to progress.currentPage,
                    "lastModified" to System.currentTimeMillis(),
                    "deviceId" to getDeviceId()
                ),
                SetOptions.merge()
            )
            .await()
    }
    
    // Real-time listener
    fun observeProgress(book: Book): Flow<BookProgress> = callbackFlow {
        val listener = progressCollection()
            .document(generateBookSyncId(book))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.toObject(BookProgress::class.java)?.let { 
                    trySend(it)
                }
            }
        awaitClose { listener.remove() }
    }
}
```

**Firestore Data Model:**

```
users/{userId}/
├── progress/{bookSyncId}
│   ├── progress: float
│   ├── scrollIndex: int
│   ├── scrollOffset: int
│   ├── currentPage: int
│   ├── speedReaderWordIndex: int
│   ├── lastModified: timestamp
│   └── deviceId: string
│
├── bookmarks/{bookmarkId}
│   ├── bookSyncId: string
│   ├── scrollIndex: int
│   ├── scrollOffset: int
│   ├── customName: string
│   └── timestamp: long
│
└── settings/
    └── readerPreferences
```

**Security Rules:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

**Pros:**
- Real-time sync
- Offline support (built-in)
- Handles conflicts automatically
- Anonymous auth possible
- Google-backed reliability
- Automatic scaling

**Cons:**
- Firebase dependency
- Google account recommended (but anonymous works)
- Cost at scale (though progress data is tiny)
- Privacy concerns for some users
- Requires Google Play Services

**Effort:** ~3-4 weeks

---

### Option G: Custom Backend Service

**Description:** Build a complete sync service with user accounts, potentially with a subscription model.

**Architecture:**

```
┌─────────────────────────────────────────────────────────────────┐
│                        Codex Cloud Service                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  API Gateway│  │  Auth       │  │  Sync Service           │  │
│  │  (Kong/     │  │  Service    │  │  (Conflict Resolution)  │  │
│  │   Envoy)    │  │  (JWT/OAuth)│  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
│                           │                    │                 │
│  ┌─────────────┐  ┌──────┴───────┐  ┌────────┴────────┐         │
│  │  User       │  │  PostgreSQL  │  │  Redis          │         │
│  │  Service    │  │  (Main DB)   │  │  (Cache/PubSub) │         │
│  └─────────────┘  └──────────────┘  └─────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
         ▲                    ▲                    ▲
         │                    │                    │
    ┌────┴────┐          ┌────┴────┐          ┌────┴────┐
    │ Device A│          │ Device B│          │ Device C│
    └─────────┘          └─────────┘          └─────────┘
```

**Features:**
- User accounts (email/password, Google, GitHub)
- End-to-end encryption option
- Cross-platform (Android, potential iOS/Web)
- Reading statistics and analytics
- Social features (optional: reading goals, book clubs)
- API for third-party integrations

**Android Client:**

```kotlin
class CodexCloudRepository @Inject constructor(
    private val api: CodexCloudApi,
    private val authManager: AuthManager,
    private val encryptionManager: EncryptionManager,
    private val bookRepository: BookRepository
) : SyncRepository {
    
    override suspend fun fullSync(): Result<SyncResult> {
        // 1. Authenticate if needed
        if (!authManager.isAuthenticated()) {
            return Result.failure(SyncException.NotAuthenticated)
        }
        
        // 2. Get server timestamp for conflict resolution
        val serverTime = api.getServerTime()
        
        // 3. Upload local changes
        val localChanges = collectLocalChanges()
        val encryptedChanges = encryptionManager.encrypt(localChanges)
        api.uploadChanges(encryptedChanges)
        
        // 4. Download and merge remote changes
        val remoteChanges = api.downloadChanges(since = lastSyncTime)
        val decrypted = encryptionManager.decrypt(remoteChanges)
        val merged = conflictResolver.merge(localChanges, decrypted)
        
        // 5. Apply merged data
        applyMergedData(merged)
        
        return Result.success(SyncResult(uploaded = localChanges.size, downloaded = decrypted.size))
    }
}
```

**Pros:**
- Full control
- Can monetize (premium sync features)
- Best user experience
- Could expand to other platforms
- Analytics opportunity

**Cons:**
- Significant development effort
- Ongoing hosting/maintenance costs
- Need to handle security carefully
- GDPR/privacy compliance
- Customer support burden

**Effort:** ~8-12 weeks (server + client)

---

### Option H: Cloud-Synced Folder (Passive Sync)

**Description:** The app monitors a user-designated "sync folder" - a folder that is automatically synced by any cloud storage provider (Google Drive, Dropbox, OneDrive, Nextcloud, iCloud, etc.). The app stores both books and a progress database in this folder. When changes are detected, the app reads/writes the progress file using timestamp-based conflict resolution.

**Architecture:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Any Cloud Storage Provider                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Sync Folder (e.g., "CodexSync/")                               │   │
│  │  ├── books/                     # Optional: synced book files    │   │
│  │  │   ├── book1.epub                                              │   │
│  │  │   └── book2.pdf                                               │   │
│  │  ├── .codex/                    # App metadata folder            │   │
│  │  │   ├── progress.db            # SQLite with progress data      │   │
│  │  │   ├── bookmarks.json         # Bookmarks data                 │   │
│  │  │   ├── device_registry.json   # Active devices & timestamps    │   │
│  │  │   └── sync_metadata.json     # Last sync info per device      │   │
│  │  └── library.json               # Library catalog (optional)     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
         ▲                           ▲                           ▲
         │                           │                           │
    ┌────┴────┐                 ┌────┴────┐                 ┌────┴────┐
    │ Device A│                 │ Device B│                 │ Device C│
    │ Codex   │                 │ Codex   │                 │ Codex   │
    │         │                 │         │                 │         │
    │ Monitors│                 │ Monitors│                 │ Monitors│
    │ folder  │                 │ folder  │                 │ folder  │
    └─────────┘                 └─────────┘                 └─────────┘
```

**Key Concepts:**

1. **Passive Sync**: The app doesn't handle file transfer - the cloud provider's desktop/mobile app does that
2. **Timestamp-Based Resolution**: Uses UTC timestamps with device IDs for conflict resolution
3. **Cloud-Agnostic**: Works with ANY provider that syncs folders (Google Drive, Dropbox, OneDrive, Nextcloud, Syncthing, etc.)
4. **Book-Optional**: Users can choose to sync just progress, or progress + books

**Implementation:**

```kotlin
// Sync folder structure
data class SyncFolderConfig(
    val folderUri: Uri,              // SAF URI to sync folder
    val syncBooks: Boolean = false,  // Whether to include book files
    val autoImport: Boolean = true,  // Auto-import new books from folder
    val syncInterval: Duration = Duration.minutes(5)
)

// Progress database schema (in .codex/progress.db)
@Entity
data class SyncedProgressEntity(
    @PrimaryKey 
    val bookSyncId: String,          // UUID/ISBN or hash-based ID
    val title: String,
    val authors: List<String>,
    
    // Progress data
    val scrollIndex: Int,
    val scrollOffset: Int,
    val progress: Float,
    val currentPage: Int,
    val speedReaderWordIndex: Int,
    
    // Conflict resolution
    val lastModifiedUtc: Long,       // UTC timestamp
    val lastModifiedBy: String,      // Device ID
    
    // File reference (for book matching)
    val fileHash: String?,           // SHA-256 of first 1MB
    val fileSize: Long
)

// Device registry (in .codex/device_registry.json)
data class DeviceRegistry(
    val devices: Map<String, DeviceInfo>  // deviceId -> DeviceInfo
)

data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val lastSeen: Long,
    val appVersion: String
)
```

```kotlin
class SyncFolderManager @Inject constructor(
    private val context: Context,
    private val bookRepository: BookRepository,
    private val contentResolver: ContentResolver
) {
    private val syncFolderFile = File(".codex/progress.db")
    private val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    
    suspend fun syncProgress(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            // 1. Read remote progress database from sync folder
            val remoteDb = readSyncDatabase()
            
            // 2. Get local progress
            val localProgress = getLocalProgress()
            
            // 3. Merge using timestamp-based conflict resolution
            val merged = mergeProgress(localProgress, remoteDb.progress)
            
            // 4. Write merged database back to sync folder
            writeSyncDatabase(merged)
            
            // 5. Apply merged progress to local database
            applyMergedProgress(merged)
            
            // 6. Update device registry
            updateDeviceRegistry()
            
            Result.success(SyncResult(uploaded = localProgress.size, downloaded = remoteDb.progress.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun mergeProgress(
        local: List<SyncedProgressEntity>,
        remote: List<SyncedProgressEntity>
    ): List<SyncedProgressEntity> {
        val merged = mutableMapOf<String, SyncedProgressEntity>()
        
        // Add all remote entries
        remote.forEach { merged[it.bookSyncId] = it }
        
        // Merge with local - last modified wins
        local.forEach { localEntry ->
            val existing = merged[localEntry.bookSyncId]
            if (existing == null) {
                // New local entry
                merged[localEntry.bookSyncId] = localEntry
            } else {
                // Conflict: use most recent timestamp
                if (localEntry.lastModifiedUtc > existing.lastModifiedUtc) {
                    merged[localEntry.bookSyncId] = localEntry
                }
                // If timestamps are equal or remote is newer, keep remote
            }
        }
        
        return merged.values.toList()
    }
    
    // Watch for file changes using FileObserver or WorkManager
    fun startWatching(uri: Uri) {
        // Use ContentResolver to watch for changes
        // Or use WorkManager for periodic polling
    }
}
```

**Conflict Resolution Strategy:**

```kotlin
// Timezone-aware timestamp handling
data class TimestampedProgress(
    val progress: SyncedProgressEntity,
    val timestamp: Instant,  // UTC
    val deviceId: String
)

fun resolveConflict(local: TimestampedProgress, remote: TimestampedProgress): TimestampedProgress {
    // 1. If timestamps differ significantly (>1 second), use newest
    val timeDiff = abs(local.timestamp.epochSeconds - remote.timestamp.epochSeconds)
    if (timeDiff > 1) {
        return if (local.timestamp > remote.timestamp) local else remote
    }
    
    // 2. If timestamps are very close, use device priority or merge
    // Option A: Keep the one with more progress (reading further)
    return if (local.progress.progress > remote.progress.progress) local else remote
    
    // Option B: Keep both and show conflict UI for user to resolve
}
```

**User Flow:**

1. User selects a "Sync Folder" in settings (using SAF)
2. This folder should be inside a cloud-synced directory (Google Drive, Dropbox, etc.)
3. App creates `.codex/` subfolder with metadata files
4. On each app start or periodic interval, app syncs progress
5. Optional: User can also store books in this folder for automatic library sync

**Settings UI:**

```kotlin
@Composable
fun SyncFolderSettings(
    config: SyncFolderConfig?,
    onConfigChange: (SyncFolderConfig) -> Unit
) {
    Column {
        // Folder picker
        ListItem(
            headline = { Text("Sync Folder") },
            supporting = { Text(config?.folderUri?.path ?: "Not configured") },
            trailing = { 
                Button(onClick = { /* Launch folder picker */ }) {
                    Text("Select")
                }
            }
        )
        
        // Options
        SwitchPreference(
            title = "Sync Book Files",
            subtitle = "Include book files in sync (more storage)",
            checked = config?.syncBooks ?: false,
            onCheckedChange = { onConfigChange(config?.copy(syncBooks = it)!!) }
        )
        
        SwitchPreference(
            title = "Auto-Import New Books",
            subtitle = "Automatically add books found in sync folder",
            checked = config?.autoImport ?: true,
            onCheckedChange = { onConfigChange(config?.copy(autoImport = it)!!) }
        )
        
        // Sync status
        ListItem(
            headline = { Text("Last Synced") },
            supporting = { Text(config?.lastSync?.toString() ?: "Never") }
        )
        
        // Connected devices
        ListItem(
            headline = { Text("Connected Devices") },
            supporting = { Text("${config?.connectedDevices?.size ?: 0} devices") }
        )
    }
}
```

**Pros:**
- **Cloud-agnostic** - works with ANY provider (Google Drive, Dropbox, OneDrive, Nextcloud, Syncthing, pCloud, etc.)
- **No API integration needed** - just file I/O
- **No user accounts required**
- **Passive** - cloud provider handles sync in background
- **Privacy-friendly** - data stays in user's own storage
- **Cross-platform** - could work on iOS/desktop if app is ported
- **Users control their storage costs**
- **Offline-first** - local data always available
- **Simple architecture** - leverages existing cloud apps

**Cons:**
- **Requires cloud provider's app** to be installed and configured
- **File-based sync** may have edge cases (simultaneous writes)
- **Conflict resolution** needs careful handling
- **Initial setup** slightly more complex for users
- **No push notifications** - relies on polling or file watching
- **Large libraries** could be slow to sync (though progress DB is small)

**Effort:** ~2-3 weeks

---

## 5. Recommended Approaches

### Tier 1: Best Fit for Current Architecture

| Option | Effort | User Experience | Privacy | Recommendation |
|--------|--------|-----------------|---------|----------------|
| **A: Manual Export/Import** | Low | Low | Excellent | ⭐⭐⭐⭐ Good starting point |
| **H: Cloud-Synced Folder** | Medium | Excellent | Excellent | ⭐⭐⭐⭐⭐ **Highly recommended** |
| **C: Google Drive** | Medium | Excellent | Good | ⭐⭐⭐⭐ Best for non-technical users |
| **F: Firebase** | Medium | Excellent | Good | ⭐⭐⭐⭐ Great dev experience |

### Tier 2: Best for Privacy-Focused Users

| Option | Effort | User Experience | Privacy | Recommendation |
|--------|--------|-----------------|---------|----------------|
| **H: Cloud-Synced Folder** | Medium | Excellent | Excellent | ⭐⭐⭐⭐⭐ **Top pick** |
| **B: Syncthing** | Medium | Good | Excellent | ⭐⭐⭐⭐ Great for power users |
| **E: Self-Hosted API** | High | Good | Excellent | ⭐⭐⭐ For advanced users |

### Tier 3: If Complete Rework Is Acceptable

| Option | Effort | User Experience | Privacy | Recommendation |
|--------|--------|-----------------|---------|----------------|
| **G: Custom Backend** | Very High | Excellent | Variable | ⭐⭐⭐ Long-term strategic option |

### My Primary Recommendation: Option H (Cloud-Synced Folder)

**Option H** is the strongest recommendation because it:

1. **Cloud-agnostic** - Users pick their preferred cloud provider (Google Drive, Dropbox, OneDrive, Nextcloud, Syncthing, etc.)
2. **No API integration** - Just file I/O, no OAuth, no SDKs
3. **Excellent privacy** - Data stays in user's own storage
4. **No accounts needed** - No user authentication required
5. **Passive sync** - Cloud provider handles file transfer in background
6. **Cross-platform ready** - Would work on iOS/desktop with same folder structure
7. **Fits existing architecture** - Clean Architecture makes this easy to add

**Alternative Recommendation: Hybrid (A + H)**

For maximum compatibility:
1. Start with **Option A (Manual Export/Import)** for immediate release
2. Add **Option H (Cloud-Synced Folder)** for automatic sync
3. This gives users both offline-control and automatic convenience

**Previous Recommendation (still valid):**

If Option H seems too complex for users to set up, implement **Option A (Manual Export/Import)** first as a foundation, then add **Option C (Google Drive)** for automatic sync. This provides:

1. **Universal compatibility** - Manual export works for everyone
2. **Convenience** - Google Drive for automatic sync
3. **Privacy choice** - Users who don't want Google can use manual export
4. **Incremental development** - Can ship Option A quickly, add Option C later

---

## 6. Complete Architecture Rework Options

If you're open to significant architectural changes, consider these options:

### Rework A: Add Account System

**Changes Required:**

1. **New User Module:**
```
domain/user/
├── User.kt
├── Account.kt
└── repository/
    └── UserRepository.kt

data/user/
├── local/
│   └── UserPreferences.kt
├── remote/
│   └── AuthApiService.kt
└── repository/
    └── UserRepositoryImpl.kt

presentation/auth/
├── LoginScreen.kt
├── RegisterScreen.kt
└── AuthViewModel.kt
```

2. **Database Changes:**
```kotlin
// Add user ID to entities
@Entity
data class BookEntity(
    // ... existing fields
    val userId: String? = null, // null for local-only
)

// New user table
@Entity
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String?,
    val createdAt: Long
)
```

3. **Navigation Changes:**
- Add authentication flow to app startup
- Add account settings screen
- Add "Sign out" option

**Impact:**
- Higher complexity
- Better user experience for sync
- Opens door to social features
- Changes app positioning (from "offline-first" to "cloud-enabled")

### Rework B: Modular Sync Architecture

**Changes Required:**

1. **Create Sync Module:**
```
:app
:sync-core          // interfaces, models
:sync-export        // Option A implementation
:sync-googledrive   // Option C implementation  
:sync-dropbox       // Option D implementation
:sync-firebase      // Option F implementation
```

2. **Plugin Architecture:**
```kotlin
interface SyncProvider {
    val id: String
    val name: String
    val icon: Int
    
    suspend fun isAvailable(): Boolean
    suspend fun authenticate(activity: Activity): Result<Unit>
    suspend fun sync(data: SyncData): Result<SyncResult>
    suspend fun signOut()
}

class SyncManager @Inject constructor(
    private val providers: Map<String, @JvmSuppressWildcards SyncProvider>
) {
    fun getAvailableProviders(): List<SyncProvider> = 
        providers.values.filter { runBlocking { it.isAvailable() } }
}
```

**Impact:**
- Maximum flexibility
- Larger codebase
- Can ship providers incrementally
- Users choose their preferred sync method

### Rework C: Headless Reader Mode

**Description:** Separate the reading engine from the library management.

**Changes Required:**

1. Extract reader functionality into a separate library module
2. Create a "Codex Cloud" companion app/service
3. Library management happens in cloud, reader is just a client

**Impact:**
- Major architectural shift
- Could enable web/desktop readers
- Sync becomes central feature, not add-on
- Changes product fundamentally

### Rework D: Sync Folder as Primary Library (Recommended Rework)

**Description:** Instead of the current model where books are stored locally and synced as an add-on, restructure the app so that a designated "Sync Folder" becomes the primary source of truth for the library. This is a significant shift that makes sync native to the app's design.

**Architecture:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Codex with Sync-First Architecture                    │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                     Sync Folder (Primary Source)                   │ │
│  │  /CodexSync/                        ← User's cloud-synced folder   │ │
│  │  ├── .codex/                        ← App metadata                 │ │
│  │  │   ├── library.db                 ← Central library database     │ │
│  │  │   ├── progress.db                ← Progress database            │ │
│  │  │   ├── bookmarks.db               ← Bookmarks                    │ │
│  │  │   ├── settings.json              ← Synced settings              │ │
│  │  │   ├── covers/                    ← Cover images                 │ │
│  │  │   └── devices.json               ← Device registry              │ │
│  │  │                                                                 │ │
│  │  └── library/                       ← Book files                   │ │
│  │      ├── CurrentlyReading/                                         │ │
│  │      ├── Finished/                                                 │ │
│  │      ├── ToRead/                                                   │ │
│  │      └── by_Author/                                                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                              ▲                                           │
│                              │ Single source of truth                    │
│                              ▼                                           │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                    Local Cache (Performance)                       │ │
│  │  - Parsed text cache for open books                                │ │
│  │  - Cover image thumbnails                                          │ │
│  │  - Recent books quick access                                       │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

**Key Changes:**

1. **Library Model Change:**
   - Current: Books imported into app's internal storage
   - New: Books live in sync folder; app references them

2. **Database Strategy:**
   - Current: Local Room database only
   - New: SQLite database IN the sync folder, cached locally for performance

3. **Import Flow Change:**
   - Current: Copy file to app storage → parse → store in DB
   - New: Add file to sync folder → parse → update shared DB

4. **App Behavior:**
   - On first launch: User selects/designates sync folder
   - On subsequent launches: App reads library from sync folder
   - Background sync: File watcher triggers updates when folder changes

**Implementation:**

```kotlin
// New domain model for sync-first architecture
data class SyncLibrary(
    val folderUri: Uri,
    val libraryDatabase: SyncableDatabase,
    val syncStatus: SyncStatus,
    val connectedDevices: List<DeviceInfo>
)

// Modified BookRepository
class SyncFirstBookRepository @Inject constructor(
    private val syncFolderManager: SyncFolderManager,
    private val localCache: LocalBookCache,
    private val conflictResolver: ConflictResolver
) : BookRepository {
    
    private val syncDb: SQLiteDatabase
        get() = syncFolderManager.getDatabase()
    
    override suspend fun getBooks(query: String): List<Book> {
        // Read from sync folder database (with local caching)
        return syncDb.query("SELECT * FROM books WHERE ...")
            .use { cursor -> mapToBooks(cursor) }
    }
    
    override suspend fun updateBook(book: Book) {
        // Write to sync folder database with timestamp
        val timestampedBook = book.copy(
            lastModifiedUtc = Instant.now().toEpochMilli(),
            modifiedByDevice = deviceId
        )
        syncDb.update("books", timestampedBook.toContentValues())
        
        // Trigger cloud provider sync (optional - they auto-sync anyway)
        syncFolderManager.requestSync()
    }
}

// Database with conflict resolution
class SyncableDatabase(
    private val dbFile: File,
    private val deviceId: String
) {
    fun update(table: String, values: ContentValues) {
        db.execSQL("UPDATE $table SET ..., last_modified = ?, modified_by = ?", 
            arrayOf(Instant.now().toEpochMilli(), deviceId))
    }
    
    fun mergeWith(remote: SyncableDatabase): MergeResult {
        // Compare timestamps, apply last-write-wins per record
    }
}
```

**UI Changes:**

```kotlin
// First-run experience
@Composable
fun SyncFolderSetupScreen(
    onFolderSelected: (Uri) -> Unit
) {
    Column {
        Text("Welcome to Codex!")
        Text("Select a folder to store your library.")
        Text("This folder will sync across all your devices.")
        Text("Tip: Choose a folder inside Google Drive, Dropbox, or any cloud storage.")
        
        Button(onClick = { /* Launch SAF folder picker */ }) {
            Text("Choose Sync Folder")
        }
        
        // Option for existing users
        OutlinedButton(onClick = { /* Import existing library */ }) {
            Text("Import Existing Books")
        }
    }
}

// Settings
@Composable
fun SyncLibrarySettings() {
    Column {
        ListItem(
            headline = { Text("Sync Folder") },
            supporting = { Text("/storage/CodexSync") },
            trailing = { Icon(Icons.Default.CloudDone, "Synced") }
        )
        
        ListItem(
            headline = { Text("Connected Devices") },
            supporting = { Text("3 devices • Last sync: 2 min ago") }
        )
        
        ListItem(
            headline = { Text("Library Size") },
            supporting = { Text("247 books • 1.2 GB") }
        )
        
        // Advanced options
        SwitchPreference(
            title = "Include Book Files in Sync",
            subtitle = "Sync actual book files (more storage, full portability)"
        )
        
        SwitchPreference(
            title = "Sync Reading Settings",
            subtitle = "Share font size, theme preferences across devices"
        )
    }
}
```

**Data Model for Sync DB:**

```sql
-- In the sync folder's .codex/library.db

CREATE TABLE books (
    book_sync_id TEXT PRIMARY KEY,  -- UUID/ISBN-based
    title TEXT NOT NULL,
    authors TEXT,                    -- JSON array
    relative_path TEXT,              -- Path relative to sync folder
    file_hash TEXT,                  -- SHA-256 of first 1MB
    file_size INTEGER,
    
    -- Progress (synced)
    scroll_index INTEGER DEFAULT 0,
    scroll_offset INTEGER DEFAULT 0,
    progress REAL DEFAULT 0,
    current_page INTEGER DEFAULT 0,
    
    -- Metadata
    cover_path TEXT,
    description TEXT,
    tags TEXT,                       -- JSON array
    series TEXT,                     -- JSON array
    
    -- Sync fields
    created_at INTEGER,
    last_modified_utc INTEGER,
    modified_by_device TEXT,
    
    -- User data
    is_favorite INTEGER DEFAULT 0,
    date_finished INTEGER,
    reading_mode TEXT DEFAULT 'TEXT'
);

CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_sync_id TEXT REFERENCES books(book_sync_id),
    scroll_index INTEGER,
    scroll_offset INTEGER,
    custom_name TEXT,
    selected_text TEXT,
    created_at INTEGER,
    modified_at INTEGER,
    modified_by_device TEXT
);

CREATE TABLE devices (
    device_id TEXT PRIMARY KEY,
    device_name TEXT,
    last_seen INTEGER,
    app_version TEXT
);
```

**Migration Path for Existing Users:**

```kotlin
class LibraryMigrator @Inject constructor(
    private val oldRepository: LegacyBookRepository,
    private val newRepository: SyncFirstBookRepository
) {
    suspend fun migrateToSyncFolder(syncFolderUri: Uri): MigrationResult {
        // 1. Create sync folder structure
        createSyncFolderStructure(syncFolderUri)
        
        // 2. Copy book files (optional - user choice)
        if (userWantsToCopyBooks) {
            copyBooksToSyncFolder()
        } else {
            // Just create references to existing locations
            createBookReferences()
        }
        
        // 3. Export local database to sync database
        val localBooks = oldRepository.getAllBooks()
        val localProgress = oldRepository.getAllProgress()
        val localBookmarks = oldRepository.getAllBookmarks()
        
        newRepository.importFromLegacy(localBooks, localProgress, localBookmarks)
        
        // 4. Verify migration
        val verification = verifyMigration()
        
        return MigrationResult(verification)
    }
}
```

**Pros:**
- **Sync is native** - not an add-on feature
- **Single source of truth** - no more sync conflicts
- **User controls data** - all data in user's chosen location
- **Cloud-agnostic** - works with any provider
- **Future-proof** - easy to add desktop/web clients
- **Backup is built-in** - user's cloud provider handles backup
- **Clean architecture** - simpler mental model

**Cons:**
- **Major architectural change** - significant development effort
- **Breaking change** - requires migration for existing users
- **Requires cloud storage** - users without cloud storage lose functionality
- **Folder management** - users must understand folder structure
- **Edge cases** - offline access, full storage, etc.

**Effort:** ~6-8 weeks

---

## 7. Security & Privacy Considerations

### Data Encryption

```kotlin
// Recommended: Encrypt sync data before upload
class SyncEncryption @Inject constructor(
    private val crypto: CryptoManager
) {
    fun encrypt(data: SyncData, key: SecretKey): EncryptedSyncData {
        val json = gson.toJson(data)
        return crypto.encrypt(json.toByteArray(), key)
    }
    
    fun decrypt(encrypted: EncryptedSyncData, key: SecretKey): SyncData {
        val decrypted = crypto.decrypt(encrypted, key)
        return gson.fromJson(String(decrypted), SyncData::class.java)
    }
}
```

### User-Controlled Encryption

For maximum privacy, let users provide their own encryption passphrase:

```kotlin
class UserProvidedEncryption(userPassphrase: String) {
    private val key = deriveKeyFromPassphrase(userPassphrase)
    
    // Data is encrypted before leaving device
    // Server never sees plaintext
}
```

### GDPR/Privacy Compliance

- Provide clear data usage disclosure
- Allow users to delete all cloud data
- Implement data export (right to portability)
- Consider data residency requirements

---

## 8. Implementation Timeline Estimates

### Option A: Manual Export/Import

| Phase | Duration | Tasks |
|-------|----------|-------|
| Design | 2 days | Data models, UI/UX design |
| Core Implementation | 5 days | Export/Import logic, book matching |
| UI Implementation | 3 days | Settings screens, file picker |
| Testing | 2 days | Unit tests, manual testing |
| **Total** | **~2 weeks** | |

### Option C: Google Drive Sync

| Phase | Duration | Tasks |
|-------|----------|-------|
| Design | 2 days | Architecture, API design |
| Auth Setup | 3 days | Google Sign-In, OAuth flow |
| Drive Integration | 5 days | Upload/download, conflict resolution |
| Background Sync | 3 days | WorkManager integration |
| UI Implementation | 3 days | Settings, status screens |
| Testing | 3 days | Integration tests, edge cases |
| **Total** | **~3 weeks** | |

### Option F: Firebase Firestore

| Phase | Duration | Tasks |
|-------|----------|-------|
| Firebase Setup | 1 day | Project, auth, Firestore rules |
| Auth Implementation | 3 days | Anonymous/Google auth |
| Sync Implementation | 5 days | Real-time listeners, offline |
| UI Implementation | 3 days | Settings, auth screens |
| Testing | 2 days | Integration tests |
| **Total** | **~2.5 weeks** | |

### Option H: Cloud-Synced Folder

| Phase | Duration | Tasks |
|-------|----------|-------|
| Design | 2 days | Folder structure, conflict resolution strategy |
| Core Implementation | 5 days | File I/O, database read/write, book matching |
| File Watching | 3 days | Detect folder changes, trigger sync |
| Conflict Resolution | 2 days | Timestamp-based merge logic |
| UI Implementation | 3 days | Folder picker, settings, status |
| Testing | 2 days | Multi-device testing, edge cases |
| **Total** | **~2.5-3 weeks** | |

### Recommended Hybrid (A + H)

| Phase | Duration | Tasks |
|-------|----------|-------|
| Phase 1: Export/Import | 2 weeks | Option A implementation |
| Buffer | 1 week | Bug fixes, polish |
| Phase 2: Sync Folder | 3 weeks | Option H implementation |
| Buffer | 1 week | Integration testing |
| **Total** | **~7 weeks** | |

### Rework D: Sync Folder as Primary Library

| Phase | Duration | Tasks |
|-------|----------|-------|
| Architecture Design | 1 week | Full system design, migration strategy |
| Sync Database Layer | 2 weeks | SQLite in sync folder, conflict resolution |
| Repository Refactor | 2 weeks | Convert to sync-first repository |
| Migration Tool | 1 week | Import existing libraries |
| UI Overhaul | 1.5 weeks | First-run setup, settings, status |
| Testing | 1 week | Multi-device, migration testing |
| **Total** | **~8-9 weeks** | |

---

## Next Steps

After you approve an approach, the next steps would be:

1. **Detailed Design Document** - Specific class diagrams, API contracts
2. **Create Implementation Branch** - From this proposal branch
3. **Implement Foundation** - Common sync infrastructure
4. **Implement Chosen Option** - Full implementation
5. **Testing** - Unit, integration, and manual testing
6. **Documentation** - User-facing docs, code documentation
7. **Release** - Staged rollout

---

## Questions for Decision

1. **Which sync approach(es) appeal to you most?**
   - Option H (Cloud-Synced Folder) is my top recommendation
   - Option A (Manual Export/Import) is good as a foundation
   - Rework D (Sync Folder as Primary) if you're open to significant changes

2. **Is preserving the offline-first nature of the app important?**
   - Option H maintains offline-first with cached data

3. **Would you consider adding user accounts, or prefer account-less sync?**
   - Options A, B, H are account-less
   - Options C, D, F, G require accounts (or use existing ones)

4. **Are you open to Firebase/Google dependencies, or prefer to avoid them?**
   - Option H avoids all third-party SDK dependencies

5. **Would you like me to implement multiple options (user-selectable)?**
   - Hybrid approach (A + H) gives maximum compatibility

6. **Is there a timeline preference for this feature?**
   - Option A: ~2 weeks
   - Option H: ~3 weeks
   - Rework D: ~8-9 weeks

7. **Are you open to a significant architecture change (Rework D) or prefer minimal changes?**
   - Rework D offers the cleanest sync experience but requires more work

---

*Document created by AI assistant. Please review and provide feedback before implementation begins.*
