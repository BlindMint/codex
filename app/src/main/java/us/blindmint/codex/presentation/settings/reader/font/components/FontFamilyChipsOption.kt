/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun FontFamilyChipsOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    val currentFontFamily = state.value.fontFamily

    val builtInFonts = provideFonts().map {
        ButtonItem(
            id = it.id,
            title = it.fontName.asString(),
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontFamily = it.font
            ),
            selected = it.id == currentFontFamily
        )
    }

    val customFonts = state.value.customFonts.map {
        ButtonItem(
            id = "custom_${it.name}",
            title = it.name,
            textStyle = MaterialTheme.typography.labelLarge,
            selected = "custom_${it.name}" == currentFontFamily
        )
    }

    val allFonts = builtInFonts + customFonts

    ChipsWithTitle(
        title = stringResource(id = R.string.font_family_option),
        chips = allFonts,
        onClick = { buttonItem ->
            mainModel.onEvent(MainEvent.OnChangeFontFamily(buttonItem.id))
        }
    )
}