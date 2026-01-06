
Current State (from screenshot):
- Title: "Filters" at top.
- Sections: 
  - "Status Presets" with list items: Reading, Planning, Already Read, Favorites (some with play arrows).
  - "Tags" with "Placeholder for tags".
  - "Authors" with "Placeholder for authors".
  - "Series" with "Placeholder for series".
  - "Publication Year" with "Placeholder for year range".
  - "Language" with "Placeholder for languages".
- Bottom nav visible (Library, History, Catalogs, Settings).
- Dark theme, minimal padding, simple text lists with occasional arrows.

Desired Improvements:
- Make sections expandable/collapsible (e.g., using ExpandableList or Accordion with arrows/icons).
- Top: "Filters" title with a "Clear All" button/icon on the right.
- Status Presets: As clickable chips (multi-select, blue when active) for "Reading", "Planning", "Already Read", "Favorites" (map to predefined tags like #reading).
- Authors/Series/Languages: Dropdown (LazyColumn in Dialog) or searchable multi-select lists (chips when selected).
- Publication Year: RangeSlider (e.g., 1900-2026, with labels).
- Bottom: "Apply" button to close and update filters.
- State: Use MutableStateFlow<FilterState> in ViewModel (FilterState data class with sets/lists for each dimension, AND/OR toggle).
- Integration: Collect from ViewModel for dynamic content (e.g., unique tags/authors from DB/OPDS). On apply, update library view.
- Error Handling: Empty states (e.g., "No tags available").
- Accessibility: Content descriptions, proper focus.
- Keep minimal: No overkill animations, match the rest of the app's material theme.

Output: Full composable function (e.g., FiltersPanel) + any needed data classes/ViewModel snippets. Use Material3 components. Assume Hilt for injection, Room for data.
