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
 * Tokyo Night theme
 * https://github.com/enkia/tokyo-night-vscode-theme
 */
@Composable
fun tokyoNightTheme(isDark: Boolean) = if (isDark) {
    androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF7AA2F7),
        onPrimary = Color(0xFF1A1B26),
        primaryContainer = Color(0xFF7AA2F7),
        onPrimaryContainer = Color(0xFF1A1B26),
        secondary = Color(0xFF9ECE6A),
        onSecondary = Color(0xFF1A1B26),
        secondaryContainer = Color(0xFF414868),
        onSecondaryContainer = Color(0xFF9ECE6A),
        tertiary = Color(0xFF7AA2F7),
        onTertiary = Color(0xFF1A1B26),
        tertiaryContainer = Color(0xFF24283B),
        onTertiaryContainer = Color(0xFFA9B1D6),
        error = Color(0xFFF7768E),
        onError = Color(0xFF1A1B26),
        errorContainer = Color(0xFFF7768E),
        onErrorContainer = Color(0xFF1A1B26),
        background = Color(0xFF1A1B26),
        onBackground = Color(0xFFA9B1D6),
        surface = Color(0xFF1A1B26),
        onSurface = Color(0xFFA9B1D6),
        surfaceVariant = Color(0xFF24283B),
        onSurfaceVariant = Color(0xFFA9B1D6),
        outline = Color(0xFF7AA2F7),
        outlineVariant = Color(0xFF565F89),
        scrim = Color(0xFF1A1B26),
        inverseSurface = Color(0xFFA9B1D6),
        inverseOnSurface = Color(0xFF1A1B26),
        inversePrimary = Color(0xFF2AC3DE),
        surfaceDim = Color(0xFF1A1B26),
        surfaceBright = Color(0xFF414868),
        surfaceContainerLowest = Color(0xFF1A1B26),
        surfaceContainerLow = Color(0xFF24283B),
        surfaceContainer = Color(0xFF24283B),
        surfaceContainerHigh = Color(0xFF24283B),
        surfaceContainerHighest = Color(0xFF414868),
    )
} else {
    androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF2AC3DE),
        onPrimary = Color(0xFF1A1B26),
        primaryContainer = Color(0xFF2AC3DE),
        onPrimaryContainer = Color(0xFF1A1B26),
        secondary = Color(0xFF9ECE6A),
        onSecondary = Color(0xFF1A1B26),
        secondaryContainer = Color(0xFFD5D6DB),
        onSecondaryContainer = Color(0xFF9ECE6A),
        tertiary = Color(0xFF2AC3DE),
        onTertiary = Color(0xFF1A1B26),
        tertiaryContainer = Color(0xFFE9F5F7),
        onTertiaryContainer = Color(0xFF565F89),
        error = Color(0xFFF7768E),
        onError = Color(0xFF1A1B26),
        errorContainer = Color(0xFFF7768E),
        onErrorContainer = Color(0xFF1A1B26),
        background = Color(0xFFE9F5F7),
        onBackground = Color(0xFF1A1B26),
        surface = Color(0xFFE9F5F7),
        onSurface = Color(0xFF1A1B26),
        surfaceVariant = Color(0xFFD5D6DB),
        onSurfaceVariant = Color(0xFF1A1B26),
        outline = Color(0xFF2AC3DE),
        outlineVariant = Color(0xFF9AA5CE),
        scrim = Color(0xFFE9F5F7),
        inverseSurface = Color(0xFF1A1B26),
        inverseOnSurface = Color(0xFFA9B1D6),
        inversePrimary = Color(0xFF7AA2F7),
        surfaceDim = Color(0xFFE9F5F7),
        surfaceBright = Color(0xFFD5D6DB),
        surfaceContainerLowest = Color(0xFFE9F5F7),
        surfaceContainerLow = Color(0xFFD5D6DB),
        surfaceContainer = Color(0xFFD5D6DB),
        surfaceContainerHigh = Color(0xFFD5D6DB),
        surfaceContainerHighest = Color(0xFFD5D6DB),
    )
}