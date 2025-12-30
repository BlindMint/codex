/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.dialog.DialogWithTextField
import us.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoPathDialog(
    book: Book,
    actionPathDialog: (BookInfoEvent.OnActionPathDialog) -> Unit,
    dismissDialog: (BookInfoEvent.OnDismissDialog) -> Unit
) {
    val context = LocalContext.current
    DialogWithTextField(
        initialValue = book.filePath.trim(),
        lengthLimit = 10000,
        onDismiss = {
            dismissDialog(BookInfoEvent.OnDismissDialog)
        },
        onAction = {
            if (it.isBlank()) return@DialogWithTextField
            actionPathDialog(
                BookInfoEvent.OnActionPathDialog(
                    path = it.trim().replace("\n", ""),
                    context = context
                )
            )
        }
    )
}