/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle

@Composable
fun SpeedReadingOsdSeparationOption(
    osdSeparation: Float,
    onOsdSeparationChange: (Float) -> Unit
) {
    SliderWithTitle(
        title = stringResource(id = R.string.speed_reading_osd_separation),
        value = Pair(osdSeparation, "%"),
        toValue = 100,
        onValueChange = onOsdSeparationChange
    )
}