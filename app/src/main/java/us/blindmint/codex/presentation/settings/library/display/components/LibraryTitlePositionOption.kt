/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.library.display.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.display.LibraryTitlePosition
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun LibraryTitlePositionOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SegmentedButtonWithTitle(
        title = stringResource(id = R.string.library_title_position_option),
        buttons = LibraryTitlePosition.entries.map {
            us.blindmint.codex.domain.ui.ButtonItem(
                id = it.name,
                title = stringResource(
                    when (it) {
                        LibraryTitlePosition.BELOW -> R.string.library_title_position_below
                        LibraryTitlePosition.HIDDEN -> R.string.library_title_position_hidden
                    }
                ),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                selected = state.value.libraryTitlePosition == it
            )
        },
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeLibraryTitlePosition(LibraryTitlePosition.valueOf(it.id))
            )
        }
    )
}