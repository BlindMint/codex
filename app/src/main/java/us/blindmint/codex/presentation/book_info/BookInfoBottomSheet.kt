/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.runtime.Composable
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.util.BottomSheet
import us.blindmint.codex.ui.book_info.BookInfoEvent
import us.blindmint.codex.ui.book_info.BookInfoScreen

@Composable
fun BookInfoBottomSheet(
    bottomSheet: BottomSheet?,
    book: Book,
    canResetCover: Boolean,
    showPathDialog: (BookInfoEvent.OnShowPathDialog) -> Unit,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    showEditBottomSheet: (BookInfoEvent.OnShowEditBottomSheet) -> Unit,
    resetTitle: (BookInfoEvent.OnResetTitle) -> Unit,
    resetAuthor: (BookInfoEvent.OnResetAuthor) -> Unit,
    resetDescription: (BookInfoEvent.OnResetDescription) -> Unit,
    clearProgressHistory: (BookInfoEvent.OnClearProgressHistory) -> Unit,
    refreshMetadataFromOpds: (BookInfoEvent.OnRefreshMetadataFromOpds) -> Unit,
    changeCover: (BookInfoEvent.OnChangeCover) -> Unit,
    resetCover: (BookInfoEvent.OnResetCover) -> Unit,
    deleteCover: (BookInfoEvent.OnDeleteCover) -> Unit,
    checkCoverReset: (BookInfoEvent.OnCheckCoverReset) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit
) {
    when (bottomSheet) {
        BookInfoScreen.CHANGE_COVER_BOTTOM_SHEET -> {
            BookInfoChangeCoverBottomSheet(
                book = book,
                canResetCover = canResetCover,
                changeCover = changeCover,
                resetCover = resetCover,
                deleteCover = deleteCover,
                checkCoverReset = checkCoverReset,
                dismissBottomSheet = dismissBottomSheet
            )
        }

        BookInfoScreen.DETAILS_BOTTOM_SHEET -> {
            BookInfoDetailsBottomSheet(
                book = book,
                showPathDialog = showPathDialog,
                showTitleDialog = showTitleDialog,
                showAuthorDialog = showAuthorDialog,
                showDescriptionDialog = showDescriptionDialog,
                showEditBottomSheet = showEditBottomSheet,
                resetTitle = resetTitle,
                resetAuthor = resetAuthor,
                resetDescription = resetDescription,
                clearProgressHistory = clearProgressHistory,
                refreshMetadataFromOpds = refreshMetadataFromOpds,
                dismissBottomSheet = dismissBottomSheet
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