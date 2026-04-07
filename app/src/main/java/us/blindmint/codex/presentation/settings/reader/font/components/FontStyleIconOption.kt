/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun FontStyleIconOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    val fontFamily = remember(state.value.fontFamily) {
        if (state.value.fontFamily.startsWith("custom_")) {
            // For custom fonts, use different built-in fonts as visual indicators for preview
            val customFontName = state.value.fontFamily.removePrefix("custom_").lowercase()
            when {
                customFontName.contains("serif") ||
                customFontName.contains("times") ||
                customFontName.contains("garamond") ||
                customFontName.contains("georgia") -> {
                    provideFonts().find { it.id == "noto_serif" } ?: provideFonts().first()
                }
                customFontName.contains("mono") ||
                customFontName.contains("code") ||
                customFontName.contains("fira") ||
                customFontName.contains("jetbrains") ||
                customFontName.contains("cascadia") ||
                customFontName.contains("source") -> {
                    provideFonts().find { it.id == "roboto" } ?: provideFonts().first()
                }
                customFontName.contains("script") ||
                customFontName.contains("hand") ||
                customFontName.contains("brush") -> {
                    provideFonts().find { it.id == "lora" } ?: provideFonts().first()
                }
                customFontName.contains("sans") ||
                customFontName.contains("arial") ||
                customFontName.contains("helvetica") ||
                customFontName.contains("verdana") -> {
                    provideFonts().find { it.id == "open_sans" } ?: provideFonts().first()
                }
                else -> {
                    provideFonts().find { it.id == "jost" } ?: provideFonts().first()
                }
            }
        } else {
            provideFonts().run {
                find {
                    it.id == state.value.fontFamily
                } ?: get(0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(title = stringResource(id = R.string.font_style_option), padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Normal style chip
            FilterChip(
                selected = !state.value.isItalic,
                onClick = {
                    mainModel.onEvent(MainEvent.OnChangeFontStyle(false))
                },
                label = {
                    StyledText(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = fontFamily.font,
                            fontStyle = FontStyle.Normal
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.FormatUnderlined,
                        contentDescription = stringResource(id = R.string.font_style_normal),
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // Italic style chip
            FilterChip(
                selected = state.value.isItalic,
                onClick = {
                    mainModel.onEvent(MainEvent.OnChangeFontStyle(true))
                },
                label = {
                    StyledText(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = fontFamily.font,
                            fontStyle = FontStyle.Italic
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.FormatItalic,
                        contentDescription = stringResource(id = R.string.font_style_italic),
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}