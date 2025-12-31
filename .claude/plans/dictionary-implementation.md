# Dictionary Support Implementation Plan

Branch: `feature/dictionary-support`

---

## Table of Contents
1. [Current Implementation Analysis](#current-implementation-analysis)
2. [Proposed Features](#proposed-features)
3. [Architecture Design](#architecture-design)
4. [File Modifications](#file-modifications)
5. [Implementation Steps](#implementation-steps)
6. [Testing Checklist](#testing-checklist)

---

## Current Implementation Analysis

### Existing Dictionary Flow

The app currently has basic dictionary support via text selection:

**Entry Point: Text Selection**
- File: `SelectionContainer.kt` (lines 39, 78-80, 217-230, 265, 289)
- When user selects text, a floating action menu appears with "Dictionary" option (`MENU_ITEM_DICTIONARY = 4`)
- Menu item triggers `onDictionaryRequested` callback with selected text

**Event Handling**
- File: `ReaderEvent.kt` (lines 65-68)
```kotlin
data class OnOpenDictionary(
    val textToDefine: String,
    val activity: ComponentActivity
) : ReaderEvent()
```

**Current Logic: ReaderModel.kt (lines 386-425)**
```kotlin
is ReaderEvent.OnOpenDictionary -> {
    launch {
        val dictionaryIntent = Intent()
        val browserIntent = Intent()

        // 1. Try ACTION_PROCESS_TEXT (dictionary apps like Google, Livio)
        dictionaryIntent.type = "text/plain"
        dictionaryIntent.action = Intent.ACTION_PROCESS_TEXT
        dictionaryIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, event.textToDefine.trim())
        dictionaryIntent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)

        // 2. Fallback to web browser (onelook.com)
        browserIntent.action = Intent.ACTION_VIEW
        val text = event.textToDefine.trim().replace(" ", "+")
        browserIntent.data = "https://www.onelook.com/?w=$text".toUri()

        // Launch dictionary first, browser as fallback
        dictionaryIntent.launchActivity(...)
        browserIntent.launchActivity(...)

        // Toast error if neither works
        "No dictionary app found".showToast(...)
    }
}
```

**String Resources**
- `strings.xml` line 71: `<string name="error_no_dictionary">No dictionary app found.</string>`
- `strings.xml` line 124: `<string name="dictionary">Dictionary</string>`

### Current Limitations

1. **No user configuration** - Users cannot choose preferred dictionary app
2. **Single fallback URL** - Only onelook.com, no alternatives
3. **No offline support** - No embedded dictionary data
4. **No word history** - No tracking of looked-up words
5. **No inline definition preview** - Always opens external app/browser
6. **Limited multi-word handling** - Works but not optimized for phrases

---

## Proposed Features

### Phase 1: Enhanced External Dictionary Support (Priority)

1. **Dictionary App Selection**
   - Settings option to choose preferred dictionary app
   - Auto-detect installed dictionary apps
   - Allow manual app selection from installed apps
   - Remember user's choice

2. **Multiple Web Fallbacks**
   - Add settings for web dictionary URL
   - Built-in options: OneLook, Wiktionary, Google Define, Merriam-Webster
   - Custom URL option with `%s` placeholder for word

3. **Double-Tap Dictionary** (like existing double-tap translate)
   - Setting to enable/disable
   - Double-tap on word opens dictionary directly

### Phase 2: Enhanced UX Features

4. **Word History / Vocabulary List**
   - Store looked-up words with timestamp
   - View history from library or reader
   - Export vocabulary list
   - Optional: Mark words as "learned"

5. **Inline Definition Preview** (Optional/Advanced)
   - Show brief definition in bottom sheet before opening full dictionary
   - Requires API integration (free APIs limited)

### Phase 3: Offline Dictionary (Future)

6. **Embedded Dictionary Data**
   - Download offline dictionary files
   - Support StarDict/dict.cc formats
   - Pronunciation support

---

## Architecture Design

### Data Layer

**New Domain Model:**
```kotlin
// domain/dictionary/DictionarySource.kt
enum class DictionarySource(val displayName: String, val urlTemplate: String?) {
    SYSTEM_DEFAULT("System Default", null),
    ONELOOK("OneLook", "https://www.onelook.com/?w=%s"),
    WIKTIONARY("Wiktionary", "https://en.wiktionary.org/wiki/%s"),
    GOOGLE_DEFINE("Google Define", "https://www.google.com/search?q=define+%s"),
    MERRIAM_WEBSTER("Merriam-Webster", "https://www.merriam-webster.com/dictionary/%s"),
    CUSTOM("Custom URL", null)
}
```

**Word History Entity (Phase 2):**
```kotlin
// data/local/dto/WordHistoryEntity.kt
@Entity(tableName = "word_history")
data class WordHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val bookId: Int?,
    val bookTitle: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val learned: Boolean = false
)
```

### State Management

**MainState additions:**
```kotlin
// ui/main/MainState.kt
val dictionarySource: DictionarySource = DictionarySource.SYSTEM_DEFAULT,
val customDictionaryUrl: String = "",
val preferredDictionaryPackage: String? = null,  // e.g., "livio.pack.lang.en_US"
val doubleTapDictionary: Boolean = false
```

**MainEvent additions:**
```kotlin
// ui/main/MainEvent.kt
data class OnChangeDictionarySource(val value: DictionarySource) : MainEvent()
data class OnChangeCustomDictionaryUrl(val value: String) : MainEvent()
data class OnChangePreferredDictionaryPackage(val value: String?) : MainEvent()
data class OnChangeDoubleTapDictionary(val value: Boolean) : MainEvent()
```

**DataStore Constants:**
```kotlin
// core/datastore/DataStoreConstants.kt
val DICTIONARY_SOURCE = stringPreferencesKey("dictionary_source")
val CUSTOM_DICTIONARY_URL = stringPreferencesKey("custom_dictionary_url")
val PREFERRED_DICTIONARY_PACKAGE = stringPreferencesKey("preferred_dictionary_package")
val DOUBLE_TAP_DICTIONARY = booleanPreferencesKey("double_tap_dictionary")
```

### Settings UI Structure

```
Settings
└── Reader Settings
    └── Dictionary (new subcategory)
        ├── Dictionary App (dropdown/dialog)
        │   ├── System Default
        │   ├── [Detected apps...]
        │   └── Custom
        ├── Web Fallback (dropdown)
        │   ├── OneLook
        │   ├── Wiktionary
        │   ├── Google Define
        │   ├── Merriam-Webster
        │   └── Custom URL
        ├── Custom URL (text field, shown when Custom selected)
        └── Double-Tap Dictionary (toggle)
```

---

## File Modifications

### New Files to Create

```
app/src/main/java/us/blindmint/codex/
├── domain/dictionary/
│   └── DictionarySource.kt                    # Enum for dictionary sources
│
├── data/local/dto/
│   └── WordHistoryEntity.kt                   # (Phase 2) Word history entity
│
├── presentation/settings/reader/dictionary/
│   ├── DictionarySubcategory.kt               # Settings subcategory container
│   └── components/
│       ├── DictionaryAppOption.kt             # App selection dialog
│       ├── DictionaryWebFallbackOption.kt     # Web fallback dropdown
│       ├── DictionaryCustomUrlOption.kt       # Custom URL input
│       └── DoubleTapDictionaryOption.kt       # Toggle option
│
└── core/util/
    └── DictionaryUtils.kt                     # Helper functions
```

### Existing Files to Modify

| File | Changes |
|------|---------|
| `MainState.kt` | Add dictionary-related state fields |
| `MainEvent.kt` | Add dictionary-related events |
| `MainModel.kt` | Handle new events, persist settings |
| `DataStoreConstants.kt` | Add dictionary preference keys |
| `ReaderModel.kt` | Update `OnOpenDictionary` handler to use settings |
| `ReaderSettingsCategory.kt` | Add DictionarySubcategory import/call |
| `ReaderSettingsBottomSheet.kt` | Add DictionarySubcategory to in-book settings |
| `strings.xml` | Add dictionary-related strings |
| `BookDao.kt` | (Phase 2) Add word history queries |
| `CodexDatabase.kt` | (Phase 2) Add WordHistoryEntity |

---

## Implementation Steps

### Step 1: Domain Model Setup

1. Create `DictionarySource.kt` enum:
   ```kotlin
   package us.blindmint.codex.domain.dictionary

   enum class DictionarySource(
       val id: String,
       val displayNameRes: Int,
       val urlTemplate: String?
   ) {
       SYSTEM_DEFAULT("system_default", R.string.dictionary_system_default, null),
       ONELOOK("onelook", R.string.dictionary_onelook, "https://www.onelook.com/?w=%s"),
       WIKTIONARY("wiktionary", R.string.dictionary_wiktionary, "https://en.wiktionary.org/wiki/%s"),
       GOOGLE_DEFINE("google_define", R.string.dictionary_google, "https://www.google.com/search?q=define+%s"),
       MERRIAM_WEBSTER("merriam_webster", R.string.dictionary_merriam_webster, "https://www.merriam-webster.com/dictionary/%s"),
       CUSTOM("custom", R.string.dictionary_custom_url, null);

       companion object {
           fun fromId(id: String): DictionarySource =
               entries.find { it.id == id } ?: SYSTEM_DEFAULT
       }
   }
   ```

### Step 2: State & Events

2. Add to `MainState.kt`:
   ```kotlin
   val dictionarySource: String = DictionarySource.SYSTEM_DEFAULT.id,
   val customDictionaryUrl: String = "",
   val preferredDictionaryPackage: String? = null,
   val doubleTapDictionary: Boolean = false
   ```

3. Add to `MainEvent.kt`:
   ```kotlin
   data class OnChangeDictionarySource(val value: String) : MainEvent()
   data class OnChangeCustomDictionaryUrl(val value: String) : MainEvent()
   data class OnChangePreferredDictionaryPackage(val value: String?) : MainEvent()
   data class OnChangeDoubleTapDictionary(val value: Boolean) : MainEvent()
   ```

4. Add to `DataStoreConstants.kt`:
   ```kotlin
   val DICTIONARY_SOURCE = stringPreferencesKey("dictionary_source")
   val CUSTOM_DICTIONARY_URL = stringPreferencesKey("custom_dictionary_url")
   val PREFERRED_DICTIONARY_PACKAGE = stringPreferencesKey("preferred_dictionary_package")
   val DOUBLE_TAP_DICTIONARY = booleanPreferencesKey("double_tap_dictionary")
   ```

5. Add handlers in `MainModel.kt`:
   ```kotlin
   is MainEvent.OnChangeDictionarySource -> {
       _state.update { it.copy(dictionarySource = event.value) }
       // Persist to datastore
   }
   // ... similar for other events
   ```

### Step 3: Settings UI

6. Create `DictionarySubcategory.kt`:
   ```kotlin
   @Composable
   fun DictionarySubcategory(
       dictionarySource: String,
       customDictionaryUrl: String,
       preferredDictionaryPackage: String?,
       doubleTapDictionary: Boolean,
       onChangeDictionarySource: (String) -> Unit,
       onChangeCustomDictionaryUrl: (String) -> Unit,
       onChangePreferredDictionaryPackage: (String?) -> Unit,
       onChangeDoubleTapDictionary: (Boolean) -> Unit
   ) {
       SettingsSubcategory(
           title = stringResource(id = R.string.dictionary_settings),
           showDivider = true
       ) {
           DictionaryWebFallbackOption(...)
           if (dictionarySource == DictionarySource.CUSTOM.id) {
               DictionaryCustomUrlOption(...)
           }
           DoubleTapDictionaryOption(...)
       }
   }
   ```

7. Create individual option components following existing patterns (e.g., `FontFamilyOption.kt`)

8. Add `DictionarySubcategory` to:
   - `ReaderSettingsCategory.kt` (full settings)
   - `ReaderSettingsBottomSheet.kt` (in-book settings)

### Step 4: Update Dictionary Handler

9. Modify `ReaderModel.kt` `OnOpenDictionary` handler:
   ```kotlin
   is ReaderEvent.OnOpenDictionary -> {
       launch {
           val word = event.textToDefine.trim()
           val source = DictionarySource.fromId(mainState.dictionarySource)

           // Try preferred app if set
           mainState.preferredDictionaryPackage?.let { pkg ->
               val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                   type = "text/plain"
                   `package` = pkg
                   putExtra(Intent.EXTRA_PROCESS_TEXT, word)
                   putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
               }
               if (intent.launchActivity(...)) return@launch
           }

           // Try system default (ACTION_PROCESS_TEXT chooser)
           if (source == DictionarySource.SYSTEM_DEFAULT) {
               // existing ACTION_PROCESS_TEXT logic
           }

           // Use configured web fallback
           val url = when (source) {
               DictionarySource.CUSTOM -> mainState.customDictionaryUrl
               else -> source.urlTemplate
           }?.replace("%s", URLEncoder.encode(word, "UTF-8"))

           if (url != null) {
               Intent(Intent.ACTION_VIEW, url.toUri()).launchActivity(...)
           }
       }
   }
   ```

### Step 5: Double-Tap Dictionary (Optional)

10. Add double-tap detection to text rendering (similar to existing double-tap translate):
    - Check `doubleTapDictionary` setting
    - On double-tap, extract word at tap position
    - Fire `OnOpenDictionary` event

### Step 6: String Resources

11. Add to `strings.xml`:
    ```xml
    <!-- Dictionary Settings -->
    <string name="dictionary_settings">Dictionary</string>
    <string name="dictionary_settings_desc">Configure dictionary lookups</string>
    <string name="dictionary_web_fallback">Web Dictionary</string>
    <string name="dictionary_web_fallback_desc">Website to use when no dictionary app is available</string>
    <string name="dictionary_custom_url">Custom URL</string>
    <string name="dictionary_custom_url_desc">Use %s as placeholder for the word</string>
    <string name="double_tap_dictionary">Double-Tap Dictionary</string>
    <string name="double_tap_dictionary_desc">Double-tap a word to look it up</string>
    <string name="dictionary_system_default">System Default</string>
    <string name="dictionary_onelook">OneLook</string>
    <string name="dictionary_wiktionary">Wiktionary</string>
    <string name="dictionary_google">Google Define</string>
    <string name="dictionary_merriam_webster">Merriam-Webster</string>
    <string name="dictionary_custom_url_option">Custom URL</string>
    ```

---

## Testing Checklist

### Phase 1 Tests

- [ ] Dictionary menu item appears on text selection
- [ ] System default opens app chooser with dictionary apps
- [ ] OneLook fallback works when selected
- [ ] Wiktionary fallback works when selected
- [ ] Google Define fallback works when selected
- [ ] Merriam-Webster fallback works when selected
- [ ] Custom URL works with `%s` placeholder
- [ ] Custom URL validates URL format
- [ ] Settings persist after app restart
- [ ] Special characters in words are URL-encoded
- [ ] Multi-word phrases work correctly
- [ ] Double-tap dictionary toggle works (if implemented)
- [ ] Double-tap opens dictionary when enabled
- [ ] Settings visible in both full settings and reader bottom sheet

### Edge Cases

- [ ] No dictionary apps installed - shows web fallback
- [ ] No internet - shows appropriate error
- [ ] Empty text selection - handled gracefully
- [ ] Very long text selection - truncated appropriately
- [ ] Special characters only - handled gracefully

---

## Dependencies

No new dependencies required. Uses:
- `Intent.ACTION_PROCESS_TEXT` (Android standard)
- `Intent.ACTION_VIEW` (Android standard)
- Existing Jetpack Compose components
- Existing DataStore infrastructure

---

## Notes

### Dictionary App Detection

To detect installed dictionary apps:
```kotlin
val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_PROCESS_TEXT, "test")
}
val resolvedActivities = context.packageManager.queryIntentActivities(
    intent, PackageManager.MATCH_ALL
)
```

### Popular Dictionary Apps

Common packages to recognize:
- `livio.pack.lang.en_US` - Livio Dictionary
- `com.google.android.googlequicksearchbox` - Google
- `com.dictionary` - Dictionary.com
- `com.wordweb.android.free` - WordWeb

### URL Templates

| Service | URL Template |
|---------|--------------|
| OneLook | `https://www.onelook.com/?w=%s` |
| Wiktionary | `https://en.wiktionary.org/wiki/%s` |
| Google | `https://www.google.com/search?q=define+%s` |
| Merriam-Webster | `https://www.merriam-webster.com/dictionary/%s` |
| Cambridge | `https://dictionary.cambridge.org/dictionary/english/%s` |
| Oxford Learner's | `https://www.oxfordlearnersdictionaries.com/definition/english/%s` |

---

## Future Enhancements (Phase 2+)

1. **Word History Database** - Track all looked-up words
2. **Vocabulary Export** - Export words for flashcard apps
3. **Offline Dictionary** - Embedded dictionary files
4. **Pronunciation** - Audio pronunciation support
5. **Definition Preview** - Show brief definition in bottom sheet
6. **Language Detection** - Auto-select dictionary based on book language
