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

fun LazyListScope.SpeedReadingSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.speed_reading_reader_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        item {
            SpeedReadingWpmOption()
        }

        item {
            SpeedReadingAccentCharacterOption()
        }

        item {
            SpeedReadingVerticalIndicatorsOption()
        }

        item {
            SpeedReadingHorizontalBarsOption()
        }

        item {
            SpeedReadingColorsOption()
        }

        item {
            SpeedReadingWordSizeOption()
        }

        item {
            SpeedReadingSentencePauseOption()
        }

        item {
            SpeedReadingCustomFontOption()
        }

        // TODO: Conditionally show font family selector when custom font is enabled
        // For now, always show it
        item {
            SpeedReadingFontFamilyOption()
        }
    }
}