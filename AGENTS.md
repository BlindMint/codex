# PROJECT KNOWLEDGE BASE

**Generated:** 2026-01-27
**Commit:** a4554cf
**Branch:** master

## OVERVIEW
Material You eBook reader for Android (Kotlin + Compose) supporting 10+ formats with Clean Architecture.

## STRUCTURE
```
app/src/main/java/us/blindmint/codex/
├── Application.kt              # Hilt app entry point (unusually at root)
├── data/                       # 122 files: Room, Retrofit, DataStore, 9+ format parsers
├── domain/                     # 68 files: Business logic, 42 use cases, repository interfaces
├── presentation/               # 418 files: Compose UI screens and components
├── ui/                         # 97 files: ViewModels (ReaderModel, LibraryModel, etc.)
├── core/                       # Crash handling, constants
└── utils/                      # Utilities (FuzzySearchHelper)
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Database schema | `data/local/room/` | Room v20, entities: Book, History, Bookmark, ColorPreset, OpdsSource |
| File format parsing | `data/parser/` | PDF, EPUB, FB2, HTML, TXT, MD, FODT, comics (CBR/CBZ/CB7) |
| Use cases | `domain/use_case/` | Organized by feature: book/, bookmark/, color_preset/, history/, opds/ |
| Reader UI | `presentation/reader/`, `ui/reader/` | Complex: text reading, comics, speed reading (RSVP) |
| Library UI | `presentation/library/`, `ui/library/` | Categorization, search, progress tracking |
| Settings | `presentation/settings/` | 60+ subdirs: appearance, reader (12 sub-features), browse, library |
| Navigation | `presentation/navigator/` | Custom navigation implementation |
| Dependency Injection | `data/di/` | Hilt modules by data layer |

## CONVENTIONS
- **Clean Architecture**: data → domain → presentation/ui layers (no reverse dependencies)
- **Parser Pattern**: Each format has FileParser + TextParser (e.g., EpubFileParser.kt, EpubTextParser.kt)
- **ViewModels**: In `ui/` package, corresponding screens in `presentation/`
- **Domain Models**: Pure Kotlin classes (no Room annotations) in `domain/`
- **Entities**: Room-annotated in `data/local/room/`
- **Mappers**: Entity → domain conversion in `data/mapper/`

## ANTI-PATTERNS (THIS PROJECT)
None explicitly documented in code comments.

## UNIQUE STYLES
- Application.kt at root package (uncommon, typically in `app/` subpackage)
- Parser system with dual classes per format (FileParser for metadata, TextParser for content)
- Speed reading with RSVP (Rapid Serial Visual Presentation) mode
- Settings organization by feature with 60+ subdirectories (high granularity)
- Custom navigation implementation (not standard Compose Navigation)

## COMMANDS
```bash
./gradlew assembleDebug          # Build debug APK (app/build/outputs/apk/debug/codex-v*.apk)
./gradlew assembleRelease        # Build signed release (requires keystore.properties)
./gradlew installDebug           # Install on connected device
./gradlew clean                  # Clean build artifacts
```

## NOTES
- Room migrations in `data/local/room/BookDatabase.kt` (current: v20)
- Signing config: keystore.properties in project root
- Build variants: debug (.debug suffix), release (minified), release-debug (.release.debug suffix)
- No linting config files (.editorconfig, .ktlint, detekt) present
- Project uses Material 3 (Material You) theming
