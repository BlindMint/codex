/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding

@Composable
fun SettingsSubcategoryTitle(
    modifier: Modifier = Modifier,
    title: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    padding: Dp = SettingsHorizontalPadding
) {
    StyledText(
        text = title,
        modifier = modifier.padding(horizontal = padding),
        style = MaterialTheme.typography.labelLarge.copy(color = color)
    )
}