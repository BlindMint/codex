/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.util.Dialog
import us.blindmint.codex.ui.book_info.BookInfoEvent
import us.blindmint.codex.ui.book_info.BookInfoScreen

@Composable
fun BookInfoDialog(
    dialog: Dialog?,
    book: Book,
    editedBook: Book?,
    onUpdateEditedBook: (Book) -> Unit,
    actionTitleDialog: (BookInfoEvent.OnActionTitleDialog) -> Unit,
    actionAuthorDialog: (BookInfoEvent.OnActionAuthorDialog) -> Unit,
    actionDescriptionDialog: (BookInfoEvent.OnActionDescriptionDialog) -> Unit,
    actionPathDialog: (BookInfoEvent.OnActionPathDialog) -> Unit,
    actionDeleteDialog: (BookInfoEvent.OnActionDeleteDialog) -> Unit,
    actionResetProgressDialog: (BookInfoEvent.OnActionResetProgressDialog) -> Unit,
    dismissDialog: (BookInfoEvent.OnDismissDialog) -> Unit,
    navigateBack: () -> Unit,
    navigateToLibrary: () -> Unit
) {
    when (dialog) {
        BookInfoScreen.DELETE_DIALOG -> {
            BookInfoDeleteDialog(
                actionDeleteDialog = actionDeleteDialog,
                dismissDialog = dismissDialog,
                navigateBack = navigateBack
            )
        }

        BookInfoScreen.RESET_PROGRESS_DIALOG -> {
            BookInfoResetProgressDialog(
                actionResetProgressDialog = actionResetProgressDialog,
                dismissDialog = dismissDialog
            )
        }

        BookInfoScreen.TITLE_DIALOG -> {
            BookInfoTitleDialog(
                book = book,
                actionTitleDialog = actionTitleDialog,
                dismissDialog = dismissDialog
            )
        }

        BookInfoScreen.AUTHOR_DIALOG -> {
            BookInfoAuthorDialog(
                book = book,
                actionAuthorDialog = actionAuthorDialog,
                dismissDialog = dismissDialog
            )
        }

        BookInfoScreen.DESCRIPTION_DIALOG -> {
            BookInfoDescriptionDialog(
                book = book,
                actionDescriptionDialog = actionDescriptionDialog,
                dismissDialog = dismissDialog
            )
        }

        BookInfoScreen.PATH_DIALOG -> {
            BookInfoPathDialog(
                book = book,
                actionPathDialog = actionPathDialog,
                dismissDialog = dismissDialog
            )
        }

        BookInfoScreen.TAGS_DIALOG -> {
            if (editedBook != null) {
                MetadataItemEditor(
                    title = stringResource(id = R.string.tags),
                    items = editedBook.tags,
                    onItemsChanged = { newTags ->
                        onUpdateEditedBook(editedBook.copy(tags = newTags))
                        dismissDialog(BookInfoEvent.OnDismissDialog)
                    },
                    onDismiss = { dismissDialog(BookInfoEvent.OnDismissDialog) }
                )
            }
        }

        BookInfoScreen.SERIES_DIALOG -> {
            if (editedBook != null) {
                MetadataItemEditor(
                    title = stringResource(id = R.string.series),
                    items = editedBook.series,
                    onItemsChanged = { newSeries ->
                        onUpdateEditedBook(editedBook.copy(series = newSeries))
                        dismissDialog(BookInfoEvent.OnDismissDialog)
                    },
                    onDismiss = { dismissDialog(BookInfoEvent.OnDismissDialog) }
                )
            }
        }

        BookInfoScreen.LANGUAGES_DIALOG -> {
            if (editedBook != null) {
                MetadataItemEditor(
                    title = stringResource(id = R.string.languages),
                    items = editedBook.languages,
                    onItemsChanged = { newLanguages ->
                        onUpdateEditedBook(editedBook.copy(languages = newLanguages))
                        dismissDialog(BookInfoEvent.OnDismissDialog)
                    },
                    onDismiss = { dismissDialog(BookInfoEvent.OnDismissDialog) }
                )
            }
        }
    }
}