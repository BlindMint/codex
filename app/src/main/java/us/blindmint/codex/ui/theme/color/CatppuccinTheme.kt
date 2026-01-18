/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.theme.color

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color




/**
 * Catppuccin theme - Ported from Mihon
 * MIT License
 * Copyright (c) 2021 Catppuccin
 * https://catppuccin.com
 */
@Composable
fun catppuccinTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFFCBA6F7),
        onPrimary = Color(0xFF11111B),
        primaryContainer = Color(0xFFCBA6F7),
        onPrimaryContainer = Color(0xFF11111B),
        secondary = Color(0xFFCBA6F7),
        onSecondary = Color(0xFF11111B),
        secondaryContainer = Color(0xFF313244),
        onSecondaryContainer = Color(0xFFCBA6F7),
        tertiary = Color(0xFFCBA6F7),
        onTertiary = Color(0xFF11111B),
        tertiaryContainer = Color(0xFF1E1E2E),
        onTertiaryContainer = Color(0xFFCDD6F4),
        error = Color(0xFFF38BA8),
        onError = Color(0xFF11111B),
        errorContainer = Color(0xFFFF0558),
        onErrorContainer = Color(0xFFEF9FB4),
        background = Color(0xFF181825),
        onBackground = Color(0xFFCDD6F4),
        surface = Color(0xFF181825),
        onSurface = Color(0xFFCDD6F4),
        surfaceVariant = Color(0xFF1E1E2E),
        onSurfaceVariant = Color(0xFFCDD6F4),
        outline = Color(0xFFCBA6F7),
        outlineVariant = Color(0xFF585B70),
        scrim = Color(0xFF11111B),
        inverseSurface = Color(0xFFEFF1F5),
        inverseOnSurface = Color(0xFF4C4F69),
        inversePrimary = Color(0xFF8839EF),
        surfaceDim = Color(0xFF181825),
        surfaceBright = Color(0xFF313244),
        surfaceContainerLowest = Color(0xFF181825),
        surfaceContainerLow = Color(0xFF1E1E2E),
        surfaceContainer = Color(0xFF1E1E2E),
        surfaceContainerHigh = Color(0xFF1E1E2E),
        surfaceContainerHighest = Color(0xFF313244),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF8839EF),
        onPrimary = Color(0xFFDCE0E8),
        primaryContainer = Color(0xFF8839EF),
        onPrimaryContainer = Color(0xFFDCE0E8),
        secondary = Color(0xFF8839EF),
        onSecondary = Color(0xFFDCE0E8),
        secondaryContainer = Color(0xFFCDD0DA),
        onSecondaryContainer = Color(0xFF8839EF),
        tertiary = Color(0xFF8839EF),
        onTertiary = Color(0xFFDCE0E8),
        tertiaryContainer = Color(0xFFEFF1F5),
        onTertiaryContainer = Color(0xFF4C4F69),
        error = Color(0xFFD20F39),
        onError = Color(0xFFDCE0E8),
        errorContainer = Color(0xFF68001C),
        onErrorContainer = Color(0xFFD61C41),
        background = Color(0xFFE6E9EF),
        onBackground = Color(0xFF4C4F69),
        surface = Color(0xFFE6E9EF),
        onSurface = Color(0xFF4C4F69),
        surfaceVariant = Color(0xFFEFF1F5),
        onSurfaceVariant = Color(0xFF4C4F69),
        outline = Color(0xFF8839EF),
        outlineVariant = Color(0xFFACB0BE),
        scrim = Color(0xFFDCE0E8),
        inverseSurface = Color(0xFF1E1E2E),
        inverseOnSurface = Color(0xFFCDD6F4),
        inversePrimary = Color(0xFFCBA6F7),
        surfaceDim = Color(0xFFE6E9EF),
        surfaceBright = Color(0xFFCDD0DA),
        surfaceContainerLowest = Color(0xFFE6E9EF),
        surfaceContainerLow = Color(0xFFEFF1F5),
        surfaceContainer = Color(0xFFEFF1F5),
        surfaceContainerHigh = Color(0xFFEFF1F5),
        surfaceContainerHighest = Color(0xFFCDD0DA),
    )
}