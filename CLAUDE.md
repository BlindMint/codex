# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Codex is a Material You eBook reader Android app built with Kotlin and Jetpack Compose. It supports multiple formats: PDF, TXT, EPUB, FB2, HTML, HTM, and MD.

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

Clean architecture with layered structure under `app/src/main/java/ua/blindmint/codex/`:

**data/** - Data layer
- `local/room/` - Room database (BookDatabase, BookDao) with entities for books, history, color presets
- `local/data_store/` - DataStore for app preferences
- `parser/` - File format parsers (epub/, fb2/, html/, pdf/, txt/) implementing FileParser interface
- `repository/` - Repository implementations
- `mapper/` - Entity to domain model mappers
- `di/` - Hilt dependency injection modules

**domain/** - Business logic
- `repository/` - Repository interfaces (BookRepository, HistoryRepository, etc.)
- `use_case/` - Use cases organized by feature (book/, history/, color_preset/, etc.)
- Domain models for library, reader, navigation, and UI

**presentation/** - UI layer with ViewModels
- Screen-specific packages: library/, reader/, browse/, settings/, history/
- `core/components/` - Reusable Compose components (dialogs, bottom sheets, settings widgets)
- `navigator/` - Custom navigation implementation

**core/** - Cross-cutting utilities (crash handling)

## Key Dependencies

- Hilt for DI (modules in `data/di/`)
- Room for persistence (version 11, schemas in `app/schemas/`)
- Jetpack Compose (Material 3)
- Format parsers: pdfbox-android (PDF), jsoup (EPUB/HTML), kotlinx-serialization (FB2)
- Coil for image loading

## Database

Room database entities: BookEntity, HistoryEntity, ColorPresetEntity, BookProgressHistoryEntity

Migrations are defined in `data/local/room/BookDatabase.kt`. Current version: 11.

## Signing

Release builds require `keystore.properties` in project root with: storeFile, storePassword, keyAlias, keyPassword.
