/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.constants.provideFonts

@Composable
fun SpeedReadingFontFamilyOption(
    enabled: Boolean = true,
    onFontChanged: (String) -> Unit
) {
    var selectedFontId by remember { mutableStateOf("default") }

    ChipsWithTitle(
        title = stringResource(id = R.string.speed_reading_font_family),
        chips = provideFonts()
            .map {
                ButtonItem(
                    id = it.id,
                    title = it.fontName.asString(),
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = it.font,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    selected = enabled && it.id == selectedFontId
                )
            },
        enabled = enabled,
        onClick = { fontId ->
            if (enabled) {
                selectedFontId = fontId
                onFontChanged(fontId)
            }
        }
    )
}