/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.reader.SpeedReadingVerticalIndicatorType

@Composable
fun SpeedReadingVerticalIndicatorTypeOption(
    selectedType: SpeedReadingVerticalIndicatorType = SpeedReadingVerticalIndicatorType.LINE,
    onTypeChange: (SpeedReadingVerticalIndicatorType) -> Unit = {},
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.speed_reading_vertical_indicator_type),
            padding = 0.dp,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        SpeedReadingVerticalIndicatorType.values().forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { if (enabled) onTypeChange(type) },
                    enabled = enabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (type) {
                        SpeedReadingVerticalIndicatorType.LINE -> stringResource(id = R.string.speed_reading_vertical_indicator_type_line)
                        SpeedReadingVerticalIndicatorType.ARROWS -> stringResource(id = R.string.speed_reading_vertical_indicator_type_arrows)
                        SpeedReadingVerticalIndicatorType.ARROWS_FILLED -> stringResource(id = R.string.speed_reading_vertical_indicator_type_arrows_filled)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}