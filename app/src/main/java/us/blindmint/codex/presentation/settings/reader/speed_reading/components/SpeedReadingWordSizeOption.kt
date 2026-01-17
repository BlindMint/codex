/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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

@Composable
fun SpeedReadingWordSizeOption(
    wordSize: Int = 48,
    onWordSizeChange: (Int) -> Unit = {}
) {
    var localWordSize by remember(wordSize) { mutableIntStateOf(wordSize) }

    Text(
        text = stringResource(id = R.string.speed_reading_word_size),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Slider
        Slider(
            value = localWordSize.toFloat(),
            onValueChange = {
                localWordSize = it.toInt()
                onWordSizeChange(it.toInt())
            },
            valueRange = 24f..96f,
            modifier = Modifier.weight(1f)
        )

        // Numeric input
        OutlinedTextField(
            value = localWordSize.toString(),
            onValueChange = { newValue ->
                val intValue = newValue.toIntOrNull() ?: localWordSize
                val coercedValue = intValue.coerceIn(24, 96)
                localWordSize = coercedValue
                onWordSizeChange(coercedValue)
            },
            label = { Text("sp") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(60.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            singleLine = true
        )
    }
}