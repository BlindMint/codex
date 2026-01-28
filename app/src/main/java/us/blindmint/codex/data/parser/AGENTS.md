# DATA PARSER SYSTEM

**Purpose:** Multi-format eBook parsing with dual-class architecture

## STRUCTURE
```
parser/
├── FileParser.kt               # Interface: extract metadata
├── TextParser.kt               # Interface: extract text content
├── DocumentParser.kt           # Base document parser
├── FileParserImpl.kt           # Factory: selects parser by format
├── TextParserImpl.kt           # Factory: selects text extractor
├── SpeedReaderWordExtractor.kt  # RSVP word tokenization
├── epub/
│   ├── EpubFileParser.kt       # EPUB metadata/cover
│   └── EpubTextParser.kt       # EPUB text extraction
├── pdf/
│   ├── PdfFileParser.kt        # PDF metadata/cover
│   └── PdfTextParser.kt        # PDF text extraction
├── fb2/
│   └── Fb2FileParser.kt        # FB2 metadata/cover
├── html/
│   ├── HtmlFileParser.kt       # HTML metadata
│   └── HtmlTextParser.kt       # HTML text extraction
├── txt/
│   ├── TxtFileParser.kt        # TXT metadata
│   └── TxtTextParser.kt        # TXT content
├── fodt/
│   ├── FodtFileParser.kt      # FODT metadata
│   └── FodtTextParser.kt       # FODT text extraction
├── xml/
│   └── XmlTextParser.kt        # Generic XML parsing
├── opf/
│   ├── OpfParser.kt           # OPF metadata parser
│   └── OpfWriter.kt           # OPF metadata writer
└── comic/
    ├── ComicFileParser.kt      # CBR/CBZ/CB7 metadata
    ├── ArchiveReader.kt         # Archive entry extraction
    └── ArchiveEntry.kt         # Archive entry model
```

## DUAL PATTERN

**FileParser:** Extracts metadata, cover image, structure
```kotlin
interface FileParser {
    suspend fun parse(cachedFile: CachedFile): BookWithCover?
}
```

**TextParser:** Extracts text content for reading/search
```kotlin
interface TextParser {
    suspend fun extractText(cachedFile: CachedFile, chapter: Int): TextContent?
}
```

**Why dual?**
- Metadata parsing is fast (used for library display)
- Text extraction is expensive (lazy-loaded when opening book)
- Separation enables progress tracking and speed reading

## SUPPORTED FORMATS

| Format | FileParser | TextParser | Notes |
|--------|-----------|-----------|-------|
| EPUB | EpubFileParser | EpubTextParser | Most common |
| PDF | PdfFileParser | PdfTextParser | via pdfbox-android |
| FB2 | Fb2FileParser | XmlTextParser | XML-based |
| HTML | HtmlFileParser | HtmlTextParser | via jsoup |
| TXT | TxtFileParser | TxtTextParser | Plain text |
| Markdown | - | MarkdownParser | via commonmark |
| FODT | FodtFileParser | FodtTextParser | Flat ODF XML |
| CBR/CBZ/CB7 | ComicFileParser | - | via junrar/compress |

## PARSER SELECTION

**FileParserImpl.kt** factory pattern:
- Detects format by file extension
- Returns appropriate FileParser implementation
- Throws exception if format unsupported

**TextParserImpl.kt** factory pattern:
- Same format detection logic
- Returns TextParser for content extraction
- Returns null for comics (no text content)

## WHERE TO LOOK

| Task | Location |
|------|----------|
| Add new format | Create [Format]FileParser.kt + [Format]TextParser.kt |
| Modify format detection | FileParserImpl.kt, TextParserImpl.kt |
| Comic archive logic | comic/ComicFileParser.kt, comic/ArchiveReader.kt |
| OPD metadata handling | opf/OpfParser.kt |
| Speed reading tokenization | SpeedReaderWordExtractor.kt |

## CONVENTIONS

- All parsers are suspend functions (file I/O)
- Parse failures return `null` (don't crash library load)
- Each format package is self-contained
- Text extraction is lazy (only when book is opened)
- Comic formats only implement FileParser (no TextParser)
