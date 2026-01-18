/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle

@Composable
fun SpeedReadingAccentCharacterOpacityOption(
    opacity: Float = 1.0f,
    onOpacityChange: (Float) -> Unit = {},
    showTitle: Boolean = true
) {
    var localOpacity by remember(opacity) { mutableStateOf<Float>(opacity) }

    if (showTitle) {
        Text(
            text = stringResource(id = R.string.speed_reading_accent_opacity),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    } else {
        Text(
            text = "Alpha",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Slider (no steps/dots like RGB sliders)
        Slider(
            value = (localOpacity * 100).toFloat(),
            onValueChange = {
                val newOpacity = it.toInt() / 100f
                localOpacity = newOpacity
                onOpacityChange(newOpacity)
            },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f)
        )

        // Numeric input
        OutlinedTextField(
            value = (localOpacity * 100).toInt().toString(),
            onValueChange = { newValue ->
                val intValue = newValue.toIntOrNull() ?: (localOpacity * 100).toInt()
                val coercedValue = intValue.coerceIn(0, 100)
                val newOpacity = coercedValue / 100f
                localOpacity = newOpacity
                onOpacityChange(newOpacity)
            },
            label = { Text("%") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(60.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            singleLine = true
        )
    }
}