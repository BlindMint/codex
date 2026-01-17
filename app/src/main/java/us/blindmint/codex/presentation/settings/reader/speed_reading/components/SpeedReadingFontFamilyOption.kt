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
import us.blindmint.codex.presentation.core.constants.provideFonts

@Composable
fun SpeedReadingFontFamilyOption(
    enabled: Boolean = true,
    onFontChanged: (String) -> Unit
) {
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
                    selected = it.id == "default" // TODO: Use actual selected font
                )
            },
        enabled = enabled,
        onClick = { buttonItem ->
            if (enabled) {
                onFontChanged(buttonItem.id)
            }
        }
    )
}