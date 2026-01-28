# READER PRESENTATION LAYER

**Purpose:** Complex reader UI with 3 reading modes (text, comic, speed reading RSVP)

## STRUCTURE
```
reader/
├── ReaderLayout.kt              # Main reader coordinator (orchestrates modes)
├── ReaderLayoutText.kt          # Text reading mode
├── ReaderLayoutTextChapter.kt   # Chapter-based text rendering
├── ReaderLayoutTextSeparator.kt  # Text separation logic
├── SpeedReadingContent.kt       # RSVP speed reading UI
├── SpeedReadingScreen.kt        # Speed reading screen scaffold
├── SpeedReadingScaffold.kt      # Speed reading layout wrapper
├── ReaderDrawer.kt             # Main navigation drawer
├── ReaderChaptersDrawer.kt      # Chapter navigation
├── ReaderTopBar.kt             # Top app bar
├── ReaderBottomBarSlider.kt    # Text mode progress slider
├── ReaderBottomBarComicSlider.kt # Comic mode slider
├── ComicReaderBottomBar.kt      # Comic-specific controls
├── ReaderSettingsBottomSheet.kt # Settings overlay
├── SpeedReadingSettingsBottomSheet.kt # Speed reading configuration
├── TextSelectionBottomSheet.kt  # Dictionary/lookup actions
├── ReaderSearchBar.kt          # In-book search
├── ReaderErrorPlaceholder.kt    # Error handling
├── ReaderHorizontalGesture.kt   # Swipe gesture handling
└── ReaderColorPresetChange.kt   # Theme switching
```

## READING MODES

**Text Mode:**
- Scrollable text rendering with chapters
- Text selection and lookup (dictionary, search)
- Progress tracking via BottomBarSlider

**Comic Mode:**
- Page-based navigation (next/prev)
- Zoom and pan
- ComicBottomBar controls

**Speed Reading (RSVP):**
- Rapid Serial Visual Presentation
- Word-by-word display at configurable WPM
- SpeedReadingSettings: WPM, chunk size, focus indicators

## COMPONENT PATTERNS

- **Mode switching:** ReaderLayout.kt delegates to sub-layouts based on reading mode
- **State management:** ReaderModel (in ui/reader/) orchestrates all reader state
- **Gestures:** HorizontalGesture handles swipe navigation (tap zones)
- **Bottom sheets:** All settings use modal bottom sheets (reader, speed reading, text selection)
- **Drawers:** Chapter navigation via slide-out drawer
- **Search:** In-book search with highlighting (via SearchBar)

## WHERE TO LOOK

| Feature | Location |
|---------|----------|
| Main layout logic | ReaderLayout.kt (orchestrator) |
| Text rendering | ReaderLayoutText.kt, ReaderLayoutTextChapter.kt |
| Comic rendering | ReaderLayout.kt (comic mode delegation) |
| Speed reading UI | SpeedReadingContent.kt, SpeedReadingScaffold.kt |
| Chapter navigation | ReaderChaptersDrawer.kt |
| Text selection | TextSelectionBottomSheet.kt |
| Settings | ReaderSettingsBottomSheet.kt |

## CONVENTIONS

- All reader UI components are stateless (state in ReaderModel)
- Bottom sheets use `ModalBottomSheetLayout` pattern
- Drawers use `DrawerContent` with `ModalDrawer`
- Gesture handling via `Modifier.pointerInput`
- Color theming via `ReaderColorPresetChange.kt`
