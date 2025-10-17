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
fun LibraryListSizeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SliderWithTitle(
        value = state.value.libraryListSize to "",
        valuePlaceholder = when (state.value.libraryListSize) {
            0 -> stringResource(R.string.library_list_size_small)
            1 -> stringResource(R.string.library_list_size_medium)
            2 -> stringResource(R.string.library_list_size_large)
            else -> stringResource(R.string.library_list_size_medium)
        },
        showPlaceholder = true,
        fromValue = 0,
        toValue = 2,
        title = stringResource(id = R.string.library_list_size_option),
        onValueChange = { value ->
            mainModel.onEvent(MainEvent.OnChangeLibraryListSize(value))
        }
    )
}