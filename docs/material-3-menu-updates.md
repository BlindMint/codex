Your **Sort & Display** menu is the cleanest and most M3-aligned of the bunch—it's structured, uses clear tabbing for sections, full-width elements (slider/toggles), and consistent spacing. The Reader settings screens feel a bit denser and less consistent by comparison: font chips are centered/left-justified in a way that doesn't fully use width, sliders have varying alignments, and segmented buttons could be more standardized.

Here are targeted, low-to-medium effort improvements to bring everything closer to current Material 3 (M3) guidelines (as of 2026: emphasis on full-width controls in forms, left-aligned labels with full-width interactive elements, FlowRow for wrapping chips, and SingleChoiceSegmentedButtonRow for exclusive selections). These keep your dark theme contrast strong (good job on that—your blues and purples pop without vibrating).

### General Recommendations Across All Menus
- **Layout Structure** → Use sections with bold headers (like "Text", "Images") and subtle dividers (M3 recommends HorizontalDivider() between major sections).
- **Alignment** → Left-align labels (as you do), but make interactive elements (sliders, button groups, chips) **full-width** or consistently wrapped (e.g., FlowRow for multi-line chips).
- **Spacing** → Standardize vertical padding ~16-24dp between sections; horizontal ~16dp start/end.
- **Colors/Contrast** → Your dark theme is solid (high contrast on selected states). Use M3 defaults: selected chips/buttons in primary/secondaryContainer, inactive in surfaceVariant. Avoid pure white text on very dark chips—your current blue highlights are perfect.
- **Button Types** → Prefer M3's built-in components:
  - Exclusive single-choice: `SingleChoiceSegmentedButtonRow`.
  - Multi-choice (if ever needed): Filter Chips in a FlowRow.
  - Binary toggles: Keep switches (as in "Display images").

### Specific Improvements by Screen

#### 1. Sort & Display (Already Strong – Minor Polish)
- It's nearly perfect: Tabbed sections, full-width slider, segmented buttons for mode.
- **Suggestions**:
  - Wrap the "Display mode" in `SingleChoiceSegmentedButtonRow` (M3's official segmented button) for smoother corners and built-in selection indicator.
  - Make toggles full-width rows (label on left, switch on right) for better touch targets.
  - Add a "Tabs" section header if it grows.
- **Result**: Even crisper, matches M3 examples for view customization.

#### 2. Reader > Text / Images Sections
- Segmented buttons are good but feel slightly cramped; sliders are centered-ish but could stretch.
- **Suggestions**:
  - **Text alignment / Color effects / Alignment**: Use `SingleChoiceSegmentedButtonRow` (full-width) for better shape handling and selection visuals.
  - **Sliders** (Line height, Paragraph spacing, etc.): Stretch to full-width with labels above or below (M3 recommends full-width for precise controls). Add value labels on the right (e.g., current value in bodySmall text).
  - **Toggles** (Images, Color effects): Convert to full-width rows: label on left, switch on right (like M3 settings examples).
  - **Corners roundness / Size**: Full-width sliders with thumb labels.
- **Result**: More spacious, easier thumb reach, matches M3 form patterns.

#### 3. Reader > Font Section (Biggest Opportunity)
- The grid of chips is visually busy and doesn't wrap elegantly on narrower screens; centering feels off with left-aligned headers.
- **Suggestions**:
  - **Switch to Filter Chips** in a `FlowRow` (M3's recommended way for multi-select or suggestion groups):
    - Single-choice: Use `SingleChoiceSegmentedButtonRow` for rows (e.g., group similar weights).
    - Or full FilterChip grid with `LazyVerticalGrid` or `FlowRow` (auto-wraps, full-width usage).
  - **Layout**: Left-align the entire chip group (not centered). Use `FlowRow` with `maxItemsInEachRow` if you want control.
  - **Selected State**: Highlight with primaryContainer background + onPrimaryContainer text (your current blue is close).
  - **Custom Fonts Button**: Make it an OutlinedButton full-width at bottom of section.
  - **Thickness / Style**: Use `SingleChoiceSegmentedButtonRow` (cleaner than custom segmented).
  - **Font Size Slider**: Full-width with value label.
- **Result**: Cleaner wrapping, better scalability (especially on phones), aligns with M3 chip guidelines for selection (e.g., font pickers in Google apps use similar filter chip flows).

### Quick Wins Summary Table

| Area                  | Current Issue                     | Recommended Change                          | Effort | M3 Benefit                  |
|-----------------------|-----------------------------------|---------------------------------------------|--------|-----------------------------|
| Segmented Buttons     | Custom, slight crowding           | Use `SingleChoiceSegmentedButtonRow`        | Low    | Official shapes, accessibility |
| Chip Groups (Fonts)   | Centered grid, no wrap            | FilterChips + FlowRow (left-aligned)        | Medium | Auto-wrap, full-width usage |
| Sliders               | Partial width, varying alignment  | Full-width, value labels                    | Low    | Precise control, readability |
| Toggles/Switches      | Compact                            | Full-width rows (label left, switch right)  | Low    | Better touch targets        |
| Section Spacing       | Inconsistent                      | 16-24dp vertical, dividers between sections | Low    | Hierarchy, breathing room   |

These changes will make your Reader settings feel as polished as Sort & Display while staying true to your minimal dark aesthetic. Start with the font section (highest impact) and the segmented button swap (quick win). If you want code snippets for any specific part (e.g., FlowRow chips or segmented row), just say!
