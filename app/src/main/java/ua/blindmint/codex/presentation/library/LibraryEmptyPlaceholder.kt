/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.ui.UIText
import ua.blindmint.codex.presentation.core.components.placeholder.EmptyPlaceholder
import ua.blindmint.codex.ui.theme.Transitions

@Composable
fun BoxScope.LibraryEmptyPlaceholder(
    isLoading: Boolean,
    isRefreshing: Boolean,
    isBooksEmpty: Boolean,
    emptyIcon: ImageVector,
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
            icon = rememberVectorPainter(emptyIcon),
            modifier = Modifier.align(Alignment.Center),
            actionTitle = stringResource(id = R.string.add_book)
        ) {
            navigateToBrowse()
        }
    }
}