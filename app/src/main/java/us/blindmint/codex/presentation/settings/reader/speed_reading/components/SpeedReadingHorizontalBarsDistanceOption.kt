/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import kotlin.math.roundToInt

@Composable
fun SpeedReadingHorizontalBarsDistanceOption(
    distance: Int = 8,
    onDistanceChange: (Int) -> Unit = {}
) {
    SliderWithTitle(
        title = stringResource(id = R.string.speed_reading_horizontal_bars_distance),
        value = Pair(distance, "dp"),
        fromValue = 0,
        toValue = 64,
        steps = 8,
        onValueChange = { newValue ->
            // Snap to multiples of 8
            val snappedValue = ((newValue + 4) / 8) * 8 // Round to nearest multiple of 8
            onDistanceChange(snappedValue.coerceIn(0, 64))
        }
    )
}
