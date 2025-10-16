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
import ua.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import ua.blindmint.codex.ui.main.MainEvent
import ua.blindmint.codex.ui.main.MainModel
import ua.blindmint.codex.ui.theme.ExpandingTransition

@Composable
fun LibraryGridSizeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ExpandingTransition(visible = state.value.libraryLayout == LibraryLayout.GRID) {
        SliderWithTitle(
            value = state.value.libraryGridSize
                    to " ${stringResource(R.string.library_grid_size_per_row)}",
            valuePlaceholder = stringResource(id = R.string.library_grid_size_auto),
            showPlaceholder = state.value.libraryAutoGridSize,
            fromValue = 0,
            toValue = 8,
            title = stringResource(id = R.string.library_grid_size_option),
            onValueChange = {
                mainModel.onEvent(MainEvent.OnChangeLibraryAutoGridSize(it == 0))
                mainModel.onEvent(
                    MainEvent.OnChangeLibraryGridSize(it)
                )
            }
        )
    }
}