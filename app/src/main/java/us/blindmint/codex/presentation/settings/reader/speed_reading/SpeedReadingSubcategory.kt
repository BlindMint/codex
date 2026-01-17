/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.reader.speed_reading

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingWpmOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingAccentCharacterOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingColorsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingSentencePauseOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingCustomFontOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingFontFamilyOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingWordSizeOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingBackgroundImageOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingColorsSection
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorsSizeOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsThicknessOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsColorOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingAccentCharacterOpacityOption

fun LazyListScope.SpeedReadingSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.speed_reading_reader_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true,
    wpm: Int = 300,
    onWpmChange: (Int) -> Unit = {}
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        // Performance
        item {
            SpeedReadingWpmOption(
                wpm = wpm,
                onWpmChange = onWpmChange
            )
        }

        item {
            SpeedReadingSentencePauseOption()
        }

        item {
            SpeedReadingWordSizeOption()
        }

        // Colors section
        item {
            SpeedReadingColorsSection(
                onResetToNormalColors = {
                    // TODO: Reset colors to normal reading colors
                }
            )
        }

        item {
            SpeedReadingBackgroundImageOption()
        }

        // Accent & Indicators
        item {
            SpeedReadingAccentCharacterOption()
        }

        item {
            SpeedReadingColorsOption() // Accent color with RGB sliders
        }

        item {
            SpeedReadingAccentCharacterOpacityOption()
        }

        item {
            SpeedReadingVerticalIndicatorsOption()
        }

        item {
            SpeedReadingVerticalIndicatorsSizeOption()
        }

        // Horizontal Bars
        item {
            SpeedReadingHorizontalBarsOption()
        }

        item {
            SpeedReadingHorizontalBarsThicknessOption()
        }

        item {
            SpeedReadingHorizontalBarsColorOption()
        }

        // Font Settings
        item {
            SpeedReadingCustomFontOption(
                onCustomFontChanged = { /* TODO: Implement state management */ }
            )
        }

        item {
            SpeedReadingFontFamilyOption(
                enabled = true, // TODO: Connect to custom font toggle state
                onFontChanged = { /* TODO: Implement font selection */ }
            )
        }
    }
}