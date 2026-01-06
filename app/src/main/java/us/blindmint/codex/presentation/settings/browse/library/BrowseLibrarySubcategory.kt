/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse.library

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

fun LazyListScope.BrowseLibrarySubcategory() {
    item {
        LibraryShowBookCountOption()
    }
}

@Composable
private fun LibraryShowBookCountOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SwitchWithTitle(
        selected = state.value.libraryShowBookCount,
        title = stringResource(id = R.string.show_book_count_option)
    ) {
        mainModel.onEvent(
            MainEvent.OnChangeLibraryShowBookCount(
                !state.value.libraryShowBookCount
            )
        )
    }
}