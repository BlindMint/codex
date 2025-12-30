/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.reader.font

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.presentation.settings.reader.font.components.CustomFontsOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontFamilyOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontSizeOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontStyleOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontThicknessOption
import us.blindmint.codex.presentation.settings.reader.font.components.LetterSpacingOption

fun LazyListScope.FontSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.font_reader_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true,
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        item {
            FontFamilyOption()
        }

        item {
            CustomFontsOption()
        }

        item {
            FontThicknessOption()
        }

        item {
            FontStyleOption()
        }

        item {
            FontSizeOption()
        }

        item {
            LetterSpacingOption()
        }
    }
}