/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.reader.comic.components.*

fun LazyListScope.ComicsReaderSettingsCategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    // Reading Mode subsection
    item {
        Text(
            text = stringResource(R.string.reading_mode_reader_settings),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 0.dp)
        )
    }
    item {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ComicReadingDirectionOption()
            ComicReaderModeOption()
            ComicTapZoneOption()
            ComicInvertTapsOption()
        }
    }

    // Display subsection
    item {
        Text(
            text = stringResource(R.string.display_tab),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 0.dp)
        )
    }
    item {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ComicImageScaleOption()
            ComicZoomStartOption()
            ComicCropBordersOption()
            ComicLandscapeZoomOption()
        }
    }
}