/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing a main settings menu item
 *
 * @deprecated Use [Preference.PreferenceItem] with [PreferenceScreen] instead.
 * This class is kept for backward compatibility and will be removed in future versions.
 */
@Deprecated(
    message = "Use Preference.PreferenceItem with PreferenceScreen instead",
    replaceWith = ReplaceWith(
        "Preference.PreferenceItem",
        imports = ["us.blindmint.codex.presentation.settings.Preference"]
    ),
    level = DeprecationLevel.WARNING
)
data class SettingsItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: Any, // ImageVector or Painter
    val onClick: () -> Unit
)
