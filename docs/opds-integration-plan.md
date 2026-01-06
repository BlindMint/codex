# OPDS Integration Implementation Plan

## Overview
This document outlines the comprehensive plan for integrating OPDS (Open Publication Distribution System) support into Codex, evolving it from a local eBook reader to a hybrid OPDS/local reading app. The implementation will preserve the app's minimalist Material You design, panel-based navigation, and existing reading features while adding robust OPDS browsing, metadata import, and enhanced filtering capabilities.

## Current App Structure Analysis

### Architecture
- **Clean Architecture**: Data (Room DB, parsers), Domain (repositories, use cases), Presentation (ViewModels, Compose UI), Core (crash handling)
- **Database**: Room v2.7.1, SQLite, entities: BookEntity, HistoryEntity, ColorPresetEntity, BookProgressHistoryEntity, BookmarkEntity (v14)
- **UI**: Jetpack Compose, Material 3, panel-based navigation
- **Supported Formats**: PDF, TXT, EPUB, FB2, HTML, HTM, MD, FODT
- **Current Features**: Local file import, categories (READING/PLANNING/ALREADY_READ/FAVORITES), basic search/sort/filter

### Key Components
- **LibraryScreen**: Tabbed view with categories, sort by name/last read/progress/author
- **BrowseScreen**: Local file browsing with filter/sort settings
- **Settings > Browse**: Display, filter, sort options (to be refactored)
- **BookEntity**: Basic metadata (title, author, description, category, filePath, progress, image)

## Change Analysis

### Major Changes Required
1. **Database Schema Evolution**: Add OPDS-related fields to BookEntity, new OpdsSource entity
2. **New Repositories**: OpdsRepository for OPDS operations, enhanced BookRepository for hybrid queries
3. **UI Refactoring**: Remove library tabs, add side panel filters, unify local/OPDS views
4. **Networking**: Add HTTP client for OPDS feeds, authentication support
5. **Metadata Import**: OPDS metadata mapping and deduplication logic
6. **Settings Enhancement**: OPDS source management, browse settings reorganization

### Preserved Elements
- Material 3 design language
- Reading experience (themes, fonts, progress tracking)
- Local file support as secondary feature
- Clean architecture principles
- Navigation structure (Library, History, Browse, Settings)

## Detailed Implementation Checklist

### Phase 1: Database and Data Layer Preparation
- [x] Update BookEntity to include OPDS fields:
  - tags: List<String> (Room TypeConverters needed)
  - seriesName: String?
  - seriesIndex: Int?
  - publicationDate: Long? (timestamp)
  - language: String?
  - publisher: String?
  - summary: String?
  - uuid: String? (from OPDS ID)
  - isbn: String?
  - source: BookSource enum (LOCAL, OPDS)
  - remoteUrl: String? (for OPDS previews)
- [x] Create OpdsSourceEntity: id, name, url, username, password (encrypted)
- [x] Add Room migrations (v14 -> v15)
- [x] Implement TypeConverters for List<String> and BookSource
- [x] Update BookDao with new queries for OPDS fields (filter by tags, series, etc.)
- [x] Create OpdsSourceDao

### Phase 2: OPDS Backend Implementation
- [x] Add networking dependencies: Retrofit, OkHttp, SimpleXML
- [x] Create OpdsApiService interface with Retrofit annotations
- [x] Implement OpdsRepository:
  - fetchFeed(url): OpdsFeed
  - search(query, filters): List<OpdsEntry>
- [x] Create domain models: OpdsFeed, OpdsEntry, OpdsLink
- [x] Implement OPDS XML parsing (ATOM/OPDS format) with SimpleXML DTOs
- [x] Add authentication support (Basic Auth)
- [x] Create OpdsUseCases: FetchCatalog, DownloadBook, ImportMetadata (implemented ImportOpdsBookUseCase covering download and import)

**Files Created/Updated:**
- app/src/main/java/us/blindmint/codex/domain/opds/OpdsFeed.kt
- app/src/main/java/us/blindmint/codex/domain/opds/OpdsEntry.kt
- app/src/main/java/us/blindmint/codex/domain/opds/OpdsLink.kt
- app/src/main/java/us/blindmint/codex/domain/repository/OpdsRepository.kt
- app/src/main/java/us/blindmint/codex/data/remote/dto/OpdsFeedDto.kt
- app/src/main/java/us/blindmint/codex/data/remote/dto/OpdsEntryDto.kt
- app/src/main/java/us/blindmint/codex/data/remote/dto/OpdsLinkDto.kt
- app/src/main/java/us/blindmint/codex/data/remote/dto/OpdsCategoryDto.kt
- app/src/main/java/us/blindmint/codex/data/remote/OpdsApiService.kt
- app/src/main/java/us/blindmint/codex/data/repository/OpdsRepositoryImpl.kt
- app/src/main/java/us/blindmint/codex/data/di/RepositoryModule.kt (updated)
- app/build.gradle.kts (updated with dependencies)

### Phase 3: Metadata Import and Deduplication
- [x] Create MetadataMapper: map OPDS entry to Book metadata
- [x] Implement deduplication logic: check UUID/ISBN on download
- [ ] Add conflict resolution UI: prompt for duplicate handling (deferred to UI phase)
- [x] Update BookRepositoryImpl to handle OPDS downloads
- [x] Create OPDS file downloader with progress tracking

**Files Created/Updated:**
- app/src/main/java/us/blindmint/codex/data/mapper/opds/OpdsMetadataMapper.kt
- app/src/main/java/us/blindmint/codex/domain/repository/BookRepository.kt (updated)
- app/src/main/java/us/blindmint/codex/data/repository/BookRepositoryImpl.kt (updated)
- app/src/main/java/us/blindmint/codex/domain/repository/OpdsRepository.kt (updated)
- app/src/main/java/us/blindmint/codex/data/repository/OpdsRepositoryImpl.kt (updated)
- app/src/main/java/us/blindmint/codex/domain/use_case/opds/ImportOpdsBookUseCase.kt

**Phase 4 Files Updated:**
- app/src/main/java/us/blindmint/codex/ui/library/LibraryScreen.kt
- app/src/main/java/us/blindmint/codex/ui/library/LibraryState.kt
- app/src/main/java/us/blindmint/codex/ui/library/LibraryEvent.kt
- app/src/main/java/us/blindmint/codex/ui/library/LibraryModel.kt

**Phase 5 Files Updated:**
- app/src/main/java/us/blindmint/codex/presentation/settings/browse/BrowseSettingsCategory.kt
- app/src/main/java/us/blindmint/codex/presentation/settings/browse/opds/BrowseOpdsSubcategory.kt

### Phase 4: UI Refactoring - Library Unification
- [x] Remove tabbed layout from LibraryScreen (unified single view)
- [ ] Add side panel trigger icon (left of search icon)
- [ ] Implement FilterPanel composable with:
  - Status presets (Reading/Planning/Already Read/Favorites - mapped to tags)
  - Tags: searchable list/cloud
  - Authors: dropdown/search
  - Series: dropdown
  - Publication year: range slider
  - Language: chips
  - Clear/Apply buttons
- [ ] Update LibraryViewModel for unified book list (local + OPDS previews) (LibraryModel updated for unified view, OPDS previews deferred)
- [ ] Add "Show OPDS Previews" toggle in top bar (cloud icon)
- [ ] Modify book cards to handle dimmed OPDS previews with download button
- [ ] Update sort options: add Publication Date, Series, Tags

### Phase 5: Enhanced Browse Settings
- [x] Reorganize Settings > Browse into sections:
  - Local Files: existing folder selection
  - OPDS: source management (add/edit/delete OPDS servers)
- [x] Remove redundant Display/Filter/Sort from Settings > Browse (move to UI)
- [x] Create OPDS source management screen with CRUD operations (placeholder added, model created)
- [ ] Add authentication UI (username/password fields)

### Phase 6: OPDS Browsing Integration
- [x] Update BrowseScreen to support OPDS catalogs (renamed to Catalogs, shows OPDS sources list)
- [ ] Add hierarchical navigation: Authors > Series > Books
- [ ] Implement OPDS search via OpenSearch
- [ ] Add OPDS book previews with metadata display
- [ ] Create OPDS download workflow: preview -> confirm -> download -> import metadata
- [x] Update bottom navigation: Browse -> Catalogs

### Phase 7: Advanced Filtering and Tags
- [ ] Implement FilterState data class with all filter dimensions
- [ ] Add local tag management in BookInfoScreen: editable chip list for add/remove
- [ ] Auto-import tags from OPDS <category> elements on download
- [ ] Create tag cloud visualization in filter panel
- [ ] Support AND/OR logic for multi-tag filtering
- [ ] Implement OPDS tag sync:
  - Individual book sync: Pull tags from OPDS source for specific book
  - Bulk sync: Sync tags for all books from configured OPDS sources
  - Sync options: Overwrite (remove local-only tags, sync 1:1 with OPDS) or Merge (add OPDS tags, preserve local tags)
  - UI prompts for sync conflicts and user choice
- [ ] Add bulk tag operations (future enhancement)

### Phase 8: Testing and Polish
- [ ] Unit tests for OPDS parsing and metadata mapping
- [ ] Integration tests for download/import workflow
- [ ] UI tests for filter panel and unified library
- [ ] Performance testing: large catalogs, slow networks
- [ ] Accessibility: screen reader support for OPDS content
- [ ] Edge case handling: offline mode, auth failures, malformed feeds

### Phase 9: Nice-to-Have Features (Post-MVP)
- [ ] Bulk operations: download series, batch tag assignment
- [ ] Metadata refresh from OPDS
- [ ] Offline OPDS feed caching
- [ ] Export OPDS book links
- [ ] Multi-catalog support (public OPDS like Gutenberg)
- [ ] Reading stats filtered by tags/authors
- [ ] RTL language support based on metadata

## Dependencies to Add
- Networking: `com.squareup.retrofit2:retrofit:2.9.0`, `com.squareup.okhttp3:logging-interceptor:4.12.0`
- XML Parsing: `com.github.bumptech.glide:okhttp3-integration:4.16.0` or SimpleXML
- Encryption: For credentials (if not using Android Keystore)
- Type Converters: For Room List<String>

## Data Migration and Database Strategy
- **No Migration Required**: For this major update, existing database data (books, progress, history) will be wiped. Users will need to re-import local books. This simplifies implementation and allows for optimal DB structure.
- **Database Improvements**: Evaluate and implement performance optimizations:
  - Add indexes on frequently queried fields (tags, seriesName, publicationDate, author, uuid, isbn)
  - Consider composite indexes for common filter combinations
  - Optimize Room queries for large datasets
  - Potential full DB redesign if Room limitations are encountered
- **Settings Preservation**: Import/Export functionality for reading settings, color presets, etc. remains intact. No compatibility guarantees with pre-update exports, but current settings will persist.
- **Categories Migration**: Existing category-based books will be lost in wipe; future category functionality will be tag-based.

## Risk Assessment
- **High Risk**: OPDS parsing reliability, tag sync logic complexity
- **Medium Risk**: UI refactoring scope, authentication handling, DB performance with large catalogs
- **Low Risk**: Feature additions that don't modify existing flows; data wipe simplifies migration

## Testing Strategy
- Manual testing with Calibre OPDS server
- Unit tests for core logic
- Integration tests for full workflows
- Beta testing with user feedback

## Rollback Plan
- Feature flags for OPDS features
- Database migration rollback scripts
- Gradual rollout to catch issues early

## Success Metrics
- Successful OPDS catalog connection and browsing
- Metadata import accuracy (>95%)
- UI performance: <100ms filter response
- User adoption: >70% of users configure OPDS sources

## Timeline Estimate
- Phase 1-3: 4-6 weeks (backend/core with DB redesign if needed)
- Phase 4-5: 3-4 weeks (UI refactoring)
- Phase 6-7: 4-5 weeks (advanced features including tag sync)
- Phase 8-9: 2-3 weeks (testing/polish)

## Performance Issues Identified
- Book import process is slow: Populating "Add books?" menu and adding selected books takes excessively long (20+ seconds for small sets), with occasional app freezing.
- First-time book opening is slow: 20-30 seconds for text parsing, despite caching working for re-opens.
- Root causes: Likely file parsing, cover image compression, or DB operations. Needs optimization before OPDS integration completion.

## UI Improvements Needed
- Restore book count as styled badge/button instead of plain text.
- Add missing toggle for library_show_book_count in settings (currently missing from UI).

## Notes and Questions
- Confirm OPDS server compatibility (Calibre-web specific features?)
- Authentication methods beyond Basic Auth?
- OPDS spec version support (1.2 vs 2.0)?
- Performance expectations for large catalogs (1000+ books)?
- Offline caching requirements?
- Tag sync: How to identify books across OPDS sources for bulk sync (UUID/ISBN matching)?
- Local tag persistence: Should local-only tags be flagged or just stored normally?

## Git Commit Strategy
Use git commits for checkpoints when phases or major features are completed. Commit messages should reference the integration plan phases (e.g., "Complete Phase 1: Database schema updates"). This enables progress tracking, easy rollback if needed, and clear history of implementation steps in addition to this document.
