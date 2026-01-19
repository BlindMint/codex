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
import us.blindmint.codex.domain.library.LibraryTabWithBooks
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
    categories: List<LibraryTabWithBooks>,
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
    showBulkEditDialog: (LibraryEvent.OnShowBulkEditDialog) -> Unit,
    actionBulkEditTags: (LibraryEvent.OnActionBulkEditTags) -> Unit,
    actionBulkEditSeries: (LibraryEvent.OnActionBulkEditSeries) -> Unit,
    actionBulkEditLanguages: (LibraryEvent.OnActionBulkEditLanguages) -> Unit,
    dismissDialog: (LibraryEvent.OnDismissDialog) -> Unit,
    sortMenuVisibility: (LibraryEvent) -> Unit,
    allSelectedBooksAreFavorites: Boolean,
    toggleSelectedBooksFavorite: () -> Unit,
    allBooksSelected: Boolean,
    selectAllBooks: (LibraryEvent.OnSelectAllBooks) -> Unit,
    navigateToBrowse: () -> Unit,
    navigateToStorage: () -> Unit,
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
        actionBulkEditTags = actionBulkEditTags,
        actionBulkEditSeries = actionBulkEditSeries,
        actionBulkEditLanguages = actionBulkEditLanguages,
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
        showBulkEditDialog = showBulkEditDialog,
        sortMenuVisibility = sortMenuVisibility,
        allSelectedBooksAreFavorites = allSelectedBooksAreFavorites,
        toggleSelectedBooksFavorite = toggleSelectedBooksFavorite,
        allBooksSelected = allBooksSelected,
        selectAllBooks = selectAllBooks,
        refreshState = refreshState,
        navigateToBrowse = navigateToBrowse,
        navigateToStorage = navigateToStorage,
        navigateToBookInfo = navigateToBookInfo,
        navigateToReader = navigateToReader
    )

    LibrarySortMenu(
        show = showSortMenu,
        onDismiss = {
            sortMenuVisibility(LibraryEvent.OnDismissSortMenu)
        }
    )

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