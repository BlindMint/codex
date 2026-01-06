/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import us.blindmint.codex.domain.library.book.SelectableNullableBook
import us.blindmint.codex.domain.util.Dialog
import us.blindmint.codex.ui.browse.BrowseEvent
import us.blindmint.codex.ui.browse.BrowseScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseDialog(
    dialog: Dialog?,
    loadingAddDialog: Boolean,
    selectedBooksAddDialog: List<SelectableNullableBook>,
    dismissAddDialog: (BrowseEvent.OnDismissAddDialog) -> Unit,
    actionAddDialog: (BrowseEvent.OnActionAddDialog) -> Unit,
    selectAddDialog: (BrowseEvent.OnSelectAddDialog) -> Unit,
    navigateToLibrary: () -> Unit
) {
    when (dialog) {
        BrowseScreen.ADD_DIALOG -> {
            BrowseAddDialog(
                loadingAddDialog = loadingAddDialog,
                selectedBooksAddDialog = selectedBooksAddDialog,
                dismissAddDialog = dismissAddDialog,
                actionAddDialog = actionAddDialog,
                selectAddDialog = selectAddDialog,
                navigateToLibrary = navigateToLibrary
            )
        }
    }
}