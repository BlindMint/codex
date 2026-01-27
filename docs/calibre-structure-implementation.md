
## Change Log

| Date | Version | Changes |
|-------|----------|----------|
| 2026-01-27 | 1.0 | Initial document creation |
| 2026-01-27 | 1.1 | Added `extractCalibreId()` and `sanitizeAuthorName()` functions |
| 2026-01-27 | 1.2 | Added `createAuthorFolder()` to `CodexDirectoryManager` interface and implementation |
| 2026-01-27 | 1.3 | Modified `ImportOpdsBookUseCase.performImport()` to use Calibre-style structure |
| 2026-01-27 | 1.4 | Added `opdsCalibreId` field to `BookEntity`, `Book` domain model |
| 2026-01-27 | 1.5 | Added `findBookByCalibreId()` query to `BookDao` |
| 2026-01-27 | 1.6 | Created database migration 23→24 to add `opdsCalibreId` column |
| 2026-01-27 | 1.7 | Updated `BookMapper` to map `opdsCalibreId` field in both directions |
| 2026-01-27 | 1.8 | Added duplicate detection by calibre_id in `ImportOpdsBookUseCase` |
| 2026-01-27 | 1.9 | Added `Duplicate` result type to `ImportOpdsResult` |
| 2026-01-27 | 2.0 | Updated implementation document with Phase 1 & 2 completion status |

