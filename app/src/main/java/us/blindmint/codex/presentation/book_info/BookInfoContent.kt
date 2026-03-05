/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.util.BottomSheet
import us.blindmint.codex.domain.util.Dialog
import us.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoContent(
    book: Book,
    bottomSheet: BottomSheet?,
    dialog: Dialog?,
    listState: LazyListState,
    editedBook: Book?,
    showDetailsBottomSheet: (BookInfoEvent.OnShowDetailsBottomSheet) -> Unit,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    actionTitleDialog: (BookInfoEvent.OnActionTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    actionAuthorDialog: (BookInfoEvent.OnActionAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    actionDescriptionDialog: (BookInfoEvent.OnActionDescriptionDialog) -> Unit,
    showPathDialog: (BookInfoEvent.OnShowPathDialog) -> Unit,
    actionPathDialog: (BookInfoEvent.OnActionPathDialog) -> Unit,
    showTagsDialog: (BookInfoEvent.OnShowTagsDialog) -> Unit,
    showSeriesDialog: (BookInfoEvent.OnShowSeriesDialog) -> Unit,
    showLanguagesDialog: (BookInfoEvent.OnShowLanguagesDialog) -> Unit,
    refreshMetadataFromOpds: (BookInfoEvent.OnRefreshMetadataFromOpds) -> Unit,
    showDeleteDialog: (BookInfoEvent.OnShowDeleteDialog) -> Unit,
    showResetProgressDialog: (BookInfoEvent.OnShowResetProgressDialog) -> Unit,
    actionDeleteDialog: (BookInfoEvent.OnActionDeleteDialog) -> Unit,
    actionResetReadingProgress: (BookInfoEvent.OnActionResetReadingProgress) -> Unit,
    dismissDialog: (BookInfoEvent.OnDismissDialog) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit,
    onConfirmSaveChanges: (android.content.Context) -> Unit,
    onCancelChanges: () -> Unit,
    onUpdateEditedBook: (Book) -> Unit,
    onCategoryChange: (us.blindmint.codex.domain.library.category.Category) -> Unit,
    toggleFavorite: () -> Unit,
    navigateToReader: () -> Unit,
    navigateToSpeedReading: (() -> Unit)? = null,
    navigateToLibrary: () -> Unit,
    navigateBack: () -> Unit
) {
    BookInfoDialog(
        dialog = dialog,
        book = book,
        editedBook = editedBook,
        onUpdateEditedBook = onUpdateEditedBook,
        actionTitleDialog = actionTitleDialog,
        actionAuthorDialog = actionAuthorDialog,
        actionDescriptionDialog = actionDescriptionDialog,
        actionPathDialog = actionPathDialog,
        actionDeleteDialog = actionDeleteDialog,
        actionResetReadingProgress = actionResetReadingProgress,
        dismissDialog = dismissDialog,
        navigateToLibrary = navigateToLibrary,
        navigateBack = navigateBack
    )

    Box(Modifier.fillMaxSize()) {
        BookInfoScaffold(
            book = book,
            listState = listState,
            showDetailsBottomSheet = showDetailsBottomSheet,
            showDeleteDialog = showDeleteDialog,
            showResetProgressDialog = showResetProgressDialog,
            toggleFavorite = toggleFavorite,
            navigateToReader = navigateToReader,
            navigateToSpeedReading = navigateToSpeedReading,
            onCategoryChange = onCategoryChange,
            navigateBack = navigateBack
        )

        // Rendered after BookInfoScaffold so it draws on top when visible
        BookInfoBottomSheet(
            bottomSheet = bottomSheet,
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

    BookInfoBackHandler(
        navigateBack = navigateBack
    )
}
