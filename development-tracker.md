# Development Tracker: Fixes and Improvements

## Session Summary
Started: Mon Jan 05 2026

## Current Session: Compilation Error Fixes for Rescan Feature
**Status:** Completed - Build now successful

### Issues Resolved:
1. **BrowseModel.kt** - Indentation & Syntax Errors
   - Fixed extra indentation on OnDismissDialog case (line 395)
   - Removed wrongly-indented extra closing braces (lines 402-403)
   - Added missing `viewModelScope.launch` wrapper in OnDismissDialog event handler

2. **BrowseEmptyPlaceholder.kt** - Missing Imports
   - Added import for `StyledText` component
   - Added import for `BrowseScreen`

3. **BrowseDialog.kt** - Experimental API Warning
   - Added `@OptIn(ExperimentalMaterial3Api::class)` annotation

4. **Function Signature Updates** - Missing Parameters
   - BrowseScaffold: Added `onRescan: () -> Unit` parameter
   - BrowseContent: Added `onRescan: () -> Unit` parameter
   - BrowseEmptyPlaceholder: Added `onRescan: () -> Unit` parameter

5. **BrowseScreen.kt** - Missing Implementation
   - Implemented `onRescan` callback to trigger `BrowseEvent.OnRefreshList`

**Build Status:** ✅ BUILD SUCCESSFUL

## Issues and Tasks

### 1. Search Scrollbar Alignment and Keyboard Adjustment
**Status:** Completed  
**Description:** Search scrollbar indicators don't align with actual match locations. On mobile, keyboard covers lower half. Need to adjust scrollbar height from bottom of top bar to top of keyboard.  
**Files to Investigate:** presentation/reader/components/ReaderSearchScrollbar.kt  
**Priority:** High  
**Notes:** Fixed position-based alignment and keyboard detection. When keyboard visible, bottomOffset set to imeInsets only.

### 2. Slow Book Adding Performance
**Status:** Pending  
**Description:** Adding large numbers of books takes too long, likely due to metadata loading. Examine if caching can be deferred until book opening.  
**Files to Investigate:** data/repository/, data/local/, domain/library/use_case/  
**Priority:** Medium  
**Notes:** Profile metadata parsing (PDF, EPUB, etc.) to identify bottlenecks. Consider lazy loading metadata.

### 3. Filename-Based Book Naming
**Status:** Pending  
**Description:** Add check for "title - author" format in filenames for in-app display, fallback to metadata.  
**Files to Investigate:** data/parser/, domain/book/, presentation/library/  
**Priority:** Low  
**Notes:** Implement regex or string parsing in book title resolution. Quality of life improvement.

## Completed Tasks
- [x] Create git branch 'fixes-improvements'
- [x] Create development-tracker.md for tracking changes
- [x] Fix search scrollbar alignment and keyboard adjustment (committed)
- [x] Optimize slow book adding performance (committed)
- [x] Implement 'title - author' filename naming check (committed)
- [x] Run lint and typecheck (passed)

## Commits
- `36c8f37`: Fix search scrollbar: align indicators to actual positions and adjust height for keyboard
- `bb21d70`: Optimize book adding: defer cover loading in previews and implement 'title - author' filename parsing
- `dda47cc`: Fix covers not loading and missing titles: re-parse with covers on insert, add title fallbacks
- `a94fa3e`: Add select all button to library selection mode
- `684250f`: Make add books dialog scrollable with fixed OK/Cancel buttons at bottom
- `b1ef914`: Replace Clear Progress History icons and reposition: Refresh icon in library and book info top bar, remove from file details
- `87e868d`: Add confirmation dialog for Clear Progress History in book info
- `6350a97`: Swap OK and Cancel button order in all confirmation dialogs
- `ca02709`: Add Rescan functionality to browse screen and settings

## Issues Fixed
- Covers now load properly by re-parsing with covers enabled during book insertion
- Titles are guaranteed to have a fallback ("Untitled Book") if parsing fails completely
- Book info screens should now populate correctly
- Added "Select All" button in library selection mode for easier bulk operations
- Improved add books dialog UX with scrollable list and always-visible action buttons
- Replaced similar-looking Clear Progress History and Delete icons with distinct Refresh icon, repositioned buttons for better UX
- Added confirmation dialog for Clear Progress History to prevent accidental resets
- Standardized button order to OK first, Cancel second in all confirmation dialogs
- Added Rescan buttons to browse screen empty state and Settings > Browse for rescanning added folders  

## Next Steps
- Investigate search scrollbar issue  
- Profile book adding performance  
- Implement filename naming logic  

## Notes
- Use this file to track progress across sessions  
- Update status as tasks are completed  
- Add code references with file_path:line_number format