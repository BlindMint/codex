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
    isEditingMetadata: Boolean,
    editedBook: Book?,
    showConfirmSaveDialog: Boolean,
    showConfirmCancelDialog: Boolean,
    showPathDialog: (BookInfoEvent.OnShowPathDialog) -> Unit,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    showTagsDialog: (BookInfoEvent.OnShowTagsDialog) -> Unit,
    showSeriesDialog: (BookInfoEvent.OnShowSeriesDialog) -> Unit,
    showLanguagesDialog: (BookInfoEvent.OnShowLanguagesDialog) -> Unit,
    showEditBottomSheet: (BookInfoEvent.OnShowEditBottomSheet) -> Unit,
    resetTitle: (BookInfoEvent.OnResetTitle) -> Unit,
    resetAuthor: (BookInfoEvent.OnResetAuthor) -> Unit,
    resetDescription: (BookInfoEvent.OnResetDescription) -> Unit,
    clearProgressHistory: (BookInfoEvent.OnClearProgressHistory) -> Unit,
    refreshMetadataFromOpds: (BookInfoEvent.OnRefreshMetadataFromOpds) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit,
    onEnterEditMode: () -> Unit,
    onConfirmEditMetadata: () -> Unit,
    onCancelEditMetadata: () -> Unit,
    onSilentCancelEditMetadata: () -> Unit,
    onConfirmSaveChanges: (android.content.Context) -> Unit,
    onDismissSaveDialog: () -> Unit,
    onDismissCancelDialog: () -> Unit,
    onUpdateEditedBook: (Book) -> Unit,
    onCategoryChange: (Category) -> Unit
) {
    when (bottomSheet) {
        BookInfoScreen.DETAILS_BOTTOM_SHEET -> {
            BookInfoDetailsBottomSheet(
                book = book,
                isEditingMetadata = isEditingMetadata,
                editedBook = editedBook,
                showConfirmSaveDialog = showConfirmSaveDialog,
                showConfirmCancelDialog = showConfirmCancelDialog,
                showPathDialog = showPathDialog,
                showTitleDialog = showTitleDialog,
                showAuthorDialog = showAuthorDialog,
                showDescriptionDialog = showDescriptionDialog,
                showTagsDialog = showTagsDialog,
                showSeriesDialog = showSeriesDialog,
                showLanguagesDialog = showLanguagesDialog,
                resetTitle = resetTitle,
                resetAuthor = resetAuthor,
                resetDescription = resetDescription,
                clearProgressHistory = clearProgressHistory,
                refreshMetadataFromOpds = refreshMetadataFromOpds,
                dismissBottomSheet = dismissBottomSheet,
                onEnterEditMode = onEnterEditMode,
                onConfirmEditMetadata = onConfirmEditMetadata,
                onCancelEditMetadata = onCancelEditMetadata,
                onSilentCancelEditMetadata = onSilentCancelEditMetadata,
                onConfirmSaveChanges = onConfirmSaveChanges,
                onDismissSaveDialog = onDismissSaveDialog,
                onDismissCancelDialog = onDismissCancelDialog,
                onUpdateEditedBook = onUpdateEditedBook,
                onCategoryChange = onCategoryChange
            )
        }

        BookInfoScreen.EDIT_BOTTOM_SHEET -> {
            BookInfoEditBottomSheet(
                book = book,
                showTitleDialog = showTitleDialog,
                showAuthorDialog = showAuthorDialog,
                showDescriptionDialog = showDescriptionDialog,
                showPathDialog = showPathDialog,
                resetTitle = resetTitle,
                resetAuthor = resetAuthor,
                resetDescription = resetDescription,
                dismissBottomSheet = dismissBottomSheet
            )
        }
    }
}