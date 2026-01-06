/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        if (hasPersistedUriPermissions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 80.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.empty_browse),
                    contentDescription = stringResource(id = R.string.browse_empty_scanned),
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                StyledText(
                    text = stringResource(id = R.string.browse_empty_scanned),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        navigateToBrowseSettings()
                    }
                ) {
                    StyledText(
                        text = stringResource(id = R.string.set_up_scanning),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
                TextButton(
                    onClick = {
                        BrowseScreen.refreshListChannel.trySend(Unit)
                    }
                ) {
                    StyledText(
                        text = stringResource(id = R.string.rescan),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        } else {
            EmptyPlaceholder(
                message = stringResource(id = R.string.browse_empty),
                icon = painterResource(id = R.drawable.empty_browse),
                actionTitle = stringResource(id = R.string.set_up_scanning),
                action = {
                    navigateToBrowseSettings()
                }
            )
        }
    }
}