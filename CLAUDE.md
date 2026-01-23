# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Codex is a Material You eBook reader Android app built with Kotlin and Jetpack Compose. It supports multiple formats: PDF, TXT, EPUB, FB2, HTML, HTM, MD, FODT, CBR, CBZ, CB7 (comic books). Additional features include speed reading mode, OPDS catalog integration, comic book reading, and extensive customization options.

Current version: 2.2.2

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK (app/build/outputs/apk/debug/codex-v*.apk)
./gradlew assembleRelease        # Build signed release APK (requires keystore.properties)
./gradlew installDebug           # Install debug build on connected device
./gradlew clean                  # Clean build artifacts
```

Build variants:
- `debug` - Debug build with `.debug` applicationId suffix
- `release` - Minified release build with signing
- `release-debug` - Release config signed with debug key (`.release.debug` suffix)

## Architecture

Clean architecture with layered structure under `app/src/main/java/us/blindmint/codex/`:

**data/** - Data layer
- `local/room/` - Room database (BookDatabase, BookDao) with entities for books, history, color presets, bookmarks, OPDS sources
- `local/data_store/` - DataStore for app preferences
- `remote/` - OPDS networking with Retrofit
- `parser/` - File format parsers (epub/, fb2/, html/, pdf/, txt/, comic/, fodt/) implementing FileParser interface
- `repository/` - Repository implementations
- `mapper/` - Entity to domain model mappers
- `di/` - Hilt dependency injection modules

**domain/** - Business logic
- `repository/` - Repository interfaces (BookRepository, HistoryRepository, OpdsRepository, etc.)
- `use_case/` - Use cases organized by feature (book/, history/, color_preset/, opds/, speed_reading/, etc.)
- Domain models for library, reader, navigation, UI, OPDS, bookmarks, and speed reading

**presentation/** - UI layer with ViewModels
- Screen-specific packages: library/, reader/, browse/, settings/, history/, about/, help/
- `core/components/` - Reusable Compose components (dialogs, bottom sheets, settings widgets)
- `navigator/` - Custom navigation implementation

**ui/** - MVVM ViewModels and state management
- Screen ViewModels (ReaderModel, LibraryModel, etc.)
- State classes and event handling

**core/** - Cross-cutting utilities (crash handling, constants, utilities)

## Key Dependencies

- Hilt for DI (modules in `data/di/`)
- Room for persistence (version 20, schemas in `app/schemas/`)
- Jetpack Compose (Material 3)
- Format parsers:
  - pdfbox-android (PDF)
  - jsoup (EPUB/HTML)
  - kotlinx-serialization (FB2)
  - junrar/commons-compress (Comic archives: CBR, CBZ, CB7)
  - commonmark (Markdown)
- Networking: Retrofit, OkHttp for OPDS
- Coil for image loading
- Accompanist for swipe refresh
- AboutLibraries for open source license display

## Database

Room database entities: BookEntity, HistoryEntity, ColorPresetEntity, BookProgressHistoryEntity, BookmarkEntity, OpdsSourceEntity

Migrations are defined in `data/local/room/BookDatabase.kt`. Current version: 20.

## Key Features

- **Multi-format support**: PDF, TXT, EPUB, FB2, HTML, HTM, MD, FODT, CBR, CBZ, CB7
- **Speed Reading**: RSVP (Rapid Serial Visual Presentation) with customizable WPM, focus indicators, and controls
- **Comic Book Reading**: Dedicated comic reader with page navigation, reading directions, and progress tracking
- **OPDS Integration**: Browse and download books from OPDS catalogs
- **Advanced Library**: Categories, search, bookmarks with custom names, reading history
- **Customization**: Custom fonts, color presets, background images, themes, and extensive reading settings
- **Text Selection**: Dictionary lookup with web views and search engines
- **Import/Export**: Settings backup and restore functionality

## Signing

Release builds require `keystore.properties` in project root with: storeFile, storePassword, keyAlias, keyPassword.
