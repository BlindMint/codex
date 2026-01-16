/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBar
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBarData
import us.blindmint.codex.presentation.navigator.NavigatorBackIconButton
import us.blindmint.codex.ui.book_info.BookInfoEvent
import us.blindmint.codex.ui.theme.DefaultTransition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoTopBar(
    book: Book,
    listState: LazyListState,
    showEditBottomSheet: (BookInfoEvent.OnShowEditBottomSheet) -> Unit,
    showDetailsBottomSheet: (BookInfoEvent.OnShowDetailsBottomSheet) -> Unit,
    toggleFavorite: () -> Unit,
    navigateBack: () -> Unit
) {
    val firstVisibleItemIndex = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }

    TopAppBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(0f),
        scrollBehavior = null,
        isTopBarScrolled = listState.canScrollBackward,

        shownTopBar = 0,
        topBars = listOf(
            TopAppBarData(
                contentID = 0,
                contentNavigationIcon = {
                    NavigatorBackIconButton {
                        navigateBack()
                    }
                },
                contentTitle = {
                    DefaultTransition(firstVisibleItemIndex.value > 0) {
                        StyledText(
                            text = book.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                    }
                },
                contentActions = {
                    IconButton(
                        icon = if (book.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (book.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                        disableOnClick = false,
                        onClick = toggleFavorite
                    )
                    IconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = R.string.edit_metadata,
                        disableOnClick = false,
                        onClick = {
                            showEditBottomSheet(BookInfoEvent.OnShowEditBottomSheet)
                        }
                    )
                    IconButton(
                        icon = Icons.Outlined.Info,
                        contentDescription = R.string.file_details,
                        disableOnClick = false,
                        onClick = {
                            showDetailsBottomSheet(BookInfoEvent.OnShowDetailsBottomSheet)
                        }
                    )
                }
            )
        )
    )
}