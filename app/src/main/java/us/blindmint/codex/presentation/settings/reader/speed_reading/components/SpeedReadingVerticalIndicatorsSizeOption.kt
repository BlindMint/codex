/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

@Composable
fun SpeedReadingVerticalIndicatorsSizeOption(
    size: Int = 32,
    onSizeChange: (Int) -> Unit = {}
) {
    // Custom slider with reduced steps for fewer dots
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle(
            title = stringResource(id = R.string.speed_reading_vertical_indicators_size),
            padding = 0.dp
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
        androidx.compose.material3.Slider(
            value = size.toFloat(),
            onValueChange = { onSizeChange(it.toInt()) },
            valueRange = 8f..64f,
            steps = 7, // Reduced from 55 to 7 steps (8 total positions)
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        )
    }
}