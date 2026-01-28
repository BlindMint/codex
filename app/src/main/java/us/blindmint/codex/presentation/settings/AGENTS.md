# PRESENTATION SETTINGS

**Purpose:** Highly granular settings UI with 60+ subdirectories organized by feature

## ORGANIZATION

```
settings/
├── components/                 # Shared settings widgets
├── general/                   # App-wide settings
├── appearance/                # Themes, colors, fonts
│   ├── colors/
│   │   └── components/        # Color preset picker
│   ├── theme_preferences/      # Light/dark/auto themes
│   └── components/            # Shared appearance widgets
├── browse/                    # Library browsing behavior
│   ├── display/               # Grid/list view
│   ├── filter/                # Category filtering
│   ├── sort/                  # Sorting options
│   ├── scan/                  # Directory scanning
│   └── opds/                  # OPDS catalog settings
├── library/                   # Library display
│   ├── display/               # Grid/list, cover size
│   ├── sort/                  # Sort by title/author/date
│   └── tabs/                  # Tab configuration
├── reader/                    # Reading experience (12 sub-features)
│   ├── reading_mode/          # Text/comic/auto detection
│   ├── font/                  # Font family, size, line height
│   ├── padding/               # Text margins
│   ├── text/                  # Text alignment, justification
│   ├── progress/              # Progress bar settings
│   ├── chapters/              # Chapter navigation
│   ├── reading_speed/          # Auto-scroll, WPM
│   ├── speed_reading/         # RSVP mode (12 settings)
│   ├── search/                # Search highlighting
│   ├── dictionary/            # Dictionary lookup
│   ├── translator/            # Translation services
│   ├── images/                # Background images
│   ├── misc/                  # Other reader settings
│   └── system/                # System integration
└── import_export/             # Settings backup/restore
```

## GRANULARITY RATIONALE

**Why 60+ subdirectories?**
- Each setting is a reusable Composable component
- Settings are grouped by feature domain
- Each sub-feature has its own `components/` directory
- Allows embedding settings in multiple contexts (e.g., reader settings in reader drawer)

**Pattern:**
```
settings/[feature]/
├── [Feature]Settings.kt          # Main settings screen
└── components/                   # Reusable sub-components
    ├── [Setting]Item.kt          # Individual setting control
    ├── [Setting]Dialog.kt         # Configuration dialog
    └── ...
```

## SHARED COMPONENTS

**settings/components/** provides:
- `SettingsSwitch.kt` - Toggle switches
- `SettingsSlider.kt` - Value sliders
- `SettingsDropdown.kt` - Dropdown selection
- `SettingsColorPicker.kt` - Color selection
- `SettingsRadioGroup.kt` - Radio buttons

Each feature's `components/` subdirectory extends these with domain-specific widgets.

## WHERE TO LOOK

| Feature | Location |
|---------|----------|
| App-wide settings | `settings/general/` |
| Themes | `settings/appearance/` |
| Library view | `settings/library/display/`, `settings/library/sort/` |
| Reading preferences | `settings/reader/` (12 sub-features) |
| Speed reading | `settings/reader/speed_reading/` |
| OPDS catalogs | `settings/browse/opds/` |
| Settings backup | `settings/import_export/` |

## CONVENTIONS

- All settings components use `@Composable` functions (no classes)
- State management via ViewModels in `ui/settings/`
- Settings saved to DataStore via domain use cases
- Bottom sheets use `ModalBottomSheetLayout`
- Dialogs use `AlertDialog` pattern
- All settings are reversible (stored in DataStore)
