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
fun SpeedReadingHorizontalBarLengthOption(
    length: Float = 0.9f,
    onLengthChange: (Float) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.speed_reading_horizontal_bars_length),
            padding = 0.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = length,
            onValueChange = onLengthChange,
            valueRange = 0.3f..1.0f, // From 30% to 100% of screen width
            modifier = Modifier.fillMaxWidth()
        )
    }
}