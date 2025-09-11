/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.reader.ReaderFontThickness
import ua.blindmint.codex.presentation.core.components.common.StyledText
import ua.blindmint.codex.presentation.core.constants.provideFonts
import ua.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import ua.blindmint.codex.ui.main.MainEvent
import ua.blindmint.codex.ui.main.MainModel

@Composable
fun FontThicknessOption() {
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
        SettingsSubcategoryTitle(title = stringResource(id = R.string.font_thickness_option), padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // First row: Thin, Extra light, Light
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                listOf(
                    ReaderFontThickness.THIN,
                    ReaderFontThickness.EXTRA_LIGHT,
                    ReaderFontThickness.LIGHT
                ).forEach { thickness ->
                    FilterChip(
                        modifier = Modifier.height(36.dp),
                        selected = thickness == state.value.fontThickness,
                        label = {
                            StyledText(
                                text = when (thickness) {
                                    ReaderFontThickness.THIN -> stringResource(id = R.string.font_thickness_thin)
                                    ReaderFontThickness.EXTRA_LIGHT -> stringResource(id = R.string.font_thickness_extra_light)
                                    ReaderFontThickness.LIGHT -> stringResource(id = R.string.font_thickness_light)
                                    ReaderFontThickness.NORMAL -> stringResource(id = R.string.font_thickness_normal)
                                    ReaderFontThickness.MEDIUM -> stringResource(id = R.string.font_thickness_medium)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = fontFamily.font,
                                    fontWeight = thickness.thickness
                                ),
                                maxLines = 1
                            )
                        },
                        onClick = {
                            mainModel.onEvent(
                                MainEvent.OnChangeFontThickness(
                                    thickness.toString()
                                )
                            )
                        },
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Second row: Normal, Medium
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                listOf(
                    ReaderFontThickness.NORMAL,
                    ReaderFontThickness.MEDIUM
                ).forEach { thickness ->
                    FilterChip(
                        modifier = Modifier.height(36.dp),
                        selected = thickness == state.value.fontThickness,
                        label = {
                            StyledText(
                                text = when (thickness) {
                                    ReaderFontThickness.THIN -> stringResource(id = R.string.font_thickness_thin)
                                    ReaderFontThickness.EXTRA_LIGHT -> stringResource(id = R.string.font_thickness_extra_light)
                                    ReaderFontThickness.LIGHT -> stringResource(id = R.string.font_thickness_light)
                                    ReaderFontThickness.NORMAL -> stringResource(id = R.string.font_thickness_normal)
                                    ReaderFontThickness.MEDIUM -> stringResource(id = R.string.font_thickness_medium)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = fontFamily.font,
                                    fontWeight = thickness.thickness
                                ),
                                maxLines = 1
                            )
                        },
                        onClick = {
                            mainModel.onEvent(
                                MainEvent.OnChangeFontThickness(
                                    thickness.toString()
                                )
                            )
                        },
                    )
                }
            },
        )
    }
}