/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.theme.color

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Kanagawa theme
 * https://github.com/rebelot/kanagawa.nvim
 */
@Composable
fun kanagawaTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF7E9CD8),
        onPrimary = Color(0xFF1F1F28),
        primaryContainer = Color(0xFF7E9CD8),
        onPrimaryContainer = Color(0xFF1F1F28),
        secondary = Color(0xFF98BB6C),
        onSecondary = Color(0xFF1F1F28),
        secondaryContainer = Color(0xFF49473C),
        onSecondaryContainer = Color(0xFF98BB6C),
        tertiary = Color(0xFF7E9CD8),
        onTertiary = Color(0xFF1F1F28),
        tertiaryContainer = Color(0xFF252535),
        onTertiaryContainer = Color(0xFFDCD7BA),
        error = Color(0xFFC34043),
        onError = Color(0xFF1F1F28),
        errorContainer = Color(0xFFC34043),
        onErrorContainer = Color(0xFF1F1F28),
        background = Color(0xFF1F1F28),
        onBackground = Color(0xFFDCD7BA),
        surface = Color(0xFF1F1F28),
        onSurface = Color(0xFFDCD7BA),
        surfaceVariant = Color(0xFF252535),
        onSurfaceVariant = Color(0xFFDCD7BA),
        outline = Color(0xFF7E9CD8),
        outlineVariant = Color(0xFF625E5A),
        scrim = Color(0xFF1F1F28),
        inverseSurface = Color(0xFFDCD7BA),
        inverseOnSurface = Color(0xFF1F1F28),
        inversePrimary = Color(0xFF2D4F67),
        surfaceDim = Color(0xFF1F1F28),
        surfaceBright = Color(0xFF49473C),
        surfaceContainerLowest = Color(0xFF1F1F28),
        surfaceContainerLow = Color(0xFF252535),
        surfaceContainer = Color(0xFF252535),
        surfaceContainerHigh = Color(0xFF252535),
        surfaceContainerHighest = Color(0xFF49473C),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF2D4F67),
        onPrimary = Color(0xFFDCB16C),
        primaryContainer = Color(0xFF2D4F67),
        onPrimaryContainer = Color(0xFFDCB16C),
        secondary = Color(0xFF98BB6C),
        onSecondary = Color(0xFFDCB16C),
        secondaryContainer = Color(0xFFE8D5B7),
        onSecondaryContainer = Color(0xFF98BB6C),
        tertiary = Color(0xFF2D4F67),
        onTertiary = Color(0xFFDCB16C),
        tertiaryContainer = Color(0xFFF4E4C1),
        onTertiaryContainer = Color(0xFF625E5A),
        error = Color(0xFFC34043),
        onError = Color(0xFFDCB16C),
        errorContainer = Color(0xFFC34043),
        onErrorContainer = Color(0xFFDCB16C),
        background = Color(0xFFF4E4C1),
        onBackground = Color(0xFF1F1F28),
        surface = Color(0xFFF4E4C1),
        onSurface = Color(0xFF1F1F28),
        surfaceVariant = Color(0xFFE8D5B7),
        onSurfaceVariant = Color(0xFF1F1F28),
        outline = Color(0xFF2D4F67),
        outlineVariant = Color(0xFF9C8961),
        scrim = Color(0xFFF4E4C1),
        inverseSurface = Color(0xFF1F1F28),
        inverseOnSurface = Color(0xFFDCD7BA),
        inversePrimary = Color(0xFF7E9CD8),
        surfaceDim = Color(0xFFF4E4C1),
        surfaceBright = Color(0xFFE8D5B7),
        surfaceContainerLowest = Color(0xFFF4E4C1),
        surfaceContainerLow = Color(0xFFE8D5B7),
        surfaceContainer = Color(0xFFE8D5B7),
        surfaceContainerHigh = Color(0xFFE8D5B7),
        surfaceContainerHighest = Color(0xFFE8D5B7),
    )
}