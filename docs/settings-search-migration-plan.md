# Settings Search Migration Plan

**Status:** Planning Phase
**Created:** January 30, 2026
**Goal:** Migrate Codex settings search to Mihon-style architecture while retaining fuzzy search capability

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Problem Analysis](#problem-analysis)
3. [Mihon Architecture Analysis](#mihon-architecture-analysis)
4. [Codex Current Architecture](#codex-current-architecture)
5. [Migration Strategy](#migration-strategy)
6. [Phase 1: Core Data Structures](#phase-1-core-data-structures)
7. [Phase 2: Fuzzy Search Integration](#phase-2-fuzzy-search-integration)
8. [Phase 3: Search Screen](#phase-3-search-screen)
9. [Phase 4: Settings Screen Migration](#phase-4-settings-screen-migration)
10. [Phase 5: Cleanup & Optimization](#phase-5-cleanup--optimization)
11. [Testing Checklist](#testing-checklist)
12. [Risks & Mitigations](#risks--mitigations)
13. [Success Criteria](#success-criteria)
14. [Appendix: Code Snippets](#appendix-code-snippets)
15. [Appendix: File Reference Map](#appendix-file-reference-map)

---

## Executive Summary

### Current Problem

Codex's settings search only searches the main category level (Appearance, Reader, Library, Browse, Import/Export, About). Individual settings within submenus are **NOT** searchable. When a user searches "font size", they only see "Reader" category, not the actual "Font Size" setting.

### Desired Solution

Adopt Mihon's Preference architecture which:
- Makes **ALL** settings searchable (categories, subcategories, and individual settings)
- Shows breadcrumbs in search results (e.g., "Reader > Font > Font Size")
- Highlights the matching item when navigating to the destination screen
- Automatically scrolls to the highlighted item

### Key Requirements

1. ✅ Adopt Mihon's `SearchableSettings` interface and `Preference` hierarchy
2. ✅ Create dedicated `SettingsSearchScreen` with dynamic search index
3. ✅ **Retain** fuzzy search capability (Mihon uses simple `contains()` matching)
4. ✅ Maintain Codex's granular component structure (60+ subdirectories)
5. ✅ Keep existing UI patterns where possible
6. ✅ Minimize breaking changes to existing functionality

### Migration Scope

| Phase | Description | Estimated Effort |
|-------|-------------|------------------|
| Phase 1 | Core data structures (Preference, SearchableSettings) | 2-3 hours |
| Phase 2 | Fuzzy search integration with Preference | 1-2 hours |
| Phase 3 | Search screen implementation | 2-3 hours |
| Phase 4 | Settings screen migration (6 main screens) | 4-6 hours |
| Phase 5 | Cleanup, testing, optimization | 2-3 hours |
| **Total** | | **11-17 hours** |

---

## Problem Analysis

### Current Codex Settings Search Architecture

```
User searches "font size"
    ↓
SettingsLayout filters SettingsItem list using FuzzySearchHelper
    ↓
Shows matching main categories (e.g., "Reader")
    ↓
User clicks "Reader" → navigates to ReaderSettingsScreen
    ↓
User must manually navigate to Font settings
```

**Issues:**
1. Only searches top-level categories (6 items total)
2. No visibility into subcategories or individual settings
3. No breadcrumbs or context in search results
4. Manual index in `SearchableSettingsIndex.kt` is incomplete and must be maintained manually
5. Search results only show category title, no description of what's inside

### Mihon's Settings Search Architecture

```
User searches "font size"
    ↓
SettingsSearchScreen builds dynamic index from all SearchableSettings screens
    ↓
Recursively searches through PreferenceGroups and PreferenceItems
    ↓
Shows individual matches with breadcrumbs (e.g., "Reader > Font > Font Size")
    ↓
User clicks result → navigates to ReaderSettingsScreen
    ↓
Screen highlights "Font Size" item and scrolls it into view
```

**Benefits:**
1. Searches ALL individual settings (hundreds of items)
2. Shows breadcrumbs for context
3. Dynamic index - no manual maintenance
4. Results show the exact setting, not just category
5. Automatic highlighting and scroll-to-item

### Key Differences: Mihon vs Codex

| Aspect | Mihon | Codex |
|--------|-------|-------|
| Search Type | Simple `contains()` matching | Fuzzy matching (FuzzyWuzzy) |
| Preference Storage | `PreferenceData<T>` with Flow | Direct state via ViewModel |
| Navigation | Voyager | Custom navigator |
| Resources | Moko Resources (`StringResource`) | Android `stringResource()` |
| Settings Organization | Preference hierarchy (Group → Item) | Composable components (60+ subdirs) |
| Highlight Animation | Background color "blip" | To be implemented |

---

## Mihon Architecture Analysis

### Core Components

#### 1. Preference Hierarchy (`Preference.kt`)

```
Preference (sealed class)
├── PreferenceItem<T, R> (sealed class)
│   ├── SwitchPreference
│   ├── SliderPreference
│   ├── ListPreference<T>
│   ├── BasicListPreference
│   ├── MultiSelectListPreference
│   ├── EditTextPreference
│   ├── TrackerPreference (Mihon-specific)
│   ├── InfoPreference
│   └── CustomPreference
└── PreferenceGroup
    └── preferenceItems: ImmutableList<PreferenceItem>
```

**Key Features:**
- All items have `title: String` and `enabled: Boolean`
- PreferenceItems have `subtitle: String?`, `icon: ImageVector?`, `onValueChanged: suspend (T) → R`
- PreferenceGroups contain multiple PreferenceItems
- Uses `kotlinx.collections.immutable` for lists

#### 2. SearchableSettings Interface (`SearchableSettings.kt`)

```kotlin
interface SearchableSettings : Screen {
    @Composable
    @ReadOnlyComposable
    fun getTitleRes(): StringResource

    @Composable
    fun getPreferences(): List<Preference>

    companion object {
        // Used for highlighting matching items
        var highlightKey: String? = null
    }
}
```

**Purpose:**
- Marks settings screens as searchable
- Provides title for breadcrumbs
- Provides preferences list for search indexing
- Static companion object for highlight state sharing

#### 3. SettingsSearchScreen (`SettingsSearchScreen.kt`)

**Flow:**
1. User types in search field
2. `getIndex()` builds search index from all `SearchableSettings` screens
3. Filters preferences using simple `contains()` matching on title and subtitle
4. Flattens PreferenceGroups (searches items inside groups)
5. Creates `SearchResultItem` with breadcrumbs
6. Shows top 10 results
7. On click: sets `highlightKey` and navigates to target screen

**Search Logic (Mihon):**
```kotlin
.filter { (_, p) ->
    val inTitle = p.title.contains(searchKey, true)
    val inSummary = p.subtitle?.contains(searchKey, true) ?: false
    inTitle || inSummary
}
```

#### 4. PreferenceScreen (`PreferenceScreen.kt`)

**Flow:**
1. Receives `items: List<Preference>`
2. Renders PreferenceGroups and PreferenceItems
3. Checks `highlightKey` on mount
4. If highlightKey found:
   - Waits 0.5 seconds
   - Animates scroll to item
   - Clears highlightKey
5. Renders each PreferenceItem with highlight animation if matches

**Highlight Animation:**
```kotlin
if (highlighted) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        content()
    }
} else {
    content()
}
```

#### 5. Settings Main Screen

**Structure:**
- Shows list of top-level categories (Appearance, Library, Reader, etc.)
- Each category is a `TextPreference` that navigates to its respective screen
- Top bar has search icon that navigates to `SettingsSearchScreen`

### Screens Implementing SearchableSettings

From `SettingsSearchScreen.kt`:
```kotlin
private val settingScreens = listOf(
    SettingsAppearanceScreen,
    SettingsLibraryScreen,
    SettingsReaderScreen,
    SettingsDownloadScreen,
    SettingsTrackingScreen,
    SettingsBrowseScreen,
    SettingsDataScreen,
    SettingsSecurityScreen,
    SettingsAdvancedScreen,
)
```

### Example: SettingsReaderScreen Structure

```kotlin
object SettingsReaderScreen : SearchableSettings {
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val readerPref = remember { Injekt.get<ReaderPreferences>() }

        return listOf(
            // Top-level items (not in a group)
            Preference.PreferenceItem.ListPreference(
                preference = readerPref.defaultReadingMode(),
                entries = ReadingMode.entries.drop(1).associate { ... },
                title = stringResource(MR.strings.pref_viewer_type),
            ),

            // PreferenceGroups (categories within the screen)
            getDisplayGroup(readerPreferences = readerPref),
            getEInkGroup(readerPreferences = readerPref),
            getReadingGroup(readerPreferences = readerPref),
            getPagedGroup(readerPreferences = readerPref),
            getWebtoonGroup(readerPreferences = readerPref),
            getNavigationGroup(readerPreferences = readerPref),
            getActionsGroup(readerPreferences = readerPref),
        )
    }

    @Composable
    private fun getDisplayGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(...),
                Preference.PreferenceItem.SwitchPreference(...),
                // ... more items
            ),
        )
    }
    // ... other group methods
}
```

**Key Patterns:**
- Object singleton (Screen pattern)
- Uses dependency injection for preferences (`Injekt.get<ReaderPreferences>()`)
- Returns `List<Preference>` containing both top-level items and groups
- Each group has a title and list of preference items
- Groups are defined as separate methods for readability

---

## Codex Current Architecture

### Current Settings Structure

```
presentation/settings/
├── SettingsItem.kt                    # Top-level category (id, title, description, icon, onClick)
├── SearchableSettingsItem.kt           # Searchable item (id, title, category, subcategory, type)
├── SearchableSettingsIndex.kt         # Manually built search index (incomplete)
├── SettingsLayout.kt                  # Main settings list with fuzzy search
├── SettingsTopBar.kt                  # Top bar with search toggle
├── SettingsScaffold.kt                # Scaffold wrapper
├── SettingsContent.kt                 # Content wrapper
├── components/                        # Shared settings widgets
│   ├── SettingsSubcategory.kt
│   ├── SettingsSubcategoryTitle.kt
│   └── SettingsSubcategoryNote.kt
├── general/                           # General settings
├── appearance/                        # Appearance settings
│   ├── theme_preferences/
│   └── colors/
├── browse/                            # Browse settings
│   ├── display/
│   ├── filter/
│   ├── sort/
│   └── opds/
├── library/                           # Library settings
│   ├── display/
│   ├── sort/
│   └── tabs/
├── reader/                            # Reader settings (12 sub-features)
│   ├── font/
│   ├── padding/
│   ├── text/
│   ├── progress/
│   ├── chapters/
│   ├── reading_speed/
│   ├── speed_reading/
│   ├── search/
│   ├── dictionary/
│   ├── translator/
│   ├── images/
│   ├── misc/
│   └── system/
└── import_export/                     # Import/Export settings
```

### Current Search Implementation

#### SettingsItem.kt
```kotlin
data class SettingsItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: Any, // ImageVector or Painter
    val onClick: () -> Unit
)
```

Represents top-level categories only (6 items total).

#### SearchableSettingsItem.kt
```kotlin
data class SearchableSettingsItem(
    val id: String,
    val title: String,
    val category: String,
    val subcategory: String? = null,
    val type: SearchableSettingsType,
    val onClick: () -> Unit
)

enum class SearchableSettingsType {
    CATEGORY,
    SUBCATEGORY,
    SUBSETTING
}
```

Intended for granular search but not fully implemented.

#### SearchableSettingsIndex.kt
```kotlin
@Composable
fun buildSearchableSettingsIndex(
    navigateToReaderSettings: () -> Unit
): List<SearchableSettingsItem> {
    return mutableListOf<SearchableSettingsItem>().apply {
        // Only includes Reader settings - incomplete!
        add(
            SearchableSettingsItem(
                id = "reader",
                title = stringResource(id = R.string.reader_settings),
                category = stringResource(id = R.string.reader_settings),
                type = SearchableSettingsType.CATEGORY,
                onClick = navigateToReaderSettings
            )
        )
        // ... only 5 more items for Reader > Search subcategory
    }
}
```

**Problems:**
- Only includes Reader > Search subcategory
- Must be manually maintained for each setting
- Not used by current search UI (SettingsLayout uses FuzzySearchHelper directly)

#### FuzzySearchHelper.kt
```kotlin
object FuzzySearchHelper {
    fun searchSettings(
        items: List<SettingsItem>,
        query: String,
        threshold: Int = 60
    ): List<SettingsItem> {
        if (query.isBlank()) return items

        val queryLower = query.lowercase()

        val itemScores = items.map { item ->
            val titleScore = FuzzySearch.partialRatio(queryLower, item.title.lowercase())
            val maxScore = titleScore

            Pair(item, maxScore)
        }

        return itemScores
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
```

**Current Behavior:**
- Only searches `SettingsItem` list (6 top-level categories)
- Searches title only (not description)
- Uses FuzzyWuzzy with 60% threshold
- Returns sorted list of matching SettingsItems

#### SettingsLayout.kt (Search UI)
```kotlin
@Composable
fun SettingsLayout(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    // ... navigation callbacks
) {
    val allSettingsItems = remember(...) {
        listOf(
            SettingsItem(id = "appearance", ...),
            SettingsItem(id = "reader", ...),
            SettingsItem(id = "library", ...),
            SettingsItem(id = "browse", ...),
            SettingsItem(id = "import_export", ...),
            SettingsItem(id = "about", ...),
        )
    }

    val filteredItems = remember(searchQuery, allSettingsItems) {
        if (searchQuery.isBlank()) {
            allSettingsItems
        } else {
            FuzzySearchHelper.searchSettings(
                items = allSettingsItems,
                query = searchQuery,
                threshold = 50
            )
        }
    }

    LazyColumnWithScrollbar(...) {
        items(filteredItems) { item ->
            SettingsLayoutItem(
                icon = item.icon,
                title = item.title,
                description = item.description,
                onClick = item.onClick
            )
        }
    }
}
```

**Current Search Flow:**
1. User types in search field (shown via `showSearch` toggle)
2. `SettingsLayout` filters `allSettingsItems` using `FuzzySearchHelper.searchSettings()`
3. Shows matching categories (e.g., "Reader" when searching "font")
4. User clicks category → navigates to that screen
5. User must manually find the setting within that screen

### Current Settings Screen Pattern

Example from reader settings (simplified):
```kotlin
@Composable
fun ReaderSettingsContent(
    // ... state, callbacks
) {
    LazyColumnWithScrollbar(...) {
        item {
            SettingsSubcategoryTitle(text = stringResource(R.string.reader_font))
        }
        item {
            FontFamilyOption(
                selected = fontFamily,
                onOptionSelected = { fontFamily = it }
            )
        }
        item {
            FontSizeOption(
                value = fontSize,
                onValueChange = { fontSize = it }
            )
        }
        // ... more settings as individual composables
    }
}
```

**Pattern:**
- Each setting is a composable component
- Components are organized in subdirectories (font/, padding/, etc.)
- State is managed in ViewModel
- No formal hierarchy or search metadata

---

## Migration Strategy

### Overall Approach

**Phased Migration with Backward Compatibility:**

1. **Phase 1: Core Infrastructure** - Create Preference hierarchy and SearchableSettings interface
2. **Phase 2: Search Integration** - Integrate fuzzy search with Preference
3. **Phase 3: Search Screen** - Implement dedicated SettingsSearchScreen
4. **Phase 4: Screen Migration** - Migrate settings screens one at a time to SearchableSettings
5. **Phase 5: Cleanup** - Remove deprecated components, optimize, test

### Why This Approach?

| Concern | Solution |
|---------|----------|
| Breaking existing UI | Keep legacy components during migration; migrate incrementally |
| Large codebase | Start with Reader settings as proof-of-concept, then expand |
| Testing | Test each phase before proceeding to next |
| Rollback | Each phase can be independently reverted if issues arise |

### Key Design Decisions

#### Decision 1: Adapt Mihon's Preference to Codex's Architecture

**Mihon:** Uses `PreferenceData<T>` which provides reactive Flow-based state management.

**Codex:** Uses direct state management via ViewModels.

**Solution:** Adapt Preference to use callback-based approach:
- `SwitchPreference` takes `checked: Boolean` and `onCheckedChanged: (Boolean) → Unit`
- `SliderPreference` takes `value: Int` and `onValueChanged: (Int) → Unit`
- Remove `PreferenceData` dependency

**Rationale:** Codex's architecture doesn't use reactive preferences; callbacks are simpler and fit existing patterns.

---

#### Decision 2: Use String Instead of StringResource

**Mihon:** Uses Moko Resources library (`StringResource`) for internationalization.

**Codex:** Uses Android's `stringResource()` directly in composables.

**Solution:** Keep `title: String` (not `StringResource`), let screens provide localized strings via `stringResource()`.

**Rationale:** No migration to Moko Resources needed; maintain compatibility with existing resource system.

---

#### Decision 3: Retain Fuzzy Search

**Mihon:** Uses simple `contains()` matching.

**Codex:** Uses FuzzyWuzzy with configurable threshold.

**Solution:** Keep fuzzy search, adapt it to work with Preference hierarchy.

**Rationale:** Fuzzy search provides better user experience for typos and partial matches; key user requirement.

---

#### Decision 4: Keep Granular Component Structure

**Mihon:** Settings screens define preferences in `getPreferences()` methods.

**Codex:** Each setting is a composable component in 60+ subdirectories.

**Solution:** Hybrid approach:
- Keep existing composable components for UI
- Add `getPreferences()` method to screens for search indexing
- Use PreferenceScreen for new UI if needed, but migrate gradually

**Rationale:** Preserves existing code organization; reduces migration risk; allows incremental adoption.

---

#### Decision 5: Icon Type Flexibility

**Mihon:** Uses `ImageVector` for icons.

**Codex:** Uses both `ImageVector` and `Painter` (e.g., for custom icons like the skull).

**Solution:** Use `icon: Any?` to support both types, cast when rendering.

**Rationale:** Supports Codex's existing custom icons without conversion.

---

#### Decision 6: Remove Mihon-Specific Types

**Mihon:** Includes `TrackerPreference`, `MultiSelectListPreference` for manga tracking.

**Codex:** Doesn't have manga tracking features.

**Solution:** Exclude these preference types from Codex's Preference hierarchy.

**Rationale:** Avoid unnecessary complexity; only include what Codex needs.

---

### Migration Compatibility Matrix

| Feature | Mihon | Codex Current | Codex Target | Compatible? |
|---------|--------|---------------|--------------|--------------|
| Preference hierarchy | ✅ | ❌ | ✅ | ✅ (new) |
| SearchableSettings interface | ✅ | ❌ | ✅ | ✅ (new) |
| Fuzzy search | ❌ | ✅ | ✅ | ✅ (retain) |
| PreferenceScreen | ✅ | ❌ | ✅ | ✅ (new) |
| PreferenceItem widget | ✅ | ❌ | ✅ | ✅ (new) |
| SettingsSearchScreen | ✅ | ❌ | ✅ | ✅ (new) |
| Search highlight animation | ✅ | ❌ | ✅ | ✅ (new) |
| Granular components | ❌ | ✅ | ✅ | ✅ (retain) |
| Breadcrumbs in search | ✅ | ❌ | ✅ | ✅ (new) |

---

## Phase 1: Core Data Structures

### Objective

Create the core Preference hierarchy and SearchableSettings interface that will be the foundation for the new search system.

### Tasks

#### Task 1.1: Create Preference.kt

**File:** `presentation/settings/Preference.kt`

**Description:** Define the Preference sealed class hierarchy that models all types of settings items.

**Status:** Pending

**Code Snippet:** See [Appendix: Preference.kt](#appendix-preferencekt)

**Key Points:**
- `Preference` is the base sealed class with `title: String` and `enabled: Boolean`
- `PreferenceItem<T, R>` extends Preference with `subtitle: String?`, `icon: Any?`, `onValueChanged`
- Supported types: `SwitchPreference`, `SliderPreference`, `ListPreference<T>`, `TextPreference`, `EditTextPreference`, `InfoPreference`, `CustomPreference`
- `PreferenceGroup` contains multiple `PreferenceItem`s with a group title
- Uses `List` (not `ImmutableList`) for compatibility with Codex's architecture

**Differences from Mihon:**
- Removed `PreferenceData` dependency (uses callbacks instead)
- Removed `TrackerPreference`, `MultiSelectListPreference` (Mihon-specific)
- Removed `BasicListPreference` (merged into `ListPreference`)
- Changed `icon` type to `Any?` to support both ImageVector and Painter
- Added `TextPreference.onClick` callback (Mihon's is optional)

---

#### Task 1.2: Create SearchableSettings.kt

**File:** `presentation/settings/SearchableSettings.kt`

**Description:** Define the interface that settings screens must implement to be searchable.

**Status:** Pending

**Code Snippet:** See [Appendix: SearchableSettings.kt](#appendix-searchablesettingskt)

**Key Points:**
- Extends Codex's Screen interface (not Voyager's Screen)
- `getTitle()` returns `String` (not `StringResource`)
- `getPreferences()` returns `List<Preference>`
- Companion object holds `highlightKey` for cross-screen communication
- Uses `@Composable` (not `@ReadOnlyComposable` since Codex uses standard composables)

**Differences from Mihon:**
- Uses Codex's Screen interface, not Voyager
- Returns `String` not `StringResource`
- No `@ReadOnlyComposable` annotation
- No `AppBarAction()` method (Codex has different navigation)

---

#### Task 1.3: Create PreferenceScreen.kt

**File:** `presentation/settings/PreferenceScreen.kt`

**Description:** Create a composable that renders a list of Preference items with support for highlighting and automatic scrolling.

**Status:** Pending

**Code Snippet:** See [Appendix: PreferenceScreen.kt](#appendix-preferencescreenkt)

**Key Points:**
- Takes `items: List<Preference>` and renders them
- Uses `LazyColumnWithScrollbar` (Codex component)
- Checks `SearchableSettings.highlightKey` on mount
- If highlightKey found: waits 0.5s, animates scroll, clears highlightKey
- Renders `PreferenceGroup`s with group header and items
- Renders top-level `PreferenceItem`s directly
- `findHighlightedIndex()` calculates the flattened index for scrolling

**Differences from Mihon:**
- Uses `LazyColumnWithScrollbar` instead of `ScrollbarLazyColumn`
- No animation in/out for groups (simplified)
- Same highlight delay (0.5s)

---

#### Task 1.4: Create PreferenceItem.kt

**File:** `presentation/settings/PreferenceItem.kt`

**Description:** Create a composable that renders individual PreferenceItem types with highlight support.

**Status:** Pending

**Code Snippet:** See [Appendix: PreferenceItem.kt](#appendix-preferenceitemkt)

**Key Points:**
- `PreferenceItem()` composable switches on item type and calls appropriate widget
- `StatusWrapper()` handles enabled/disabled and highlight states
- Uses `AnimatedVisibility` for fade in/out when enabled state changes
- Highlight shows primary container background color
- Widget implementations:
  - `SwitchPreferenceWidget`: Row with text + Switch
  - `SliderPreferenceWidget`: Uses existing `SliderWithTitle` component
  - `ListPreference`: Uses existing `GenericOption` (needs dialog integration)
  - `TextPreferenceWidget`: Row with icon, title, subtitle, click handler
  - `EditTextPreferenceWidget`: Shows value, opens dialog on click
  - `InfoPreference`: Uses existing `SettingsSubcategoryNote`
  - `CustomPreference`: Renders custom content directly

**Differences from Mihon:**
- No reactive state collection (uses callback-based values)
- Uses Codex's existing components (`SliderWithTitle`, `GenericOption`, `SettingsSubcategoryNote`)
- Simpler widgets (no separate widget classes, just composable functions)
- List preference dialog not implemented in Phase 1 (deferred)

---

### Phase 1 Checklist

- [ ] Create `presentation/settings/Preference.kt`
  - [ ] Define `Preference` sealed class
  - [ ] Define `PreferenceItem<T, R>` sealed class
  - [ ] Implement `SwitchPreference`
  - [ ] Implement `SliderPreference`
  - [ ] Implement `ListPreference<T>`
  - [ ] Implement `TextPreference`
  - [ ] Implement `EditTextPreference`
  - [ ] Implement `InfoPreference`
  - [ ] Implement `CustomPreference`
  - [ ] Define `PreferenceGroup`
- [ ] Create `presentation/settings/SearchableSettings.kt`
  - [ ] Define `SearchableSettings` interface
  - [ ] Define `getTitle()` method
  - [ ] Define `getPreferences()` method
  - [ ] Add companion object with `highlightKey`
- [ ] Create `presentation/settings/PreferenceScreen.kt`
  - [ ] Define `PreferenceScreen()` composable
  - [ ] Implement highlight detection and scroll
  - [ ] Implement `PreferenceGroup` rendering
  - [ ] Implement `PreferenceItem` rendering
  - [ ] Implement `findHighlightedIndex()` helper
- [ ] Create `presentation/settings/PreferenceItem.kt`
  - [ ] Define `PreferenceItem()` composable
  - [ ] Define `StatusWrapper()` composable
  - [ ] Implement `SwitchPreferenceWidget`
  - [ ] Implement `SliderPreferenceWidget`
  - [ ] Implement `ListPreference` widget
  - [ ] Implement `TextPreferenceWidget`
  - [ ] Implement `EditTextPreferenceWidget`
  - [ ] Implement `InfoPreference` widget
  - [ ] Implement `CustomPreference` widget
- [ ] Add necessary imports
- [ ] Compile and verify no errors

---

### Phase 1 Testing

- [ ] Verify all Preference types compile without errors
- [ ] Verify SearchableSettings interface compiles
- [ ] Verify PreferenceScreen compiles
- [ ] Verify PreferenceItem compiles
- [ ] Test PreferenceScreen with empty list
- [ ] Test PreferenceScreen with simple PreferenceItems
- [ ] Test PreferenceScreen with PreferenceGroups
- [ ] Test highlight scroll functionality (manual test)

---

## Phase 2: Fuzzy Search Integration

### Objective

Extend `FuzzySearchHelper` to work with the new Preference hierarchy, enabling fuzzy search across all settings items including those inside groups.

### Tasks

#### Task 2.1: Add searchPreferences() to FuzzySearchHelper

**File:** `utils/FuzzySearchHelper.kt`

**Description:** Add a new function that searches through Preference lists, including PreferenceGroups, using fuzzy matching.

**Status:** Pending

**Code Snippet:** See [Appendix: FuzzySearchHelper.kt (Updated)](#appendix-fuzzysearchhelperkt-updated)

**Key Points:**
- New function: `searchPreferences(preferences, query, threshold, breadcrumbs)`
- Returns `List<SearchResult>` with preference, score, and breadcrumbs
- Recursively searches through PreferenceGroups
- Searches both `title` and `subtitle` fields
- Uses existing `FuzzySearch.partialRatio()` for fuzzy matching
- Builds breadcrumb path as it traverses groups
- Filters by `enabled` and non-blank title
- Sorts results by score (descending)
- Limits results to 50 items

**Search Logic:**
```kotlin
val titleScore = FuzzySearch.partialRatio(queryLower, item.title.lowercase())
val subtitleScore = item.subtitle?.let {
    FuzzySearch.partialRatio(queryLower, it.lowercase())
} ?: 0
val maxScore = maxOf(titleScore, subtitleScore)

if (maxScore >= threshold) {
    results.add(SearchResult(preference, maxScore, breadcrumbs))
}
```

**Breadcrumb Building:**
```kotlin
val newBreadcrumbs = if (breadcrumbs.isEmpty()) {
    pref.title
} else {
    "$breadcrumbs > ${pref.title}"
}
searchItems(pref.preferenceItems, newBreadcrumbs)
```

---

#### Task 2.2: Define SearchResult Data Class

**File:** `utils/FuzzySearchHelper.kt`

**Description:** Add a data class to hold search result information.

**Status:** Pending

**Code Snippet:**
```kotlin
data class SearchResult(
    val preference: Preference.PreferenceItem<*, *>,
    val score: Int,
    val breadcrumbs: String,
)
```

**Key Points:**
- `preference`: The matching PreferenceItem
- `score`: Fuzzy match score (0-100)
- `breadcrumbs`: Path to the item (e.g., "Reader > Font")

---

### Phase 2 Checklist

- [ ] Add `SearchResult` data class to `FuzzySearchHelper.kt`
- [ ] Add `searchPreferences()` function to `FuzzySearchHelper.kt`
  - [ ] Implement empty query handling
  - [ ] Implement PreferenceItem search
  - [ ] Implement PreferenceGroup recursion
  - [ ] Implement breadcrumb building
  - [ ] Implement fuzzy matching on title and subtitle
  - [ ] Implement score calculation and filtering
  - [ ] Implement sorting and limiting
- [ ] Add necessary imports (Preference class)
- [ ] Compile and verify no errors
- [ ] Test with simple Preference list
- [ ] Test with PreferenceGroups
- [ ] Test fuzzy matching (typos, partial matches)
- [ ] Test breadcrumb generation

---

### Phase 2 Testing

- [ ] Test `searchPreferences()` with empty query (returns empty)
- [ ] Test `searchPreferences()` with exact title match
- [ ] Test `searchPreferences()` with typo (e.g., "fnt" → "font")
- [ ] Test `searchPreferences()` with subtitle match
- [ ] Test `searchPreferences()` with PreferenceGroups
- [ ] Test breadcrumb generation (e.g., "Reader > Font > Font Size")
- [ ] Test threshold filtering (low threshold = more results)
- [ ] Test result limiting (max 50 items)
- [ ] Verify results are sorted by score (highest first)

---

## Phase 3: Search Screen

### Objective

Create a dedicated `SettingsSearchScreen` that provides a full-screen search experience with fuzzy matching, breadcrumbs, and navigation to matching settings.

### Tasks

#### Task 3.1: Create SettingsSearchScreen.kt

**File:** `presentation/settings/SettingsSearchScreen.kt`

**Description:** Create a screen that allows users to search all settings and navigate to matching items.

**Status:** Pending

**Code Snippet:** See [Appendix: SettingsSearchScreen.kt](#appendix-settingssearchscreenkt)

**Key Points:**
- Extends Codex's Screen interface
- Top bar with:
  - Back button
  - Search text field (auto-focused on launch)
  - Clear button (shows when text is non-empty)
- Content area:
  - Empty state when no query
  - Results list when query exists
  - "No results found" when no matches
- Search logic:
  - `getIndex()` builds index from all SearchableSettings screens
  - `buildSearchResults()` searches using `FuzzySearchHelper.searchPreferences()`
  - Shows results with title and breadcrumbs
- Navigation:
  - Sets `SearchableSettings.highlightKey` before navigating
  - Replaces current screen with target screen (not push)
- UX features:
  - Auto-focus search field on launch
  - Hide keyboard when scrolling
  - Hide keyboard when leaving screen
  - Execute search on keyboard "search" action

**Search Result Item Display:**
```
Font Size                       ← Title (titleMedium)
Reader > Font                   ← Breadcrumbs (bodySmall, muted)
```

**Data Structures:**
```kotlin
data class SearchableSettingsData(
    val title: String,
    val screen: Screen,
    val preferences: List<Preference>,
)

data class SearchResultItem(
    val targetScreen: Screen,
    val title: String,
    val breadcrumbs: String,
    val preference: Preference.PreferenceItem<*, *>,
)
```

**Index Building (Phase 4):**
```kotlin
@Composable
@NonRestartableComposable
private fun getSearchableSettingsScreens(): List<SearchableSettingsData> {
    return listOf(
        SearchableSettingsData(
            title = ReaderSettingsScreen.getTitle(),
            screen = ReaderSettingsScreen,
            preferences = ReaderSettingsScreen.getPreferences(),
        ),
        // ... more screens
    )
}
```

**Note:** In Phase 3, `getSearchableSettingsScreens()` will be empty or commented out until screens implement SearchableSettings in Phase 4.

---

#### Task 3.2: Update SettingsTopBar to Navigate to Search Screen

**File:** `presentation/settings/SettingsTopBar.kt`

**Description:** Update the search button to navigate to the new SettingsSearchScreen.

**Status:** Pending

**Code Change:**
```kotlin
// Current:
IconButton(
    icon = Icons.Default.Search,
    contentDescription = R.string.search_content_desc,
    disableOnClick = true
) {
    onSearchVisibilityChange(true)
}

// Updated:
IconButton(
    icon = Icons.Default.Search,
    contentDescription = R.string.search_content_desc,
    disableOnClick = true
) {
    // Navigate to SettingsSearchScreen
    val navigator = LocalNavigator.current
    navigator?.push(SettingsSearchScreen())
}
```

**Note:** Need to import `LocalNavigator` from Codex's navigator package.

---

### Phase 3 Checklist

- [ ] Create `presentation/settings/SettingsSearchScreen.kt`
  - [ ] Define `SettingsSearchScreen` class (Screen)
  - [ ] Implement `Content()` composable
  - [ ] Add top bar with search field
  - [ ] Add auto-focus on launch
  - [ ] Add keyboard hiding on scroll and screen exit
  - [ ] Add clear button in top bar
  - [ ] Implement `SearchResult()` composable
  - [ ] Implement empty state handling
  - [ ] Implement "no results" state
  - [ ] Implement search results list
  - [ ] Implement result item display (title + breadcrumbs)
  - [ ] Implement navigation on result click
  - [ ] Implement `getSearchableSettingsScreens()` (placeholder)
  - [ ] Implement `buildSearchResults()` function
  - [ ] Implement `getLocalizedBreadcrumb()` function
  - [ ] Add `SearchableSettingsData` data class
  - [ ] Add `SearchResultItem` data class
- [ ] Update `SettingsTopBar.kt`
  - [ ] Import `LocalNavigator`
  - [ ] Update search button to navigate to SettingsSearchScreen
- [ ] Add necessary imports
- [ ] Compile and verify no errors

---

### Phase 3 Testing

- [ ] Test SettingsSearchScreen opens when clicking search button
- [ ] Test search field is auto-focused
- [ ] Test keyboard shows/hides correctly
- [ ] Test typing in search field
- [ ] Test clear button appears/disappears
- [ ] Test clear button clears text
- [ ] Test empty state (no query)
- [ ] Test "no results" state (no matches)
- [ ] Test results display with title and breadcrumbs
- [ ] Test result item layout
- [ ] Test navigation on result click (will fail until Phase 4)
- [ ] Test back button returns to main settings

---

## Phase 4: Settings Screen Migration

### Objective

Migrate all settings screens to implement the `SearchableSettings` interface, providing `getPreferences()` methods that return Preference hierarchies for search indexing.

### Tasks Overview

Migrate these screens in order (simpler to more complex):

1. **ReaderSettingsScreen** - Most comprehensive, proof-of-concept
2. **AppearanceSettingsScreen** - Moderate complexity
3. **LibrarySettingsScreen** - Moderate complexity
4. **BrowseSettingsScreen** - Moderate complexity
5. **GeneralSettingsScreen** - Simple
6. **ImportExportSettingsScreen** - Simple

---

### Task 4.1: Migrate ReaderSettingsScreen

**File:** `presentation/settings/reader/ReaderSettingsScreen.kt` (or equivalent)

**Description:** Implement `SearchableSettings` interface for Reader settings.

**Status:** Pending

**Implementation Steps:**

#### Step 4.1.1: Update Screen Class Declaration

```kotlin
// Current:
object ReaderSettingsScreen : Screen() {
    @Composable
    override fun Content() {
        // ... existing UI
    }
}

// Updated:
object ReaderSettingsScreen : Screen(), SearchableSettings {
    @Composable
    override fun getTitle() = stringResource(id = R.string.reader_settings)

    @Composable
    override fun Content() {
        // ... existing UI (keep for now)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        // Return Preference hierarchy (see below)
    }
}
```

#### Step 4.1.2: Implement getPreferences()

**Structure:**
```kotlin
@Composable
override fun getPreferences(): List<Preference> {
    return listOf(
        // Top-level items (if any)
        // Preference.PreferenceItem.ListPreference(...),

        // PreferenceGroups (categories within the screen)
        getFontGroup(),
        getPaddingGroup(),
        getTextGroup(),
        getProgressGroup(),
        getChaptersGroup(),
        getReadingSpeedGroup(),
        getSpeedReadingGroup(),
        getSearchGroup(),
        getDictionaryGroup(),
        getTranslatorGroup(),
        getImagesGroup(),
        getMiscGroup(),
        getSystemGroup(),
    )
}
```

#### Step 4.1.3: Implement Font Group

```kotlin
@Composable
private fun getFontGroup(): Preference.PreferenceGroup {
    return Preference.PreferenceGroup(
        title = stringResource(id = R.string.font_settings),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                value = fontFamily.value,
                entries = FontFamily.entries.associateWith { font ->
                    font.name
                },
                title = stringResource(id = R.string.font_family),
                onValueChange = { fontFamily.value = it },
            ),
            Preference.PreferenceItem.SliderPreference(
                value = fontSize.value,
                valueRange = 8..32,
                title = stringResource(id = R.string.font_size),
                onValueChanged = { fontSize.value = it },
            ),
            Preference.PreferenceItem.SliderPreference(
                value = lineHeight.value,
                valueRange = 1.0f..3.0f,
                title = stringResource(id = R.string.line_height),
                valueString = "${lineHeight.value}x",
                onValueChanged = { lineHeight.value = it },
            ),
            Preference.PreferenceItem.SwitchPreference(
                title = stringResource(id = R.string.custom_fonts),
                checked = useCustomFonts.value,
                onCheckedChanged = { useCustomFonts.value = it },
            ),
        ),
    )
}
```

#### Step 4.1.4: Implement Other Groups

Repeat pattern for all subcategories (padding, text, progress, etc.).

**Key Points:**
- Use ViewModel state (`viewModel.fontSize.value`, etc.)
- Map state values to Preference values
- Map Preference callbacks to state updates
- Use appropriate Preference type for each setting
- Group related settings in PreferenceGroups
- Use `stringResource()` for localized titles

---

### Task 4.2: Update getSearchableSettingsScreens()

**File:** `presentation/settings/SettingsSearchScreen.kt`

**Description:** Add ReaderSettingsScreen to the search index.

**Status:** Pending

**Code Change:**
```kotlin
@Composable
@NonRestartableComposable
private fun getSearchableSettingsScreens(): List<SearchableSettingsData> {
    return listOf(
        SearchableSettingsData(
            title = ReaderSettingsScreen.getTitle(),
            screen = ReaderSettingsScreen,
            preferences = ReaderSettingsScreen.getPreferences(),
        ),
    )
}
```

---

### Task 4.3: Test Reader Settings Search

**Test Cases:**
- [ ] Search "font" → shows font-related settings with breadcrumbs
- [ ] Search "font size" → shows Font Size setting
- [ ] Search "line height" → shows Line Height setting
- [ ] Search "custom fonts" → shows Custom Fonts switch
- [ ] Click result → navigates to ReaderSettingsScreen
- [ ] Result is highlighted on destination screen
- [ ] Result scrolls into view automatically

---

### Task 4.4: Migrate AppearanceSettingsScreen

**File:** `presentation/settings/appearance/AppearanceSettingsScreen.kt`

**Description:** Implement `SearchableSettings` interface for Appearance settings.

**Status:** Pending

**Implementation:**
```kotlin
object AppearanceSettingsScreen : Screen(), SearchableSettings {
    @Composable
    override fun getTitle() = stringResource(id = R.string.appearance_settings)

    @Composable
    override fun Content() {
        // ... existing UI
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        return listOf(
            getThemePreferencesGroup(),
            getColorsGroup(),
            getSystemGroup(),
        )
    }

    @Composable
    private fun getThemePreferencesGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.theme_preferences),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = darkThemeOption.value,
                    entries = mapOf(
                        DarkThemeOption.LIGHT to "Light",
                        DarkThemeOption.DARK to "Dark",
                        DarkThemeOption.AUTO to "Auto",
                    ),
                    title = stringResource(id = R.string.dark_theme),
                    onValueChange = { darkThemeOption.value = it },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    title = stringResource(id = R.string.pure_dark),
                    checked = pureDark.value,
                    onCheckedChanged = { pureDark.value = it },
                ),
            ),
        )
    }

    @Composable
    private fun getColorsGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.colors),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = selectedColorPreset.value,
                    entries = colorPresets.associate { it to it.name },
                    title = stringResource(id = R.string.color_preset),
                    onValueChange = { selectedColorPreset.value = it },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(id = R.string.background_image),
                    subtitle = backgroundImage.value?.name,
                    onClick = { /* open image picker */ },
                ),
            ),
        )
    }

    @Composable
    private fun getSystemGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.system),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = screenOrientation.value,
                    entries = mapOf(
                        ScreenOrientation.AUTO to "Auto",
                        ScreenOrientation.PORTRAIT to "Portrait",
                        ScreenOrientation.LANDSCAPE to "Landscape",
                    ),
                    title = stringResource(id = R.string.screen_orientation),
                    onValueChange = { screenOrientation.value = it },
                ),
            ),
        )
    }
}
```

---

### Task 4.5: Update getSearchableSettingsScreens()

```kotlin
@Composable
@NonRestartableComposable
private fun getSearchableSettingsScreens(): List<SearchableSettingsData> {
    return listOf(
        SearchableSettingsData(
            title = ReaderSettingsScreen.getTitle(),
            screen = ReaderSettingsScreen,
            preferences = ReaderSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = AppearanceSettingsScreen.getTitle(),
            screen = AppearanceSettingsScreen,
            preferences = AppearanceSettingsScreen.getPreferences(),
        ),
    )
}
```

---

### Task 4.6: Test Appearance Settings Search

**Test Cases:**
- [ ] Search "dark theme" → shows Dark Theme setting
- [ ] Search "color" → shows color-related settings
- [ ] Search "background" → shows Background Image setting
- [ ] Search "orientation" → shows Screen Orientation setting
- [ ] Click result → navigates to AppearanceSettingsScreen
- [ ] Result is highlighted and scrolled into view

---

### Task 4.7: Migrate Remaining Screens

Repeat the pattern for:
- **LibrarySettingsScreen**
- **BrowseSettingsScreen**
- **GeneralSettingsScreen**
- **ImportExportSettingsScreen**

---

### Task 4.8: Complete Search Index

**Final `getSearchableSettingsScreens()`:**
```kotlin
@Composable
@NonRestartableComposable
private fun getSearchableSettingsScreens(): List<SearchableSettingsData> {
    return listOf(
        SearchableSettingsData(
            title = ReaderSettingsScreen.getTitle(),
            screen = ReaderSettingsScreen,
            preferences = ReaderSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = AppearanceSettingsScreen.getTitle(),
            screen = AppearanceSettingsScreen,
            preferences = AppearanceSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = LibrarySettingsScreen.getTitle(),
            screen = LibrarySettingsScreen,
            preferences = LibrarySettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = BrowseSettingsScreen.getTitle(),
            screen = BrowseSettingsScreen,
            preferences = BrowseSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = GeneralSettingsScreen.getTitle(),
            screen = GeneralSettingsScreen,
            preferences = GeneralSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = ImportExportSettingsScreen.getTitle(),
            screen = ImportExportSettingsScreen,
            preferences = ImportExportSettingsScreen.getPreferences(),
        ),
    )
}
```

---

### Phase 4 Checklist

- [ ] Migrate ReaderSettingsScreen to SearchableSettings
  - [ ] Update class declaration
  - [ ] Implement getTitle()
  - [ ] Implement getPreferences()
  - [ ] Implement all PreferenceGroups
  - [ ] Test search functionality
- [ ] Migrate AppearanceSettingsScreen to SearchableSettings
  - [ ] Update class declaration
  - [ ] Implement getTitle()
  - [ ] Implement getPreferences()
  - [ ] Implement all PreferenceGroups
  - [ ] Test search functionality
- [ ] Migrate LibrarySettingsScreen to SearchableSettings
  - [ ] Update class declaration
  - [ ] Implement getTitle()
  - [ ] Implement getPreferences()
  - [ ] Implement all PreferenceGroups
  - [ ] Test search functionality
- [ ] Migrate BrowseSettingsScreen to SearchableSettings
  - [ ] Update class declaration
  - [ ] Implement getTitle()
  - [ ] Implement getPreferences()
  - [ ] Implement all PreferenceGroups
  - [ ] Test search functionality
- [ ] Migrate GeneralSettingsScreen to SearchableSettings
  - [ ] Update class declaration
  - [ ] Implement getTitle()
  - [ ] Implement getPreferences()
  - [ ] Implement all PreferenceGroups
  - [ ] Test search functionality
- [ ] Migrate ImportExportSettingsScreen to SearchableSettings
  - [ ] Update class declaration
  - [ ] Implement getTitle()
  - [ ] Implement getPreferences()
  - [ ] Implement all PreferenceGroups
  - [ ] Test search functionality
- [ ] Update getSearchableSettingsScreens() with all screens
- [ ] Test cross-screen search (search "font" finds Reader settings)

---

### Phase 4 Testing

For each migrated screen:
- [ ] Search by screen title (e.g., "reader") → shows screen as option
- [ ] Search by group title (e.g., "font") → shows items in that group
- [ ] Search by setting title (e.g., "font size") → shows individual setting
- [ ] Search by setting description/subtitle (if applicable)
- [ ] Click search result → navigates to correct screen
- [ ] Matching item is highlighted
- [ ] Matching item scrolls into view
- [ ] Highlight clears after scrolling
- [ ] Breadcrumbs are correct (e.g., "Reader > Font > Font Size")
- [ ] Fuzzy matching works (e.g., "fnt" → "font")

---

## Phase 5: Cleanup & Optimization

### Objective

Remove deprecated components, optimize performance, and ensure code quality.

### Tasks

#### Task 5.1: Deprecate Legacy Components

**Files to Deprecate:**
- `presentation/settings/SettingsItem.kt` - Replace with Preference
- `presentation/settings/SearchableSettingsItem.kt` - Replace with Preference-based search
- `presentation/settings/SearchableSettingsIndex.kt` - Replace with dynamic search index

**Action:** Add deprecation warnings but keep files for backward compatibility during transition.

```kotlin
@Deprecated(
    "Use Preference.PreferenceItem instead",
    ReplaceWith("Preference.PreferenceItem.TextPreference", "us.blindmint.codex.presentation.settings.Preference"),
)
data class SettingsItem(...)
```

---

#### Task 5.2: Remove Deprecated Code

**Once all screens are migrated:**
- Delete `SettingsItem.kt`
- Delete `SearchableSettingsItem.kt`
- Delete `SearchableSettingsIndex.kt`
- Remove usage of these types from remaining code

---

#### Task 5.3: Optimize Search Performance

**Optimizations:**
1. **Caching:** Cache `getPreferences()` results to avoid rebuilding on each search
2. **Debouncing:** Debounce search input to avoid rebuilding index on every keystroke
3. **Limiting:** Ensure result limit (50 items) is respected
4. **Lazy Evaluation:** Use `produceState` with proper keys for result generation

**Debounce Implementation:**
```kotlin
val debouncedQuery by remember {
    derivedStateOf {
        // Debounce logic using kotlinx.coroutines.delay
    }
}

val results by produceState<List<SearchResultItem>?>(initialValue = null, debouncedQuery) {
    value = buildSearchResults(screens, debouncedQuery, isLtr)
}
```

---

#### Task 5.4: Improve Search UI/UX

**Enhancements:**
1. **Search History:** Show recent searches
2. **Popular Searches:** Show commonly searched settings
3. **Quick Actions:** Add buttons for common settings (e.g., "Dark Theme", "Font Size")
4. **Keyboard Shortcuts:** Add Ctrl+F (or equivalent) to open search
5. **Search Suggestions:** Autocomplete based on available settings

**Example: Search History**
```kotlin
// Add to SettingsSearchScreen top bar (below search field)
if (recentSearches.isNotEmpty()) {
    FlowRow(...) {
        recentSearches.forEach { query ->
            SuggestionChip(
                label = { Text(query) },
                onClick = { textFieldState.text = query },
            )
        }
    }
}
```

---

#### Task 5.5: Accessibility Improvements

**Enhancements:**
1. **Screen Reader Support:** Add proper content descriptions for search results
2. **Keyboard Navigation:** Ensure arrow keys work in search results
3. **Focus Management:** Ensure focus moves correctly between search field and results

**Example: Content Description**
```kotlin
Column(
    modifier = Modifier
        .clickable { onItemClick(item) }
        .semantics {
            contentDescription = "${item.title} in ${item.breadcrumbs}"
        },
) {
    // ... content
}
```

---

#### Task 5.6: Code Quality

**Actions:**
1. **Linting:** Run ktlint and fix issues
2. **Documentation:** Add KDoc comments to all public APIs
3. **Tests:** Write unit tests for `FuzzySearchHelper.searchPreferences()`
4. **Code Review:** Ensure all code follows Codex conventions

**Example: KDoc**
```kotlin
/**
 * Search through Preference items using fuzzy matching.
 *
 * @param preferences List of Preference items to search (can include PreferenceGroups)
 * @param query Search query string
 * @param threshold Minimum similarity score (0-100) to consider a match (default: 60)
 * @param breadcrumbs Breadcrumb path for nested items (default: empty)
 * @return List of matching PreferenceItems with scores and breadcrumb paths
 */
fun searchPreferences(
    preferences: List<Preference>,
    query: String,
    threshold: Int = 60,
    breadcrumbs: String = ""
): List<SearchResult>
```

---

### Phase 5 Checklist

- [ ] Add deprecation warnings to legacy components
- [ ] Remove deprecated components after migration is complete
- [ ] Implement search result caching
- [ ] Implement search debouncing
- [ ] Verify result limiting is working
- [ ] Add search history (optional)
- [ ] Add popular searches (optional)
- [ ] Add keyboard shortcuts (optional)
- [ ] Add content descriptions for accessibility
- [ ] Test keyboard navigation in search
- [ ] Run ktlint and fix issues
- [ ] Add KDoc comments to public APIs
- [ ] Write unit tests for FuzzySearchHelper
- [ ] Final code review
- [ ] Performance testing (large preference lists)

---

## Testing Checklist

### Unit Tests

- [ ] `FuzzySearchHelper.searchPreferences()` tests
  - [ ] Test with empty query
  - [ ] Test with exact match
  - [ ] Test with typo (fuzzy match)
  - [ ] Test with PreferenceGroups
  - [ ] Test breadcrumb generation
  - [ ] Test threshold filtering
  - [ ] Test result sorting
  - [ ] Test result limiting

### Integration Tests

- [ ] Search screen integration
  - [ ] Test opening search from main settings
  - [ ] Test typing in search field
  - [ ] Test clearing search
  - [ ] Test navigation to results
  - [ ] Test back navigation

- [ ] Settings screen integration
  - [ ] Test all screens implement SearchableSettings
  - [ ] Test all screens provide getPreferences()
  - [ ] Test highlight animation
  - [ ] Test scroll to highlighted item

### UI Tests

- [ ] Search UI
  - [ ] Test search field focus
  - [ ] Test keyboard show/hide
  - [ ] Test clear button visibility
  - [ ] Test empty state display
  - [ ] Test "no results" state
  - [ ] Test results list display
  - [ ] Test result item layout
  - [ ] Test breadcrumb display

- [ ] Settings UI
  - [ ] Test PreferenceGroup rendering
  - [ ] Test PreferenceItem rendering (all types)
  - [ ] Test highlight background
  - [ ] Test enabled/disabled states

### Manual Testing Scenarios

#### Scenario 1: Basic Search
1. Open Settings
2. Click search button
3. Type "font"
4. Verify results show font-related settings
5. Verify breadcrumbs are correct (e.g., "Reader > Font")
6. Click "Font Size"
7. Verify navigation to Reader Settings
8. Verify "Font Size" is highlighted
9. Verify "Font Size" scrolls into view

#### Scenario 2: Fuzzy Search
1. Open Settings
2. Click search button
3. Type "clr" (typo for "color")
4. Verify results show color-related settings
5. Click a result
6. Verify navigation works correctly

#### Scenario 3: No Results
1. Open Settings
2. Click search button
3. Type "xyz123"
4. Verify "No results found" message is shown

#### Scenario 4: Search by Breadcrumb
1. Open Settings
2. Click search button
3. Type "reader font"
4. Verify results show items within "Reader > Font"
5. Verify breadcrumbs are displayed

#### Scenario 5: Search by Description/Subtitle
1. Open Settings
2. Click search button
3. Type search term that matches a subtitle
4. Verify matching items are shown

#### Scenario 6: Cross-Screen Search
1. Open Settings
2. Click search button
3. Type "dark"
4. Verify results from multiple screens (Appearance, Reader, etc.)
5. Click a result from Appearance
6. Verify navigation to Appearance Settings
7. Repeat for other screens

#### Scenario 7: Keyboard Interaction
1. Open Settings
2. Click search button
3. Type query
4. Press Enter
5. Verify keyboard is hidden
6. Use arrow keys to navigate results
7. Press Enter to select result
8. Verify navigation works

#### Scenario 8: Rapid Search
1. Open Settings
2. Click search button
3. Type rapidly (multiple characters)
4. Verify results update smoothly
5. Verify no crashes or ANRs

---

## Risks & Mitigations

### Risk 1: Breaking Existing Settings UI

**Description:** Migration may break existing settings screens or components.

**Impact:** High - Users may lose access to settings

**Probability:** Medium

**Mitigation:**
- Keep legacy components in place during migration
- Migrate screens one at a time
- Test each screen thoroughly before moving to next
- Keep existing UI rendering alongside PreferenceScreen
- Use feature flags to enable/disable new search

**Rollback Plan:** Revert changes to affected screen, continue using legacy search

---

### Risk 2: Fuzzy Search Performance Issues

**Description:** Fuzzy matching on hundreds of preference items may be slow.

**Impact:** Medium - Slow search results, poor UX

**Probability:** Medium

**Mitigation:**
- Limit search results to 50 items
- Implement result caching
- Use debouncing to avoid excessive recomputation
- Profile performance with large preference lists
- Consider using more efficient fuzzy search algorithm if needed

**Rollback Plan:** Increase threshold, reduce result limit, or switch to simple matching

---

### Risk 3: Navigation Complexity

**Description:** Integrating new search screen with existing navigator may cause navigation issues.

**Impact:** High - Users may get stuck or lost

**Probability:** Low

**Mitigation:**
- Test navigation thoroughly (all paths)
- Ensure back button works correctly
- Verify screen replacement (not push) works
- Test from all entry points

**Rollback Plan:** Use push instead of replace if replace causes issues

---

### Risk 4: Highlight Animation Issues

**Description:** Highlight and scroll-to-item may not work correctly on all devices.

**Impact:** Low - Minor UX issue

**Probability:** Low

**Mitigation:**
- Test on multiple devices/form factors
- Adjust delay timing if needed
- Test with different screen sizes
- Test with both enabled and disabled items

**Rollback Plan:** Remove highlight animation if issues persist

---

### Risk 5: Migration Effort Underestimated

**Description:** Actual migration may take longer than estimated.

**Impact:** Medium - Delayed timeline

**Probability:** Medium

**Mitigation:**
- Start with Reader settings as proof-of-concept
- Measure actual effort and adjust estimates
- Prioritize critical screens first
- Consider partial deployment (migrate some screens, keep others legacy)

**Rollback Plan:** Deploy partial migration, complete remaining screens later

---

### Risk 6: User Confusion

**Description:** Users may be confused by new search behavior or UI.

**Impact:** Low - Poor UX, complaints

**Probability:** Low

**Mitigation:**
- Maintain similar search UI (top bar search button)
- Keep search results familiar (title + description)
- Add breadcrumbs for clarity
- Add onboarding tooltips if needed

**Rollback Plan:** Add help text or documentation

---

### Risk 7: State Management Complexity

**Description:** Managing state between existing ViewModel and Preference callbacks may be complex.

**Impact:** Medium - Bugs, inconsistent state

**Probability:** Medium

**Mitigation:**
- Keep existing ViewModel structure
- Map ViewModel state to Preference values
- Map Preference callbacks to ViewModel updates
- Test state changes thoroughly
- Consider refactor to unify state management if needed

**Rollback Plan:** Keep existing UI, only add getPreferences() for search

---

## Success Criteria

### Functional Requirements

- [ ] **FR1:** All individual settings are searchable (not just top-level categories)
- [ ] **FR2:** Search results show breadcrumbs (e.g., "Reader > Font > Font Size")
- [ ] **FR3:** Clicking search result navigates to correct settings screen
- [ ] **FR4:** Matching item is highlighted on destination screen
- [ ] **FR5:** Highlighted item scrolls into view automatically
- [ ] **FR6:** Search works with typos and partial matches (fuzzy matching)
- [ ] **FR7:** Search is case-insensitive
- [ ] **FR8:** Empty state shows appropriate message
- [ ] **FR9:** All settings screens implement SearchableSettings interface
- [ ] **FR10:** PreferenceGroups render correctly
- [ ] **FR11:** All preference types render correctly (Switch, Slider, List, Text, etc.)

### Non-Functional Requirements

- [ ] **NFR1:** Search results appear within 300ms of typing
- [ ] **NFR2:** Navigation to result completes within 500ms
- [ ] **NFR3:** App doesn't crash or ANR during search
- [ ] **NFR4:** Memory usage increase is < 5MB
- [ ] **NFR5:** Code follows Codex style conventions
- [ ] **NFR6:** All public APIs have KDoc comments
- [ ] **NFR7:** Unit test coverage > 80% for new code

### User Experience Requirements

- [ ] **UX1:** Search is discoverable (clear search button in top bar)
- [ ] **UX2:** Search results are readable (clear title, muted breadcrumbs)
- [ ] **UX3:** Keyboard management is natural (auto-focus, hide on scroll)
- [ ] **UX4:** Highlight animation is smooth (0.5s delay, animated scroll)
- [ ] **UX5:** Back button works correctly from all screens
- [ ] **UX6:** No jarring UI transitions (animations throughout)

### Migration Requirements

- [ ] **MR1:** Existing functionality is not broken
- [ ] **MR2:** Legacy components can coexist with new components during migration
- [ ] **MR3:** Each phase can be independently reverted if issues arise
- [ ] **MR4:** Migration effort is within estimated time (11-17 hours)

---

## Appendix: Code Snippets

### Appendix: Preference.kt

**File:** `presentation/settings/Preference.kt`

```kotlin
/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing a settings preference.
 *
 * Preferences can be individual items (Switch, Slider, List, etc.) or groups
 * of related items. All preferences have a title and enabled state.
 *
 * @property title The display title for the preference
 * @property enabled Whether the preference is enabled and interactive
 */
sealed class Preference {
    abstract val title: String
    abstract val enabled: Boolean

    /**
     * Sealed class representing individual preference items.
     *
     * Each item type has specific properties and behavior. All items have
     * a title, optional subtitle, optional icon, and a value change callback.
     *
     * @param T The value type for this preference
     * @param R The result type returned by the value change callback
     * @property subtitle Optional subtitle/description text
     * @property icon Optional icon (ImageVector or Painter)
     * @property onValueChanged Callback invoked when the preference value changes
     */
    sealed class PreferenceItem<T, R> : Preference() {
        abstract val subtitle: String?
        abstract val icon: Any?
        abstract val onValueChanged: suspend (value: T) -> R

        /**
         * A preference item that displays a two-state toggleable option.
         *
         * @property checked The current checked state
         * @property onCheckedChanged Callback invoked when the checked state changes
         */
        data class SwitchPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            val checked: Boolean,
            val onCheckedChanged: (Boolean) -> Unit,
        ) : PreferenceItem<Boolean, Unit>() {
            override val icon: Any? = null
            override val onValueChanged: suspend (value: Boolean) -> Unit = { onCheckedChanged(it) }
        }

        /**
         * A preference item that provides a slider to select an integer number.
         *
         * @property value The current slider value
         * @property valueRange The range of valid values
         * @property valueString Optional string representation of the value
         * @property steps The number of discrete steps between range endpoints
         * @property onValueChanged Callback invoked when the slider value changes
         */
        data class SliderPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            val value: Int,
            val valueRange: IntProgression,
            val valueString: String? = null,
            val steps: Int = with(valueRange) { (last - first) - 1 },
            val onValueChanged: (Int) -> Unit,
        ) : PreferenceItem<Int, Unit>() {
            override val icon: Any? = null
        }

        /**
         * A preference item that displays a list of entries as a dialog.
         *
         * @property value The currently selected value
         * @property entries Map of possible values to their display strings
         * @property onValueChange Callback invoked when a new value is selected
         */
        data class ListPreference<T>(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val value: T,
            val entries: Map<T, String>,
            val onValueChange: (T) -> Unit,
        ) : PreferenceItem<T, Unit>() {
            override val onValueChanged: suspend (value: T) -> Unit = { onValueChange(it) }
        }

        /**
         * A preference item that displays text and is clickable.
         *
         * @property onClick Callback invoked when the preference is clicked
         */
        data class TextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val onClick: () -> Unit,
        ) : PreferenceItem<String, Unit>() {
            override val onValueChanged: suspend (value: String) -> Unit = {}
        }

        /**
         * A preference item that shows an EditText in a dialog.
         *
         * @property value The current text value
         * @property onValueChange Callback invoked when a new value is entered.
         *                   Return true to accept the change, false to reject.
         */
        data class EditTextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val value: String,
            val onValueChange: (String) -> Boolean,
        ) : PreferenceItem<String, Boolean>() {
            override val onValueChanged: suspend (value: String) -> Boolean = { onValueChange(it) }
        }

        /**
         * A preference item that displays informational text.
         *
         * Used for headers or explanatory text within settings groups.
         */
        data class InfoPreference(
            override val title: String,
        ) : PreferenceItem<String, Unit>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: Any? = null
            override val onValueChanged: suspend (value: String) -> Unit = {}
        }

        /**
         * A preference item that displays custom content.
         *
         * Allows for completely custom UI within the preferences list.
         *
         * @property content Composable function rendering the custom content
         */
        data class CustomPreference(
            override val title: String,
            val content: @Composable () -> Unit,
        ) : PreferenceItem<Unit, Unit>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: Any? = null
            override val onValueChanged: suspend (value: Unit) -> Unit = {}
        }
    }

    /**
     * A group of related preference items with a header title.
     *
     * Groups provide visual organization by showing a header title
     * above a list of preference items.
     *
     * @property preferenceItems The list of preference items in this group
     */
    data class PreferenceGroup(
        override val title: String,
        override val enabled: Boolean = true,
        val preferenceItems: List<Preference.PreferenceItem<out Any, out Any>>,
    ) : Preference()
}
```

---

### Appendix: SearchableSettings.kt

**File:** `presentation/settings/SearchableSettings.kt`

```kotlin
/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.runtime.Composable
import us.blindmint.codex.presentation.navigator.Screen

/**
 * Interface for settings screens that support search.
 *
 * Settings screens should implement this interface to make their
 * individual settings searchable via the search screen.
 *
 * The interface provides:
 * - A title for breadcrumbs in search results
 * - A list of preferences for building the search index
 * - A highlight key mechanism for highlighting matching items
 */
interface SearchableSettings : Screen {

    /**
     * Returns the screen's title for use in search breadcrumbs.
     *
     * @return The localized screen title
     */
    @Composable
    fun getTitle(): String

    /**
     * Returns the screen's preferences for search indexing.
     *
     * This method should return a list of Preference objects that
     * represent all the settings on this screen. The preferences
     * can include PreferenceGroups for organization.
     *
     * @return List of preferences for this screen
     */
    @Composable
    fun getPreferences(): List<Preference>

    companion object {
        /**
         * The title of the target PreferenceItem to highlight.
         *
         * This should be set before navigating to a settings screen
         * and will be reset after the highlight animation completes.
         *
         * HACK: This static property is used for cross-screen
         * communication to trigger the highlight animation when
         * navigating from search results.
         */
        var highlightKey: String? = null
    }
}
```

---

### Appendix: PreferenceScreen.kt

**File:** `presentation/settings/PreferenceScreen.kt`

```kotlin
/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar

/**
 * Composable that renders a list of preferences.
 *
 * Supports highlighting and automatic scrolling to a highlighted item
 * when [SearchableSettings.highlightKey] is set.
 *
 * @param items The list of preferences to render
 * @param modifier Modifier to be applied to the layout
 * @param contentPadding Padding values for the content
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey

    // Scroll to highlighted item if set
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds) // Wait for UI to settle
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null // Reset highlight
        }
    }

    LazyColumnWithScrollbar(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                is Preference.PreferenceGroup -> {
                    // Skip disabled groups
                    if (!preference.enabled) return@fastForEachIndexed

                    // Render group header
                    item {
                        Column {
                            SettingsSubcategoryTitle(text = preference.title)
                        }
                    }

                    // Render group items
                    items(preference.preferenceItems) { item ->
                        PreferenceItem(
                            item = item,
                            highlightKey = highlightKey,
                        )
                    }

                    // Add spacer between groups
                    item {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Render top-level preference items (not in a group)
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

/**
 * Finds the flattened index of a highlighted item.
 *
 * Accounts for group headers and spacers in the layout.
 *
 * @param highlightKey The title of the item to find
 * @return The index of the item, or -1 if not found
 */
private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            // Flatten group: header + items + spacer
            buildList<String?> {
                add(null) // Header (no title)
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer (no title)
            }
        } else {
            // Top-level item
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}
```

---

### Appendix: PreferenceItem.kt

**File:** `presentation/settings/PreferenceItem.kt`

```kotlin
/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.presentation.core.components.settings.GenericOption
import us.blindmint.codex.presentation.core.components.settings.StyledText
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryNote

/**
 * Composable that renders a single preference item.
 *
 * Dispatches to the appropriate widget based on the preference type
 * and handles the highlight animation if the item matches [highlightKey].
 *
 * @param item The preference item to render
 * @param highlightKey The title of the item to highlight, or null
 */
@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
) {
    StatusWrapper(
        item = item,
        highlightKey = highlightKey,
    ) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                SwitchPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    checked = item.checked,
                    onCheckedChanged = item.onCheckedChanged,
                )
            }
            is Preference.PreferenceItem.SliderPreference -> {
                SliderWithTitle(
                    value = item.value,
                    valueRange = item.valueRange,
                    label = item.title,
                    subtitle = item.subtitle,
                    valueString = item.valueString,
                    onValueChange = item.onValueChanged,
                    steps = item.steps,
                )
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                val selectedEntry = item.entries[item.value]
                GenericOption(
                    label = item.title,
                    value = selectedEntry,
                    onClick = {
                        // TODO: Show selection dialog
                    },
                )
            }
            is Preference.PreferenceItem.TextPreference -> {
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onClick = item.onClick,
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                EditTextPreferenceWidget(
                    value = item.value,
                    title = item.title,
                    subtitle = item.subtitle,
                    onConfirm = item.onValueChange,
                )
            }
            is Preference.PreferenceItem.InfoPreference -> {
                SettingsSubcategoryNote(text = item.title)
            }
            is Preference.PreferenceItem.CustomPreference -> {
                item.content()
            }
        }
    }
}

/**
 * Wrapper that handles enabled/disabled states and highlight animation.
 *
 * @param item The preference item to wrap
 * @param highlightKey The title of the item to highlight
 * @param content The content to render
 */
@Composable
private fun StatusWrapper(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val enabled = item.enabled
    val highlighted = item.title == highlightKey

    AnimatedVisibility(
        visible = enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        if (highlighted) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

/**
 * Widget for rendering a switch preference.
 */
@Composable
private fun SwitchPreferenceWidget(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChanged(!checked) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StyledText(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        subtitle?.let {
            StyledText(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChanged,
        )
    }
}

/**
 * Widget for rendering a text preference.
 */
@Composable
private fun TextPreferenceWidget(
    title: String,
    subtitle: String? = null,
    icon: Any? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                painter = when (it) {
                    is androidx.compose.ui.graphics.vector.ImageVector -> rememberVectorPainter(it)
                    is androidx.compose.ui.graphics.painter.Painter -> it
                    else -> null
                } ?: return@let,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(24.dp))
        }
        StyledText(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        subtitle?.let {
            StyledText(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Widget for rendering an edit text preference.
 */
@Composable
private fun EditTextPreferenceWidget(
    value: String,
    title: String,
    subtitle: String? = null,
    onConfirm: (String) -> Boolean,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StyledText(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        StyledText(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        subtitle?.let {
            StyledText(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showDialog) {
        // TODO: Show EditText dialog
        showDialog = false
    }
}
```

---

### Appendix: FuzzySearchHelper.kt (Updated)

**File:** `utils/FuzzySearchHelper.kt`

Add to existing file:

```kotlin
/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.utils

import me.xdrop.fuzzywuzzy.FuzzySearch
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.presentation.settings.Preference
import us.blindmint.codex.presentation.settings.SettingsItem

/**
 * Fuzzy search utility for OPDS catalog browsing and settings.
 *
 * Provides fuzzy search capabilities to improve discoverability in OPDS catalogs and settings menus.
 * Uses the FuzzyWuzzy library for string similarity matching.
 *
 * @property threshold Minimum similarity score (0-100) to consider a match
 */
object FuzzySearchHelper {

    /**
     * Search through OPDS entries using fuzzy matching.
     *
     * @param entries List of OPDS entries to search
     * @param query Search query string
     * @param threshold Minimum similarity score (0-100) to consider a match
     * @return Filtered and sorted list of entries
     */
    fun searchEntries(
        entries: List<OpdsEntry>,
        query: String,
        threshold: Int = 60
    ): List<OpdsEntry> {
        if (query.isBlank()) return entries

        val queryLower = query.lowercase()

        val entryScores = entries.map { entry ->
            val titleScore = entry.title?.let { title ->
                FuzzySearch.partialRatio(queryLower, title.lowercase())
            } ?: 0

            val authorScore = entry.author?.let { author ->
                FuzzySearch.partialRatio(queryLower, author.lowercase())
            } ?: 0

            val summaryScore = entry.summary?.let { summary ->
                FuzzySearch.partialRatio(queryLower, summary.lowercase())
            } ?: 0

            val maxScore = maxOf(titleScore, authorScore, summaryScore)

            Pair(entry, maxScore)
        }

        return entryScores
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Search through settings items using fuzzy matching.
     *
     * @param items List of settings items to search
     * @param query Search query string
     * @param threshold Minimum similarity score (0-100) to consider a match
     * @return Filtered and sorted list of settings items
     */
    fun searchSettings(
        items: List<SettingsItem>,
        query: String,
        threshold: Int = 60
    ): List<SettingsItem> {
        if (query.isBlank()) return items

        val queryLower = query.lowercase()

        val itemScores = items.map { item ->
            val titleScore = FuzzySearch.partialRatio(queryLower, item.title.lowercase())
            val maxScore = titleScore

            Pair(item, maxScore)
        }

        return itemScores
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Search through Preference items using fuzzy matching.
     *
     * Recursively searches through PreferenceGroups and PreferenceItems,
     * building breadcrumb paths for nested items.
     *
     * @param preferences List of Preference items to search (can include PreferenceGroups)
     * @param query Search query string
     * @param threshold Minimum similarity score (0-100) to consider a match (default: 60)
     * @param breadcrumbs Breadcrumb path for nested items (default: empty)
     * @return List of matching PreferenceItems with scores and breadcrumb paths
     */
    fun searchPreferences(
        preferences: List<Preference>,
        query: String,
        threshold: Int = 60,
        breadcrumbs: String = ""
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val queryLower = query.lowercase()
        val results = mutableListOf<SearchResult>()

        /**
         * Recursively search through preference items.
         *
         * @param items The items to search
         * @param currentBreadcrumbs The breadcrumb path for these items
         */
        fun searchItems(items: List<Preference.PreferenceItem<*, *>>, currentBreadcrumbs: String) {
            items.forEach { item ->
                // Skip disabled or blank items
                if (!item.enabled || item.title.isBlank()) return@forEach

                // Calculate fuzzy match scores
                val titleScore = FuzzySearch.partialRatio(queryLower, item.title.lowercase())
                val subtitleScore = item.subtitle?.let {
                    FuzzySearch.partialRatio(queryLower, it.lowercase())
                } ?: 0

                val maxScore = maxOf(titleScore, subtitleScore)

                // Add result if score meets threshold
                if (maxScore >= threshold) {
                    results.add(
                        SearchResult(
                            preference = item,
                            score = maxScore,
                            breadcrumbs = currentBreadcrumbs,
                        )
                    )
                }
            }
        }

        // Search through preferences (top-level items and groups)
        preferences.forEach { pref ->
            when (pref) {
                is Preference.PreferenceGroup -> {
                    // Skip disabled groups
                    if (!pref.enabled) return@forEach

                    // Build breadcrumb for this group
                    val newBreadcrumbs = if (breadcrumbs.isEmpty()) {
                        pref.title
                    } else {
                        "$breadcrumbs > ${pref.title}"
                    }

                    // Search items in this group
                    searchItems(pref.preferenceItems, newBreadcrumbs)
                }
                is Preference.PreferenceItem<*, *> -> {
                    // Skip disabled or blank items
                    if (!pref.enabled || pref.title.isBlank()) return@forEach

                    // Calculate fuzzy match scores
                    val titleScore = FuzzySearch.partialRatio(queryLower, pref.title.lowercase())
                    val subtitleScore = pref.subtitle?.let {
                        FuzzySearch.partialRatio(queryLower, it.lowercase())
                    } ?: 0

                    val maxScore = maxOf(titleScore, subtitleScore)

                    // Add result if score meets threshold
                    if (maxScore >= threshold) {
                        results.add(
                            SearchResult(
                                preference = pref,
                                score = maxScore,
                                breadcrumbs = breadcrumbs,
                            )
                        )
                    }
                }
            }
        }

        // Sort by score (highest first) and limit results
        return results
            .sortedByDescending { it.score }
            .take(50) // Limit to 50 results
    }

    /**
     * Data class representing a search result.
     *
     * @property preference The matching preference item
     * @property score The fuzzy match score (0-100)
     * @property breadcrumbs The breadcrumb path to this item (e.g., "Reader > Font")
     */
    data class SearchResult(
        val preference: Preference.PreferenceItem<*, *>,
        val score: Int,
        val breadcrumbs: String,
    )
}
```

---

### Appendix: SettingsSearchScreen.kt

**File:** `presentation/settings/SettingsSearchScreen.kt`

```kotlin
/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.common.NavigatorBackIconButton
import us.blindmint.codex.presentation.core.components.settings.StyledText
import us.blindmint.codex.presentation.navigator.Screen
import us.blindmint.codex.utils.FuzzySearchHelper

/**
 * Search screen for finding individual settings across all settings screens.
 *
 * Provides a full-screen search interface with fuzzy matching, breadcrumb
 * paths, and navigation to matching settings.
 */
class SettingsSearchScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val softKeyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val listState = rememberLazyListState()

        // Hide keyboard when leaving screen
        DisposableEffect(Unit) {
            onDispose {
                softKeyboardController?.hide()
            }
        }

        // Hide keyboard when scrolling
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        // Auto-focus search field on launch
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        val textFieldState = rememberTextFieldState()

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            NavigatorBackIconButton(
                                navigateBack = { navigator?.pop() }
                            )
                        },
                        title = {
                            BasicTextField(
                                state = textFieldState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyLarge
                                    .copy(color = MaterialTheme.colorScheme.onSurface),
                                lineLimits = TextFieldLineLimits.SingleLine,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                onKeyboardAction = { focusManager.clearFocus() },
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorator = {
                                    if (textFieldState.text.isEmpty()) {
                                        Text(
                                            text = "Search settings...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                    it()
                                },
                            )
                        },
                        actions = {
                            if (textFieldState.text.isNotEmpty()) {
                                IconButton(onClick = { textFieldState.clearText() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            },
        ) { contentPadding ->
            SearchResult(
                searchKey = textFieldState.text.toString(),
                listState = listState,
                contentPadding = contentPadding,
            ) { result ->
                SearchableSettings.highlightKey = result.preference.title
                navigator?.replace(result.targetScreen)
            }
        }
    }
}

/**
 * Composable that renders search results.
 *
 * Shows empty state when no query, "no results" state when no matches,
 * and a list of matching items when results are available.
 *
 * @param searchKey The search query
 * @param listState LazyListState for the results list
 * @param contentPadding Padding values for the content
 * @param onItemClick Callback invoked when a result is clicked
 */
@Composable
private fun SearchResult(
    searchKey: String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    onItemClick: (SearchResultItem) -> Unit,
) {
    if (searchKey.isEmpty()) return

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val screens = remember { getSearchableSettingsScreens() }

    val results by produceState<List<SearchResultItem>?>(initialValue = null, searchKey) {
        value = buildSearchResults(screens, searchKey, isLtr)
    }

    Crossfade(
        targetState = results,
        label = "search_results",
    ) {
        when {
            it == null -> {
                // Loading state
            }
            it.isEmpty() -> {
                // No results state
                LazyColumnWithScrollbar(
                    modifier = modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                ) {
                    item {
                        Text(
                            text = "No results found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                // Results list
                LazyColumnWithScrollbar(
                    modifier = modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(
                        items = it,
                        key = { i -> i.hashCode() },
                    ) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                        ) {
                            StyledText(
                                text = item.title,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            StyledText(
                                text = item.breadcrumbs,
                                modifier = Modifier.paddingFromBaseline(top = 16.dp),
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns the list of searchable settings screens.
 *
 * This function builds the search index by calling getTitle() and
 * getPreferences() on each SearchableSettings screen.
 *
 * @return List of settings data for search indexing
 */
@Composable
@NonRestartableComposable
private fun getSearchableSettingsScreens(): List<SearchableSettingsData> {
    // TODO: Add all settings screens once they implement SearchableSettings
    return listOf(
        // Example (uncomment after implementing SearchableSettings on screens):
        // SearchableSettingsData(
        //     title = ReaderSettingsScreen.getTitle(),
        //     screen = ReaderSettingsScreen,
        //     preferences = ReaderSettingsScreen.getPreferences(),
        // ),
        // SearchableSettingsData(
        //     title = AppearanceSettingsScreen.getTitle(),
        //     screen = AppearanceSettingsScreen,
        //     preferences = AppearanceSettingsScreen.getPreferences(),
        // ),
    )
}

/**
 * Builds search results from searchable settings screens.
 *
 * @param screens List of searchable settings data
 * @param searchKey The search query
 * @param isLtr Whether the locale is left-to-right
 * @return List of search result items
 */
private fun buildSearchResults(
    screens: List<SearchableSettingsData>,
    searchKey: String,
    isLtr: Boolean
): List<SearchResultItem> {
    return screens.flatMap { screenData ->
        FuzzySearchHelper.searchPreferences(
            preferences = screenData.preferences,
            query = searchKey,
            threshold = 60,
        ).map { searchResult ->
            SearchResultItem(
                targetScreen = screenData.screen,
                title = searchResult.preference.title,
                breadcrumbs = getLocalizedBreadcrumb(
                    path = screenData.title,
                    node = searchResult.breadcrumbs,
                    isLtr = isLtr,
                ),
                preference = searchResult.preference,
            )
        }
    }
}

/**
 * Generates a localized breadcrumb string.
 *
 * Handles both left-to-right and right-to-left locales.
 *
 * @param path The base path (screen title)
 * @param node The sub-path (group title, or empty)
 * @param isLtr Whether the locale is left-to-right
 * @return Formatted breadcrumb string
 */
private fun getLocalizedBreadcrumb(path: String, node: String, isLtr: Boolean): String {
    return if (node.isBlank()) {
        path
    } else {
        if (isLtr) {
            // Left-to-right: "Screen > Group"
            "$path > $node"
        } else {
            // Right-to-left: "Group < Screen"
            "$node < $path"
        }
    }
}

/**
 * Data class representing a searchable settings screen.
 *
 * @property title The screen's title for breadcrumbs
 * @property screen The screen instance for navigation
 * @property preferences The screen's preferences for search indexing
 */
private data class SearchableSettingsData(
    val title: String,
    val screen: Screen,
    val preferences: List<Preference>,
)

/**
 * Data class representing a search result item.
 *
 * @property targetScreen The screen to navigate to when clicked
 * @property title The display title of the matching item
 * @property breadcrumbs The breadcrumb path to the item
 * @property preference The preference item (used for highlighting)
 */
private data class SearchResultItem(
    val targetScreen: Screen,
    val title: String,
    val breadcrumbs: String,
    val preference: Preference.PreferenceItem<*, *>,
)
```

---

## Appendix: File Reference Map

### New Files to Create

| File Path | Purpose | Phase |
|-----------|---------|-------|
| `presentation/settings/Preference.kt` | Preference hierarchy definitions | 1 |
| `presentation/settings/SearchableSettings.kt` | SearchableSettings interface | 1 |
| `presentation/settings/PreferenceScreen.kt` | Preference list renderer | 1 |
| `presentation/settings/PreferenceItem.kt` | Individual preference widget | 1 |
| `presentation/settings/SettingsSearchScreen.kt` | Dedicated search screen | 3 |

### Files to Modify

| File Path | Changes | Phase |
|-----------|---------|-------|
| `utils/FuzzySearchHelper.kt` | Add searchPreferences() and SearchResult | 2 |
| `presentation/settings/SettingsTopBar.kt` | Update search button to navigate to search screen | 3 |
| `presentation/settings/reader/ReaderSettingsScreen.kt` | Implement SearchableSettings | 4 |
| `presentation/settings/appearance/AppearanceSettingsScreen.kt` | Implement SearchableSettings | 4 |
| `presentation/settings/library/LibrarySettingsScreen.kt` | Implement SearchableSettings | 4 |
| `presentation/settings/browse/BrowseSettingsScreen.kt` | Implement SearchableSettings | 4 |
| `presentation/settings/general/GeneralSettingsScreen.kt` | Implement SearchableSettings | 4 |
| `presentation/settings/import_export/ImportExportSettingsScreen.kt` | Implement SearchableSettings | 4 |

### Files to Deprecate/Delete

| File Path | Action | Phase |
|-----------|--------|-------|
| `presentation/settings/SettingsItem.kt` | Deprecate, then delete | 5 |
| `presentation/settings/SearchableSettingsItem.kt` | Deprecate, then delete | 5 |
| `presentation/settings/SearchableSettingsIndex.kt` | Deprecate, then delete | 5 |

---

## Appendix: Migration Example: Reader Settings Font Group

### Before (Current Approach)

```kotlin
@Composable
fun FontSubcategory(
    fontFamily: FontFamily,
    onFontFamilyChange: (FontFamily) -> Unit,
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
) {
    Column {
        SettingsSubcategoryTitle(text = stringResource(R.string.font_settings))

        FontFamilyOption(
            selected = fontFamily,
            onOptionSelected = onFontFamilyChange,
        )

        FontSizeOption(
            value = fontSize,
            onValueChange = onFontSizeChange,
        )
    }
}
```

### After (SearchableSettings Approach)

```kotlin
object ReaderSettingsScreen : Screen(), SearchableSettings {

    @Composable
    override fun getTitle() = stringResource(R.string.reader_settings)

    @Composable
    override fun Content() {
        // ... existing UI (can keep for now)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        return listOf(
            getFontGroup(),
            // ... other groups
        )
    }

    @Composable
    private fun getFontGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.font_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = fontFamily.value,
                    entries = FontFamily.entries.associateWith { it.name },
                    title = stringResource(R.string.font_family),
                    onValueChange = { fontFamily.value = it },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = fontSize.value,
                    valueRange = 8..32,
                    title = stringResource(R.string.font_size),
                    onValueChanged = { fontSize.value = it },
                ),
            ),
        )
    }
}
```

---

## Appendix: Glossary

| Term | Definition |
|-------|------------|
| **Preference** | Mihon-style data class representing a settings item |
| **PreferenceGroup** | A collection of related PreferenceItems with a group header |
| **PreferenceItem** | An individual settings item (Switch, Slider, List, etc.) |
| **SearchableSettings** | Interface for settings screens that support search |
| **Fuzzy Search** | Search that uses string similarity matching (not exact matches) |
| **Breadcrumbs** | Hierarchical path showing where a setting is located (e.g., "Reader > Font") |
| **Highlight Key** | The title of the item to highlight when navigating from search |
| **PreferenceScreen** | Composable that renders a list of Preference items |
| **SettingsSearchScreen** | Dedicated screen for searching all settings |
| **Partial Ratio** | FuzzyWuzzy algorithm for string similarity matching |

---

## Appendix: References

### Mihon Repository

- **Repository:** https://github.com/mihonapp/mihon
- **Settings Directory:** `app/src/main/java/eu/kanade/presentation/more/settings/`
- **Key Files:**
  - `Preference.kt` - Preference hierarchy
  - `PreferenceScreen.kt` - Preference renderer
  - `PreferenceItem.kt` - Preference widget
  - `SearchableSettings.kt` - Searchable interface
  - `SettingsSearchScreen.kt` - Search screen
  - `SettingsMainScreen.kt` - Main settings screen
  - `screen/SettingsReaderScreen.kt` - Example SearchableSettings implementation

### Codex Repository

- **Repository:** https://github.com/BlindMint/codex
- **Settings Directory:** `app/src/main/java/us/blindmint/codex/presentation/settings/`
- **Key Files:**
  - `SettingsItem.kt` - Current settings item (to be replaced)
  - `SearchableSettingsItem.kt` - Current searchable item (to be replaced)
  - `SearchableSettingsIndex.kt` - Manual search index (to be replaced)
  - `SettingsLayout.kt` - Main settings list
  - `SettingsTopBar.kt` - Top bar with search
  - `reader/` - Reader settings (first to migrate)
  - `appearance/` - Appearance settings
  - `library/` - Library settings
  - `browse/` - Browse settings
  - `general/` - General settings
  - `import_export/` - Import/Export settings

### External Libraries

- **FuzzyWuzzy:** https://github.com/xdrop/fuzzywuzzy
  - String similarity matching library
  - Used by Codex for fuzzy search
  - Key method: `FuzzySearch.partialRatio(query, target)`

---

## Appendix: Troubleshooting

### Issue: Search results not showing

**Symptoms:** User types in search field, but no results appear.

**Possible Causes:**
1. Settings screens don't implement SearchableSettings
2. `getSearchableSettingsScreens()` is empty
3. `getPreferences()` returns empty list
4. Fuzzy search threshold is too high
5. Preferences have blank titles

**Solutions:**
1. Verify screens implement SearchableSettings: `object X : Screen(), SearchableSettings`
2. Verify `getSearchableSettingsScreens()` includes all screens
3. Verify `getPreferences()` returns non-empty list
4. Lower threshold (try 40 instead of 60)
5. Verify all PreferenceItems have non-blank titles

---

### Issue: Highlight not showing

**Symptoms:** User clicks search result, navigates to screen, but item is not highlighted.

**Possible Causes:**
1. `highlightKey` not set before navigation
2. `highlightKey` doesn't match preference title
3. PreferenceScreen not checking `highlightKey`
4. Highlight animation not triggering

**Solutions:**
1. Verify `SearchableSettings.highlightKey = result.preference.title` is called before navigation
2. Verify preference title matches exactly (case-sensitive)
3. Verify `SearchableSettings.highlightKey` is checked in PreferenceScreen
4. Verify LaunchedEffect is triggering (add logging)

---

### Issue: Scroll to highlight not working

**Symptoms:** Item is highlighted but doesn't scroll into view.

**Possible Causes:**
1. `findHighlightedIndex()` returning -1
2. Scroll index calculation incorrect
3. Animation not triggering
4. Delay too short (UI not ready)

**Solutions:**
1. Debug `findHighlightedIndex()` to verify it finds the item
2. Verify index accounts for group headers and spacers
3. Verify `animateScrollToItem()` is called
4. Increase delay (try 1 second instead of 0.5)

---

### Issue: Search performance is slow

**Symptoms:** Search results take > 1 second to appear.

**Possible Causes:**
1. Searching too many items
2. Fuzzy matching on large strings
3. No debouncing
4. Rebuilding index on every keystroke

**Solutions:**
1. Reduce result limit (try 25 instead of 50)
2. Increase threshold (try 70 instead of 60)
3. Implement debouncing (wait 300ms after typing stops)
4. Cache `getPreferences()` results

---

### Issue: Breadcrumbs incorrect

**Symptoms:** Search results show wrong breadcrumb path.

**Possible Causes:**
1. Breadcrumb building logic incorrect
2. RTL/LTR handling wrong
3. Group title not included
4. Screen title not included

**Solutions:**
1. Verify `getLocalizedBreadcrumb()` logic
2. Test with RTL locale
3. Verify group titles are included in breadcrumbs
4. Verify screen titles are included in breadcrumbs

---

## Appendix: Performance Considerations

### Search Index Size

**Estimate:** If each screen has ~50 preferences and there are 6 screens, total index size = 300 items.

**Impact:**
- Building index: ~10-20ms
- Fuzzy matching: ~50-100ms for 300 items
- Acceptable for current use case

**Optimizations:**
- Cache index (rebuild only when settings change)
- Debounce search input
- Limit results to 50 items

### Memory Usage

**Estimate:**
- Each Preference: ~200 bytes
- Each SearchResult: ~100 bytes
- Total: 300 × 200 + 50 × 100 = 65KB

**Impact:** Negligible

### Rendering Performance

**Estimate:**
- Rendering 50 results: ~50ms
- Animation overhead: ~20ms

**Impact:** Acceptable

**Optimizations:**
- Use `key` parameter in `items()` for stable IDs
- Use `LazyColumn` for efficient rendering
- Avoid expensive calculations in composables

---

## Appendix: Future Enhancements

### Potential Improvements

1. **Search History**
   - Remember recent searches
   - Show as chips below search field
   - Allow one-tap to re-search

2. **Quick Actions**
   - Add buttons for common settings
   - Examples: Dark Theme, Font Size, Orientation
   - Display when search field is empty

3. **Advanced Search**
   - Support search operators (AND, OR, NOT)
   - Allow filtering by category
   - Support regex patterns

4. **Voice Search**
   - Add microphone button
   - Use speech recognition API
   - Search by voice command

5. **Keyboard Shortcuts**
   - Ctrl+F / Cmd+F to open search
   - Escape to close search
   - Arrow keys to navigate results

6. **Search Suggestions**
   - Autocomplete as user types
   - Show top matches inline
   - Highlight match in suggestion

7. **Search Analytics**
   - Track popular searches
   - Identify hard-to-find settings
   - Improve settings organization based on usage

8. **Export/Import Search Results**
   - Share search results via clipboard
   - Export to text/JSON
   - Import from file

---

## Appendix: Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-30 | Initial migration plan document |

---

## Document End

**Author:** Sisyphus AI Agent
**Last Updated:** January 30, 2026
**Status:** Ready for Review
