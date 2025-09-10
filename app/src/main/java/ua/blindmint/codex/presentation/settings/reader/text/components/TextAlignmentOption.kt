/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.reader.text.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.reader.ReaderTextAlignment
import ua.blindmint.codex.domain.ui.ButtonItem
import ua.blindmint.codex.presentation.core.components.settings.GenericOption
import ua.blindmint.codex.presentation.core.components.settings.OptionConfig
import ua.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import ua.blindmint.codex.ui.main.MainEvent

@Composable
fun TextAlignmentOption() {
    GenericOption(
        OptionConfig(
            stateSelector = { it.textAlignment.toString() },
            eventCreator = { MainEvent.OnChangeTextAlignment(it) },
            component = { value, onChange ->
                SegmentedButtonWithTitle(
                    title = stringResource(id = R.string.text_alignment_option),
                    buttons = ReaderTextAlignment.entries.map {
                        ButtonItem(
                            id = it.toString(),
                            title = when (it) {
                                ReaderTextAlignment.START -> stringResource(id = R.string.alignment_start)
                                ReaderTextAlignment.JUSTIFY -> stringResource(id = R.string.alignment_justify)
                                ReaderTextAlignment.CENTER -> stringResource(id = R.string.alignment_center)
                                ReaderTextAlignment.END -> stringResource(id = R.string.alignment_end)
                            },
                            textStyle = MaterialTheme.typography.labelLarge,
                            selected = it.toString() == value
                        )
                    },
                    onClick = { onChange(it.id) }
                )
            }
        )
    )
}