/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoveUp
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.LibraryTabWithBooks
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.SearchTextField
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBar
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBarData
import us.blindmint.codex.presentation.navigator.NavigatorIconButton
import us.blindmint.codex.ui.library.LibraryEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
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
    categories: List<LibraryTabWithBooks>,
    libraryShowCategoryTabs: Boolean,
    libraryShowBookCount: Boolean,
    searchVisibility: (LibraryEvent.OnSearchVisibility) -> Unit,
    requestFocus: (LibraryEvent.OnRequestFocus) -> Unit,
    searchQueryChange: (LibraryEvent.OnSearchQueryChange) -> Unit,
    search: (LibraryEvent.OnSearch) -> Unit,
    clearSelectedBooks: (LibraryEvent.OnClearSelectedBooks) -> Unit,
    showMoveDialog: (LibraryEvent.OnShowMoveDialog) -> Unit,
    showDeleteDialog: (LibraryEvent.OnShowDeleteDialog) -> Unit,
    showClearProgressHistoryDialog: (LibraryEvent.OnShowClearProgressHistoryDialog) -> Unit,
    sortMenuVisibility: (LibraryEvent) -> Unit,
    allSelectedBooksAreFavorites: Boolean,
    toggleSelectedBooksFavorite: () -> Unit,
    allBooksSelected: Boolean,
    selectAllBooks: (LibraryEvent.OnSelectAllBooks) -> Unit
) {
    val animatedItemCountBackgroundColor = animateColorAsState(
        if (hasSelectedItems) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    TopAppBar(
        scrollBehavior = null,
        isTopBarScrolled = hasSelectedItems,

        shownTopBar = when {
            hasSelectedItems -> 2
            showSearch -> 1
            else -> if (libraryShowCategoryTabs) 0 else 0
        },
        topBars = listOf(
            TopAppBarData(
                contentID = 0,
                contentNavigationIcon = {},
                contentTitle = {
                    if (libraryShowCategoryTabs) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StyledText(text = stringResource(id = R.string.library_screen))
                            if (libraryShowBookCount) {
                                Spacer(modifier = Modifier.width(6.dp))
                                StyledText(
                                    text = bookCount.toString(),
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainer,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = LocalTextStyle.current.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    } else {
                        StyledText(text = categories.getOrNull(pagerState.currentPage)?.title?.asString() ?: stringResource(id = R.string.library_screen))
                    }
                },
                contentActions = {
                    IconButton(
                        icon = Icons.Default.Search,
                        contentDescription = R.string.search_content_desc,
                        disableOnClick = true,
                    ) {
                        searchVisibility(LibraryEvent.OnSearchVisibility(true))
                    }
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = R.string.sort_content_desc,
                        disableOnClick = false,
                    ) {
                        sortMenuVisibility(LibraryEvent.OnShowSortMenu)
                    }
                }
            ),

            TopAppBarData(
                contentID = 1,
                contentNavigationIcon = {
                    IconButton(
                        icon = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = R.string.exit_search_content_desc,
                        disableOnClick = true
                    ) {
                        searchVisibility(LibraryEvent.OnSearchVisibility(false))
                    }
                },
                contentTitle = {
                    SearchTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                requestFocus(LibraryEvent.OnRequestFocus(focusRequester))
                            },
                        initialQuery = searchQuery,
                        onQueryChange = {
                            searchQueryChange(LibraryEvent.OnSearchQueryChange(it))
                        },
                        onSearch = {
                            search(LibraryEvent.OnSearch)
                        }
                    )
                },
                contentActions = {},
            ),

            TopAppBarData(
                contentID = 2,
                contentNavigationIcon = {
                    IconButton(
                        icon = Icons.Default.Clear,
                        contentDescription = R.string.clear_selected_items_content_desc,
                        disableOnClick = true
                    ) {
                        clearSelectedBooks(LibraryEvent.OnClearSelectedBooks)
                    }
                },
                contentTitle = {
                    StyledText(
                        text = stringResource(
                            id = R.string.selected_items_count_query,
                            selectedItemsCount.coerceAtLeast(1)
                        ),
                        maxLines = 1
                    )
                },
                contentActions = {
                    IconButton(
                        icon = Icons.Outlined.Checklist,
                        contentDescription = R.string.select_all_files_content_desc,
                        enabled = !isLoading && !isRefreshing,
                        disableOnClick = false,
                    ) {
                        selectAllBooks(LibraryEvent.OnSelectAllBooks)
                    }
                    IconButton(
                        icon = if (allSelectedBooksAreFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (allSelectedBooksAreFavorites) R.string.remove_from_favorites else R.string.add_to_favorites,
                        enabled = !isLoading && !isRefreshing,
                        disableOnClick = false,
                    ) {
                        toggleSelectedBooksFavorite()
                    }
                    IconButton(
                        icon = Icons.Outlined.History,
                        contentDescription = R.string.reset_reading_progress,
                        enabled = !isLoading && !isRefreshing,
                        disableOnClick = false
                    ) {
                        showClearProgressHistoryDialog(LibraryEvent.OnShowClearProgressHistoryDialog)
                    }
                    IconButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = R.string.delete_books_content_desc,
                        enabled = !isLoading && !isRefreshing,
                        disableOnClick = false
                    ) {
                        showDeleteDialog(LibraryEvent.OnShowDeleteDialog)
                    }
                }
            ),
        ),
        customContent = {
            if (libraryShowCategoryTabs) {
                LibraryTabs(
                    categories = categories,
                    pagerState = pagerState,
                    itemCountBackgroundColor = animatedItemCountBackgroundColor.value,
                    showBookCount = libraryShowBookCount
                )
            }
        }
    )
}