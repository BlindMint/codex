/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.reader

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import us.blindmint.codex.presentation.settings.reader.chapters.ChaptersSubcategory
import us.blindmint.codex.presentation.settings.reader.dictionary.DictionarySubcategory
import us.blindmint.codex.presentation.settings.reader.font.FontSubcategory
import us.blindmint.codex.presentation.settings.reader.images.ImagesSubcategory
import us.blindmint.codex.presentation.settings.reader.misc.MiscSubcategory
import us.blindmint.codex.presentation.settings.reader.padding.PaddingSubcategory
import us.blindmint.codex.presentation.settings.reader.progress.ProgressSubcategory
import us.blindmint.codex.presentation.settings.reader.reading_mode.ReadingModeSubcategory
import us.blindmint.codex.presentation.settings.reader.reading_speed.ReadingSpeedSubcategory
import us.blindmint.codex.presentation.settings.reader.search.SearchSubcategory
import us.blindmint.codex.presentation.settings.reader.speed_reading.SpeedReadingSubcategory
import us.blindmint.codex.presentation.settings.reader.system.SystemSubcategory
import us.blindmint.codex.presentation.settings.reader.text.TextSubcategory

fun LazyListScope.BooksReaderSettingsCategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    FontSubcategory(
        titleColor = titleColor
    )
    TextSubcategory(
        titleColor = titleColor
    )
    ImagesSubcategory(
        titleColor = titleColor
    )
    ChaptersSubcategory(
        titleColor = titleColor
    )
    ReadingModeSubcategory(
        titleColor = titleColor
    )
    PaddingSubcategory(
        titleColor = titleColor
    )
    SystemSubcategory(
        titleColor = titleColor
    )
    ReadingSpeedSubcategory(
        titleColor = titleColor
    )
    ProgressSubcategory(
        titleColor = titleColor
    )
    SearchSubcategory(
        titleColor = titleColor
    )
    DictionarySubcategory(
        titleColor = titleColor
    )
    MiscSubcategory(
        titleColor = titleColor,
        showDivider = false
    )
}

fun LazyListScope.SpeedReadingReaderSettingsCategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    SpeedReadingSubcategory(
        tab = null, // Show all speed reading settings in one tab
        titleColor = titleColor,
        showTitle = false,
        showDivider = false
    )
}