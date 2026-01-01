# Offline Dictionary Implementation Plan

Branch: `feature/dictionary-support`

---

## Overview

Implement a **self-contained, offline dictionary** for Codex that:
- Is optional and downloadable (not bundled with APK)
- Can be enabled during initial app setup or later via Settings
- Provides a "Look Up" menu item in the text selection menu
- Shows definitions in an in-app bottom sheet (not external apps)
- Works completely offline once downloaded

---

## Table of Contents
1. [Feature Requirements](#feature-requirements)
2. [Dictionary Data Source](#dictionary-data-source)
3. [Architecture Design](#architecture-design)
4. [UI/UX Flow](#uiux-flow)
5. [File Structure](#file-structure)
6. [Implementation Steps](#implementation-steps)
7. [Testing Checklist](#testing-checklist)

---

## Feature Requirements

### Core Features
1. **Downloadable Dictionary**
   - Dictionary data stored as SQLite database or compressed JSON
   - Download size ~5-15MB (English dictionary)
   - Stored in app's internal storage
   - Can be deleted to free space

2. **Setup Integration**
   - Optional prompt during first app launch: "Download offline dictionary?"
   - Skip option available
   - Can enable/download later from Settings

3. **Settings Integration**
   - Toggle: Enable/Disable offline dictionary
   - Download button (if not downloaded)
   - Delete button (if downloaded)
   - Show download size and storage used

4. **Text Selection Menu**
   - "Look Up" menu item in selection toolbar
   - Only appears if offline dictionary is enabled AND downloaded
   - Separate from Android's built-in dictionary

5. **Definition Display**
   - Bottom sheet with word definition
   - Shows: word, pronunciation (text, e.g., "/ˈwɜːrd/"), part of speech, definitions
   - Multiple definitions if available
   - Swipe to dismiss

### Nice-to-Have (Future)
- Multiple language dictionaries
- Word history/vocabulary list
- Favorite words
- Search dictionary directly (not just from text selection)

---

## Dictionary Data Source

### Option 1: WordNet (Recommended)
- **Source**: Princeton WordNet 3.1
- **License**: BSD-style, free for commercial use
- **Size**: ~12MB compressed, ~30MB uncompressed SQLite
- **Content**: 155,000+ words, definitions, synonyms, examples
- **Format**: Can be converted to SQLite

### Option 2: Simple English Dictionary
- **Source**: Various open-source JSON dictionaries on GitHub
- **Size**: ~3-5MB
- **Content**: Basic definitions, fewer words
- **Format**: JSON or SQLite

### Chosen Approach
Use a **pre-processed WordNet SQLite database** hosted on GitHub releases or a CDN.

**Database Schema:**
```sql
CREATE TABLE words (
    id INTEGER PRIMARY KEY,
    word TEXT NOT NULL,
    pronunciation TEXT,  -- IPA pronunciation
    UNIQUE(word)
);

CREATE TABLE definitions (
    id INTEGER PRIMARY KEY,
    word_id INTEGER NOT NULL,
    part_of_speech TEXT,  -- noun, verb, adj, adv, etc.
    definition TEXT NOT NULL,
    example TEXT,
    FOREIGN KEY (word_id) REFERENCES words(id)
);

CREATE INDEX idx_word ON words(word);
```

---

## Architecture Design

### Data Layer

**New Files:**
```
data/
├── dictionary/
│   ├── DictionaryDatabase.kt      # Room database for dictionary
│   ├── DictionaryDao.kt           # Data access object
│   ├── DictionaryRepository.kt    # Repository interface
│   └── DictionaryRepositoryImpl.kt
│
└── local/dto/
    ├── DictionaryWordEntity.kt    # Word entity
    └── DictionaryDefinitionEntity.kt  # Definition entity
```

**Domain Layer:**
```
domain/
├── dictionary/
│   ├── DictionaryWord.kt          # Domain model
│   ├── DictionaryDefinition.kt
│   └── DictionaryStatus.kt        # NOT_DOWNLOADED, DOWNLOADING, READY, ERROR
│
└── use_case/dictionary/
    ├── LookupWord.kt              # Look up a word
    ├── DownloadDictionary.kt      # Download dictionary file
    ├── DeleteDictionary.kt        # Delete dictionary data
    └── GetDictionaryStatus.kt     # Check if downloaded/enabled
```

### State Management

**MainState additions:**
```kotlin
val offlineDictionaryEnabled: Boolean = false,
val offlineDictionaryDownloaded: Boolean = false,
val offlineDictionaryDownloading: Boolean = false,
val offlineDictionaryProgress: Float = 0f  // 0.0 to 1.0
```

**MainEvent additions:**
```kotlin
data class OnToggleOfflineDictionary(val enabled: Boolean) : MainEvent()
data object OnDownloadDictionary : MainEvent()
data object OnDeleteDictionary : MainEvent()
data class OnDictionaryDownloadProgress(val progress: Float) : MainEvent()
data class OnDictionaryDownloadComplete(val success: Boolean) : MainEvent()
```

**ReaderState additions:**
```kotlin
val dictionaryBottomSheet: DictionaryResult? = null  // null = hidden
```

**ReaderEvent additions:**
```kotlin
data class OnLookupWord(val word: String) : ReaderEvent()
data object OnDismissDictionarySheet : ReaderEvent()
```

### DataStore Constants
```kotlin
val OFFLINE_DICTIONARY_ENABLED = booleanPreferencesKey("offline_dictionary_enabled")
// Downloaded status determined by checking if database file exists
```

---

## UI/UX Flow

### Initial Setup Flow
```
App First Launch
    ↓
Welcome Screen
    ↓
"Would you like to download the offline dictionary?"
"This allows you to look up word definitions without internet."
[Download size: ~12MB]
    ↓
[Download Now]  [Skip for Now]
    ↓
If Download: Show progress → Complete → Continue to app
If Skip: Continue to app (can download later in Settings)
```

### Settings UI
```
Settings
└── Reader Settings
    └── Dictionary
        ├── Offline Dictionary [Toggle]
        │   └── (if OFF and not downloaded): "Download dictionary to enable"
        │   └── (if OFF and downloaded): "Dictionary downloaded but disabled"
        │   └── (if ON): "Dictionary ready"
        │
        ├── Download Dictionary [Button] (if not downloaded)
        │   └── Shows progress bar while downloading
        │
        └── Delete Dictionary [Button] (if downloaded)
            └── "Free up 12MB of storage"
```

### Text Selection Flow
```
User selects text in reader
    ↓
Floating toolbar appears with:
[Copy] [Share] [Web Search] [Translate] [Look Up]
                                            ↑
                            (only if dictionary enabled + downloaded)
    ↓
User taps "Look Up"
    ↓
Bottom Sheet appears:
┌─────────────────────────────────────┐
│  example                            │
│  /ɪɡˈzæmpəl/                       │
│                                     │
│  noun                               │
│  1. A thing characteristic of its   │
│     kind or illustrating a rule.    │
│     "it's a good example of how     │
│     European cities work"           │
│                                     │
│  2. A person or thing regarded as   │
│     a model to be imitated.         │
│                                     │
│  verb                               │
│  1. Be illustrated or exemplified.  │
│     "the extent of Allied          │
│     preparations is exampled by..." │
└─────────────────────────────────────┘
```

---

## File Structure

### New Files to Create

```
app/src/main/java/us/blindmint/codex/

├── data/dictionary/
│   ├── DictionaryDatabase.kt
│   ├── DictionaryDao.kt
│   ├── DictionaryRepository.kt
│   └── DictionaryRepositoryImpl.kt
│
├── data/local/dto/
│   ├── DictionaryWordEntity.kt
│   └── DictionaryDefinitionEntity.kt
│
├── domain/dictionary/
│   ├── DictionaryWord.kt
│   ├── DictionaryDefinition.kt
│   ├── DictionaryResult.kt
│   └── DictionaryStatus.kt
│
├── domain/use_case/dictionary/
│   ├── LookupWord.kt
│   ├── DownloadDictionary.kt
│   ├── DeleteDictionary.kt
│   └── GetDictionaryStatus.kt
│
├── presentation/reader/
│   └── DictionaryBottomSheet.kt
│
├── presentation/settings/reader/dictionary/
│   ├── DictionarySubcategory.kt
│   └── components/
│       ├── OfflineDictionaryToggle.kt
│       ├── DownloadDictionaryButton.kt
│       └── DeleteDictionaryButton.kt
│
└── presentation/start/
    └── DictionarySetupScreen.kt  (or integrate into existing StartScreen)
```

### Files to Modify

| File | Changes |
|------|---------|
| `MainState.kt` | Add offline dictionary state fields |
| `MainEvent.kt` | Add dictionary download/toggle events |
| `MainModel.kt` | Handle dictionary events |
| `DataStoreConstants.kt` | Add offline dictionary preference key |
| `ReaderState.kt` | Add dictionary bottom sheet state |
| `ReaderEvent.kt` | Add lookup/dismiss events |
| `ReaderModel.kt` | Handle lookup, show bottom sheet |
| `ReaderScaffold.kt` | Include DictionaryBottomSheet |
| `SelectionContainer.kt` | Conditionally show "Look Up" based on dictionary status |
| `StartScreen.kt` | Add dictionary setup prompt |
| `strings.xml` | Add dictionary-related strings |
| `RepositoryModule.kt` | Add DI for dictionary repository |

---

## Implementation Steps

### Phase 1: Dictionary Data & Storage

1. **Create dictionary database schema**
   - `DictionaryWordEntity.kt`
   - `DictionaryDefinitionEntity.kt`
   - `DictionaryDatabase.kt` (separate from main CodexDatabase)
   - `DictionaryDao.kt`

2. **Create dictionary repository**
   - `DictionaryRepository.kt` interface
   - `DictionaryRepositoryImpl.kt` with download, lookup, delete methods

3. **Prepare dictionary data file**
   - Process WordNet into SQLite database
   - Host on GitHub releases or CDN
   - Create download URL constant

### Phase 2: Download & Management

4. **Implement download functionality**
   - Use `DownloadManager` or `OkHttp` for downloading
   - Show progress in notification and UI
   - Handle errors, resume interrupted downloads

5. **Add state management**
   - Update `MainState.kt`, `MainEvent.kt`, `MainModel.kt`
   - Persist enabled state in DataStore
   - Check download status by file existence

6. **Create settings UI**
   - `OfflineDictionaryToggle.kt`
   - `DownloadDictionaryButton.kt` with progress
   - `DeleteDictionaryButton.kt` with confirmation

### Phase 3: Lookup & Display

7. **Create lookup use case**
   - `LookupWord.kt` - queries local database
   - Handle word not found
   - Return `DictionaryResult` with definitions

8. **Create definition bottom sheet**
   - `DictionaryBottomSheet.kt`
   - Show word, pronunciation, part of speech, definitions
   - Swipe to dismiss

9. **Integrate with text selection**
   - Update `SelectionContainer.kt` to check dictionary status
   - Only show "Look Up" if enabled AND downloaded
   - Fire `OnLookupWord` event

10. **Connect to reader**
    - Update `ReaderState.kt`, `ReaderEvent.kt`, `ReaderModel.kt`
    - Show bottom sheet on lookup
    - Handle dismiss

### Phase 4: Setup Integration

11. **Add setup prompt**
    - Modify `StartScreen.kt` or create `DictionarySetupScreen.kt`
    - Show optional dictionary download prompt
    - Skip option available

---

## Dictionary Download URL

Host the dictionary database file at:
```
https://github.com/BlindMint/codex/releases/download/dictionary-v1/english-dictionary.db
```

Or use a CDN for faster downloads.

**File details:**
- Filename: `english-dictionary.db`
- Format: SQLite database
- Size: ~12MB compressed, ~30MB uncompressed
- Location in app: `context.filesDir/dictionary/english-dictionary.db`

---

## String Resources

```xml
<!-- Dictionary Setup -->
<string name="dictionary_setup_title">Offline Dictionary</string>
<string name="dictionary_setup_description">Download the offline dictionary to look up word definitions without internet.</string>
<string name="dictionary_setup_size">Download size: %s</string>
<string name="dictionary_download_now">Download Now</string>
<string name="dictionary_skip">Skip for Now</string>

<!-- Dictionary Settings -->
<string name="dictionary_settings">Dictionary</string>
<string name="offline_dictionary">Offline Dictionary</string>
<string name="offline_dictionary_enabled">Dictionary ready</string>
<string name="offline_dictionary_disabled">Dictionary downloaded but disabled</string>
<string name="offline_dictionary_not_downloaded">Download dictionary to enable</string>
<string name="download_dictionary">Download Dictionary</string>
<string name="downloading_dictionary">Downloading…</string>
<string name="delete_dictionary">Delete Dictionary</string>
<string name="delete_dictionary_confirm">Delete offline dictionary? This will free up %s of storage.</string>
<string name="dictionary_download_failed">Download failed. Please try again.</string>
<string name="dictionary_download_complete">Dictionary downloaded successfully</string>

<!-- Dictionary Bottom Sheet -->
<string name="word_not_found">Word not found in dictionary</string>
<string name="definition_example">Example:</string>

<!-- Text Selection Menu -->
<string name="look_up">Look Up</string>
```

---

## Testing Checklist

### Download & Storage
- [ ] Dictionary downloads successfully
- [ ] Download progress shows correctly
- [ ] Download can be cancelled
- [ ] Interrupted download can be resumed (or restarted)
- [ ] Downloaded file is stored correctly
- [ ] Delete removes file and frees space
- [ ] Storage space shown correctly

### Settings
- [ ] Toggle enables/disables dictionary
- [ ] Toggle state persists after app restart
- [ ] Download button shows when not downloaded
- [ ] Delete button shows when downloaded
- [ ] Delete confirmation dialog works

### Lookup & Display
- [ ] "Look Up" appears in text selection menu (when enabled + downloaded)
- [ ] "Look Up" hidden when dictionary disabled
- [ ] "Look Up" hidden when dictionary not downloaded
- [ ] Bottom sheet shows for found words
- [ ] "Not found" message for unknown words
- [ ] Multiple definitions display correctly
- [ ] Pronunciation displays correctly
- [ ] Part of speech displays correctly
- [ ] Bottom sheet dismisses on swipe
- [ ] Bottom sheet dismisses on outside tap

### Setup
- [ ] Setup prompt appears on first launch
- [ ] "Download Now" downloads dictionary
- [ ] "Skip" continues without download
- [ ] Setup doesn't show again after first completion

### Edge Cases
- [ ] Works offline after download
- [ ] Handles special characters in words
- [ ] Handles multi-word selections (looks up first word or shows error)
- [ ] Low storage warning before download
- [ ] Handles corrupted database file

---

## Dependencies

**New dependencies (if needed):**
```kotlin
// For ZIP extraction (if dictionary is compressed)
implementation("org.apache.commons:commons-compress:1.26.0")

// OR use Android's built-in GZIPInputStream for .gz files
```

**Existing dependencies used:**
- Room (for SQLite database)
- Hilt (for dependency injection)
- Compose (for UI)
- DataStore (for preferences)
- OkHttp or DownloadManager (for downloading)

---

## Notes

### Database Separation
The dictionary database should be **separate** from the main `CodexDatabase` to:
- Allow independent download/delete
- Avoid migration issues
- Keep dictionary data isolated

### File Size Considerations
- Compress the database file for download (~12MB → ~5MB with gzip)
- Decompress on device after download
- Show accurate size in UI (download size vs extracted size)

### Offline-First
- Once downloaded, dictionary works completely offline
- No network requests for lookups
- Only network needed for initial download
