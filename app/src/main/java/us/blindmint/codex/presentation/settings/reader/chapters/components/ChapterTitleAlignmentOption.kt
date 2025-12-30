/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.chapters.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun ChapterTitleAlignmentOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SegmentedButtonWithTitle(
        title = stringResource(id = R.string.chapter_title_alignment_option),
        buttons = ReaderTextAlignment.entries
            .filter { it != ReaderTextAlignment.ORIGINAL }  // ORIGINAL only applies to body text
            .map {
                ButtonItem(
                    id = it.toString(),
                    title = when (it) {
                        ReaderTextAlignment.START -> stringResource(id = R.string.alignment_start)
                        ReaderTextAlignment.JUSTIFY -> stringResource(id = R.string.alignment_justify)
                        ReaderTextAlignment.CENTER -> stringResource(id = R.string.alignment_center)
                        ReaderTextAlignment.END -> stringResource(id = R.string.alignment_end)
                        ReaderTextAlignment.ORIGINAL -> ""  // Filtered out above
                    },
                    textStyle = MaterialTheme.typography.labelLarge,
                    selected = it == state.value.chapterTitleAlignment
                )
            },
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeChapterTitleAlignment(
                    it.id
                )
            )
        }
    )
}