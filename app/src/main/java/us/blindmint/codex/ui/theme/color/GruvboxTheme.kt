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
 * Gruvbox theme
 * https://github.com/morhetz/gruvbox
 */
@Composable
fun gruvboxTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFFD4BE98),
        onPrimary = Color(0xFF282828),
        primaryContainer = Color(0xFFD4BE98),
        onPrimaryContainer = Color(0xFF282828),
        secondary = Color(0xFFA9B665),
        onSecondary = Color(0xFF282828),
        secondaryContainer = Color(0xFF504E3A),
        onSecondaryContainer = Color(0xFFA9B665),
        tertiary = Color(0xFFD4BE98),
        onTertiary = Color(0xFF282828),
        tertiaryContainer = Color(0xFF3C3836),
        onTertiaryContainer = Color(0xFFD4BE98),
        error = Color(0xFFEA6962),
        onError = Color(0xFF282828),
        errorContainer = Color(0xFFEA6962),
        onErrorContainer = Color(0xFF282828),
        background = Color(0xFF282828),
        onBackground = Color(0xFFD4BE98),
        surface = Color(0xFF282828),
        onSurface = Color(0xFFD4BE98),
        surfaceVariant = Color(0xFF3C3836),
        onSurfaceVariant = Color(0xFFD4BE98),
        outline = Color(0xFFA9B665),
        outlineVariant = Color(0xFF665C54),
        scrim = Color(0xFF282828),
        inverseSurface = Color(0xFFD4BE98),
        inverseOnSurface = Color(0xFF282828),
        inversePrimary = Color(0xFF7C6F64),
        surfaceDim = Color(0xFF282828),
        surfaceBright = Color(0xFF504945),
        surfaceContainerLowest = Color(0xFF282828),
        surfaceContainerLow = Color(0xFF3C3836),
        surfaceContainer = Color(0xFF3C3836),
        surfaceContainerHigh = Color(0xFF3C3836),
        surfaceContainerHighest = Color(0xFF504945),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF7C6F64),
        onPrimary = Color(0xFFFBF1C7),
        primaryContainer = Color(0xFF7C6F64),
        onPrimaryContainer = Color(0xFFFBF1C7),
        secondary = Color(0xFF427B58),
        onSecondary = Color(0xFFFBF1C7),
        secondaryContainer = Color(0xFFD5C4A1),
        onSecondaryContainer = Color(0xFF427B58),
        tertiary = Color(0xFF7C6F64),
        onTertiary = Color(0xFFFBF1C7),
        tertiaryContainer = Color(0xFFEBDBB2),
        onTertiaryContainer = Color(0xFF3C3836),
        error = Color(0xFF9D0006),
        onError = Color(0xFFFBF1C7),
        errorContainer = Color(0xFF9D0006),
        onErrorContainer = Color(0xFFFBF1C7),
        background = Color(0xFFFBF1C7),
        onBackground = Color(0xFF3C3836),
        surface = Color(0xFFFBF1C7),
        onSurface = Color(0xFF3C3836),
        surfaceVariant = Color(0xFFEBDBB2),
        onSurfaceVariant = Color(0xFF3C3836),
        outline = Color(0xFF7C6F64),
        outlineVariant = Color(0xFFA89984),
        scrim = Color(0xFFFBF1C7),
        inverseSurface = Color(0xFF282828),
        inverseOnSurface = Color(0xFFD4BE98),
        inversePrimary = Color(0xFFD4BE98),
        surfaceDim = Color(0xFFFBF1C7),
        surfaceBright = Color(0xFFD5C4A1),
        surfaceContainerLowest = Color(0xFFFBF1C7),
        surfaceContainerLow = Color(0xFFEBDBB2),
        surfaceContainer = Color(0xFFEBDBB2),
        surfaceContainerHigh = Color(0xFFEBDBB2),
        surfaceContainerHighest = Color(0xFFD5C4A1),
    )
}