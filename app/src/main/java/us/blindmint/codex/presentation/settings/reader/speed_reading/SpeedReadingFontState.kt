/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberSpeedReadingFontState(): SpeedReadingFontState {
    return remember {
        SpeedReadingFontState()
    }
}

class SpeedReadingFontState {
    var isCustomFontEnabled by mutableStateOf(false)
    var selectedFontId by mutableStateOf("default")
}