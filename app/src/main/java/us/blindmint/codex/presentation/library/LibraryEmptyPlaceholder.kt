/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.presentation.core.components.placeholder.EmptyPlaceholder
import us.blindmint.codex.ui.theme.Transitions

@Composable
fun BoxScope.LibraryEmptyPlaceholder(
    isLoading: Boolean,
    isRefreshing: Boolean,
    isBooksEmpty: Boolean,
    emptyIcon: Painter,
    emptyMessage: UIText,
    navigateToBrowse: () -> Unit
) {
    AnimatedVisibility(
        visible = !isLoading
                && !isRefreshing
                && isBooksEmpty,
        modifier = Modifier.align(Alignment.Center),
        enter = Transitions.DefaultTransitionIn,
        exit = fadeOut(tween(0))
    ) {
        EmptyPlaceholder(
            message = emptyMessage.asString(),
            icon = emptyIcon,
            modifier = Modifier.align(Alignment.Center),
            actionTitle = stringResource(id = R.string.add_book)
        ) {
            navigateToBrowse()
        }
    }
}