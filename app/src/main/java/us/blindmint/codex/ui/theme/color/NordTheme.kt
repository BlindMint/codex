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
 * Nord theme - Ported from Mihon
 * https://www.nordtheme.com/
 */
@Composable
fun nordTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF88C0D0),
        onPrimary = Color(0xFF2E3440),
        primaryContainer = Color(0xFF88C0D0),
        onPrimaryContainer = Color(0xFF2E3440),
        secondary = Color(0xFF88C0D0),
        onSecondary = Color(0xFF2E3440),
        secondaryContainer = Color(0xFF506275),
        onSecondaryContainer = Color(0xFF88C0D0),
        tertiary = Color(0xFF5E81AC),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF5E81AC),
        onTertiaryContainer = Color(0xFF000000),
        error = Color(0xFFBF616A),
        onError = Color(0xFF2E3440),
        errorContainer = Color(0xFFBF616A),
        onErrorContainer = Color(0xFF000000),
        background = Color(0xFF2E3440),
        onBackground = Color(0xFFECEFF4),
        surface = Color(0xFF2E3440),
        onSurface = Color(0xFFECEFF4),
        surfaceVariant = Color(0xFF414C5C),
        onSurfaceVariant = Color(0xFFECEFF4),
        outline = Color(0xFF6d717b),
        outlineVariant = Color(0xFF90939a),
        inverseSurface = Color(0xFFD8DEE9),
        inverseOnSurface = Color(0xFF2E3440),
        inversePrimary = Color(0xFF88C0D0),
        surfaceDim = Color(0xFF2E3440),
        surfaceBright = Color(0xFF4C566A),
        surfaceContainerLowest = Color(0xFF373F4D),
        surfaceContainerLow = Color(0xFF3E4756),
        surfaceContainer = Color(0xFF414C5C),
        surfaceContainerHigh = Color(0xFF4E5766),
        surfaceContainerHighest = Color(0xFF505968),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF5E81AC),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF5E81AC),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFF81A1C1),
        onSecondary = Color(0xFF2E3440),
        secondaryContainer = Color(0xFF91B4D7),
        onSecondaryContainer = Color(0xFF2E3440),
        tertiary = Color(0xFF88C0D0),
        onTertiary = Color(0xFF2E3440),
        tertiaryContainer = Color(0xFF88C0D0),
        onTertiaryContainer = Color(0xFF2E3440),
        error = Color(0xFFBF616A),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFECEFF4),
        onBackground = Color(0xFF2E3440),
        surface = Color(0xFFE5E9F0),
        onSurface = Color(0xFF2E3440),
        surfaceVariant = Color(0xFFDAE0EA),
        onSurfaceVariant = Color(0xFF2E3440),
        outline = Color(0xFF6d717b),
        inverseSurface = Color(0xFF3B4252),
        inverseOnSurface = Color(0xFFD8DEE9),
        inversePrimary = Color(0xFF8CA8CD),
        surfaceDim = Color(0xFFD1D5DB),
        surfaceBright = Color(0xFFECEFF4),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFE8ECF0),
        surfaceContainer = Color(0xFFECEFF4),
        surfaceContainerHigh = Color(0xFFF0F4F8),
        surfaceContainerHighest = Color(0xFFF0F4F8),
    )
}