/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.appearance.colors.components.BackgroundImageOption
import us.blindmint.codex.presentation.settings.appearance.colors.components.ColorPresetOption
import us.blindmint.codex.presentation.settings.appearance.colors.components.FastColorPresetChangeOption
import us.blindmint.codex.presentation.settings.appearance.theme_preferences.components.AbsoluteDarkOption
import us.blindmint.codex.presentation.settings.appearance.theme_preferences.components.AppThemeOption
import us.blindmint.codex.presentation.settings.appearance.theme_preferences.components.DarkThemeOption
import us.blindmint.codex.presentation.settings.appearance.theme_preferences.components.PureDarkOption
import us.blindmint.codex.presentation.settings.appearance.theme_preferences.components.ThemeContrastOption

fun LazyListScope.AppearanceSettingsCategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    // Theme Preferences section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.theme_appearance_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DarkThemeOption()
                AppThemeOption()
                ThemeContrastOption()
                PureDarkOption()
                AbsoluteDarkOption()
            }
        }
    }

    // Colors section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.colors_appearance_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ColorPresetOption(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    showNestedBackground = false
                )
                FastColorPresetChangeOption()
            }
        }
    }

    // Background section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Background",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BackgroundImageOption(backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow)
            }
        }
    }
}