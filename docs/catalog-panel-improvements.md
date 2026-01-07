Based on your app's current design (minimal dark theme, Material You elements like icons/sliders/chips, bottom nav with Library/History/Catalogs/Settings), the alpha Filters panel (which we're iterating on separately), and the old Browse screenshots, here's a thoughtful approach to reformatting the "Catalogs" screen. This aligns with our OPDS integration plan: Catalogs serves as a discovery/preview hub for addable books from configured sources (local folders via SAF + OPDS catalogs like calibre-web), without cluttering the main Library. It emphasizes on-demand browsing (hierarchical for OPDS, flat for local), previews (metadata/covers/summaries), and easy adds (one-tap download/import).
Key Design Principles for Alignment

Unified but Source-Aware: Keep one screen to avoid silos, but differentiate sources visually (e.g., badges/icons) for clarity.
Minimalism: Mirror your app's clean lists (e.g., Bookmarks' simple vertical scroll, Settings' icon-led sections). Use empty states like the old "Set up scanning" for guidance.
OPDS Focus: OPDS feeds are hierarchical (e.g., authors > series > books), so support drill-down (expandable items or navigation). Local is flatter (file-based), so a single list fits your idea.
Actions: Preview details on tap (summary/cover); "Add" button for import/download (triggers metadata pull + DB insert).
Empty/Populated States: Handle no sources ("Add sources in Settings > Browse") or all added ("All books added—browse for more or add sources").
Bottom Nav Tie-In: Catalogs tab highlights when active (as in screenshot).

My Recommendation: Separate Sections on One Page with Collapsible Headers
This strikes the best balance—unified screen but source-aware, minimal UI additions, and OPDS-friendly. It avoids tabs (keeping your vertical flow) and folder breakdowns for local (flat list per section). For OPDS, use expandables for hierarchies (e.g., click "Authors" to load sub-feed). Empty state guides to Settings.

Why This Over Others?
Aligns with OPDS: Feeds are navigable (e.g., start at root, drill to books)—sections allow this without new screens.
Fits Your Workflow: Local as flat "available books" list (scan all subdirs recursively, show unadded ones).
Matches App Style: Like Settings' sections or alpha Filters' grouped lists.
Scalable: If multiple OPDS sources, each gets a header.


Detailed Layout Description

Top Bar: "Catalogs" title, search icon (queries all sources), menu (e.g., refresh).
Main Content: Scrollable LazyColumn with sections.
Empty State (if no sources or all added): Centered icon (books stack, like old screenshot), text: "No catalogs available. Add local folders or OPDS sources in Settings > Browse." Button: "Go to Settings".
Populated State:
Section Headers: Bold, with expand/collapse icons (chevron). E.g., "Local Sources" (count of addable books), "My Calibre-Web" (named from settings).
Under Local: Flat list of unadded books (title, author if parsed, size/date, "Add" button). No subdirs—aggregate all.
Under OPDS: Hierarchical—initially show top-level feeds (e.g., "Authors", "Series", "Tags" as clickable items). Tap to expand/load sub-items (books with previews: cover, summary snippet, "Add" button).


Item Design: List rows like old Browse (icon, title, subtitle with size/date/author, action button).
Actions:
Add: Downloads/imports to Library (with dedup check).
Preview: Tap book for modal with full metadata (summary, tags).

Bottom: Floating "Refresh" if needed; nav as-is.

Text-Based Mock-Up:
text[Top Bar]
Catalogs              [Search] [Menu]

[Scrollable Content]
Local Sources (12 addable books)  [Chevron Down]

- Book 1: Title, Author, Size/Date  [Add Button]
- Book 2: ...                       [Add]
...

My Calibre-Web (Connected)        [Chevron Down]

- Authors                         [> Arrow]
- Series                          [> Arrow]
- Recent Additions                [> Arrow]

(If expanded under Authors:)
  - Dan Abnett                    [> Arrow]
    - Book Preview: Title, Cover, Summary Snippet  [Add]

[Empty if all added: Centered books icon + "All books added from sources. Add more in Settings > Browse." + Button]
Implementation Notes

Use Compose: LazyColumn for list, ExpandableModifier for sections.
ViewModel: CatalogsViewModel fetches local (SAF scan unadded files) + OPDS (root feed, load on expand).
Tie to Settings: Load sources from DataStore.
Testing: Ensure offline fallback (cached OPDS previews).

This keeps Catalogs as a "pre-add" space, complementing Library's filtered views. If you prefer tabs for growth (e.g., many sources), we can pivot—but sections feel more "Codex-like" from your screens. Let me know if you want a refined LLM prompt for this screen's code!
