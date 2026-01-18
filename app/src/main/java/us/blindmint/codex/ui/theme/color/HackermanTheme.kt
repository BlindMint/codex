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
 * Hackerman theme
 */
@Composable
fun hackermanTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF50F872),
        onPrimary = Color(0xFF0B0C16),
        primaryContainer = Color(0xFF50F872),
        onPrimaryContainer = Color(0xFF0B0C16),
        secondary = Color(0xFF7CF8F7),
        onSecondary = Color(0xFF0B0C16),
        secondaryContainer = Color(0xFF1A2E4D),
        onSecondaryContainer = Color(0xFF7CF8F7),
        tertiary = Color(0xFF50F872),
        onTertiary = Color(0xFF0B0C16),
        tertiaryContainer = Color(0xFF0F1419),
        onTertiaryContainer = Color(0xFFDDF7FF),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF0B0C16),
        errorContainer = Color(0xFFFF6B6B),
        onErrorContainer = Color(0xFF0B0C16),
        background = Color(0xFF0B0C16),
        onBackground = Color(0xFFDDF7FF),
        surface = Color(0xFF0B0C16),
        onSurface = Color(0xFFDDF7FF),
        surfaceVariant = Color(0xFF0F1419),
        onSurfaceVariant = Color(0xFFDDF7FF),
        outline = Color(0xFF50F872),
        outlineVariant = Color(0xFF334155),
        scrim = Color(0xFF0B0C16),
        inverseSurface = Color(0xFFDDF7FF),
        inverseOnSurface = Color(0xFF0B0C16),
        inversePrimary = Color(0xFF00D4AA),
        surfaceDim = Color(0xFF0B0C16),
        surfaceBright = Color(0xFF1A2E4D),
        surfaceContainerLowest = Color(0xFF0B0C16),
        surfaceContainerLow = Color(0xFF0F1419),
        surfaceContainer = Color(0xFF0F1419),
        surfaceContainerHigh = Color(0xFF0F1419),
        surfaceContainerHighest = Color(0xFF1A2E4D),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF00D4AA),
        onPrimary = Color(0xFF0B0C16),
        primaryContainer = Color(0xFF00D4AA),
        onPrimaryContainer = Color(0xFF0B0C16),
        secondary = Color(0xFF7CF8F7),
        onSecondary = Color(0xFF0B0C16),
        secondaryContainer = Color(0xFFB8E6F5),
        onSecondaryContainer = Color(0xFF7CF8F7),
        tertiary = Color(0xFF00D4AA),
        onTertiary = Color(0xFF0B0C16),
        tertiaryContainer = Color(0xFFE6F9F5),
        onTertiaryContainer = Color(0xFF334155),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF0B0C16),
        errorContainer = Color(0xFFFF6B6B),
        onErrorContainer = Color(0xFF0B0C16),
        background = Color(0xFFE6F9F5),
        onBackground = Color(0xFF0B0C16),
        surface = Color(0xFFE6F9F5),
        onSurface = Color(0xFF0B0C16),
        surfaceVariant = Color(0xFFB8E6F5),
        onSurfaceVariant = Color(0xFF0B0C16),
        outline = Color(0xFF00D4AA),
        outlineVariant = Color(0xFF5BA3D0),
        scrim = Color(0xFFE6F9F5),
        inverseSurface = Color(0xFF0B0C16),
        inverseOnSurface = Color(0xFFDDF7FF),
        inversePrimary = Color(0xFF50F872),
        surfaceDim = Color(0xFFE6F9F5),
        surfaceBright = Color(0xFFB8E6F5),
        surfaceContainerLowest = Color(0xFFE6F9F5),
        surfaceContainerLow = Color(0xFFB8E6F5),
        surfaceContainer = Color(0xFFB8E6F5),
        surfaceContainerHigh = Color(0xFFB8E6F5),
        surfaceContainerHighest = Color(0xFFB8E6F5),
    )
}