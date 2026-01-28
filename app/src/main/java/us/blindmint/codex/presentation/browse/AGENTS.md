# BROWSE PRESENTATION

**Purpose:** File browsing and OPDS catalog integration

## STRUCTURE
```
browse/
├── BrowseScreen.kt            # Main browse screen
├── BrowseTopBar.kt           # Search and filters
├── FileBrowser.kt             # File system navigation
├── FilePickerDialog.kt        # File selection
├── opds/                      # OPDS catalog UI
│   ├── OpdsCatalogScreen.kt   # Catalog browsing
│   ├── OpdsFeedScreen.kt      # Feed entry point
│   ├── OpdsEntryScreen.kt     # Entry details
│   └── ... (catalog navigation components)
└── ... (21 files total)
```

## FEATURES

**Local File Browsing:**
- Directory navigation
- File filtering by format
- Multi-file selection
- Import to library

**OPDS Catalogs:**
- Browse OPDS feeds
- Search catalogs
- Download books
- Save entries to library

## WHERE TO LOOK

| Feature | Location |
|---------|----------|
| File browser | FileBrowser.kt |
| OPDS catalog | opds/ directory |
| File selection | FilePickerDialog.kt |
| Main screen | BrowseScreen.kt |

## CONVENTIONS

- State management via BrowseModel (in `ui/browse/`)
- OPDS data via OpdsRepository
- File selection via Storage Access Framework (SAF)
- OPDS downloads use DownloadManager
