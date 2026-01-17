/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.ColorPickerWithTitle

@Composable
fun SpeedReadingColorsSection(
    onResetToNormalColors: () -> Unit
) {
    var backgroundColor by remember { mutableStateOf(Color.White) }
    var fontColor by remember { mutableStateOf(Color.Black) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Colors section header with reset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.speed_reading_colors_section),
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = {
                    // TODO: Reset to normal reading colors
                    onResetToNormalColors()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.reset_colors),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Background color picker
        SpeedReadingBackgroundColorOption()

        // Font color picker
        SpeedReadingFontColorOption()
    }
}