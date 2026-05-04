# Codex Deep App Analysis

Date: 2026-05-03  
Branch: `docs/deep-app-analysis`  
Scope: Android ebook and comic reader audit across build config, storage, import, parsing, reader UI, library UI, OPDS, and maintainability.  
Constraint: preserve all user-visible functionality, supported formats, settings, reader modes, navigation, and workflows.

## Executive Summary

Codex already has a broad feature set: EPUB/PDF/FB2/HTML/TXT/Markdown/FODT parsing, CBZ/CBR/CB7 comics, OPDS, Material 3 settings, text reader, PDF reader, comic reader, and speed reading. The app builds successfully with `./gradlew assembleDebug`.

The most important issues are not missing features. They are performance, memory, persistence correctness, and release-hardening risks that become visible with large libraries, large comics, large PDFs/EPUBs, authenticated OPDS sources, and long reading sessions.

Highest-priority work identified during the audit:

1. Bound comic bitmap memory and avoid re-scanning archives for each page.
2. Stream OPDS downloads and remove body-level network logging outside controlled debug diagnostics.
3. Fix Room schema export and list metadata serialization before more migrations accumulate.
4. Move library search/filter/sort toward DAO-level queries and projections.
5. Make parser and speed-reader caches memory-aware and file-identity-aware.

## Implementation Status

This branch now implements the highest-priority low-risk improvements from the audit while preserving the app's visible workflows and supported formats:

- Room schema export is enabled, the database is version 28, and the current schema is generated under the current `us.blindmint...` package path.
- List metadata is serialized as JSON arrays with a migration from the legacy comma-separated storage.
- OPDS downloads stream to temporary files and normal OkHttp body logging is disabled.
- CBZ reads use ZIP random access instead of re-scanning the archive for every page; CBR/CB7 continue to use libarchive.
- Comic bitmap caching is bounded by page count and an app-memory-class-derived byte budget.
- Bulk import paths and hashes use sets, and duplicate hash checks happen before metadata parsing where files can be reopened.
- Library search uses a DAO-level candidate prefilter before fuzzy scoring.
- Reader text and speed-reader caches are memory-aware and keyed by file identity.
- EPUB parsing avoids repeated full ZIP entry enumeration.
- Release minification and resource shrinking are enabled for release variants.
- One unused reader preloader helper and the duplicate comic display cache state were removed.
- Unit coverage now exists for the list metadata converter behavior that motivated the Room migration.

Remaining work is concentrated in tests, deeper library projection/paging work, PDF page-by-page text extraction, dependency alignment, and runtime device/emulator smoke testing.

## Current Build And Project Health

`./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, and `./gradlew assembleRelease-debug` succeed. The build produced these non-blocking warnings:

- MuPDF Java compilation uses source/target 8 under JDK 21.
- `ExportSettings.kt` uses an experimental serialization API without opt-in.
- `ScreenOrientationDropdownOption.kt` uses a deprecated Material exposed dropdown typealias.

There is initial local unit coverage for `TypeConverters`; broader repository, parser, migration, and instrumentation coverage remains open. Room schema output is configured in [app/build.gradle.kts](/home/samurai/dev/codex/app/build.gradle.kts:36), and schema export is now enabled in [BookDatabase.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/local/room/BookDatabase.kt:35). The current database version is 28. The repo still also contains an old version 10 schema under a previous `ua.blindmint...` package path.

Release minification and resource shrinking are now enabled in [app/build.gradle.kts](/home/samurai/dev/codex/app/build.gradle.kts:66), with ProGuard rules adjusted for the current dependency graph.

## Priority Findings

### P0: Comic Archive Reading Is Expensive Per Page

The comic archive layer memory maps the entire archive file with `Os.mmap` in [ArchiveReader.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/parser/comic/ArchiveReader.kt:123). For every page request, `getInputStream` creates a new archive reader, scans headers from the beginning until it finds the requested entry, then reads the full entry into a `ByteArray` before decoding it in [ArchiveReader.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/parser/comic/ArchiveReader.kt:145).

Impact:

- Page N can cost O(N) archive scans, which gets worse deep into large manga/comic archives.
- Reading each page into a full `ByteArray` duplicates memory before bitmap decode.
- Memory mapping a very large archive can increase address-space pressure and failure modes on lower-memory devices.
- CBR/CB7 formats may be especially painful because random access is limited.

Implementation plan:

1. Introduce a `ComicPageSource` abstraction with `open()`, `pageCount`, `pageName(index)`, `decodePage(index, targetSize?)`, and `close()`.
2. Use `ZipFile` or a ZIP-specific implementation for CBZ random access by entry name.
3. Keep libarchive for CBR/CB7, but cache entry order and minimize repeated full scans where the library allows it.
4. Decode from stream directly where possible; avoid `readBytes()` for full page payloads unless an archive format forces it.
5. Keep all current comic formats, natural ordering, RTL/webtoon modes, gestures, invert colors, and progress behavior unchanged.

Implemented in this branch:

- CBZ now uses `ZipFile` random access by entry name.
- CBR/CB7 retain the existing libarchive path because those formats do not provide the same cheap ZIP-style random access in this implementation.

Acceptance criteria:

- Page turns in a 500+ page CBZ do not slow down as page index increases.
- CBR/CB7 still open and page in natural order.
- Large archive failures produce a user-facing error instead of an uncaught crash.

### P0: Comic Bitmap Cache Is Unbounded In Practice

`MAX_CACHED_PAGES` is defined in [ComicReaderLayout.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/presentation/reader/ComicReaderLayout.kt:51), but `removeEldestEntry` always returns `false` in [ComicReaderLayout.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/presentation/reader/ComicReaderLayout.kt:92). Loaded bitmaps are only recycled when the book changes or the composable is disposed. There is a second unused-looking `loadedPages` map in `ComicReaderDisplay`, which adds confusion and should be removed or connected to actual cache ownership.

Impact:

- Long comic sessions can accumulate dozens or hundreds of decoded `ARGB_8888` bitmaps.
- High-resolution pages can push the app into memory pressure or OOM.
- `largeHeap=true` in the manifest may mask the issue rather than fixing it.

Implementation plan:

1. Replace the map with a bounded LRU cache keyed by physical page index.
2. Recycle bitmaps immediately on eviction and on reader disposal.
3. Size the cache by memory budget, not only page count. A practical default is the smaller of 50 pages or a fixed MB budget based on device memory class.
4. Prefer decoding to target display dimensions where possible for fit/fill modes.
5. Keep the existing prefetch window but make it respect cache capacity.

Implemented in this branch:

- The comic page map now evicts least-recently-used pages.
- Evicted bitmaps are recycled.
- Cache size is bounded by both `MAX_CACHED_PAGES` and a byte budget derived from Android memory class.

Acceptance criteria:

- Memory reaches a stable ceiling while reading through a long comic.
- Returning to recent pages remains fast.
- No visible change to page scaling, gestures, or progress UI.

### P0: OPDS Download And Logging Are Risky

OPDS downloads buffer the full response into an `okio.Buffer`, convert it to a `ByteArray`, then return it from [OpdsRepositoryImpl.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/repository/OpdsRepositoryImpl.kt:164). The OkHttp client logs full request/response bodies in [OpdsRepositoryImpl.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/repository/OpdsRepositoryImpl.kt:255), and additional logs include feed previews and content previews in [OpdsRepositoryImpl.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/repository/OpdsRepositoryImpl.kt:70).

Impact:

- Downloading a large ebook or comic can duplicate the payload in memory.
- Authenticated OPDS requests may expose sensitive URLs, headers, or content in logs.
- Body logging is expensive on large feeds or downloads.

Implementation plan:

1. Change OPDS import/download internals to stream directly to a temp file in app cache or the configured Codex downloads directory.
2. Report progress while copying stream chunks.
3. Return a file handle or `CachedFile` instead of a `ByteArray` through internal use-case boundaries.
4. Keep the external user workflow unchanged: browse, authenticate, download, import, and open.
5. Replace body logging with no logging by default and opt-in redacted debug logging for development builds only.

Implemented in this branch:

- OPDS downloads stream to a temporary file and report progress while copying.
- Download imports parse and hash from files/streams instead of passing a full payload `ByteArray`.
- OkHttp body logging and content preview logs are removed for normal operation.

Acceptance criteria:

- Downloading large OPDS comics/books does not allocate the whole payload as one array.
- Credentials and response bodies are not logged in normal debug/release usage.
- OPDS v1, OPDS v2, authentication, search, and filename extraction still work.

### P0: Room Schema History Is Not Trustworthy

The database version is 27, but `exportSchema = false` disables current schema export in [BookDatabase.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/local/room/BookDatabase.kt:34). The Gradle schema directory exists, but the repo only has an old version 10 schema under a previous package name.

Impact:

- Future migrations are harder to validate.
- Auto-migration and migration-test confidence is low.
- Schema drift can go unnoticed until users upgrade.

Implementation plan:

1. Enable schema export.
2. Generate and commit the current schema path for the current package.
3. Add Room migration tests from the oldest supported schema version to version 27.
4. Decide whether stale `ua.blindmint...` schema files should be retained for historical compatibility or moved into explicit migration-test assets.

Implemented in this branch:

- Schema export is enabled.
- Version 28 schema is generated under `app/schemas/us.blindmint.codex.data.local.room.BookDatabase/`.
- Migration tests are still needed.

Acceptance criteria:

- Current schema JSON is committed.
- Migration tests cover all hand-written migrations.
- No destructive migration or user data reset is introduced.

### P1: List Metadata Serialization Corrupts Comma-Containing Values

`TypeConverters` serialize `List<String>` by joining values with commas and parse by splitting on commas in [TypeConverters.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/local/room/TypeConverters.kt:14). This affects authors, tags, series, and languages. Code comments in `BookDao` claim these are JSON stored, but the converter is not JSON.

Impact:

- Author names like `Last, First`, tags with commas, and series names with commas cannot round-trip.
- Filtering and search can silently operate on corrupted metadata.
- Future migration work becomes more complex if this is not addressed early.

Implementation plan:

1. Replace comma serialization with JSON array serialization for `List<String>`.
2. Add a versioned migration that converts existing comma-separated values as best as possible.
3. For ambiguous existing comma-containing values, preserve current split behavior during migration because there is no reliable way to infer original grouping.
4. Add converter tests for empty lists, whitespace, commas, Unicode, and duplicate values.

Implemented in this branch:

- New list values are written as JSON arrays.
- Legacy comma-separated values remain readable.
- Migration 27 to 28 converts existing stored values to JSON arrays using the previous split semantics.
- Unit tests cover comma-containing values, duplicate values, legacy comma strings, and null/empty input.

Acceptance criteria:

- New values with commas round-trip correctly.
- Existing data remains readable after migration.
- Library filters and bulk edit preserve metadata lists.

### P1: Library Search, Filter, Sort Still Load Too Much

`BookRepositoryImpl.getBooks` loads all books for both empty and non-empty query paths in [BookRepositoryImpl.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/repository/BookRepositoryImpl.kt:77). `LibraryModel.getBooksFromDatabase` then filters and sorts in memory in [LibraryModel.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/ui/library/LibraryModel.kt:676). Metadata loading also scans all books.

Impact:

- Large libraries pay full entity read and mapping costs for common UI operations.
- Fuzzy search repeats distance calculations while filtering and sorting.
- Sorting by `lastOpened` requires a history lookup and in-memory ordering.

Implementation plan:

1. Add lightweight `BookListItem` DAO projection for library screens.
2. Add indexed DAO queries for title/category/favorite/source/progress and common sort orders.
3. Add a search candidate query using `LIKE` or FTS, then run fuzzy scoring only on the candidate set.
4. Store or materialize latest-opened data in a queryable way, either via a `MAX(time)` join or denormalized field updated with history.
5. Keep visible sorting, filtering, tabs, search behavior, and bulk edit workflows unchanged.

Partially implemented in this branch:

- Search now uses a SQLite candidate query before fuzzy scoring.
- Fuzzy scoring is computed once per candidate.
- Full projection/paging work remains open.

Acceptance criteria:

- Large libraries avoid full-row reads for normal list rendering.
- Search latency scales with candidate count rather than total library size.
- Existing sort/filter results remain behaviorally equivalent.

### P1: Reader Text And Speed-Reader Caches Are Count-Based

The repository keeps parsed reader text for 5 books and speed-reader words for 10 books in [BookRepositoryImpl.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/repository/BookRepositoryImpl.kt:61). Those entries can represent very different memory sizes.

Impact:

- Five large EPUB/PDF text object graphs can consume far more memory than expected.
- Speed-reader word lists duplicate derived data from parsed text.
- Cache entries are keyed only by book id, so changed files with the same database row can serve stale content until process restart.

Implementation plan:

1. Replace count-only caches with memory-aware caches.
2. Include file identity in cache keys: path/URI, file size, last modified when available, and content hash when available.
3. Consider on-disk parsed text cache for large text books with explicit invalidation.
4. Avoid extracting speed-reader words automatically when normal text reading does not need them, unless the speed-reader total/count UI requires it.

Implemented in this branch:

- Parsed text and speed-reader word caches are memory-aware.
- Cache keys include book id, file path, file size, modified time, and content hash.

Acceptance criteria:

- Opening several large books does not retain unbounded parsed text.
- Modified files invalidate cached text.
- Speed reader still opens quickly after initial extraction.

### P1: Parser Implementations Load Entire Documents

EPUB parsing reads each chapter entry fully into memory and parses with Jsoup in [EpubTextParser.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/parser/epub/EpubTextParser.kt:129). PDF text mode extracts full document text via PDFBox before splitting in [PdfTextParser.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/data/parser/pdf/PdfTextParser.kt:27). This is simple and functional, but it is not ideal for large books.

Impact:

- Large EPUB/PDF open time can be long.
- Memory pressure spikes during parsing.
- Speed-reader extraction amplifies the work by deriving a second representation.

Implementation plan:

1. Add parser timing and size diagnostics behind a debug flag.
2. For EPUB, reduce repeated `zip.entries().toList()` calls and prefer spine-driven processing.
3. For EPUB, consider sequential or bounded parallel parsing based on chapter count and size.
4. For PDF text mode, consider page-by-page extraction with progress and cancellation.
5. Cache expensive parse results when file identity is stable.

Partially implemented in this branch:

- EPUB ZIP entries are enumerated once and reused.
- Parsed reader text caches now invalidate with file identity changes.
- PDF page-by-page extraction remains open.

Acceptance criteria:

- Large books show bounded memory growth during open.
- Cancellation and reader navigation remain responsive.
- Parsed output remains equivalent for supported formats.

### P1: Import Duplicate Detection Can Be Cheaper

Bulk import gets existing path/hash pairs but creates `existingPaths` as a list and checks membership with repeated scans in [BulkImportBooksFromFolder.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/domain/use_case/book/BulkImportBooksFromFolder.kt:31). It also parses files before computing partial hashes for duplicate detection.

Impact:

- Folder import cost grows with `new files * existing books`.
- Duplicates by content may still pay parse cost before being skipped.

Implementation plan:

1. Convert existing paths and hashes to `HashSet`s.
2. Check path duplicates before parsing, as it does now, but in O(1).
3. Compute partial hash before full metadata parsing when the input stream can be reopened safely.
4. Keep the current import progress UI and duplicate behavior.

Implemented in this branch:

- Existing paths and hashes use sets.
- Folder and Codex-directory imports check duplicate hashes before full metadata parsing.
- Newly imported paths/hashes are added to the in-memory duplicate sets during the run.

Acceptance criteria:

- Large imports skip duplicates faster.
- Failed or inaccessible files still produce the same user-visible progress/error behavior.

### P2: Settings State Is Centralized And Broad

`MainModel` contains a very large event surface for app, library, reader, comic, speed-reading, dictionary, background image, and display settings. Settings writes go through `handleDatastoreUpdate` in [MainModel.kt](/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/ui/main/MainModel.kt:1246).

Impact:

- Many UI surfaces collect the same large state object, increasing recomposition scope.
- The model is difficult to test and reason about.
- Small settings changes can be more expensive than necessary.

Implementation plan:

1. First add tests around settings persistence and default loading.
2. Introduce selector flows or smaller state holders for high-frequency reader/comic settings.
3. Only split the model after tests exist, because the current state surface is feature-sensitive.
4. Preserve all setting screens, labels, defaults, and persistence keys.

Acceptance criteria:

- No setting key migration loss.
- Reader setting changes still apply immediately where they do today.
- Recomposition profiling improves on reader/settings screens.

### P2: Build Dependencies Are Mixed Across Compose Versions

The app mixes Compose `1.7.8`, `1.8.0-beta03`, Material 3 `1.4.0-alpha08`, and related artifacts in [app/build.gradle.kts](/home/samurai/dev/codex/app/build.gradle.kts:137). This may be intentional for specific APIs, but it increases regression risk.

Implementation plan:

1. Document which beta/alpha APIs are required.
2. Align to a stable Compose BOM when AboutLibraries handling permits it, or pin a consistent explicit set.
3. Run UI smoke tests for reader, comic reader, library, settings, and OPDS after alignment.

Acceptance criteria:

- Dependency versions are coherent and intentionally documented.
- No visual or behavioral regressions in core flows.

### P2: Release Hardening Is Incomplete

Release minification and resource shrinking are disabled in [app/build.gradle.kts](/home/samurai/dev/codex/app/build.gradle.kts:66). The manifest uses `largeHeap=true`, likely to tolerate reader/comic memory spikes.

Implementation plan:

1. Fix known memory issues first, especially comic bitmap caching and OPDS streaming.
2. Re-enable R8 minification and resource shrinking in a release-candidate branch.
3. Keep and refine ProGuard rules for Simple XML, MuPDF, serialization, and Compose.
4. Compare APK size, startup, OPDS parsing, and reader behavior before/after.

Implemented in this branch:

- Release minification and resource shrinking are enabled.
- The `release-debug` variant builds with release shrink settings and debug signing.

Acceptance criteria:

- Release APK is smaller without crashing reflection-heavy XML/OPDS code.
- `largeHeap` can be reevaluated after memory work.

### P3: Dead Or Ineffective Code Should Be Cleaned Up Carefully

`PagePreloader` exists but no usage was found. Comic display defines a separate `loadedPages` map that appears disconnected from actual page loading. Several comments mention TODOs or implementation assumptions that are no longer true.

Implementation plan:

1. Remove unused helpers only after confirming there are no planned references.
2. Replace misleading comments with behavior-level comments.
3. Keep cleanup separate from functional performance changes to simplify review.

Implemented in this branch:

- The disconnected `loadedPages` state in `ComicReaderDisplay` was removed.
- The unused `PagePreloader` helper was removed.

Acceptance criteria:

- No feature behavior changes.
- Reduced code paths around comic preloading and reader cache ownership.

## Cross-Cutting Test Plan

Add tests before or alongside fixes:

- Room migration tests through version 27.
- Type converter round-trip tests for `List<String>`.
- Repository tests for library filtering, sorting, latest-opened behavior, and metadata extraction.
- Import tests for path duplicate, partial-hash duplicate, inaccessible file, and mixed supported/unsupported folders.
- OPDS tests with authenticated requests, v1 XML, v2 JSON, search, large download streaming, and filename extraction.
- Parser tests using small fixtures for EPUB, PDF, FB2, FODT, HTML, Markdown, TXT, CBZ, CBR, and CB7.
- Reader instrumentation or macrobenchmark tests for large library rendering, large EPUB open, PDF page navigation, comic page turning, and speed-reader launch.

Recommended profiling scenarios:

- 10,000-book library: open app, search, filter, sort, bulk select.
- 500+ page CBZ and CBR: page from start to end, jump with slider, switch RTL/webtoon.
- 100 MB EPUB/PDF: open, search, bookmark, leave and restore.
- OPDS: browse authenticated Calibre-like feed, search, download large file, import.

## Suggested Implementation Order

1. Add tests and schema export for persistence safety. Schema export and converter tests are done; migration/instrumentation tests remain.
2. Fix `TypeConverters` and migration coverage. Serialization, migration, and converter tests are done; migration tests remain.
3. Fix comic page cache and archive page retrieval. Done for cache and CBZ; CBR/CB7 remain constrained by archive format/library behavior.
4. Stream OPDS downloads and remove body logging. Done.
5. Optimize library DAO query/projection paths. Search candidate prefilter is done; full projections/paging remain.
6. Make parser/text/speed-reader caches memory-aware. Done for repository caches; PDF parser streaming remains.
7. Align dependencies and re-enable release shrinking. Release shrinking is done; dependency alignment remains.
8. Clean up dead code and stale comments. Initial dead code cleanup is done.

This order reduces data-loss risk first, then addresses the highest user-visible performance and memory problems, then handles release polish.

## Functionality Preservation Checklist

Each implementation phase must explicitly verify:

- All supported formats still import/open: EPUB, PDF, FB2, HTML, TXT, Markdown, FODT, CBZ, CBR, CB7.
- Reader modes remain: text reader, PDF reader, comic paged reader, webtoon/vertical reading, speed reader.
- Existing settings retain defaults and persisted values.
- Library categories, filters, search, sorting, favorites, metadata editing, and progress indicators remain visible and equivalent.
- OPDS v1/v2 browsing, authentication, search, downloads, and Calibre metadata import remain available.
- Book progress, bookmarks, history, and restored positions survive upgrades.
