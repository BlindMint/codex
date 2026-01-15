# Backup Branch Integration Guide

This document serves as a reference for integrating features and code from the backup branch located at `~/dev/codex.bak/`. It provides guidance on the integration process and identifies key locations for code changes.

**Integration Status**: Backend Complete ✅ | UI Implementation Pending 🎯

**Last Updated**: January 2026 - OPDS Infrastructure Integration Complete

## Integration Process

### 1. Feature Analysis
- Examine the backup branch implementation in `~/dev/codex.bak/`
- Identify the specific components, use cases, and UI elements involved
- Note any new dependencies or configuration changes required

### 2. Code Location Reference

#### Core Architecture Layers
- **Domain Layer**: `app/src/main/java/us/blindmint/codex/domain/`
  - Use cases: `domain/use_case/`
  - Models: `domain/library/`, `domain/browse/`, `domain/reader/`
  - Repositories: `domain/repository/`

- **Data Layer**: `app/src/main/java/us/blindmint/codex/data/`
  - Repositories: `data/repository/`
  - Parsers: `data/parser/`
  - Database: `data/local/room/`
  - Data Store: `data/local/data_store/`

- **Presentation Layer**: `app/src/main/java/us/blindmint/codex/presentation/`
  - UI Components: `presentation/*/components/`
  - Screens: `presentation/*/`
  - Constants: `presentation/core/constants/`

- **UI Layer**: `app/src/main/java/us/blindmint/codex/ui/`
  - ViewModels: `ui/*/`
  - Navigation: `ui/*/Screen.kt`
  - State management: `ui/*/Model.kt`

#### Key Integration Points
- **Dependency Injection**: `data/di/` - Add new use cases, repositories, and dependencies
- **Navigation**: `ui/*/Screen.kt` - Update screen refresh channels and navigation logic
- **String Resources**: `app/src/main/res/values/strings.xml` - Add user-facing text
- **Constants**: `presentation/core/constants/` - Update supported formats, defaults, etc.

### 3. Implementation Steps

#### A. Add New Dependencies
- Update `data/di/` modules to provide new use cases and repositories
- Ensure proper Hilt injection for ViewModels and composables

#### B. Update Supported Formats
- Modify `presentation/core/constants/SupportedExtensionConstants.kt`
- Update `data/parser/FileParserImpl.kt` for new file type handling

#### C. Implement Use Cases
- Create new use cases in `domain/use_case/`
- Add repository methods in `domain/repository/` interfaces
- Implement repository methods in `data/repository/`

#### D. Update UI Components
- Modify composables in `presentation/*/components/`
- Update ViewModels in `ui/*/` for state management
- Add progress indicators and user feedback

#### E. Handle Navigation & State
- Update screen refresh channels for real-time updates
- Ensure proper error handling and user notifications
- Add loading states and progress tracking

### 4. Testing & Validation

#### Build Verification
```bash
./gradlew compileDebugKotlin    # Check compilation
./gradlew lintDebug            # Code quality check
./gradlew assembleDebug        # Full build test
```

#### Key Validation Points
- Dependency injection works correctly
- File parsing handles new formats appropriately
- UI updates reflect state changes
- Navigation flows work as expected
- Error handling provides user feedback

### 5. Common Integration Patterns

#### Adding New File Types
1. Update `SupportedExtensionConstants.kt`
2. Add parsing logic in `FileParserImpl.kt`
3. Update file filtering in repositories

#### Implementing Bulk Operations
1. Create use case with progress callbacks
2. Add repository methods for batch processing
3. Update UI with progress indicators
4. Trigger screen refreshes on completion

#### UI State Management
1. Add state variables to ViewModels
2. Update composables to reflect state changes
3. Handle loading states and error conditions
4. Provide user feedback via toasts/snackbars

## Backup Branch Structure

The backup branch (`~/dev/codex.bak/`) contains:
- Complete source code in `app/src/main/java/us/blindmint/codex/`
- Resources in `app/src/main/res/`
- Build configuration files
- Documentation in `docs/`

## Useful Commands

```bash
# Compare current vs backup
diff -r app/src/main/java/us/blindmint/codex/ ~/dev/codex.bak/app/src/main/java/us/blindmint/codex/

# Find specific components
find ~/dev/codex.bak/ -name "*ComponentName*" -type f

# Check for new dependencies
diff build.gradle.kts ~/dev/codex.bak/build.gradle.kts
```

## OPDS Integration Status

### Completed Integration Steps

The following OPDS integration steps have been completed:

1. **Database Schema Updates** ✅
    - Updated `BookEntity.kt` to include all OPDS metadata fields (tags, seriesName, seriesIndex, publicationDate, language, publisher, summary, uuid, isbn, source, remoteUrl)
    - Added comic-specific fields (isComic, pageCount, currentPage, lastPageRead, readingDirection, comicReaderMode, archiveFormat)
    - Created `BookSource` enum for tracking LOCAL vs OPDS book sources
    - Added `OpdsSourceEntity` for storing OPDS server configurations
    - Created database migration `MIGRATION_14_15` to safely upgrade existing databases

2. **Dependencies & Infrastructure** ✅
    - Added Retrofit2 networking libraries (retrofit, converter-simplexml, okhttp3-logging-interceptor)
    - Added KotlinX serialization libraries for JSON parsing
    - Added simple-xml library for OPDS feed parsing
    - Created `OpdsSourceDao` for database operations
    - Added missing DataStore constants (CODEX_ROOT_URI)

3. **API Components & DTOs** ✅
    - Copied `OpdsApiService.kt` for network API calls
    - Implemented complete OPDS DTO structures (OpdsFeedDto, OpdsEntryDto, OpdsLinkDto, OpdsAuthorDto, OpdsCategoryDto)
    - Added backup/restore models (CodexBackup, OpdsSourceData, etc.)

4. **Domain Layer Integration** ✅
    - All OPDS domain models already present (OpdsEntry, OpdsFeed, OpdsLink, OpfMetadata)
    - OPDS use cases and repository interfaces already integrated
    - CodexDirectoryManager and OPDS repository implementation already present

5. **Configuration Updates** ✅
    - `SupportedExtensionConstants.kt` already includes `.opf` and `.xml` extensions
    - All dependency injection bindings already configured

6. **Build Verification** ✅
    - Compilation successful (`./gradlew compileDebugKotlin`)
    - Linting passed (`./gradlew lintDebug`)
    - All integration issues resolved

### Remaining Integration Steps

The core OPDS infrastructure is complete. The remaining work focuses on implementing the user interface components:

#### 1. **Implement OPDS Catalog Panel** (High Priority)
- Replace placeholder in `OpdsCatalogPanel.kt` with full catalog browsing interface
- Include OPDS source selection, catalog navigation, search, and download functionality

#### 2. **Create OPDS Source Management Screens** (High Priority)
- Build screens to add/edit/delete OPDS sources with URL and authentication
- Integrate with existing `OpdsSourceEntity` database model

#### 3. **Implement UI Components** (Medium Priority)
- `OpdsCatalogContent.kt` - Main catalog browsing with book lists and pagination
- `OpdsBookPreview.kt` - Book preview cards with covers and metadata
- `OpdsBookDownloadDialog.kt` - Download progress with cancel functionality
- `OpdsBookDetailsBottomSheet.kt` - Detailed book information display
- `OpdsCatalogScreen.kt` - Main screen container with navigation
- `OpdsAddSourceDialog.kt` - Dialog for adding new OPDS sources

#### 4. **Integrate OPDS Settings** (Medium Priority)
- Add OPDS sources management section to settings screens
- Create UI for managing multiple OPDS sources with enable/disable functionality

#### 5. **Update Navigation System** (Low Priority)
- Ensure proper navigation between OPDS catalog views and existing screens
- Handle back navigation for OPDS-specific flows
- Update screen routing to support OPDS source management

### Implementation Resources

- **UI Components Reference**: `~/dev/codex.bak/app/src/main/java/us/blindmint/codex/ui/browse/opds/`
- **Settings Integration**: `~/dev/codex.bak/app/src/main/java/us/blindmint/codex/ui/settings/opds/`
- **Screen Management**: `~/dev/codex.bak/app/src/main/java/us/blindmint/codex/ui/browse/OpdsCatalogScreen.kt`

### Build Verification Commands

After completing the integration:

```bash
# Test compilation
./gradlew compileDebugKotlin

# Run linting
./gradlew lintDebug

# Test full build
./gradlew assembleDebug

# Run tests if available
./gradlew testDebug
```

### Integration Challenges

- **API Compatibility**: Ensure backup branch API matches current branch expectations
- **Database Schema**: Book entity field additions require careful migration
- **Dependency Management**: New external libraries must be compatible with existing versions
- **UI State Management**: OPDS features require complex state handling for catalogs and downloads

## Notes

- Always test integrations thoroughly before committing
- Keep feature implementations modular and well-documented
- Update this guide as new integration patterns emerge
- Reference specific feature documentation for implementation details

## Integration Summary

**Current Status**: OPDS backend infrastructure is 100% complete. All data models, networking, database schema, and business logic are implemented and tested.

**Remaining Work**: UI implementation only. The foundation is solid - all the complex data layer and business logic integration is finished. The remaining tasks are primarily implementing user interface components to provide a complete OPDS browsing and downloading experience.

**Key Achievements**:
- Successfully integrated comprehensive metadata support (tags, series, publication info, etc.)
- Implemented robust database migration system for existing users
- Added full OPDS networking capabilities with authentication support
- Maintained backward compatibility with existing book data
- All code compiles and passes linting without issues

**Next Steps**: Focus on UI development using the backup branch implementations as reference. The UI components are the final piece needed to complete the OPDS integration.