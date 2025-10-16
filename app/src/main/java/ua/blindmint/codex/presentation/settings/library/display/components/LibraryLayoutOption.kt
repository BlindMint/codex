/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.library.display.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.library.display.LibraryLayout
import ua.blindmint.codex.domain.ui.ButtonItem
import ua.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import ua.blindmint.codex.ui.main.MainEvent
import ua.blindmint.codex.ui.main.MainModel

@Composable
fun LibraryLayoutOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SegmentedButtonWithTitle(
        title = stringResource(id = R.string.layout_option),
        buttons = LibraryLayout.entries.map {
            ua.blindmint.codex.domain.ui.ButtonItem(
                id = it.name,
                title = stringResource(
                    when (it) {
                        LibraryLayout.LIST -> R.string.layout_list
                        LibraryLayout.GRID -> R.string.layout_grid
                    }
                ),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                selected = state.value.libraryLayout == it
            )
        },
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeLibraryLayout(
                    LibraryLayout.entries[LibraryLayout.entries.indexOfFirst { layout ->
                        layout.name == it.id
                    }]
                )
            )
        }
    )
}