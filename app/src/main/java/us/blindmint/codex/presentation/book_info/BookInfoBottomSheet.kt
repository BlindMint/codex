/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.runtime.Composable
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.util.BottomSheet
import us.blindmint.codex.ui.book_info.BookInfoEvent
import us.blindmint.codex.ui.book_info.BookInfoScreen

@Composable
fun BookInfoBottomSheet(
    bottomSheet: BottomSheet?,
    book: Book,
    editedBook: Book?,
    showTagsDialog: (BookInfoEvent.OnShowTagsDialog) -> Unit,
    showSeriesDialog: (BookInfoEvent.OnShowSeriesDialog) -> Unit,
    showLanguagesDialog: (BookInfoEvent.OnShowLanguagesDialog) -> Unit,
    refreshMetadataFromOpds: (BookInfoEvent.OnRefreshMetadataFromOpds) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit,
    onConfirmSaveChanges: (android.content.Context) -> Unit,
    onCancelChanges: () -> Unit,
    onUpdateEditedBook: (Book) -> Unit,
    onCategoryChange: (Category) -> Unit
) {
    when (bottomSheet) {
        BookInfoScreen.DETAILS_BOTTOM_SHEET -> {
            BookInfoDetailsPanel(
                book = book,
                editedBook = editedBook,
                showTagsDialog = showTagsDialog,
                showSeriesDialog = showSeriesDialog,
                showLanguagesDialog = showLanguagesDialog,
                refreshMetadataFromOpds = refreshMetadataFromOpds,
                dismissBottomSheet = dismissBottomSheet,
                onConfirmSaveChanges = onConfirmSaveChanges,
                onCancelChanges = onCancelChanges,
                onUpdateEditedBook = onUpdateEditedBook,
                onCategoryChange = onCategoryChange
            )
        }
    }
}
