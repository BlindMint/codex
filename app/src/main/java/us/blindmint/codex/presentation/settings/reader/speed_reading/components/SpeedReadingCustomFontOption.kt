/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle

@Composable
fun SpeedReadingCustomFontOption(
    onCustomFontChanged: (Boolean) -> Unit
) {
    var isCustomFontEnabled by remember { mutableStateOf(false) }

    SwitchWithTitle(
        selected = isCustomFontEnabled,
        title = stringResource(id = R.string.speed_reading_custom_font),
        onClick = {
            isCustomFontEnabled = !isCustomFontEnabled
            onCustomFontChanged(isCustomFontEnabled)
        }
    )
}