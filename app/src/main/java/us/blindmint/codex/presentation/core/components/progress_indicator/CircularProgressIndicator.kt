/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.components.progress_indicator

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular progress indicator with a subtle skull accent.
 * This is the app-wide indeterminate progress indicator.
 */
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    @Suppress("UNUSED_PARAMETER") gapSize: Dp = 2.dp
) {
    SkullProgressIndicator(
        modifier = modifier,
        progressColor = color,
        strokeWidth = strokeWidth,
        size = 48.dp
    )
}