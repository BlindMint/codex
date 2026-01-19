# Speed Reading Word Picker Panel Implementation Plan

## Overview
Add a Word Picker Panel to the speed reader that allows users to select any word in the book as their starting point for speed reading.

## Trigger
- Tap book icon (MenuBook) in speed reader bottom bar (SpeedReadingContent.kt:620-626)

## New File
**`/home/samurai/dev/codex/app/src/main/java/us/blindmint/codex/presentation/reader/SpeedReadingWordPickerSheet.kt`**

## Files to Modify
1. **SpeedReadingContent.kt** - Add book icon click handler, add `wordPickerActive` parameter to disable tap menu
2. **SpeedReadingScaffold.kt** - Add word picker sheet state and display

---

## UI Structure

```
ModalBottomSheet (fillMaxHeight(0.9f))
├── Search Bar Row
│   ├── OutlinedTextField (search input, debounced 300ms)
│   ├── Result count text ("3/15")
│   ├── IconButton (previous result - KeyboardArrowUp)
│   └── IconButton (next result - KeyboardArrowDown)
│
├── Divider
│
├── LazyColumn (scrollable word content)
│   └── items(paragraphs) { FlowRow with tappable words }
│       - Extra spacing: 12dp horizontal, 8dp vertical
│       - Left-aligned (not justified)
│       - Current word: highlighted with primaryContainer
│       - Selected word: highlighted with tertiaryContainer
│       - Search matches: secondaryContainer at 50% alpha
│
└── Bottom Row
    ├── Spacer(weight(1f))
    ├── TextButton("Cancel")
    └── Button("Confirm")
```

---

## Data Model

```kotlin
data class WordPosition(
    val word: String,
    val textIndex: Int,          // Index in List<ReaderText>
    val wordIndexInText: Int,    // Word position within that text
    val globalWordIndex: Int     // Global word index across book
)
```

Words extracted from `List<ReaderText>`, filtering for `ReaderText.Text` and splitting by whitespace.

---

## Key Behaviors

1. **On Open**: Auto-scroll to current word (based on `currentProgress`), highlight it
2. **Tap Word**: Select as potential new starting point (shows different highlight)
3. **Search**: Debounced search highlights all matches, next/prev navigates and scrolls
4. **Confirm**: Calculate progress from `textIndex / text.lastIndex`, call `onChangeProgress(newProgress)`
5. **Cancel**: Dismiss without changes

---

## Progress Calculation

```kotlin
val newProgress = selectedWordPosition.textIndex.toFloat() / text.lastIndex.toFloat()
```

This matches existing `onChangeProgress` usage which expects a Float (0.0 to 1.0).

---

## Tap Menu Disable

Add `wordPickerActive: Boolean` parameter to SpeedReadingContent. In the `pointerInput` tap handler:

```kotlin
if (wordPickerActive) continue  // Skip all tap processing
```

State flows: `showWordPicker` in SpeedReadingScaffold -> `wordPickerActive` passed to SpeedReadingContent

---

## Implementation Steps

### Phase 1: Core Structure
1. Create `SpeedReadingWordPickerSheet.kt` with ModalBottomSheet skeleton
2. Add `onShowWordPicker: () -> Unit` callback to SpeedReadingContent
3. Add click handler to book icon
4. Add sheet state in SpeedReadingScaffold

### Phase 2: Word Display
5. Extract words from `List<ReaderText>` into `List<WordPosition>`
6. Group by `textIndex` for paragraph-based LazyColumn items
7. Render with FlowRow, proper spacing, tap handlers
8. Implement selection highlighting

### Phase 3: Search
9. Add search bar with debounced input
10. Implement local search over `allWords`
11. Add next/prev navigation with scroll-to

### Phase 4: Integration
12. Implement progress calculation on confirm
13. Wire to `onChangeProgress` callback
14. Add `wordPickerActive` state to disable tap menu

### Phase 5: Polish
15. Auto-scroll to current word on open
16. Test all flows

---

## Verification

1. Build: `./gradlew assembleDebug`
2. Test on device:
   - Open speed reader, tap book icon -> panel opens
   - Current word is highlighted and visible
   - Tap any word -> selection highlight changes
   - Search for word -> matches highlighted, next/prev works
   - Confirm -> speed reader starts from selected word
   - Cancel -> returns without change
   - While panel is open, tapping outside doesn't trigger tap menu
