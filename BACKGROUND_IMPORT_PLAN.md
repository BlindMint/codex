# Background Import Task Plan

## Goal
Move Local folder importing to background process while maintaining accurate progress display across navigation. Allow smooth app navigation during long imports.

## Current State
- Local folder imports block UI on Settings screen
- If user navigates away, import may cancel or be lost
- Progress is lost if user navigates to different screen

## Architecture Overview

### 1. App-Level Import Progress Management
Create singleton `ImportProgressViewModel`:
- Maintains import state independent of screen lifecycle
- Emits progress via `StateFlow<List<ImportOperation>>`
- Survives navigation away from Settings screen
- Accessible from any screen via Hilt injection

**Data structures:**
```kotlin
data class ImportOperation(
    val id: String,
    val folderName: String,
    val folderPath: String,
    val totalBooks: Int,
    val currentProgress: Int,
    val status: ImportStatus,
    val errorMessage: String? = null
)

enum class ImportStatus {
    STARTING,
    SCANNING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

### 2. Extract Import Logic to Use Cases
Move folder scanning and book import from Settings screen into dedicated use cases:
- `LocalFolderImportProgressUseCase` - emits `Flow<ImportProgress>`
- `UpdateLocalFolderProgressUseCase` - emits progress for folder updates

Pattern emits:
1. `STARTING` state
2. `SCANNING` state
3. `IN_PROGRESS` with `current/total` progress
4. `COMPLETED` or `FAILED`

### 3. Progress State Persistence
`ImportProgressViewModel` handles:
- Collecting progress from use case Flow
- Updating state as progress emits
- Storing in StateFlow for multi-screen observation
- Surviving screen destruction

### 4. UI Layer Changes
**Settings > Storage Screen:**
- Call `importProgressViewModel.startImport(folderUri)` instead of blocking
- Observe progress from ViewModel
- Display detailed progress bar

**Other screens (Library, Settings, etc.):**
- Optionally observe progress
- Show as banner/snackbar
- Navigate back to Settings > Storage for detailed view

### 5. Lifecycle & Scope
- Use singleton-scoped ViewModel
- Coroutines survive screen destruction
- Consider foreground Service for long operations (10+ seconds)

### 6. Multi-Operation Handling
**Strategy: Sequential with queue**
- One import at a time
- Queue subsequent imports
- Less resource overhead, simpler implementation

### 7. Folder Update Operations
Similar architecture for update button:
- Track which folders updating
- Emit progress for each
- Allow updating while imports running

## Files to Create/Modify

### New Files
- `ImportProgressViewModel.kt` (@Singleton, app-level)
- `ImportOperation.kt` (data model)
- `ImportStatus.kt` (enum)
- `LocalFolderImportProgressUseCase.kt` (use case returning Flow)
- `UpdateLocalFolderProgressUseCase.kt` (use case for updates)
- `ImportProgressBar.kt` (reusable UI component)

### Modified Files
- `StorageSettingsScreen.kt` - remove blocking import, use ViewModel
- `BookRepository.kt` - add methods returning `Flow<ImportProgress>`
- `MainActivity.kt` - provide ImportProgressViewModel
- Any screen displaying progress - observe ViewModel

## Implementation Order
1. Create ImportOperation data classes
2. Create singleton ImportProgressViewModel
3. Extract import logic to return Flow<ImportProgress>
4. Update Settings screen to use ViewModel
5. Add progress bar display
6. Extend to other screens
7. Implement update folder background task

## Key Considerations
- **ViewModel Scope:** Use singleton, not screen-scoped
- **Error Handling:** Show error state, allow retry
- **Cancellation:** Allow user to cancel ongoing import
- **Database:** Ensure Room queries work in background coroutines
- **SAF Permissions:** Verify Uri validity during background operation
- **Memory:** Large imports may need pagination
- **Testing:** Background operations require special test setup

---

Status: Planning Phase - Ready to implement when needed
