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
fun SpeedReadingVerticalIndicatorsLengthOption(
    verticalIndicatorsSize: Int = 8,
    onVerticalIndicatorsSizeChange: (Int) -> Unit = {},
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.speed_reading_vertical_indicators_length),
            padding = 0.dp,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = verticalIndicatorsSize.toFloat(),
            onValueChange = { newValue ->
                if (enabled) onVerticalIndicatorsSizeChange(newValue.toInt())
            },
            valueRange = 0f..40f,
            steps = 4, // 0, 8, 16, 24, 32, 40 (5 steps = 6 positions)
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}