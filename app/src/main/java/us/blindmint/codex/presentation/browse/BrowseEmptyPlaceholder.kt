/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.components.placeholder.EmptyPlaceholder
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.ui.theme.Transitions

@Composable
fun BoxScope.BrowseEmptyPlaceholder(
    filesEmpty: Boolean,
    dialogHidden: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean,
    pinnedPaths: List<String>,
    navigateToBrowseSettings: () -> Unit
) {
    val context = LocalContext.current
    val hasPersistedUriPermissions = remember {
        context.contentResolver?.persistedUriPermissions?.isNotEmpty() == true
    }
    AnimatedVisibility(
        visible = !isLoading
                && dialogHidden
                && filesEmpty
                && !isRefreshing,
        modifier = Modifier.align(Alignment.Center),
        enter = Transitions.DefaultTransitionIn,
        exit = Transitions.NoExitAnimation
    ) {
        EmptyPlaceholder(
            message = stringResource(
                id = if (hasPersistedUriPermissions) R.string.browse_empty_scanned
                else R.string.browse_empty
            ),
            icon = painterResource(id = R.drawable.empty_browse),
            actionTitle = stringResource(id = R.string.set_up_scanning),
            action = {
                navigateToBrowseSettings()
            }
        )
    }
}