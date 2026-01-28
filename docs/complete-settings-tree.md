# Codex Settings Tree Structure

Complete documentation of all settings menus throughout the Codex app, organized by location and screen.

---

## Primary Settings Menu
*(Accessed from main app Settings button)*

```
SettingsScreen
├── 🎨 Appearance Settings
│   ├── Screen Orientation
│   ├── Theme Preferences
│   │   ├── Dark Theme (Auto/Light/Dark)
│   │   ├── App Theme (Material You/Traditional)
│   │   ├── Theme Contrast (Standard/Medium/High)
│   │   ├── Pure Dark (OLED black)
│   │   └── Absolute Dark (pure black background)
│   └── Colors
│       ├── Color Preset
│       ├── Fast Color Preset Change
│       └── Background Image
│
├── 📚 Reader Settings (3 tabs)
│   ├── Tab 1: Books
│   │   ├── Font
│   │   │   ├── Font Family
│   │   │   ├── Custom Fonts
│   │   │   ├── Font Thickness
│   │   │   ├── Font Style
│   │   │   ├── Font Size
│   │   │   └── Letter Spacing
│   │   ├── Text
│   │   │   ├── Text Alignment (Left/Center/Right/Justify/Original)
│   │   │   └── Text Formatting (hidden when Original alignment)
│   │   │       ├── Line Height
│   │   │       ├── Paragraph Height
│   │   │       └── Paragraph Indentation
│   │   ├── Images
│   │   │   ├── Show Images (Show/Hide)
│   │   │   ├── Color Effects
│   │   │   ├── Corners Roundness
│   │   │   ├── Image Alignment
│   │   │   └── Image Width
│   │   ├── Chapters
│   │   │   └── Chapter Title Alignment
│   │   ├── Progress
│   │   │   ├── Progress Count
│   │   │   ├── Progress Bar
│   │   │   ├── Progress Bar Font Size
│   │   │   ├── Progress Bar Padding
│   │   │   └── Progress Bar Alignment
│   │   └── Speed Reading
│   │       ├── WPM
│   │       ├── Manual Sentence Pause
│   │       ├── Sentence Pause Duration (when manual enabled)
│   │       ├── Auto-hide OSD
│   │       ├── Playback Controls
│   │       ├── Keep Screen On
│   │       ├── Word Size
│   │       ├── Color Preset
│   │       ├── Background Image
│   │       ├── Use Custom Font
│   │       └── Font Family (when custom font enabled)
│   │
│   ├── Tab 2: Speed Reading
│   │   ├── Performance
│   │   │   ├── WPM (Words Per Minute)
│   │   │   ├── Manual Sentence Pause
│   │   │   ├── Sentence Pause Duration
│   │   │   ├── Auto-hide OSD
│   │   │   ├── Playback Controls
│   │   │   ├── Keep Screen On
│   │   │   └── Word Size
│   │   ├── Appearance
│   │   │   ├── Color Preset
│   │   │   ├── Background Image
│   │   │   ├── Use Custom Font
│   │   │   └── Font Family (when custom font enabled)
│   │   ├── Focus Indicators
│   │   │   ├── Center Word
│   │   │   ├── Focal Point Position
│   │   │   └── Focus Indicators (LINES/VERTICAL/BARS/OFF)
│   │   │       ├── Horizontal Bars Color & Opacity (when LINES/BARS)
│   │   │       └── Accent Character (when not Center Word)
│   │   │           ├── Accent Color & Opacity
│   │   │           ├── Show Vertical Indicators
│   │   │           ├── Vertical Indicator Type (LINE/DOT)
│   │   │           ├── Vertical Indicators Size
│   │   │           ├── Show Horizontal Bars
│   │   │           ├── Horizontal Bars Thickness
│   │   │           ├── Horizontal Bars Distance
│   │   │           ├── Horizontal Bars Length
│   │   │           └── Horizontal Bars Color & Opacity
│   │
│   └── Tab 3: Comics
│       ├── Reading Mode
│       │   ├── Reading Direction (LTR/RTL/Vertical)
│       │   ├── Tap Zone Mode
│       │   └── Invert Taps
│       ├── Display
│       │   ├── Image Scale (Fit/Stretch/Original)
│       │   └── Background Color
│       └── Progress
│           ├── Progress Count
│           ├── Progress Bar
│           ├── Progress Bar Font Size
│           ├── Progress Bar Padding
│           └── Progress Bar Alignment
│
├── 📖 Library Settings
│   ├── Display
│   │   ├── Layout (Grid/List)
│   │   ├── Grid Size (when Grid layout)
│   │   ├── List Size (when List layout)
│   │   ├── Title Position (when Grid layout)
│   │   ├── Show Read Button
│   │   └── Show Progress
│   ├── Tabs
│   │   ├── Show Category Tabs
│   │   └── Show Book Count
│   └── Sort
│       └── Sort Order (Name/Last Read/Progress/Author)
│
├── 🔍 Browse Settings
│   ├── Storage Location Picker
│   ├── Scan
│   │   ├── Scan on Startup
│   │   └── Scan for OPF files
│   └── OPDS
│       └── Manage OPDS Sources
│
├── 📤 Import/Export Settings
│   ├── Import Settings
│   └── Export Settings
│
└── ℹ️ About
```

---

## Library Screen Settings
*(Accessible from Library screen via sort/filter menu)*

```
LibrarySortMenu (Modal Drawer)
├── Sort Tab
│   ├── Sort Order
│   │   ├── Name (↑/↓)
│   │   ├── Last Read (↑/↓)
│   │   ├── Progress (↑/↓)
│   │   └── Author (↑/↓)
│   └── Display Options
│       ├── Layout (Grid/List)
│       ├── Grid Size (when Grid)
│       ├── List Size (when List)
│       ├── Title Position (when Grid)
│       ├── Show Read Button
│       └── Show Progress
│
└── Filter Tab
    ├── Status Presets
    │   ├── Clear All
    │   ├── Reading
    │   ├── Planning
    │   └── Already Read
    ├── Tags → (Tags Subpanel)
    ├── Authors → (Authors Subpanel)
    ├── Series → (Series Subpanel)
    └── Languages
        └── [Language Filter Chips]

Filter Subpanels (Tags/Authors/Series)
├── Select All
├── Deselect All
├── Reset
└── [Filter items with checkboxes]

BulkEditBottomSheet (for selected books)
├── Tags (MetadataItemEditor)
├── Series (MetadataItemEditor)
├── Authors (MetadataItemEditor)
├── Languages (MetadataItemEditor)
└── Category/Status
    ├── Reading
    ├── Planning
    └── Already Read
```

---

## Browse Screen Settings
*(Accessible from Browse screen)*

```
BrowseFilterBottomSheet (3 tabs)
├── Filter Tab
│   └── File Format Filters
│       ├── PDF, TXT, EPUB, FB2, HTML, HTM, MD, FODT, CBR, CBZ, CB7
│
├── Sort Tab
│   ├── Name (↑/↓)
│   ├── Last Modified (↑/↓)
│   ├── Size (↑/↓)
│   └── Type (↑/↓)
│
└── Display Tab
    ├── Layout (Grid/List)
    └── Grid Size

BrowseAddDialog
└── Select and Add Books to Library
```

---

## Normal Book Reader Settings
*(Accessible while reading a book - via settings icon or swipe)*

```
ReaderSettingsBottomSheet (3 tabs)
├── Tab 1: General
│   ├── Reading Mode
│   │   ├── Horizontal Gesture (Page/Position)
│   │   ├── Horizontal Scroll Mode (Page/Continuous)
│   │   ├── Horizontal Gesture Sensitivity
│   │   ├── Pull Animation (Slide/Fade)
│   │   └── Animation Alpha
│   ├── Padding
│   │   ├── Side Padding
│   │   ├── Vertical Padding
│   │   ├── Cutout Padding (notch)
│   │   └── Bottom Bar Padding
│   ├── System
│   │   ├── Custom Screen Brightness
│   │   ├── Screen Brightness
│   │   └── Screen Orientation
│   ├── Reading Speed
│   │   ├── Highlighted Reading
│   │   ├── Highlighted Reading Thickness
│   │   ├── Perception Expander
│   │   ├── Perception Expander Padding
│   │   └── Perception Expander Thickness
│   ├── Search
│   │   ├── Search Highlight Color
│   │   ├── Show Search Scrollbar
│   │   └── Search Scrollbar Opacity
│   ├── Dictionary
│   │   └── Open Lookups In App
│   └── Misc
│       ├── Fullscreen
│       ├── Keep Screen On
│       └── Hide Bars On Fast Scroll
│
├── Tab 2: Font & Text
│   ├── Font
│   │   ├── Font Family
│   │   ├── Custom Fonts
│   │   ├── Font Thickness
│   │   ├── Font Style
│   │   ├── Font Size
│   │   └── Letter Spacing
│   ├── Text
│   │   ├── Text Alignment
│   │   └── Text Formatting
│   │       ├── Line Height
│   │       ├── Paragraph Height
│   │       └── Paragraph Indentation
│   ├── Images
│   │   ├── Show Images
│   │   ├── Color Effects
│   │   ├── Corners Roundness
│   │   ├── Image Alignment
│   │   └── Image Width
│   ├── Chapters
│   │   └── Chapter Title Alignment
│   ├── Progress
│   │   ├── Progress Count
│   │   ├── Progress Bar
│   │   ├── Progress Bar Font Size
│   │   ├── Progress Bar Padding
│   │   └── Progress Bar Alignment
│   └── Speed Reading
│       ├── WPM
│       ├── Manual Sentence Pause
│       ├── Sentence Pause Duration
│       ├── Auto-hide OSD
│       ├── Playback Controls
│       ├── Keep Screen On
│       ├── Word Size
│       ├── Color Preset
│       ├── Background Image
│       ├── Use Custom Font
│       └── Font Family
│
└── Tab 3: Colors
    ├── Color Preset
    ├── Fast Color Preset Change
    └── Background Image
```

---

## Comic Reader Settings
*(Accessible while reading a comic book)*

```
ReaderSettingsBottomSheet (Comic variant - no tabs)
├── Reading Mode
│   ├── Reading Direction (LTR/RTL/Vertical)
│   ├── Tap Zone Mode
│   └── Invert Taps
│
├── Display
│   ├── Image Scale (Fit/Stretch/Original)
│   └── Background Color
│
└── Progress
    ├── Progress Count
    ├── Progress Bar
    ├── Progress Bar Font Size
    ├── Progress Bar Padding
    └── Progress Bar Alignment
```

---

## Speed Reader Settings
*(Accessible while in speed reading mode)*

```
SpeedReadingSettingsBottomSheet (2 tabs)
├── Tab 1: General
│   ├── Performance
│   │   ├── WPM (Words Per Minute)
│   │   ├── Manual Sentence Pause
│   │   ├── Sentence Pause Duration
│   │   ├── Auto-hide OSD
│   │   ├── Playback Controls
│   │   ├── Keep Screen On
│   │   └── Word Size
│   ├── Appearance
│   │   ├── Color Preset
│   │   ├── Background Image
│   │   ├── Use Custom Font
│   │   └── Font Family (when custom font enabled)
│   └── Focus Indicators
│       ├── Center Word (overrides accent/indicators)
│       ├── Focal Point Position (when not centered)
│       ├── Focus Indicators (LINES/VERTICAL/BARS/OFF)
│       │   ├── Horizontal Bars Color & Opacity (when LINES/BARS)
│       │   └── Accent Character (when not centered)
│       │       ├── Accent Color & Opacity
│       │       ├── Show Vertical Indicators
│       │       ├── Vertical Indicator Type (LINE/DOT)
│       │       ├── Vertical Indicators Size
│       │       ├── Show Horizontal Bars
│       │       ├── Horizontal Bars Thickness
│       │       ├── Horizontal Bars Distance
│       │       ├── Horizontal Bars Length
│       │       └── Horizontal Bars Color & Opacity
│
└── Tab 2: Focus
    ├── Center Word (when enabled: disables accent/indicators)
    ├── Focal Point Position
    ├── Focus Indicators (LINES/VERTICAL/BARS/OFF)
    │   ├── Horizontal Bars Color & Opacity
    │   └── Accent Character (when not centered)
    │       ├── Accent Color & Opacity
```

---

## Book Info Settings
*(Accessible from book details/info screen)*

```
BookInfoBottomSheet
├── Change Cover
│   └── Cover Image Selection & Management
│
├── Details
│   └── Full Book Details View
│
└── Edit
    └── Quick Edit Menu
        ├── Edit Title → BookInfoTitleDialog
        ├── Edit Author → BookInfoAuthorDialog
        ├── Edit Description → BookInfoDescriptionDialog
        ├── Edit Path → BookInfoPathDialog
        ├── Edit Tags → BookInfoTagsDialog
        ├── Edit Series → BookInfoSeriesDialog
        ├── Edit Languages → BookInfoLanguagesDialog
        └── Move to Category → BookInfoMoveDialog

BookInfoDialog Router
├── Delete → BookInfoDeleteDialog
├── Reset Progress → BookInfoResetProgressDialog
├── Edit Title
├── Edit Author
├── Edit Description
├── Edit Path
├── Edit Tags (via MetadataItemEditor)
├── Edit Series (via MetadataItemEditor)
└── Edit Languages (via MetadataItemEditor)
```

---

## History Screen Settings
*(Accessible from History screen)*

```
History Screen
└── Delete Whole History → HistoryDeleteWholeHistoryDialog
```

---

## Settings Available in Multiple Locations

### Color Presets & Background Image
- **Primary Settings** → Appearance → Colors
- **Reader (Book)** → Settings Bottom Sheet → Tab 3 (Colors)
- **Reader (Speed Reading)** → Settings Bottom Sheet → Tab 1 (Appearance)

### Font Settings
- **Primary Settings** → Reader Settings → Tab 2 (Books) → Font
- **Reader (Book)** → Settings Bottom Sheet → Tab 2 (Font & Text) → Font

### Screen Orientation
- **Primary Settings** → Appearance → Screen Orientation
- **Reader (Book)** → Settings Bottom Sheet → Tab 1 (General) → System

### Progress Bar Settings
- **Primary Settings** → Reader Settings → Tab 1 (Books) → Progress
- **Reader (Book)** → Settings Bottom Sheet → Tab 2 (Font & Text) → Progress
- **Reader (Comic)** → Settings Bottom Sheet → Progress

### Layout/Grid Settings
- **Primary Settings** → Library Settings → Display
- **Library Screen** → Sort Menu → Display Options
- **Browse Screen** → Filter Bottom Sheet → Display Tab

### Sort Options
- **Primary Settings** → Library Settings → Sort
- **Library Screen** → Sort Menu → Sort Tab
- **Browse Screen** → Filter Bottom Sheet → Sort Tab

### Speed Reading Settings
- **Primary Settings** → Reader Settings → Tab 1 (Books) → Speed Reading
- **Reader (Book)** → Settings Bottom Sheet → Tab 2 → Speed Reading
- **Speed Reading Mode** → Settings Bottom Sheet (2 tabs: General/Focus)

---

## File Locations Reference

### Primary Settings Files
- `app/src/main/java/us/blindmint/codex/ui/settings/SettingsScreen.kt`
- `app/src/main/java/us/blindmint/codex/presentation/settings/appearance/AppearanceSettingsCategory.kt`
- `app/src/main/java/us/blindmint/codex/presentation/settings/reader/ReaderSettingsCategory.kt`
- `app/src/main/java/us/blindmint/codex/presentation/settings/library/LibrarySettingsCategory.kt`
- `app/src/main/java/us/blindmint/codex/presentation/settings/browse/BrowseSettingsCategory.kt`
- `app/src/main/java/us/blindmint/codex/presentation/settings/import_export/ImportExportSettingsLayout.kt`

### Reader Settings Files
- `app/src/main/java/us/blindmint/codex/presentation/reader/ReaderSettingsBottomSheet.kt`
- `app/src/main/java/us/blindmint/codex/presentation/reader/SpeedReadingSettingsBottomSheet.kt`
- `app/src/main/java/us/blindmint/codex/presentation/settings/reader/` (subcategories)

### Library/Browse Settings Files
- `app/src/main/java/us/blindmint/codex/presentation/library/LibrarySortMenu.kt`
- `app/src/main/java/us/blindmint/codex/presentation/library/BulkEditBottomSheet.kt`
- `app/src/main/java/us/blindmint/codex/presentation/browse/BrowseFilterBottomSheet.kt`

### Book Info Settings Files
- `app/src/main/java/us/blindmint/codex/presentation/book_info/BookInfoBottomSheet.kt`
- `app/src/main/java/us/blindmint/codex/presentation/book_info/BookInfoDialog.kt`
- `app/src/main/java/us/blindmint/codex/presentation/book_info/MetadataItemEditor.kt`

---

## Notes

1. **Conditional Visibility**: Many settings appear/hide based on other settings (e.g., Font Layout changes, Text Alignment mode)
2. **Platform Support**: Cutout Padding is Android-specific for notched displays
3. **Format Support**: Format filters in Browse match all supported eBook formats (PDF, TXT, EPUB, FB2, HTML, HTM, MD, FODT, CBR, CBZ, CB7)
4. **Theme System**: Material You theme integrates with system dynamic colors, Traditional uses static Material Design
5. **Comic Mode**: Reading direction determines reader mode (LTR/RTL = Paged, Vertical = Webtoon)

---

*Generated: January 27, 2026*
*Codex Version: 2.2.2*
