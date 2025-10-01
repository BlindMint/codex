/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.ui.ButtonItem
import ua.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import ua.blindmint.codex.presentation.core.components.settings.GenericOption
import ua.blindmint.codex.presentation.core.components.settings.OptionConfig
import ua.blindmint.codex.presentation.core.constants.provideFonts
import ua.blindmint.codex.ui.main.MainEvent

@Composable
fun FontFamilyOption() {
    GenericOption(
        OptionConfig(
            stateSelector = { it.fontFamily },
            eventCreator = { MainEvent.OnChangeFontFamily(it) },
            component = { value, onChange ->
                // Handle custom font selection - if a custom font is selected,
                // don't show any selection in the built-in fonts
                val selectedFontId = if (value.startsWith("custom_")) null else value

                val fontFamily = provideFonts().run {
                    find { it.id == selectedFontId } ?: get(0)
                }

                ChipsWithTitle(
                    title = stringResource(id = R.string.font_family_option),
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