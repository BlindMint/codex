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

@Composable
fun SpeedReadingAccentCharacterOpacityOption() {
    SliderWithTitle(
        title = stringResource(id = R.string.speed_reading_accent_opacity),
        value = Pair(100, "%"), // TODO: Add to MainState, default 100%
        fromValue = 0,
        toValue = 100,
        onValueChange = { /* TODO: Save to settings */ }
    )
}