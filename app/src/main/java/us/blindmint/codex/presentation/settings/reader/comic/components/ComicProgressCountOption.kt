/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.comic.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderProgressCount
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun ComicProgressCountOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SegmentedButtonWithTitle(
        title = stringResource(id = R.string.progress_count_option),
        buttons = ReaderProgressCount.entries.filter { it != ReaderProgressCount.QUANTITY }.map {
            ButtonItem(
                id = it.toString(),
                title = when (it) {
                    ReaderProgressCount.PERCENTAGE -> stringResource(id = R.string.progress_count_percentage)
                    ReaderProgressCount.QUANTITY -> stringResource(id = R.string.progress_count_quantity)
                    ReaderProgressCount.PAGE -> stringResource(id = R.string.progress_count_page)
                },
                textStyle = MaterialTheme.typography.labelLarge,
                selected = it == state.value.comicProgressCount
            )
        },
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeComicProgressCount(
                    it.id
                )
            )
        }
    )
}
