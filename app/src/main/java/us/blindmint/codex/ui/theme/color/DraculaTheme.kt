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
 * Dracula theme
 * https://draculatheme.com/
 */
@Composable
fun draculaTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFFBD93F9),
        onPrimary = Color(0xFF282A36),
        primaryContainer = Color(0xFFBD93F9),
        onPrimaryContainer = Color(0xFF282A36),
        secondary = Color(0xFF50FA7B),
        onSecondary = Color(0xFF282A36),
        secondaryContainer = Color(0xFF44475A),
        onSecondaryContainer = Color(0xFF50FA7B),
        tertiary = Color(0xFFBD93F9),
        onTertiary = Color(0xFF282A36),
        tertiaryContainer = Color(0xFF1E1E2E),
        onTertiaryContainer = Color(0xFFF8F8F2),
        error = Color(0xFFFF5555),
        onError = Color(0xFF282A36),
        errorContainer = Color(0xFFFF5555),
        onErrorContainer = Color(0xFF282A36),
        background = Color(0xFF282A36),
        onBackground = Color(0xFFF8F8F2),
        surface = Color(0xFF282A36),
        onSurface = Color(0xFFF8F8F2),
        surfaceVariant = Color(0xFF1E1E2E),
        onSurfaceVariant = Color(0xFFF8F8F2),
        outline = Color(0xFFBD93F9),
        outlineVariant = Color(0xFF6272A4),
        scrim = Color(0xFF282A36),
        inverseSurface = Color(0xFFF8F8F2),
        inverseOnSurface = Color(0xFF282A36),
        inversePrimary = Color(0xFF8BE9FD),
        surfaceDim = Color(0xFF282A36),
        surfaceBright = Color(0xFF44475A),
        surfaceContainerLowest = Color(0xFF282A36),
        surfaceContainerLow = Color(0xFF1E1E2E),
        surfaceContainer = Color(0xFF1E1E2E),
        surfaceContainerHigh = Color(0xFF1E1E2E),
        surfaceContainerHighest = Color(0xFF44475A),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF8BE9FD),
        onPrimary = Color(0xFF282A36),
        primaryContainer = Color(0xFF8BE9FD),
        onPrimaryContainer = Color(0xFF282A36),
        secondary = Color(0xFF50FA7B),
        onSecondary = Color(0xFF282A36),
        secondaryContainer = Color(0xFFBD93F9),
        onSecondaryContainer = Color(0xFF282A36),
        tertiary = Color(0xFF8BE9FD),
        onTertiary = Color(0xFF282A36),
        tertiaryContainer = Color(0xFFF8F8F2),
        onTertiaryContainer = Color(0xFF44475A),
        error = Color(0xFFFF5555),
        onError = Color(0xFF282A36),
        errorContainer = Color(0xFFFF5555),
        onErrorContainer = Color(0xFF282A36),
        background = Color(0xFFF8F8F2),
        onBackground = Color(0xFF282A36),
        surface = Color(0xFFF8F8F2),
        onSurface = Color(0xFF282A36),
        surfaceVariant = Color(0xFFE9E9F4),
        onSurfaceVariant = Color(0xFF282A36),
        outline = Color(0xFF8BE9FD),
        outlineVariant = Color(0xFFBD93F9),
        scrim = Color(0xFFF8F8F2),
        inverseSurface = Color(0xFF282A36),
        inverseOnSurface = Color(0xFFF8F8F2),
        inversePrimary = Color(0xFFBD93F9),
        surfaceDim = Color(0xFFF8F8F2),
        surfaceBright = Color(0xFFE9E9F4),
        surfaceContainerLowest = Color(0xFFF8F8F2),
        surfaceContainerLow = Color(0xFFE9E9F4),
        surfaceContainer = Color(0xFFE9E9F4),
        surfaceContainerHigh = Color(0xFFE9E9F4),
        surfaceContainerHighest = Color(0xFFE9E9F4),
    )
}