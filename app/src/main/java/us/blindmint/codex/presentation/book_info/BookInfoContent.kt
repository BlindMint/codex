/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
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
    canResetCover: Boolean,
    showChangeCoverBottomSheet: (BookInfoEvent.OnShowChangeCoverBottomSheet) -> Unit,
    showEditBottomSheet: (BookInfoEvent.OnShowEditBottomSheet) -> Unit,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    actionTitleDialog: (BookInfoEvent.OnActionTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    actionAuthorDialog: (BookInfoEvent.OnActionAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    actionDescriptionDialog: (BookInfoEvent.OnActionDescriptionDialog) -> Unit,
    showPathDialog: (BookInfoEvent.OnShowPathDialog) -> Unit,
    actionPathDialog: (BookInfoEvent.OnActionPathDialog) -> Unit,
    resetTitle: (BookInfoEvent.OnResetTitle) -> Unit,
    resetAuthor: (BookInfoEvent.OnResetAuthor) -> Unit,
    resetDescription: (BookInfoEvent.OnResetDescription) -> Unit,
    clearProgressHistory: (BookInfoEvent.OnClearProgressHistory) -> Unit,
    showDeleteDialog: (BookInfoEvent.OnShowDeleteDialog) -> Unit,
    actionDeleteDialog: (BookInfoEvent.OnActionDeleteDialog) -> Unit,
    changeCover: (BookInfoEvent.OnChangeCover) -> Unit,
    resetCover: (BookInfoEvent.OnResetCover) -> Unit,
    deleteCover: (BookInfoEvent.OnDeleteCover) -> Unit,
    checkCoverReset: (BookInfoEvent.OnCheckCoverReset) -> Unit,
    dismissDialog: (BookInfoEvent.OnDismissDialog) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit,
    toggleFavorite: () -> Unit,
    navigateToReader: () -> Unit,
    navigateToSpeedReading: (() -> Unit)? = null,
    navigateToLibrary: () -> Unit,
    navigateBack: () -> Unit
) {
    BookInfoDialog(
        dialog = dialog,
        book = book,
        actionTitleDialog = actionTitleDialog,
        actionAuthorDialog = actionAuthorDialog,
        actionDescriptionDialog = actionDescriptionDialog,
        actionPathDialog = actionPathDialog,
        actionDeleteDialog = actionDeleteDialog,
        dismissDialog = dismissDialog,
        navigateToLibrary = navigateToLibrary,
        navigateBack = navigateBack
    )

    BookInfoBottomSheet(
        bottomSheet = bottomSheet,
        book = book,
        showPathDialog = showPathDialog,
        showTitleDialog = showTitleDialog,
        showAuthorDialog = showAuthorDialog,
        showDescriptionDialog = showDescriptionDialog,
        resetTitle = resetTitle,
        resetAuthor = resetAuthor,
        resetDescription = resetDescription,
        clearProgressHistory = clearProgressHistory,
        canResetCover = canResetCover,
        changeCover = changeCover,
        resetCover = resetCover,
        deleteCover = deleteCover,
        checkCoverReset = checkCoverReset,
        dismissBottomSheet = dismissBottomSheet
    )

    BookInfoScaffold(
        book = book,
        listState = listState,
        showChangeCoverBottomSheet = showChangeCoverBottomSheet,
        showEditBottomSheet = showEditBottomSheet,
        showDeleteDialog = showDeleteDialog,
        clearProgressHistory = clearProgressHistory,
        toggleFavorite = toggleFavorite,
        navigateToReader = navigateToReader,
        navigateToSpeedReading = navigateToSpeedReading,
        navigateBack = navigateBack
    )

    BookInfoBackHandler(
        navigateBack = navigateBack
    )
}