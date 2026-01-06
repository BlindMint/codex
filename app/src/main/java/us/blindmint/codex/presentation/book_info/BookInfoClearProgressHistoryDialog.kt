/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.dialog.Dialog
import us.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoClearProgressHistoryDialog(
    actionClearProgressHistoryDialog: (BookInfoEvent.OnActionClearProgressHistoryDialog) -> Unit,
    dismissDialog: (BookInfoEvent.OnDismissDialog) -> Unit
) {
    val context = LocalContext.current

    Dialog(
        title = stringResource(id = R.string.clear_progress_history),
        icon = Icons.Default.Refresh,
        description = stringResource(
            id = R.string.clear_progress_history_description
        ),
        onDismiss = { dismissDialog(BookInfoEvent.OnDismissDialog) },
        withContent = false,
        actionEnabled = true,
        onAction = {
            actionClearProgressHistoryDialog(
                BookInfoEvent.OnActionClearProgressHistoryDialog(
                    context = context
                )
            )
        }
    )
}