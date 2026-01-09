/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.colors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ColorPreset
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.settings.SettingsEvent
import us.blindmint.codex.ui.settings.SettingsModel

@Composable
fun SimpleColorPresetSelector() {
    val settingsModel = hiltViewModel<SettingsModel>()
    val state = settingsModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.color_preset_option),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
        ) {
            items(
                items = state.value.colorPresets,
                key = { colorPreset -> colorPreset.id }
            ) { colorPreset ->
                SimpleColorPresetItem(
                    colorPreset = colorPreset,
                    isSelected = state.value.selectedColorPreset?.id == colorPreset.id,
                    onClick = {
                        settingsModel.onEvent(SettingsEvent.OnSelectColorPreset(colorPreset.id))
                    }
                )
            }
        }
    }
}

@Composable
private fun SimpleColorPresetItem(
    colorPreset: ColorPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val title = remember(colorPreset.id, colorPreset.name) {
        if ((colorPreset.name ?: "").isBlank()) {
            // Use the preset ID for consistent numbering
            context.getString(R.string.color_preset_query, colorPreset.id.toString())
        } else {
            colorPreset.name!!
        }
    }

    val borderColor = remember(isSelected, colorPreset.fontColor, colorPreset.backgroundColor) {
        if (!isSelected) {
            // For better contrast, use a darker border for light presets and lighter border for dark presets
            // Calculate luminance manually: 0.299*R + 0.587*G + 0.114*B
            val r = colorPreset.backgroundColor.red
            val g = colorPreset.backgroundColor.green
            val b = colorPreset.backgroundColor.blue
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b

            if (luminance > 0.5f) {
                // Light preset - use darker border for contrast
                Color(0xFF1C1B1F) // Dark color for light background
            } else {
                // Dark preset - use the font color with some transparency
                colorPreset.fontColor.copy(0.5f)
            }
        } else {
            colorPreset.fontColor
        }
    }

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = CircleShape
            )
            .background(colorPreset.backgroundColor, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
                tint = colorPreset.fontColor
            )
        }

        Text(
            text = title.trim(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = colorPreset.fontColor
            ),
            maxLines = 1
        )
    }
}