/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.ColorPickerWithTitle

@Composable
fun SpeedReadingColorsOption(
    color: Color = Color.Red,
    opacity: Float = 1.0f,
    onColorChange: (Color) -> Unit = {},
    onOpacityChange: (Float) -> Unit = {}
) {
    ColorPickerWithTitle(
        title = stringResource(id = R.string.speed_reading_accent_color),
        value = color,
        presetId = 0, // TODO: Use proper preset ID for speed reading
        showRgbInputs = true,
        onValueChange = onColorChange
    )

    // Opacity slider as part of the color picker
    SpeedReadingAccentCharacterOpacityOption(
        opacity = opacity,
        onOpacityChange = onOpacityChange,
        showTitle = false // Don't show the title since it's now part of the color section
    )
}