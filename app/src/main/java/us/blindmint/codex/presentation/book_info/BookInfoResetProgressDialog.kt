/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.dialog.Dialog
import us.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoResetProgressDialog(
    actionResetProgressDialog: (BookInfoEvent.OnActionResetProgressDialog) -> Unit,
    dismissDialog: (BookInfoEvent.OnDismissDialog) -> Unit
) {
    val context = LocalContext.current

    Dialog(
        title = stringResource(id = R.string.reset_reading_progress),
        icon = Icons.Outlined.RestartAlt,
        description = stringResource(
            id = R.string.reset_reading_progress_description
        ),
        onDismiss = { dismissDialog(BookInfoEvent.OnDismissDialog) },
        withContent = false,
        actionEnabled = true,
        onAction = {
            actionResetProgressDialog(
                BookInfoEvent.OnActionResetProgressDialog(
                    context = context
                )
            )
        }
    )
}