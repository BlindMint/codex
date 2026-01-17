/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun SpeedReadingHorizontalBarsOption(
    selected: Boolean = true,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    SwitchWithTitle(
        selected = selected,
        title = stringResource(id = R.string.speed_reading_horizontal_bars),
        onClick = { onSelectionChange(!selected) }
    )
}