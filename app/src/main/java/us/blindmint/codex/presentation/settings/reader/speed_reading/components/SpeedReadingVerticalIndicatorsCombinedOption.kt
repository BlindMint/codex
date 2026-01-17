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
fun SpeedReadingVerticalIndicatorsCombinedOption(
    showVerticalIndicators: Boolean = true,
    verticalIndicatorsSize: Int = 8,
    onVerticalIndicatorsChange: (Boolean, Int) -> Unit = { _, _ -> }
) {
    // Convert current state to slider value
    val currentSliderValue = if (showVerticalIndicators) verticalIndicatorsSize.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.speed_reading_vertical_indicators),
            padding = 0.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = currentSliderValue,
            onValueChange = { newValue ->
                val intValue = newValue.toInt()
                if (intValue == 0) {
                    onVerticalIndicatorsChange(false, 32) // Keep size for when re-enabled
                } else {
                    onVerticalIndicatorsChange(true, intValue)
                }
            },
            valueRange = 0f..40f,
            steps = 4, // 0, 8, 16, 24, 32, 40 (5 steps = 6 positions)
            modifier = Modifier.fillMaxWidth()
        )
    }
}