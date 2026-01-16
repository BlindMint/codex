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
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.components.settings.GenericOption
import us.blindmint.codex.presentation.core.components.settings.OptionConfig
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.ui.main.MainEvent

@Composable
fun SpeedReadingFontFamilyOption() {
    GenericOption(
        OptionConfig(
            stateSelector = { "default" }, // TODO: Add speed reading font to MainState
            eventCreator = { MainEvent.OnChangeFontFamily(it) }, // TODO: Use speed reading font event
            component = { value, onChange ->
                val selectedFontId = if (value.startsWith("custom_")) null else value

                val fontFamily = provideFonts().run {
                    find { it.id == selectedFontId } ?: get(0)
                }

                ChipsWithTitle(
                    title = stringResource(id = R.string.speed_reading_font_family),
                    chips = provideFonts()
                        .map {
                            ButtonItem(
                                id = it.id,
                                title = it.fontName.asString(),
                                textStyle = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = it.font
                                ),
                                selected = selectedFontId != null && it.id == fontFamily.id
                            )
                        },
                    onClick = { onChange(it.id) }
                )
            }
        )
    )
}