/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.reader.images.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.blindmint.codex.R
import ua.blindmint.codex.presentation.core.components.settings.GenericOption
import ua.blindmint.codex.presentation.core.components.settings.OptionConfig
import ua.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import ua.blindmint.codex.ui.main.MainEvent

@Composable
fun ImagesOption() {
    GenericOption(
        OptionConfig(
            stateSelector = { it.images },
            eventCreator = { MainEvent.OnChangeImages(it) },
            component = { value, onChange ->
                SwitchWithTitle(
                    selected = value,
                    title = stringResource(id = R.string.images_option),
                    description = stringResource(id = R.string.images_option_desc),
                    onClick = { onChange(!value) }
                )
            }
        )
    )
}