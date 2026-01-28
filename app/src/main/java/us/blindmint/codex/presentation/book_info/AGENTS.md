# BOOK INFO PRESENTATION

**Purpose:** Book metadata display, editing, and cover management

## STRUCTURE
```
book_info/
├── BookInfoScreen.kt           # Main book info screen
├── BookInfoTopBar.kt          # Navigation bar
├── CoverImage.kt              # Cover display/edit
├── BookDetails.kt             # Metadata display
├── EditBookDialog.kt          # Metadata editing dialog
└── ... (32 files total)
```

## FEATURES

**Display:**
- Cover image (editable)
- Title, author, description
- File path, format
- Reading progress
- Book statistics

**Editing:**
- Edit metadata (title, author, description)
- Change cover image
- Delete book

**Actions:**
- Open book (start reading)
- Delete book
- Share book
- Export book

## WHERE TO LOOK

| Task | Location |
|------|----------|
| Main screen | BookInfoScreen.kt |
| Cover handling | CoverImage.kt |
| Metadata editing | EditBookDialog.kt |
| Navigation | BookInfoTopBar.kt |

## CONVENTIONS

- State management via BookInfoModel (in `ui/book_info/`)
- Cover images loaded via Coil
- Edit operations use domain use cases
- Dialog pattern for all edits
