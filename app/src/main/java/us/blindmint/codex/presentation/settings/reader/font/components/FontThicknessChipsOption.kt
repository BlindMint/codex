/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun FontThicknessChipsOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    val fontFamily = remember(state.value.fontFamily) {
        if (state.value.fontFamily.startsWith("custom_")) {
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

    val currentThickness = state.value.fontThickness

    val thicknessChips = ReaderFontThickness.entries.map { thickness ->
        ButtonItem(
            id = thickness.toString(),
            title = when (thickness) {
                ReaderFontThickness.THIN -> stringResource(id = R.string.font_thickness_thin)
                ReaderFontThickness.EXTRA_LIGHT -> stringResource(id = R.string.font_thickness_extra_light)
                ReaderFontThickness.LIGHT -> stringResource(id = R.string.font_thickness_light)
                ReaderFontThickness.NORMAL -> stringResource(id = R.string.font_thickness_normal)
                ReaderFontThickness.MEDIUM -> stringResource(id = R.string.font_thickness_medium)
                ReaderFontThickness.SEMI_BOLD -> stringResource(id = R.string.font_thickness_semi_bold)
                ReaderFontThickness.BOLD -> stringResource(id = R.string.font_thickness_bold)
                ReaderFontThickness.EXTRA_BOLD -> stringResource(id = R.string.font_thickness_extra_bold)
                ReaderFontThickness.BLACK -> stringResource(id = R.string.font_thickness_black)
            },
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontFamily = fontFamily.font,
                fontWeight = thickness.thickness
            ),
            selected = thickness == currentThickness
        )
    }

    ChipsWithTitle(
        title = stringResource(id = R.string.font_thickness_option),
        chips = thicknessChips,
        onClick = { buttonItem ->
            mainModel.onEvent(MainEvent.OnChangeFontThickness(buttonItem.id))
        }
    )
}