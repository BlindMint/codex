/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings


/**
 * Data class representing a main settings menu item (Appearance, Reader, Library, etc.).
 * Used by [SettingsLayout] and [FuzzySearchHelper] for the top-level settings navigation.
 */
data class SettingsItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: Any, // ImageVector or Painter
    val onClick: () -> Unit
)
