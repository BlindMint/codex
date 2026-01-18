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
 * Rose Pine theme
 * https://rosepinetheme.com/
 */
@Composable
fun rosePineTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF9CCFD8),
        onPrimary = Color(0xFF191724),
        primaryContainer = Color(0xFF9CCFD8),
        onPrimaryContainer = Color(0xFF191724),
        secondary = Color(0xFFEBBCBA),
        onSecondary = Color(0xFF191724),
        secondaryContainer = Color(0xFF2A273F),
        onSecondaryContainer = Color(0xFFEBBCBA),
        tertiary = Color(0xFF9CCFD8),
        onTertiary = Color(0xFF191724),
        tertiaryContainer = Color(0xFF1F1D2E),
        onTertiaryContainer = Color(0xFFE0DEF4),
        error = Color(0xFFB4637A),
        onError = Color(0xFF191724),
        errorContainer = Color(0xFFB4637A),
        onErrorContainer = Color(0xFF191724),
        background = Color(0xFF191724),
        onBackground = Color(0xFFE0DEF4),
        surface = Color(0xFF191724),
        onSurface = Color(0xFFE0DEF4),
        surfaceVariant = Color(0xFF1F1D2E),
        onSurfaceVariant = Color(0xFFE0DEF4),
        outline = Color(0xFF9CCFD8),
        outlineVariant = Color(0xFF6E6A86),
        scrim = Color(0xFF191724),
        inverseSurface = Color(0xFFE0DEF4),
        inverseOnSurface = Color(0xFF191724),
        inversePrimary = Color(0xFF31748F),
        surfaceDim = Color(0xFF191724),
        surfaceBright = Color(0xFF2A273F),
        surfaceContainerLowest = Color(0xFF191724),
        surfaceContainerLow = Color(0xFF1F1D2E),
        surfaceContainer = Color(0xFF1F1D2E),
        surfaceContainerHigh = Color(0xFF1F1D2E),
        surfaceContainerHighest = Color(0xFF2A273F),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF31748F),
        onPrimary = Color(0xFFFAF4ED),
        primaryContainer = Color(0xFF31748F),
        onPrimaryContainer = Color(0xFFFAF4ED),
        secondary = Color(0xFF9CCFD8),
        onSecondary = Color(0xFFFAF4ED),
        secondaryContainer = Color(0xFFE6E9EF),
        onSecondaryContainer = Color(0xFF9CCFD8),
        tertiary = Color(0xFF31748F),
        onTertiary = Color(0xFFFAF4ED),
        tertiaryContainer = Color(0xFFF2E9E1),
        onTertiaryContainer = Color(0xFF575279),
        error = Color(0xFFB4637A),
        onError = Color(0xFFFAF4ED),
        errorContainer = Color(0xFFB4637A),
        onErrorContainer = Color(0xFFFAF4ED),
        background = Color(0xFFFAF4ED),
        onBackground = Color(0xFF575279),
        surface = Color(0xFFFAF4ED),
        onSurface = Color(0xFF575279),
        surfaceVariant = Color(0xFFF2E9E1),
        onSurfaceVariant = Color(0xFF575279),
        outline = Color(0xFF31748F),
        outlineVariant = Color(0xFFBCC3CE),
        scrim = Color(0xFFFAF4ED),
        inverseSurface = Color(0xFF191724),
        inverseOnSurface = Color(0xFFE0DEF4),
        inversePrimary = Color(0xFF9CCFD8),
        surfaceDim = Color(0xFFFAF4ED),
        surfaceBright = Color(0xFFE6E9EF),
        surfaceContainerLowest = Color(0xFFFAF4ED),
        surfaceContainerLow = Color(0xFFF2E9E1),
        surfaceContainer = Color(0xFFF2E9E1),
        surfaceContainerHigh = Color(0xFFF2E9E1),
        surfaceContainerHighest = Color(0xFFE6E9EF),
    )
}