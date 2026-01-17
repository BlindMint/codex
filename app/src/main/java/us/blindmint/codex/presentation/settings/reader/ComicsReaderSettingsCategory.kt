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
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.presentation.settings.reader.comic.components.*
import us.blindmint.codex.presentation.settings.reader.comic.ComicProgressSubcategory

fun LazyListScope.ComicsReaderSettingsCategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    // Reading Mode subsection
    SettingsSubcategory(
        titleColor = titleColor,
        title = { stringResource(R.string.reading_mode_reader_settings) },
        showTitle = true,
        showDivider = true
    ) {
        item {
            ComicReadingDirectionOption()
        }
        item {
            ComicReaderModeOption()
        }
        item {
            ComicTapZoneOption()
        }
        item {
            ComicInvertTapsOption()
        }
    }

    // Display subsection
    SettingsSubcategory(
        titleColor = titleColor,
        title = { stringResource(R.string.display_tab) },
        showTitle = true,
        showDivider = true
    ) {
        item {
            ComicImageScaleOption()
        }
        item {
            ComicZoomStartOption()
        }
        item {
            ComicCropBordersOption()
        }
        item {
            ComicLandscapeZoomOption()
        }
    }

    // Progress subsection
    ComicProgressSubcategory(
        titleColor = titleColor,
        showDivider = false
    )
}