# Speed Reading Settings UI Improvement Plan

## Current State Analysis

### Existing Settings Structure

The Speed reading settings menu (accessed via Speed reading icon > tap menu > Settings) currently has this structure:

**1. Performance Settings**
- WPM (Words Per Minute) - Slider
- Manual sentence pause - Toggle
- Sentence pause duration - Slider (conditional)
- Speed reading OSD - Toggle
- Word size - Slider

**2. Color/Theme Settings**
- Color preset option (shared with normal reading)
- Background image option (shared with normal reading)

**3. Accent Character Section**
- Accent character - Toggle
- Accent color - Full RGB sliders + opacity (conditional, expandable)

**4. Horizontal Bars Section**
- Horizontal bars - Toggle
- Bar thickness - Slider (conditional)
- Bar length - Slider (conditional)
- Bar distance from word - Slider (conditional)
- Bar color - Full RGB sliders + opacity (conditional, expandable)

**5. Focal Point Section** (has SettingsSubcategoryTitle header)
- Vertical indicator type - Radio buttons (LINE/ARROWS/ARROWS_FILLED)
- Vertical indicators length - Slider
- Focal point position - Slider

**6. Custom Font Section**
- Custom font - Toggle
- Font family selector - Chip grid (conditional, expandable)

### Key Issues Identified

1. **Menu Length**: With 6 major sections and multiple expandable color pickers, the menu becomes very long when all options are enabled

2. **Focus Settings Scattered**: Focus-related settings span three sections (Accent Character, Horizontal Bars, Focal Point), making it hard to see the complete focus visualization system at a glance

3. **Color Picker Space**: Each color picker (Accent color, Bar color) uses significant vertical space with full RGB sliders + opacity, taking ~300-400dp when expanded

4. **Custom Font Distance**: The Custom Font toggle is at the bottom, far from the Color Presets at the top, even though they're both appearance/styling settings

5. **Confusing Relationships**:
   - "Bar distance from word" is in the Horizontal Bars section but may affect vertical indicator distance
   - Vertical Indicator settings are separate from Horizontal Bars even though they work together to create the focal point

6. **Section Headers**: Only "Focal Point" has an explicit section header, other sections rely on visual spacing and dividers

## Recommended Improvements

### Option A: Two-Tab Approach (RECOMMENDED)

Split Speed reading settings into two focused tabs to reduce scrolling and improve organization:

#### **Tab 1: "General"**
Groups all non-focus settings together:

1. **Performance** (no header needed)
   - WPM slider
   - Manual sentence pause toggle
   - Sentence pause duration slider (conditional)
   - Speed reading OSD toggle
   - Word size slider

2. **Appearance** (add SettingsSubcategoryTitle header)
   - Color preset option
   - Background image option
   - Custom font toggle
   - Font family selector (conditional, expandable)

**Benefits:**
- Custom Font moved near Color Presets (both appearance-related)
- Clean separation of performance vs appearance
- Shorter scrolling distance
- Clear purpose: "How fast + how it looks"

#### **Tab 2: "Focus"**
Groups all focus visualization settings:

1. **Focal Point** (SettingsSubcategoryTitle header)
   - Focal point position slider (MOVE UP - most fundamental setting)
   - Vertical indicator type radio buttons
   - Vertical indicators length slider

2. **Accent Character** (SettingsSubcategoryTitle header)
   - Accent character toggle
   - Accent color - STREAMLINED compact color picker (conditional)

3. **Horizontal Bars** (SettingsSubcategoryTitle header)
   - Horizontal bars toggle
   - Bar thickness slider (conditional)
   - Bar length slider (conditional)
   - Bar distance from word slider (conditional)
   - Bar color - STREAMLINED compact color picker (conditional)

**Benefits:**
- All focus elements grouped logically
- Easy to understand how accent, bars, and indicators work together
- Can adjust focal point position first, then customize indicators around it
- Related settings are adjacent

### Streamlined Color Picker Design

Replace the current full ColorPickerWithTitle (which shows title + hex + full RGB sliders + opacity sliders) with a **compact inline design**:

**Collapsed State (Default):**
```
Accent color          [    #FF5722    ] [color box] [expand ▼]
```
- Single row: Title + Hex input (100dp wide) + Color preview box (40dp) + Expand button
- Similar to SearchHighlightColorOption's compact header pattern
- Saves ~250dp of vertical space per color picker

**Expanded State (When expand button clicked):**
```
Accent color          [    #FF5722    ] [color box] [collapse ▲]

R: 255  [slider================●]
G: 87   [slider=====●          ]
B: 34   [slider==●             ]
Opacity: 100%  [slider================●]
```
- Shows full RGB + Opacity sliders below
- No numeric inputs (keeps it cleaner, hex input covers precise needs)
- Can collapse back to single row

**Implementation:**
- Wrap ColorPickerWithTitle in AnimatedVisibility
- Add expand/collapse button to toggle visibility
- Keep hex input and preview always visible
- Use same animation pattern as other conditional settings (expandVertically + fadeIn)

**Benefits:**
- Dramatically reduces vertical space when collapsed
- Still provides quick hex editing without expanding
- Full control available when needed
- Consistent with Material 3 progressive disclosure pattern
- Similar to how settings like "Sentence pause duration" work

### Option B: Single Menu with Better Organization

If tabs aren't desired, improve the single menu:

1. **Performance** (no change, keep at top)

2. **Appearance** (add header, move Custom Font up)
   - Color preset option
   - Background image option
   - Custom font toggle
   - Font family selector (conditional)

3. **Focus Settings** (add header, combine all three focus sections)
   - Focal point position slider (move up - most fundamental)
   - Vertical indicator type radio buttons
   - Vertical indicators length slider
   - Accent character toggle
   - Accent color - streamlined picker (conditional)
   - Horizontal bars toggle
   - Bar thickness slider (conditional)
   - Bar length slider (conditional)
   - Bar distance from word slider (conditional)
   - Bar color - streamlined picker (conditional)

**Benefits:**
- Single scrolling menu (familiar pattern)
- Custom Font near Color Presets
- All focus elements together
- Streamlined color pickers reduce length

**Drawbacks:**
- Still potentially long when all options enabled
- Less clear separation than tabs

### Section Naming Recommendations

1. **"Appearance"** instead of "Color/Theme Settings"
   - More concise
   - Matches main Settings > Appearance category naming
   - Encompasses color presets, background, AND custom fonts

2. **"Focus Settings"** or **"Focus Indicators"**
   - Clear umbrella term for accent/bars/focal point
   - Describes purpose (help user focus on current word)

3. **Keep "Performance"** implicit (no header needed)
   - First items, obvious purpose
   - Matches pattern from other settings (not everything needs headers)

### Dynamic Toggle Recommendations

**Keep Current Pattern:**
- Sentence pause duration (depends on manual pause)
- Accent color picker (depends on accent enabled)
- All horizontal bar properties (depends on bars enabled)
- Font family selector (depends on custom font enabled)

**New Addition:**
- Color picker expand/collapse (manual toggle for each picker)
- Starts collapsed by default
- Expands to show full sliders when user clicks expand button

**Do NOT Add:**
- Collapsible section headers (accordion style) - not used elsewhere in app
- Auto-hide based on context - keep controls visible/accessible

## Implementation Notes

### Two-Tab Implementation

Similar to Reader Settings (Books/Comics tabs):

1. **Create TabRow** in SpeedReadingSettingsBottomSheet.kt
   - Two tabs: "General" and "Focus"
   - Use HorizontalPager with rememberPagerState()
   - Custom tab indicator animation (same as ReaderSettingsLayout)

2. **Split SpeedReadingSubcategory.kt** into two composables:
   - `SpeedReadingGeneralTab()` - Performance + Appearance sections
   - `SpeedReadingFocusTab()` - Focal Point + Accent + Bars sections

3. **Update bottom sheet height** if needed (currently 70% of screen)

### Compact Color Picker Component

**New Component:** `CompactColorPickerWithTitle.kt`

Based on SearchHighlightColorOption's pattern but with expand/collapse:

```kotlin
@Composable
fun CompactColorPickerWithTitle(
    color: Color,
    onColorChange: (Color) -> Unit,
    title: String,
    opacity: Float? = null,
    onOpacityChange: ((Float) -> Unit)? = null,
    isLocked: Boolean = false
)
```

**State:**
- `var isExpanded by remember { mutableStateOf(false) }`

**Layout:**
- Always show: Title + Hex input (100dp) + Preview box (40dp) + Expand button
- Conditional (AnimatedVisibility): RGB sliders + Opacity slider
- No numeric inputs for R/G/B (hex input covers precise needs)

### Files to Modify

1. **SpeedReadingSettingsBottomSheet.kt**
   - Add TabRow if using two-tab approach
   - Add HorizontalPager
   - Update tab state management

2. **SpeedReadingSubcategory.kt**
   - Split into SpeedReadingGeneralTab and SpeedReadingFocusTab (if tabs)
   - OR reorganize sections (if single menu)
   - Reorder settings per plan
   - Add SettingsSubcategoryTitle for "Appearance" and "Focus Settings"

3. **Create CompactColorPickerWithTitle.kt** (new file)
   - In `presentation/core/components/`
   - Implements compact color picker with expand/collapse

4. **SpeedReadingColorsOption.kt**
   - Update to use CompactColorPickerWithTitle instead of ColorPickerWithTitle

5. **SpeedReadingHorizontalBarsColorOption.kt**
   - Update to use CompactColorPickerWithTitle instead of ColorPickerWithTitle

### Testing Checklist

1. **Visual Testing**
   - Open book > Speed reading icon > tap menu > Settings > Speed reading
   - Verify tab switching works smoothly (if tabs)
   - Verify all sections render correctly
   - Check color pickers expand/collapse properly
   - Verify custom font selector still expands

2. **Functional Testing**
   - Test all settings save correctly
   - Verify color changes reflect in real-time during speed reading
   - Test focal point position changes update immediately
   - Verify horizontal bars distance affects visual correctly
   - Test accent character appears at correct position

3. **Edge Cases**
   - All toggles OFF - menu should be short
   - All toggles ON - verify scrolling works, no overlap
   - Long preset names - verify truncation
   - Many custom fonts - verify chip wrapping

4. **Comparison Testing**
   - Compare with other settings screens (Appearance, Reader > Books)
   - Verify spacing matches (18dp horizontal, 8dp vertical)
   - Verify colors match Material 3 tokens
   - Verify animations match existing pattern (expandVertically + fadeIn)

## Final Recommendations

### Primary Recommendation: Two-Tab with Compact Color Pickers

**Implement:**
1. Two-tab approach (General + Focus)
2. Move Custom Font to Appearance section in General tab
3. Combine all focus settings in Focus tab
4. Implement compact expandable color pickers
5. Add "Appearance" section header in General tab
6. Reorder Focal Point settings (position first, then indicators)

**Rationale:**
- Matches established pattern (Reader Settings uses tabs)
- Significantly reduces scrolling
- Clear mental model: "How it runs + looks" vs "How I focus"
- Compact color pickers save space without losing functionality
- Custom Font near Color Presets makes logical sense
- All focus elements visible together for holistic understanding

### Alternative: Single Menu (If tabs not desired)

**Implement:**
1. Single menu with reorganized sections
2. Add "Appearance" header, move Custom Font up
3. Add "Focus Settings" header, combine accent/bars/focal point
4. Implement compact expandable color pickers

**Rationale:**
- Simpler implementation
- Familiar single-scroll pattern
- Still achieves most benefits via reorganization + compact pickers
