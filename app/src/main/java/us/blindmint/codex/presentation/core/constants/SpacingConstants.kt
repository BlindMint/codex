/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized spacing values following Material 3 guidelines.
 * Use these constants for consistent padding and margins throughout the app.
 */
object Spacing {
    /** Extra small spacing - 4dp */
    val extraSmall: Dp = 4.dp

    /** Small spacing - 8dp */
    val small: Dp = 8.dp

    /** Medium spacing - 12dp */
    val medium: Dp = 12.dp

    /** Default/standard spacing - 16dp (Material 3 default) */
    val default: Dp = 16.dp

    /** Large spacing - 24dp */
    val large: Dp = 24.dp

    /** Extra large spacing - 32dp */
    val extraLarge: Dp = 32.dp
}

/**
 * Standard horizontal padding for settings components.
 * Using 16dp as the Material 3 standard.
 */
val SettingsHorizontalPadding: Dp = Spacing.default

/**
 * Standard vertical padding for settings components.
 */
val SettingsVerticalPadding: Dp = Spacing.small
