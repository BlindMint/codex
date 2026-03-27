/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.text.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun TextAlignmentChipsOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    val currentAlignment = state.value.textAlignment

    val alignmentChips = ReaderTextAlignment.entries.map { alignment ->
        ButtonItem(
            id = alignment.toString(),
            title = when (alignment) {
                ReaderTextAlignment.START -> stringResource(id = R.string.alignment_start)
                ReaderTextAlignment.JUSTIFY -> stringResource(id = R.string.alignment_justify)
                ReaderTextAlignment.CENTER -> stringResource(id = R.string.alignment_center)
                ReaderTextAlignment.END -> stringResource(id = R.string.alignment_end)
                ReaderTextAlignment.ORIGINAL -> stringResource(id = R.string.alignment_original)
            },
            textStyle = MaterialTheme.typography.labelLarge,
            selected = alignment == currentAlignment
        )
    }

    ChipsWithTitle(
        title = stringResource(id = R.string.text_alignment_option),
        chips = alignmentChips,
        onClick = { buttonItem ->
            mainModel.onEvent(MainEvent.OnChangeTextAlignment(buttonItem.id))
        }
    )
}