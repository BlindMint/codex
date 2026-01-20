/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoScaffold(
    book: Book,
    listState: LazyListState,
    showChangeCoverBottomSheet: (BookInfoEvent.OnShowChangeCoverBottomSheet) -> Unit,
    showDetailsBottomSheet: (BookInfoEvent.OnShowDetailsBottomSheet) -> Unit,
    showDeleteDialog: (BookInfoEvent.OnShowDeleteDialog) -> Unit,
    showResetProgressDialog: (BookInfoEvent.OnShowResetProgressDialog) -> Unit,
    toggleFavorite: () -> Unit,
    navigateToReader: () -> Unit,
    navigateToSpeedReading: (() -> Unit)? = null,
    onCategoryChange: (us.blindmint.codex.domain.library.category.Category) -> Unit,
    navigateBack: () -> Unit
) {
    Scaffold(
        Modifier
            .fillMaxSize()
            .imePadding()
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            BookInfoTopBar(
                book = book,
                listState = listState,
                showDetailsBottomSheet = showDetailsBottomSheet,
                showDeleteDialog = showDeleteDialog,
                showResetProgressDialog = showResetProgressDialog,
                toggleFavorite = toggleFavorite,
                navigateBack = navigateBack
            )
        }
    ) { paddingValues ->
        BookInfoLayout(
            book = book,
            listState = listState,
            paddingValues = paddingValues,
            showChangeCoverBottomSheet = showChangeCoverBottomSheet,
            navigateToReader = navigateToReader,
            navigateToSpeedReading = navigateToSpeedReading,
            onCategoryChange = onCategoryChange
        )
    }
}