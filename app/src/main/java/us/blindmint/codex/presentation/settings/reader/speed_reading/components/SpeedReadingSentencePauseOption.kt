/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

@Composable
fun SpeedReadingSentencePauseOption(
    sentencePauseDuration: Int = 350,
    onSentencePauseDurationChange: (Int) -> Unit = {}
) {
    var pauseDuration by remember(sentencePauseDuration) { mutableIntStateOf(sentencePauseDuration) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = "Manual sentence pause duration",
            padding = 0.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Slider
        Slider(
            value = pauseDuration.toFloat(),
            onValueChange = {
                val newValue = (it / 5).toInt() * 5
                pauseDuration = newValue
                onSentencePauseDurationChange(newValue)
            },
            valueRange = 150f..700f,
            steps = 0,
            modifier = Modifier.weight(1f)
        )

        // Numeric input with vertical centering fix
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.width(80.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedTextField(
                value = pauseDuration.toString(),
                onValueChange = { newValue ->
                    val intValue = newValue.toIntOrNull() ?: pauseDuration
                    val coercedValue = intValue.coerceIn(150, 700)
                    pauseDuration = coercedValue
                    onSentencePauseDurationChange(coercedValue)
                },
                label = { Text("ms") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center
                ),
                singleLine = true
            )
        }
    }
}
}