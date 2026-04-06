# Comic Reader Vertical Mode - Issue Tracking

## Original Issues (Fixed)
- [x] Issue 1: First page doesn't touch top of viewport (gap at top)
- [x] Issue 2: Scrolling up jumps back to first page

## New Issues Discovered

### Issue 3: Missing/Blank Pages (CBR files)
- **Severity:** High
- **Description:** Pages 10-15 sometimes show as blank (only background color visible)
- **Reproduction:** Primarily CBR files, re-opening sometimes fixes or changes which page is blank
- **Root cause identified**: LRU cache `removeEldestEntry` recycles bitmaps that may still be in use by Composables
- **Fix applied**: Disabled bitmap recycling in `removeEldestEntry` - cleanup happens when book closes anyway

### Issue 4: "Static" Noise at End of Comics
- **Severity:** High
- **Description:** Black/white noise appears at end of comics, progress slider stops updating, scrolling continues infinitely
- **Root cause**: Likely same as Issue 3 - recycled bitmaps showing garbage
- **Fix applied**: Same as Issue 3 (disabled bitmap recycling)

### Issue 5: Progress Slider Jumping Back
- **Severity:** High
- **Description:** Dragging slider past page 10-15 jumps back to ~15-16. Repeated drags jump back further (20, 21, 23, 24...)
- **CBZ Variant:** App closes/crashes when dragging slider ahead
- **Root cause 1**: `LaunchedEffect(currentPage)` updates `sliderValue` during slider drag, causing fighting
- **Fix 1**: Added `isDragging` state to skip `LaunchedEffect` update during drag
- **Root cause 2**: `isAlreadyVisible` check was too lenient - checked if target was ANYWHERE in visible list
- **Fix 2**: Changed to check `firstVisibleIndex == targetPhysicalPage` (must be at top of viewport)
- **Root cause 3**: When slider seeks to a page, `scrollToItem` triggers `snapshotFlow` to emit OLD `firstVisibleItemIndex` before layout updates
- **Fix 3**: Added `programmaticScrollTarget` tracking - skip `onPageChanged` emissions while programmatic scroll is in progress (guarded by `scrollTarget != -1 && scrollTarget != physicalIndex`)

### Issue 6: Book Processing on App Startup
- **Severity:** Low
- **Description:** App appears to load/process all books on startup (logs show "Loading book:" for all library items)
- **Status:** Not investigated yet

## Summary of Changes

### ComicReaderDisplay.kt
1. **Issue 1 fix**: Set vertical mode `contentPadding.top = 0.dp`
2. **Issue 2 fix**: Changed `isAlreadyVisible` to `isAlreadyAtTop` - only skip scroll if target is FIRST visible item
3. **Issue 5 fix**: Added `isDragging` guard to prevent `LaunchedEffect` from fighting with slider
4. **Issue 5 fix**: Added `programmaticScrollTarget` tracking to skip `onPageChanged` during programmatic scroll
5. **Issue 3 fix**: Disabled bitmap recycling in `removeEldestEntry`
6. **Cleanup**: Removed redundant cache check in `prefetchPages`

### ComicReaderLayout.kt
1. **Issue 3 fix**: Disabled bitmap recycling in `removeEldestEntry`

### ReaderBottomBarComicSlider.kt
1. **Issue 5 fix**: Added `isDragging` state with `interactionSource.collectIsDraggedAsState()` to prevent slider fighting with `LaunchedEffect`
