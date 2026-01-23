/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.appearance

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import us.blindmint.codex.presentation.settings.appearance.colors.ColorsSubcategory
import us.blindmint.codex.presentation.settings.appearance.components.ScreenOrientationOption
import us.blindmint.codex.presentation.settings.appearance.theme_preferences.ThemePreferencesSubcategory

fun LazyListScope.AppearanceSettingsCategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    item {
        ScreenOrientationOption()
    }
    ThemePreferencesSubcategory(
        titleColor = titleColor
    )
    ColorsSubcategory(
        titleColor = titleColor,
        showDivider = false
    )
}