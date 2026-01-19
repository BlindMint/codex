/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.runtime.Composable
import us.blindmint.codex.domain.library.book.SelectableBook
import us.blindmint.codex.domain.library.LibraryTabWithBooks
import us.blindmint.codex.domain.util.Dialog
import us.blindmint.codex.ui.library.LibraryEvent
import us.blindmint.codex.ui.library.LibraryScreen

@Composable
fun LibraryDialog(
    dialog: Dialog?,
    books: List<SelectableBook>,
    categories: List<LibraryTabWithBooks>,
    selectedItemsCount: Int,
    actionMoveDialog: (LibraryEvent.OnActionMoveDialog) -> Unit,
    actionDeleteDialog: (LibraryEvent.OnActionDeleteDialog) -> Unit,
    actionClearProgressHistoryDialog: (LibraryEvent.OnActionClearProgressHistoryDialog) -> Unit,
    actionBulkEditTags: (LibraryEvent.OnActionBulkEditTags) -> Unit,
    actionBulkEditSeries: (LibraryEvent.OnActionBulkEditSeries) -> Unit,
    actionBulkEditLanguages: (LibraryEvent.OnActionBulkEditLanguages) -> Unit,
    dismissDialog: (LibraryEvent.OnDismissDialog) -> Unit
) {
    when (dialog) {
        LibraryScreen.DELETE_DIALOG -> {
            LibraryDeleteDialog(
                selectedItemsCount = selectedItemsCount,
                actionDeleteDialog = actionDeleteDialog,
                dismissDialog = dismissDialog
            )
        }

        "clear_progress_history_dialog" -> {
            LibraryDeleteDialog(
                selectedItemsCount = selectedItemsCount,
                actionDeleteDialog = { event ->
                    actionClearProgressHistoryDialog(
                        LibraryEvent.OnActionClearProgressHistoryDialog(event.context)
                    )
                },
                dismissDialog = dismissDialog,
                title = "Clear Progress History?",
                description = "This will remove the progress history for all selected books. This action cannot be undone.",
                confirmText = "Clear History"
            )
        }

        LibraryScreen.BULK_EDIT_DIALOG -> {
            val selectedBooks = books.filter { it.selected }.map { it.data }
            if (selectedBooks.isNotEmpty()) {
                BulkEditBottomSheet(
                    selectedBooks = selectedBooks,
                    actionBulkEditTags = actionBulkEditTags,
                    actionBulkEditSeries = actionBulkEditSeries,
                    actionBulkEditLanguages = actionBulkEditLanguages,
                    dismissDialog = dismissDialog
                )
            }
        }
    }
}