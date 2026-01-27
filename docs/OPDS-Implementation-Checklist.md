# OPDS Implementation Checklist

**Project**: Codex - Material You eBook Reader  
**Date**: 2026-01-26  
**Branch**: `opds-analysis-improvements`

---

## Quick Reference

- 📋 **Task** = Pending implementation
- ✅ **Done** = Completed and verified
- ⏭️ **Skip** = Intentionally skipped
- ❌ **Blocked** = Cannot proceed due to dependency

---

## Phase 1: Security (CRITICAL) ⚡

### Task 1.1: Encrypt OPDS Credentials at Rest

**Priority**: 🔴 P1 - CRITICAL  
**Estimated Time**: 4-6 hours  
**Dependencies**: `androidx.security:security-crypto`, `androidx.security:security-identity`

#### Subtasks

- [x] Add encryption library dependency to `build.gradle.kts` (Google Tink 1.15.0)
- [x] Create `CredentialEncryptor` utility class using Tink + Android Keystore
- [x] Implement AES-256-GCM encryption for username/password
- [x] Create migration 21->22: Add encrypted credential columns
- [x] Create migration 22->23: Remove plaintext columns
- [x] Update `OpdsSourcesModel` to use encrypted credentials on insert/update
- [x] Update `OpdsSourcesModel` to decrypt credentials for API calls
- [x] Update `OpdsCatalogModel` to decrypt credentials from source entity
- [x] Update authentication checks to use encrypted fields
- [x] Update database version to 23 and add migrations to builder
- [ ] Write unit tests for encryption/decryption
- [ ] Test migration flow (plaintext → encrypted)
- [ ] Test with multiple OPDS sources
- [ ] Verify API calls use decrypted credentials correctly
- [x] Update OPDS-Analysis.md with implementation details

**Acceptance Criteria**:
- [x] No plaintext credentials in database (deprecated fields removed in v23)
- [x] Credentials encrypted with AES-256-GCM in database
- [x] Credentials decrypted only in memory during API call
- [x] Migration successfully handles existing sources via migrateExistingCredentials()
- [ ] All tests pass (unit + integration)

---

## Phase 2: Performance & Pagination (HIGH) ⚡

### Task 2.1: Implement Paging Library 3 for OPDS Catalog

**Priority**: 🔴 P1 - HIGH  
**Estimated Time**: 8-12 hours  
**Dependencies**: `androidx.paging:paging-runtime:3.x`, `androidx.paging:paging-compose:3.x`

#### Subtasks

- [ ] Add `androidx.paging:paging-runtime:3.x` dependency
- [ ] Add `androidx.paging:paging-compose:3.x` dependency
- [ ] Create `OpdsPagingSource` class extending `PagingSource<String, OpdsEntry>`
- [ ] Implement `load()` method with `LoadResult.Page`
- [ ] Implement `getRefreshKey()` method
- [ ] Implement `getRefreshKey()` method
- [ ] Update `OpdsCatalogModel` to create `Pager` instance
- [ ] Create factory method `createPager(OpdsSourceEntity)` in ViewModel
- [ ] Update `OpdsCatalogContent` to use `collectAsLazyPagingItems()`
- [ ] Add item keys: `key = lazyPagingItems.itemKey { it.id }`
- [ ] Implement LoadState handlers (loading, error, notLoading)
- [ ] Add retry button in LoadState.Error footer
- [ ] Add loading indicator in LoadState.Loading footer
- [ ] Test with feeds having < 20 items
- [ ] Test with feeds having 20-100 items
- [ ] Test with feeds having 100+ items (pagination triggers)
- [ ] Test "Load More" functionality
- [ ] Test error handling (network failure, 401, 404)
- [ ] Verify memory usage with large catalogs
- [ ] Remove old `loadMore()` button logic (replaced by Paging 3)
- [ ] Update `OpdsCatalogState` to remove manual pagination fields

**Acceptance Criteria**:
- [ ] Paging Library 3 successfully loads OPDS feeds
- [ ] Only visible items rendered (lazy loading)
- [ ] Smooth scrolling with prefetched pages
- [ ] Item keys prevent unnecessary recomposition
- [ ] Error states handled gracefully
- [ ] Memory usage < 50MB with 1000+ books

---

### Task 2.2: Add Skeleton Loading Screens

**Priority**: 🟡 P2 - MEDIUM  
**Estimated Time**: 2-3 hours

#### Subtasks

- [ ] Create `BookCardSkeleton` composable
- [ ] Match skeleton layout to actual `OpdsBookPreview` layout
- [ ] Add shimmer/pulse animation effect
- [ ] Update `OpdsCatalogContent` to show skeleton during initial load
- [ ] Fade transition from skeleton to actual content
- [ ] Test skeleton with different screen sizes
- [ ] Verify skeleton disappears smoothly when data loads

**Acceptance Criteria**:
- [ ] Skeleton matches book card dimensions
- [ ] Shimmer animation is smooth (60fps)
- [ ] Fade-in transition < 300ms
- [ ] Works on both phone and tablet layouts

---

## Phase 3: Fuzzy Search (MEDIUM) ⚡

### Task 3.1: Add Fuzzy Search to OPDS Catalog

**Priority**: 🟡 P2 - MEDIUM  
**Estimated Time**: 3-5 hours  
**Dependencies**: `me.xdrop:fuzzywuzzy:1.4.0` (already in project)

#### Subtasks

- [ ] Create `FuzzySearchHelper` object in `utils/` package
- [ ] Implement `searchEntries(List<OpdsEntry>, query, threshold)` function
- [ ] Add fuzzy matching for title field
- [ ] Add fuzzy matching for author field
- [ ] Add fuzzy matching for summary field
- [ ] Implement result sorting by relevance score
- [ ] Add debouncing (250ms) in `OpdsCatalogContent`
- [ ] Replace exact `contains()` search with fuzzy search
- [ ] Add score visualization (optional, percentage indicator)
- [ ] Test with typos (e.g., "potter" → "Potter")
- [ ] Test with partial matches (e.g., "harry" → "Harry Potter")
- [ ] Test with multi-word queries (e.g., "harry potter")
- [ ] Tune threshold value (start at 60, test 40-80 range)
- [ ] Document fuzzy search behavior
- [ ] Add search debouncing to prevent excessive API calls

**Acceptance Criteria**:
- [ ] Fuzzy search handles typos
- [ ] Partial matches work correctly
- [ ] Results sorted by relevance
- [ ] Debouncing reduces unnecessary searches
- [ ] Performance acceptable with 1000+ entries

---

## Phase 4: Material 3 Enhancements (LOW) ⚡

### Task 4.1: Improve Empty States

**Priority**: 🟢 P3 - LOW  
**Estimated Time**: 2-3 hours

#### Subtasks

- [ ] Create `EmptyCatalogState` composable
- [ ] Add illustration/icon for empty state
- [ ] Add descriptive message
- [ ] Add action button ("Refresh", "Add Source", etc.)
- [ ] Update `OpdsCatalogContent` to use `EmptyCatalogState`
- [ ] Create `EmptySearchState` composable
- [ ] Add suggestion to try different search term
- [ ] Test empty states for catalog, categories, books, search

**Acceptance Criteria**:
- [ ] Clear visual feedback for empty states
- [ ] Actionable button provided
- [ ] Material 3 compliant
- [ ] Works across all empty scenarios

---

### Task 4.2: Add Category Filter Chips

**Priority**: 🟢 P3 - LOW  
**Estimated Time**: 2-3 hours

#### Subtasks

- [ ] Create `CategoryFilterChips` composable
- [ ] Use `FilterChip` component for categories
- [ ] Implement horizontal scrolling for many categories
- [ ] Add active state styling
- [ ] Update `OpdsCatalogContent` to include category chips
- [ ] Filter books list when category selected
- [ ] Add "All Categories" option to clear filter
- [ ] Test with 5, 10, 20+ categories
- [ ] Ensure Material 3 spacing and sizing

**Acceptance Criteria**:
- [ ] Chips display correctly
- [ ] Horizontal scrolling works smoothly
- [ ] Active category clearly indicated
- [ ] Filter correctly updates book list
- [ ] "All Categories" clears filter

---

## Testing & Quality Assurance

### Unit Tests

- [ ] Test `CredentialEncryptor` encryption/decryption
- [ ] Test `FuzzySearchHelper` with edge cases
- [ ] Test `OpdsPagingSource` with empty feed
- [ ] Test `OpdsPagingSource` with single page
- [ ] Test `OpdsPagingSource` with multiple pages
- [ ] Test URL resolution (relative vs absolute)
- [ ] Test OPDS v1 parsing
- [ ] Test OPDS v2 parsing
- [ ] Test malformed feed handling

### Integration Tests

- [ ] Test real OPDS catalog connection (feedbooks.org test catalog)
- [ ] Test large category (1000+ books)
- [ ] Test authentication with valid credentials
- [ ] Test authentication with invalid credentials
- [ ] Test network error handling
- [ ] Test pagination navigation (prev/next)
- [ ] Test fuzzy search with typos
- [ ] Test credential migration flow
- [ ] Test encrypted credential retrieval

### Manual Testing

- [ ] Test on various screen sizes (phone, tablet, foldable)
- [ ] Test with different Android versions
- [ ] Test with dark/light theme
- [ ] Test with offline network
- [ ] Test performance (scroll smoothness, memory)
- [ ] Test UI responsiveness during loading
- [ ] Test error states and recovery
- [ ] Test accessibility (screen reader, keyboard nav)

---

## Documentation

- [ ] Update CLAUDE.md with new OPDS features
- [ ] Document credential encryption approach
- [ ] Document fuzzy search implementation
- [ ] Document Paging Library 3 integration
- [ ] Update README.md if needed
- [ ] Add code comments for complex logic
- [ ] Create migration guide for users
- [ ] Document security best practices

---

## Release Notes

### Version 2.5.0 (Proposed)

**Security**:
- 🔒 Encrypt OPDS credentials at rest using AndroidX Security Crypto

**Performance**:
- ⚡ Implement Paging Library 3 for efficient catalog browsing
- ⚡ Add skeleton loading screens for better perceived performance
- ⚡ Optimize LazyColumn with item keys

**Features**:
- 🔍 Add fuzzy search for OPDS catalog browsing
- 🎨 Improve empty states with illustrations and actions
- 🏷️ Add category filter chips for quick navigation

**Bug Fixes**:
- 🐛 Fix large category loading performance issue
- 🐛 Improve pagination for catalogs with 1000+ books

---

## Progress Summary

| Phase | Tasks | Completed | % Done |
|--------|--------|-----------|---------|
| **Phase 1: Security** | 11 | 0 | 0% |
| **Phase 2: Performance** | 22 | 0 | 0% |
| **Phase 3: Fuzzy Search** | 14 | 0 | 0% |
| **Phase 4: Material 3** | 18 | 0 | 0% |
| **Testing & QA** | 23 | 0 | 0% |
| **Documentation** | 9 | 0 | 0% |
| **TOTAL** | **97** | **0** | **0%** |

---

**Created**: 2026-01-26  
**Last Updated**: 2026-01-26  
**Status**: 📋 Ready to begin implementation
