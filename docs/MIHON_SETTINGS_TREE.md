# Mihon App - Complete Settings Menu Tree

**Legend:**
- `[Screen]` - Full screen navigation
- `[Dialog]` - Modal dialog popup
- `[TabbedDialog]` - Tabbed modal dialog
- `[Switch]` - Toggle on/off setting
- `[List]` - Dropdown list selection
- `[Slider]` - Slider control with range
- `[Text]` - Clickable text item
- `[Edit]` - Editable text field
- `[Multi]` - Multi-select from list
- `[Info]` - Informational text only
- `[ChipRow]` - Horizontal chip selection row
- `[TriState]` - Three-state filter (All/Included/Excluded)

---

## 1. Main Settings Screen (SettingsMainScreen)

```
📱 Settings [Screen]
├── 🎨 Appearance [Screen]
│   ├── Theme [Group]
│   │   ├── App Theme Mode [List] (Light/Dark/From System)
│   │   ├── App Theme [Custom] (Various color themes)
│   │   └── Dark theme pure black [Switch]
│   └── Display [Group]
│       ├── App Language [Text] → AppLanguageScreen
│       ├── Tablet UI mode [List]
│       ├── Date format [List]
│       ├── Relative time format [Switch]
│       └── Display images in description [Switch]
│
├── 📚 Library [Screen]
│   ├── Categories [Group]
│   │   ├── Edit Categories [Text] → CategoryScreen
│   │   ├── Default Category [List]
│   │   └── Categorized Display Settings [Switch]
│   ├── Global Update [Group]
│   │   ├── Library Update Interval [List]
│   │   ├── Update Restrictions [Multi] (WiFi/Network/Charging)
│   │   ├── Categories to Update [Text] → TriStateListDialog
│   │   ├── Update Metadata Automatically [Switch]
│   │   ├── Smart Update Options [Multi]
│   │   └── Show Update Tab Badge [Switch]
│   └── Behavior [Group]
│       ├── Chapter Swipe Start Action [List]
│       ├── Chapter Swipe End Action [List]
│       ├── Mark Duplicate Read Chapter [Multi]
│       └── Hide Missing Chapter Indicators [Switch]
│
├── 📖 Reader [Screen]
│   ├── [Top Level Settings]
│   │   ├── Default Reading Mode [List]
│   │   ├── Double-Tap Animation Speed [List]
│   │   ├── Show Reading Mode [Switch]
│   │   ├── Show Navigation Mode [Switch]
│   │   └── Page Transitions [Switch]
│   ├── Display [Group]
│   │   ├── Rotation Type [List]
│   │   ├── Reader Theme [List] (Black/Gray/White/Auto)
│   │   ├── Fullscreen [Switch]
│   │   ├── Draw Under Cutout [Switch]
│   │   ├── Keep Screen On [Switch]
│   │   └── Show Page Number [Switch]
│   ├── E-Ink [Group]
│   │   ├── Flash on Page Change [Switch]
│   │   ├── Flash Duration [Slider]
│   │   ├── Flash Interval [Slider]
│   │   └── Flash Style [List] (Black/White/White-Black)
│   ├── Reading [Group]
│   │   ├── Skip Read Chapters [Switch]
│   │   ├── Skip Filtered Chapters [Switch]
│   │   ├── Skip Duplicate Chapters [Switch]
│   │   └── Always Show Chapter Transition [Switch]
│   ├── Pager Viewer [Group]
│   │   ├── Navigation Mode (Tap Zones) [List]
│   │   ├── Tapping Inverted [List]
│   │   ├── Image Scale Type [List]
│   │   ├── Zoom Start [List]
│   │   ├── Crop Borders [Switch]
│   │   ├── Landscape Zoom [Switch]
│   │   ├── Navigate to Pan [Switch]
│   │   ├── Dual Page Split [Switch]
│   │   ├── Dual Page Invert [Switch]
│   │   ├── Rotate to Fit [Switch]
│   │   └── Rotate to Fit Invert [Switch]
│   ├── Webtoon Viewer [Group]
│   │   ├── Navigation Mode (Tap Zones) [List]
│   │   ├── Tapping Inverted [List]
│   │   ├── Webtoon Side Padding [Slider]
│   │   ├── Hide Threshold [List]
│   │   ├── Crop Borders [Switch]
│   │   ├── Dual Page Split [Switch]
│   │   ├── Dual Page Invert [Switch]
│   │   ├── Rotate to Fit [Switch]
│   │   ├── Rotate to Fit Invert [Switch]
│   │   ├── Double-Tap Zoom [Switch]
│   │   └── Disable Zoom Out [Switch]
│   ├── Navigation [Group]
│   │   ├── Read with Volume Keys [Switch]
│   │   └── Volume Keys Inverted [Switch]
│   └── Actions [Group]
│       ├── Read with Long Tap [Switch]
│       └── Create Folder per Manga [Switch]
│
├── 📥 Downloads [Screen]
│   ├── [Top Level Settings]
│   │   ├── Download Only Over WiFi [Switch]
│   │   ├── Save Chapters as CBZ [Switch]
│   │   └── Split Tall Images [Switch]
│   ├── Concurrency [Group]
│   │   ├── Concurrent Sources [Slider] (1-10)
│   │   └── Concurrent Pages [Slider] (1-15)
│   ├── Delete Chapters [Group]
│   │   ├── Remove After Marked as Read [Switch]
│   │   ├── Remove After Read Slots [List]
│   │   ├── Remove Bookmarked Chapters [Switch]
│   │   └── Exclude Categories [Multi]
│   ├── Auto Download [Group]
│   │   ├── Download New Chapters [Switch]
│   │   ├── Download New Unread Chapters Only [Switch]
│   │   └── Categories to Download [Text] → TriStateListDialog
│   └── Download Ahead [Group]
│       ├── Auto Download While Reading [List]
│       └── [Info] Download ahead info
│
├── 🔗 Tracking [Screen]
│   ├── Auto Update Track [Switch]
│   ├── Auto Update on Mark Read [List]
│   ├── Services [Group]
│   │   ├── MyAnimeList [Tracker] (Login/Logout)
│   │   ├── AniList [Tracker] (Login/Logout)
│   │   ├── Kitsu [Tracker] (Login/Logout)
│   │   ├── MangaUpdates [Tracker] (Login/Logout)
│   │   ├── Shikimori [Tracker] (Login/Logout)
│   │   └── Bangumi [Tracker] (Login/Logout)
│   ├── Enhanced Services [Group]
│   │   └── [Enhanced tracking services - dynamic] [Tracker]
│   └── [Info] Tracking info
│
├── 🔍 Browse [Screen]
│   ├── Sources [Group]
│   │   ├── Hide In Library Items [Switch]
│   │   └── Extension Repos [Text] → ExtensionReposScreen
│   └── NSFW Content [Group]
│       ├── Show NSFW Sources [Switch]
│       └── [Info] Parental controls info
│
├── 💾 Data & Storage [Screen]
│   ├── Storage Location [Text] → Storage Picker
│   └── [Info] Storage location info
│   ├── Backup & Restore [Group]
│   │   ├── Create Backup [Text] → CreateBackupScreen
│   │   ├── Restore Backup [Text] → RestoreBackupScreen
│   │   ├── Backup Interval [List]
│   │   └── [Info] Backup info
│   ├── Storage Usage [Group]
│   │   ├── Storage Info Display [Custom] → StorageInfo component
│   │   ├── Clear Chapter Cache [Text]
│   │   └── Auto Clear Chapter Cache [Switch]
│   └── Export [Group]
│       └── Library List [Text] → CSV export dialog
│
├── 🔒 Security [Screen]
│   ├── Security [Group]
│   │   ├── Lock with Biometrics [Switch]
│   │   ├── Lock When Idle [List]
│   │   ├── Hide Notification Content [Switch]
│   │   └── Secure Screen [List]
│   └── [Firebase - if included] [Group]
│       ├── Crashlytics [Switch]
│       └── Analytics [Switch]
│
├── ⚙️ Advanced [Screen]
│   ├── [Top Level Settings]
│   │   ├── Dump Crash Logs [Text]
│   │   ├── Verbose Logging [Switch]
│   │   ├── Debug Info [Text] → DebugInfoScreen
│   │   ├── Onboarding Guide [Text] → OnboardingScreen
│   │   └── Manage Notifications [Text] → System notification settings
│   ├── Background Activity [Group]
│   │   ├── Disable Battery Optimization [Text]
│   │   └── "Don't kill my app!" [Text] → External link
│   ├── Data [Group]
│   │   ├── Invalidate Download Cache [Text]
│   │   └── Clear Database [Text] → ClearDatabaseScreen
│   ├── Network [Group]
│   │   ├── Clear Cookies [Text]
│   │   ├── Clear WebView Data [Text]
│   │   ├── DNS over HTTPS [List]
│   │   ├── User Agent String [Edit]
│   │   └── Reset User Agent String [Text]
│   ├── Library [Group]
│   │   ├── Refresh Library Covers [Text]
│   │   ├── Reset Viewer Flags [Text]
│   │   ├── Update Library Manga Titles [Switch]
│   │   └── Disallow Non-ASCII Filenames [Switch]
│   ├── Reader [Group]
│   │   ├── Hardware Bitmap Threshold [List]
│   │   ├── Always Decode Long Strip with SSIV [Switch]
│   │   └── Display Profile [Text] → File picker
│   └── Extensions [Group]
│       ├── Extension Installer [List]
│       └── Revoke Trust [Text]
│
└── ℹ️ About [Screen]
    ├── Version [Text] (Click to copy debug info)
    ├── Check for Updates [Text] (if enabled)
    ├── What's New [Text] → External link
    ├── Licenses [Text] → OpenSourceLicensesScreen
    ├── Privacy Policy [Text] → External link
    └── Links (Website, Discord, X, Facebook, Reddit, GitHub) [Text] → External links
```

## 2. Reader Settings Dialog (In-Comic Reader)

```
📖 Reader Settings [TabbedDialog] (3 Tabs)
├── Reading Mode Tab
│   └── For This Series [Group]
│       ├── Reading Mode [ChipRow] (RTL, LTR, Vertical, Webtoon, Continuous Vertical)
│       ├── Rotation Type [ChipRow] (Free, Portrait, Landscape, Locked Portrait, Locked Landscape)
│       ├── [Pager Viewer Settings]
│       │   ├── Navigation Mode [ChipRow]
│       │   ├── Tapping Inverted [ChipRow]
│       │   ├── Image Scale Type [ChipRow]
│       │   ├── Zoom Start [ChipRow]
│       │   ├── Crop Borders [Switch]
│       │   ├── Landscape Zoom [Switch]
│       │   ├── Navigate to Pan [Switch]
│       │   ├── Dual Page Split [Switch]
│       │   ├── Dual Page Invert [Switch] (conditional)
│       │   ├── Rotate to Fit [Switch]
│       │   └── Rotate to Fit Invert [Switch] (conditional)
│       └── [Webtoon Viewer Settings]
│           ├── Navigation Mode [ChipRow]
│           ├── Tapping Inverted [ChipRow]
│           ├── Side Padding [Slider]
│           ├── Crop Borders [Switch]
│           ├── Dual Page Split [Switch]
│           ├── Dual Page Invert [Switch] (conditional)
│           ├── Rotate to Fit [Switch]
│           ├── Rotate to Fit Invert [Switch] (conditional)
│           ├── Double-Tap Zoom [Switch]
│           └── Disable Zoom Out [Switch]
│
├── General Tab
│   ├── Reader Theme [ChipRow] (Black/Gray/White/Auto)
│   ├── Show Page Number [Switch]
│   ├── Fullscreen [Switch]
│   ├── Draw Under Cutout [Switch] (conditional)
│   ├── Keep Screen On [Switch]
│   ├── Read with Long Tap [Switch]
│   ├── Always Show Chapter Transition [Switch]
│   ├── Page Transitions [Switch]
│   └── Flash on Page Change [Switch]
│       ├── Flash Duration [Slider]
│       ├── Flash Interval [Slider]
│       └── Flash Style [ChipRow]
│
└── Color Filter Tab
    ├── Custom Brightness [Switch]
    │   └── Brightness Value [Slider] (-75 to 100)
    ├── Custom Color Filter [Switch]
    │   ├── Red Value [Slider] (0-255)
    │   ├── Green Value [Slider] (0-255)
    │   ├── Blue Value [Slider] (0-255)
    │   ├── Alpha Value [Slider] (0-255)
    │   └── Color Filter Mode [ChipRow]
    ├── Grayscale [Switch]
    └── Inverted Colors [Switch]
```

## 3. Library Settings Dialog

```
📚 Library Settings [TabbedDialog] (3 Tabs)
├── Filter Tab
│   ├── Downloaded [TriState]
│   ├── Unread [TriState]
│   ├── Started [TriState]
│   ├── Bookmarked [TriState]
│   ├── Completed [TriState]
│   ├── Custom Interval [TriState] (debug only)
│   └── Tracked [TriState] (one or multiple, depending on configured trackers)
│
├── Sort Tab
│   ├── Alphabetical [Radio/Sort]
│   ├── Total Chapters [Radio/Sort]
│   ├── Last Read [Radio/Sort]
│   ├── Last Update [Radio/Sort]
│   ├── Unread Count [Radio/Sort]
│   ├── Latest Chapter [Radio/Sort]
│   ├── Chapter Fetch Date [Radio/Sort]
│   ├── Date Added [Radio/Sort]
│   ├── Tracker Score [Radio/Sort] (if trackers configured)
│   └── Random [Button]
│
└── Display Tab
    ├── Display Mode [ChipRow]
    │   ├── Compact Grid
    │   ├── Comfortable Grid
    │   ├── Cover Only Grid
    │   └── List
    ├── Columns [Slider] (0-10, 0 = auto) [for grid modes]
    └── Overlay [Group]
        ├── Download Badge [Switch]
        ├── Unread Badge [Switch]
        ├── Local Badge [Switch]
        ├── Language Badge [Switch]
        ├── Continue Reading Button [Switch]
    └── Tabs [Group]
        ├── Show Tabs [Switch]
        └── Show Number of Items [Switch]
```

## 4. Chapter Settings Dialog

```
📑 Chapter Settings [TabbedDialog] (3 Tabs)
├── Filter Tab
│   ├── Downloaded [TriState]
│   ├── Unread [TriState]
│   ├── Bookmarked [TriState]
│   └── Scanlator [TriState]
│
├── Sort Tab
│   ├── By Source [Radio/Sort]
│   ├── By Chapter Number [Radio/Sort]
│   ├── By Upload Date [Radio/Sort]
│   └── Alphabetically [Radio/Sort]
│
└── Display Tab
    ├── Show Title [Radio]
    └── Show Chapter Number [Radio]
│
└── [Menu Options] (Dropdown menu)
    ├── Set as Default [Dialog]
    │   └── [Checkbox] Also set for library
    └── Reset to Default [Action]
```

## 5. Additional Sub-Screens

### Extension Repos Screen [Screen]
- Add/Edit/Remove extension repositories
- Custom dialog for adding repos

### App Language Screen [Screen]
- List of available languages [List]

### Open Source Licenses Screen [Screen]
- List of open source libraries [Text items]
- Each opens Library License details [Screen]

### Debug Info Screen [Screen]
- Device and app debug information display [Text]

### Worker Info Screen [Screen]
- Background worker information display [Text]

### Backup Schema Screen [Screen]
- Backup schema information display [Text]

### Clear Database Screen [Screen]
- Options for clearing database [Switch/Text actions]

### Create Backup Screen [Screen]
- Backup creation options [Multi-select checkboxes]
- Create/Cancel actions

### Restore Backup Screen [Screen]
- Restore options and progress display
- Confirm/Cancel actions

### Storage Info Component [Custom Widget]
- Visual storage usage display (bars/charts)
- Storage breakdown by category

### About Screen [Screen]
- Version info, update checking
- Links to external resources

### Category Screen [Screen]
- Edit/manage library categories
- Add/Edit/Delete categories

### Search Settings Screen [Screen]
- Search through all settings preferences
- Display matching settings with navigation

---

**Note:** The app uses a tabbed dialog pattern for many settings screens, with tabs labeled Filter, Sort, and Display being common across different contexts (Library and Chapter settings). The reader settings dialog also uses a tabbed pattern with Reading Mode, General, and Color Filter tabs.
