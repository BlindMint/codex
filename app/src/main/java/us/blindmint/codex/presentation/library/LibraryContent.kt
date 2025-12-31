/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import us.blindmint.codex.domain.library.book.SelectableBook
import us.blindmint.codex.domain.library.category.CategoryWithBooks
import us.blindmint.codex.domain.util.Dialog
import us.blindmint.codex.ui.library.LibraryEvent

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibraryContent(
    books: List<SelectableBook>,
    selectedItemsCount: Int,
    hasSelectedItems: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    bookCount: Int,
    showSortMenu: Boolean,
    focusRequester: FocusRequester,
    pagerState: PagerState,
    isLoading: Boolean,
    isRefreshing: Boolean,
    doublePressExit: Boolean,
    categories: List<CategoryWithBooks>,
    refreshState: PullRefreshState,
    dialog: Dialog?,
    libraryShowCategoryTabs: Boolean,
    libraryShowBookCount: Boolean,
    selectBook: (LibraryEvent.OnSelectBook) -> Unit,
    searchVisibility: (LibraryEvent.OnSearchVisibility) -> Unit,
    requestFocus: (LibraryEvent.OnRequestFocus) -> Unit,
    searchQueryChange: (LibraryEvent.OnSearchQueryChange) -> Unit,
    search: (LibraryEvent.OnSearch) -> Unit,
    clearSelectedBooks: (LibraryEvent.OnClearSelectedBooks) -> Unit,
    showMoveDialog: (LibraryEvent.OnShowMoveDialog) -> Unit,
    showDeleteDialog: (LibraryEvent.OnShowDeleteDialog) -> Unit,
    actionMoveDialog: (LibraryEvent.OnActionMoveDialog) -> Unit,
    actionDeleteDialog: (LibraryEvent.OnActionDeleteDialog) -> Unit,
    showClearProgressHistoryDialog: (LibraryEvent.OnShowClearProgressHistoryDialog) -> Unit,
    dismissDialog: (LibraryEvent.OnDismissDialog) -> Unit,
    sortMenuVisibility: (LibraryEvent) -> Unit,
    navigateToBrowse: () -> Unit,
    navigateToBookInfo: (id: Int) -> Unit,
    navigateToReader: (id: Int) -> Unit
) {
    LibraryDialog(
        dialog = dialog,
        books = books,
        categories = categories,
        selectedItemsCount = selectedItemsCount,
        actionMoveDialog = actionMoveDialog,
        actionDeleteDialog = actionDeleteDialog,
        actionClearProgressHistoryDialog = { event ->
            showClearProgressHistoryDialog(
                LibraryEvent.OnShowClearProgressHistoryDialog
            )
        },
        dismissDialog = dismissDialog
    )

    LibraryScaffold(
        selectedItemsCount = selectedItemsCount,
        hasSelectedItems = hasSelectedItems,
        showSearch = showSearch,
        searchQuery = searchQuery,
        bookCount = bookCount,
        showSortMenu = showSortMenu,
        focusRequester = focusRequester,
        pagerState = pagerState,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        categories = categories,
        libraryShowCategoryTabs = libraryShowCategoryTabs,
        libraryShowBookCount = libraryShowBookCount,
        searchVisibility = searchVisibility,
        requestFocus = requestFocus,
        searchQueryChange = searchQueryChange,
        search = search,
        selectBook = selectBook,
        clearSelectedBooks = clearSelectedBooks,
        showMoveDialog = showMoveDialog,
        showDeleteDialog = showDeleteDialog,
        showClearProgressHistoryDialog = showClearProgressHistoryDialog,
        sortMenuVisibility = sortMenuVisibility,
        refreshState = refreshState,
        navigateToBrowse = navigateToBrowse,
        navigateToBookInfo = navigateToBookInfo,
        navigateToReader = navigateToReader
    )

    if (showSortMenu) {
        LibrarySortMenu(
            onDismiss = {
                sortMenuVisibility(LibraryEvent.OnDismissSortMenu)
            }
        )
    }

    LibraryBackHandler(
        hasSelectedItems = hasSelectedItems,
        showSearch = showSearch,
        showSortMenu = showSortMenu,
        pagerState = pagerState,
        doublePressExit = doublePressExit,
        clearSelectedBooks = clearSelectedBooks,
        searchVisibility = searchVisibility,
        sortMenuVisibility = sortMenuVisibility,
    )
}