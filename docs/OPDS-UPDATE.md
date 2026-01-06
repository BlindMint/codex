# Codex App Update Implementation Document

## Overview
This document outlines a comprehensive plan for updating the Codex e-reader app to incorporate OPDS support, enhanced library filtering, tag management, and UI refinements. The goal is to evolve Codex from a minimalist local e-reader into a hybrid OPDS/local file reading app that seamlessly integrates with remote OPDS catalogs like Calibre/calibre-web, while preserving its core strengths: simplicity, customization (themes/fonts), and lightweight performance.

Key principles:
- **Minimalism**: Avoid UI bloat; prioritize discoverability and curation for small, on-device libraries.
- **OPDS-Centric**: Leverage OPDS for metadata (tags, series, authors, etc.) to reduce manual input.
- **Unified Experience**: Blend local and remote books without silos.

## Summary
I want to add OPDS Catalog support to this app, Codex. The app is currently used for importing book files locally from an Android system (supports most modern phones and tablets). I want to preserve that local functionality, but its usage will be secondary to OPDS browsing, adding, pulling metadata, etc. The current themes, reading settings, color presets, and similar settings for actually reading books (regardless of initial source) should be largely or entirely unchanged with this update.

I want to remove the multiple tabs from the top of the Library (Reading, Planning, Already Read, Favorites) and instead use a single "Library" page - the book count indicator at the top can remain, and in fact should reflect the new filters that we are going to add - e.g. if there are 200 books in the Library, "200" will display, but if the user filters by a tag or author and only 25 books are displayed, the book count should reflect "25". The functionality of the other tabs/categories currently on top of the library that will be moved to the new side panel. To open this new panel, we need to add an icon to the top of the library screen, to the left of the "search" icon - you can re-use the 3-line icon from the in-book menu to open Chapters, so the icons are "side panel", "search", "Sort & Display".

We need to add OPDS authentication, browsing, and importing settings and similar to Settings > Browse for browsing OPDS remote feeds/information and downloading (similar to how local files are handled now, but tailored to browsing, connecting to, and authenticating, etc. with OPDS sources)

Settings > Browse currently has options towards the bottom of the menu for Display, Filter, and Sort. These aren't too useful here - the "Browse" menu that opens after scanning folders (where the designated folders and subfolders are displayed and where users can select books to be added) contains icons and functions for searching, sorting, and filtering, etc. Let's remove these settings from Settings > Browse which will also leave room for the primary OPDS scanning/browsing implementation settings. For usability, we also need to add sorting, filtering, etc to the proper places for browsing and adding OPDS books. Under Settings > Browse there should be at least two sections: Local files and OPDS (you can use these names or create slightly improved ones if you can think of better options).

## Complete Feature List
### Core Features
- **OPDS Catalog Support**:
  - Add multiple OPDS sources (e.g., calibre-web URL with auth).
  - Hierarchical browsing (authors > series > books, tags > books).
  - Search via OPDS OpenSearch.
  - Book previews: View metadata/summaries/covers without downloading.
  - Download/add books with auto-metadata import.
- **Metadata Import from OPDS**:
  - Fields: Title, authors (multi), tags/categories, series (with index), summary, publication date, language, publisher, rights, identifiers (UUID/ISBN), covers.
  - Custom Calibre columns are not currently in-use, but may be added later.
- **Enhanced Filtering**:
  - Dimensions: Authors, tags, series, publication date (range/sort), language, publisher, rights.
  - Multi-select with AND/OR logic.
  - Quick presets for status (e.g., "Reading," "Planning" as tag-based).
- **Tag Management**:
  - Auto-import tags from OPDS.
  - Local add/remove via book details (chip input).
  - Search by tag; filter by multiple tags.
  - Optional tag cloud in filter panel.
- **Unified Library View**:
  - Single screen showing all books (local + optional OPDS previews).
  - Grid/list toggle with customizable size, title position, progress visibility.
  - Dimmed previews for undownloaded books with "Download" button.
- **Deduplication**:
  - UUID/ISBN-based checks on download (prompt/skip).
- **Sorting Enhancements**:
  - Add OPDS fields: Publication date, series order, tags.

### Nice-to-Haves (Prioritize After Core)
- Bulk actions: Download series, add tags to selected.
- Refresh metadata from OPDS (per-book or global).
- Offline OPDS feed caching.
- Export/share OPDS book links.
- Accessibility: Auto-RTL based on language metadata.
- Stats: Reading progress filtered by tags/authors.
- Multi-catalog: Public OPDS (e.g., Gutenberg).

### Removals/Deprecations
- Manual folder navigation: Replace with OPDS browsing (keep basic file picker as fallback).
- Mutually exclusive tabs: Evolve to dynamic tag-based filters.
- Basic categories: Merge into tags.
- Prominent metadata editing: Demote to "local overrides" only.

## Architecture Changes
- **Database Schema Updates** (Using Room or SQLite, per repo):
  - Add fields to Book entity: `tags` (List<String>), `seriesName` (String), `seriesIndex` (Int), `publicationDate` (Date), `language` (String), `publisher` (String), `summary` (String), `uuid` (String, from OPDS ID), `isbn` (String), `source` (Enum: Local/OPDS).
  - Add `remoteUrl` for undownloaded previews.
  - Migration: Handle existing books by parsing embedded OPF if needed.
- **Networking**:
  - Use Retrofit or Ktor for HTTP; OkHttp for auth/caching.
  - Parse OPDS feeds with a library (e.g., [opds-browser](https://github.com/opds-community/opds-browser) or custom XML/Atom parsing via SimpleXML).
- **State Management**:
  - Use ViewModel + LiveData/Flow for library state (local books + fetched OPDS previews).
  - Repository pattern: Separate LocalBookRepo and OpdsRepo; combine in LibraryViewModel.
- **Hybrid View Logic**:
  - LibraryAdapter: Handle two item types (LocalBookItem, OpdsPreviewItem) with diffing (ListAdapter).
  - When "Show Available" toggle is on, fetch OPDS root feed and merge results dynamically (e.g., via search/filter queries).

Mock Code Snippet (Kotlin, LibraryViewModel):
```kotlin
class LibraryViewModel(private val localRepo: LocalBookRepo, private val opdsRepo: OpdsRepo) : ViewModel() {
    val showOpdsPreviews = MutableStateFlow(false)  // Toggle state
    val filters = MutableStateFlow(FilterState())   // Tags, authors, etc.

    val libraryItems: Flow<List<LibraryItem>> = combine(showOpdsPreviews, filters) { showPreviews, filter ->
        val local = localRepo.getBooks(filter)
        if (!showPreviews) return@combine local
        val opds = opdsRepo.fetchPreviews(filter)  // Query OPDS with filter params
        mergeLocalAndOpds(local, opds)  // Dedupe by UUID
    }
}
```

## UI Layout
### Main Library Screen Mock-Up (Text-Based)
```
[Top Bar]
Library (13)          [Search Icon] [Filter Icon] [Sort Icon] [Cloud Toggle (on/off)]

[Horizontal Preset Chips (Optional)]
All | Reading | Planning | Favorites

[Grid/List View]
[Book Card 1: Cover, Title, Author, Progress %, Play Button]
[Book Card 2: Dimmed Cover (OPDS), Title, Author, Download Button]
...

[Bottom Nav]
Library | History | Browse (OPDS Catalogs) | Settings
```

- **Filter Side Panel** (Swipe left or tap Filter Icon):
  ```
  Filters
  [Clear All]

  Status Presets:
  - Reading [Chip]
  - Planning [Chip]
  - Already Read [Chip]
  - Favorites [Chip]

  Tags:
  [Tag Cloud: Fantasy(large), Sci-Fi, To-Read...]
  OR [Searchable List: Multi-select chips]

  Authors: [Dropdown or Search]
  Series: [Dropdown]
  Publication Year: [Slider: 1900-2026]
  Language: [Chips: en, fr...]
  [Apply Button]
  ```

- **Changes from Screenshots**:
  - Remove horizontal tabs (Reading/Planning/etc.); replace with preset chips or side panel section.
  - Add Cloud Toggle icon in top bar (e.g., using Material Icons: `ic_cloud`).
  - Enhance Sort & Display: Add "Show Tags" toggle, new sorts (Date, Series).

Implementation: Use Jetpack Compose for modern UI (if migrating from XML) or ConstraintLayout/RecyclerView. For side panel, use DrawerLayout or BottomSheetDialog.

## OPDS Integration Details
- **Libraries**: [OPDS Java Client](https://github.com/opds-community/opds-java-client) or parse manually with Jsoup/XMLPullParser.
- **Flow**:
  1. Settings: Add OPDS source (URL, username/password).
  2. Browse: Navigate feeds (e.g., GET /opds → parse <feed> entries).
  3. Search: Use <opensearch:description> link to query.
  4. Download: Follow <link rel="acquisition">; save file, import metadata.
- **Error Handling**: Offline mode (cache last feed), auth failures.
- Resources: [OPDS Spec](https://specs.opds.io/opds-1.2), [Calibre OPDS Docs](https://manual.calibre-ebook.com/generated/en/server.html#opds).

Mock Code Snippet (OpdsRepo):
```kotlin
suspend fun fetchPreviews(filter: FilterState): List<OpdsItem> {
    val response = httpClient.get("$baseUrl/opds/search?q=${filter.query}&tags=${filter.tags.joinToString()}")
    // Parse Atom XML to OpdsItem objects
    return parseOpdsFeed(response.bodyAsText())
}
```

## Tag Handling
- **Import**: From OPDS <category> tags.
- **UI**: Book details → Editable chip list.
- **Filtering**: In side panel, list unique tags from DB (local + OPDS).
- **Status Mapping**: Predefine tags like "#reading", "#planning"; apply on add if metadata matches (e.g., Calibre shelf).

## Filtering System
- **FilterState Class**: Data class with query, tags (Set<String>), authors, dateRange, etc.
- **Application**: Use SQL queries for local (e.g., WHERE tags LIKE '%fantasy%'), append to OPDS URLs for remote.
- **Logic**: AND for multi-tags by default; settings toggle for OR.

## Deduplication
- On download intent: Check local DB for matching UUID/ISBN.
- Prompt: AlertDialog "Duplicate detected – Download anyway?"
- Code: In DownloadUseCase, query `SELECT * FROM books WHERE uuid = ?`.

## Implementation and Update Order
1. **Prep (1-2 days)**: Update DB schema/migration. Add OpdsSource entity.
2. **OPDS Backend (3-5 days)**: Implement OpdsRepo, parsing, basic fetching.
3. **Metadata Import (2 days)**: Handle import on download.
4. **UI Basics (2-3 days)**: Unified library RecyclerView with item types.
5. **Toggle & Previews (2 days)**: Add top bar toggle, dimmed items.
6. **Filtering (3-4 days)**: Side panel, FilterState, apply to local/OPDS.
7. **Tags (2 days)**: Import, editing, tag cloud.
8. **Sorting/Deduplication (1-2 days)**: Enhance menu, add checks.
9. **Polish/Nice-to-Haves (3-5 days)**: Bulk, caching, testing.
10. **Release**: Update README, GitHub issues for feedback.

## Checklists
### Pre-Implementation
- [ ] Review OPDS feeds from your calibre-web (e.g., curl http://your-server/opds).
- [ ] Set up test Calibre library with tags/series.
- [ ] Backup current repo branch.

### Testing
- [ ] OPDS: Connect, browse, download, metadata accuracy.
- [ ] Filtering: Multi-tag, date range, hybrid view.
- [ ] Edge Cases: Offline, duplicates, large feeds.
- [ ] Performance: Small library (your use case) vs. 100+ books.
- [ ] UI: Dark/light mode, tablet landscape.

### Post-Release
- [ ] GitHub: Add OPDS setup guide, changelog.
- [ ] Solicit feedback: Poll on Reddit/r/androidapps or GitHub discussions.

## Resources/Links
- Android OPDS Examples: [FBReader OPDS Code](https://github.com/geometer/FBReaderJ) (inspiration).
- UI Components: [Material Design Docs](https://m3.material.io/) for chips/sliders.
- Libraries: Retrofit ([square/retrofit](https://github.com/square/retrofit)), Room ([Android Room](https://developer.android.com/training/data-storage/room)).
- Mock-Ups Tools: Use Figma or Excalidraw for visuals if needed.

This document is self-contained but iterative—update as you implement. If issues arise (e.g., OPDS parsing quirks), reference the linked specs or test with tools like Postman. Happy coding!

