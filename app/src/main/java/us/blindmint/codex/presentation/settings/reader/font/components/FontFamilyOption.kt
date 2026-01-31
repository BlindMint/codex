/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.settings.GenericOption
import us.blindmint.codex.presentation.core.components.settings.OptionConfig
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.settings.FontSelectionScreen

@Composable
fun FontFamilyOption() {
    val navigator = LocalNavigator.current

    GenericOption(
        OptionConfig(
            stateSelector = { it.fontFamily },
            eventCreator = { us.blindmint.codex.ui.main.MainEvent.OnChangeFontFamily(it) },
            component = { value, _ ->
                // Handle custom font selection - if a custom font is selected,
                // don't show any selection in the built-in fonts
                val selectedFontId = if (value.startsWith("custom_")) null else value

                val selectedFont = provideFonts().run {
                    find { it.id == selectedFontId } ?: get(0)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navigator.push(FontSelectionScreen) }
                        .padding(horizontal = SettingsHorizontalPadding, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.font_family_option),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.height(36.dp),
                            selected = selectedFontId != null,
                            label = {
                                StyledText(
                                    text = selectedFont.fontName.asString(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = selectedFont.font
                                    ),
                                    maxLines = 1
                                )
                            },
                            onClick = { navigator.push(FontSelectionScreen) },
                        )
                    }
                }
            }
        )
    )
}