# Color Picker UI/UX Fixes - Implementation Plan

**Status:** Documented for future implementation
**Created:** 2026-01-02
**Context:** Multiple issues in Color Picker under Settings > Colors (in book and main settings)

## Issues to Fix

### 1. Missing Lock/Unlock Button for Custom Themes
**Location:** Settings > Appearance > Colors (main settings) and Settings > Colors (in reader)
**Problem:** Custom color themes do not have a lock/unlock button beside the delete button
**Solution:**
- Add a lock icon button next to each custom theme's delete button
- Locked themes should be protected from accidental deletion
- Lock state should be stored in the database (add `isLocked: Boolean` field to ColorPreset entity)
- When a theme is locked, disable or hide the delete button
- Add visual indication (lock icon appears filled/different color when locked)

**Files to Modify:**
- `ColorPresetEntity.kt` - Add `isLocked: Boolean = false` field
- `ColorPreset.kt` (domain model) - Add `isLocked: Boolean = false` field
- Database migration (version bump) - Add column to ColorPresetEntity table
- `ColorPresetMapperImpl.kt` - Map new field in toColorPresetEntity() and toColorPreset()
- Color picker UI component that displays custom themes
- `ColorPresetsViewModel.kt` or similar - Add use case to toggle lock state

### 2. No Delete Confirmation Dialog for Custom Themes
**Location:** Settings > Appearance > Colors (main settings) and Settings > Colors (in reader)
**Problem:** Users can accidentally delete custom themes with a single tap
**Solution:**
- Show AlertDialog before deleting a custom theme
- Dialog should show: "Delete [ThemeName]?" with description "This cannot be undone."
- Only allow delete if theme is not locked (see issue #1)
- Confirm and Cancel buttons

**Implementation:** Add AlertDialog composable showing theme name and confirmation options before calling delete

### 3. HEX Color Field Alignment
**Location:** Both color settings screens
**Problem:** Background HEX color field is not on the same line as the "Background color" label
**Problem:** Same issue for Font color HEX box
**Solution:**
- Change layout from:
  ```
  Column:
    Text("Background color")
    HexTextField
  ```
- To:
  ```
  Row:
    Text("Background color")
    HexTextField
  ```
- Apply same fix for Font color row

**Files to Modify:**
- `ColorPickerWithTitle.kt` - Reorganize Row/Column layout for background and font color sections

### 4. Text Input Not Opening for Color Value Boxes
**Location:** Both color settings screens
**Problem:** When tapping the editable box to the right of a slider (RGB values), the box highlights but keyboard doesn't appear and text cannot be edited
**Root Cause:** TextField may be in readOnly state or focus request is not working properly
**Solution:**
- Verify TextField is not readOnly
- Add FocusRequester and LaunchedEffect to request focus when tapped
- Ensure keyboard appears on focus
- Test with: tap box → keyboard opens → type value → value updates

**Files to Modify:**
- `ColorPickerWithTitle.kt` - Update RGB value TextFields with proper focus handling:
  ```kotlin
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(isFocused) {
    if (isFocused) focusRequester.requestFocus()
  }

  OutlinedTextField(
    value = rgbValue,
    onValueChange = { rgbValue = it },
    modifier = Modifier.focusRequester(focusRequester),
    ...
  )
  ```

### 5. Remove Redundant Value Display Under Sliders
**Location:** Both color settings screens
**Problem:** Color sliders show values below them AND users can edit in the box to the right - redundant
**Solution:**
- Remove the Text() that displays slider value below each slider
- Each color line should have:
  1. RGB name (e.g., "Red:", "Green:", "Blue:")
  2. Slider (takes up most space)
  3. Editable value box (right-aligned, updates with slider or human input)
  4. Reset button (far right)

**Layout Example (per color row):**
```
Row(
  Row: [Text("Red:"), Slider(takes weight), TextField("255"), ResetButton]
)
```

**Files to Modify:**
- `ColorPickerWithTitle.kt` - Remove value Text() below each slider, ensure Row layout has proper spacing

## Implementation Order

1. **Start with #3 & #5** (layout fixes) - No database changes, lower risk
2. **Then #4** (keyboard/focus fix) - Logic fix, no database changes
3. **Then #1 & #2** (lock/delete) - Requires database migration, more complex

## Files Affected (Summary)

### UI Components
- `ColorPickerWithTitle.kt` - Main color picker component (primary changes)
- Color picker screen composables in main Settings
- Color picker screen in reader Settings

### Data Layer
- `ColorPresetEntity.kt` - Add `isLocked` field
- `ColorPreset.kt` - Add `isLocked` field in domain model
- `BookDatabase.kt` - Add database migration
- `ColorPresetMapperImpl.kt` - Map new field
- Repository/DAO changes to handle lock state

### Logic Layer
- ViewModel for custom color presets - add toggleLockTheme use case
- Delete validation - check if theme is locked before allowing delete

## Database Migration Details

When adding `isLocked` field:
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `ColorPresetEntity` ADD COLUMN `isLocked` INTEGER NOT NULL DEFAULT 0")
    }
}
```

## Testing Checklist

- [ ] Lock button appears next to delete for custom themes
- [ ] Locked theme delete button is disabled/hidden
- [ ] Delete confirmation dialog appears before deletion
- [ ] Can unlock a locked theme
- [ ] HEX fields are on same line as color labels
- [ ] RGB value boxes accept keyboard input
- [ ] Typing in RGB box updates slider position
- [ ] Moving slider updates RGB box
- [ ] No duplicate value display (only in editable box)
- [ ] Reset button works for all color types
- [ ] Works in both main Settings and in-reader Settings
- [ ] Database migration runs without errors on upgrade

## Notes

- All changes should use Material 3 components for consistency
- Maintain existing color preset functionality (save, load, delete)
- Consider adding haptic feedback when toggling lock state
- Lock icon should be consistent with Material Design guidelines
