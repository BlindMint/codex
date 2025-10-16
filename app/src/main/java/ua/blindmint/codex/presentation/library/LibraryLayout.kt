/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.blindmint.codex.domain.library.display.LibraryLayout
import ua.blindmint.codex.presentation.core.components.common.LazyVerticalGridWithScrollbar
import ua.blindmint.codex.presentation.core.constants.providePrimaryScrollbar
import ua.blindmint.codex.ui.main.MainModel

@Composable
fun LibraryLayout(
    items: LazyGridScope.() -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    when (state.value.libraryLayout) {
        LibraryLayout.GRID -> {
            LazyVerticalGridWithScrollbar(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier.fillMaxSize(),
                scrollbarSettings = providePrimaryScrollbar(false),
                contentPadding = PaddingValues(8.dp)
            ) {
                items()
            }
        }
        LibraryLayout.LIST -> {
            // For list layout, we need to create a list-based layout
            // Since the current items function is for grid, we'll keep grid for now
            // TODO: Implement proper list layout for library items
            LazyVerticalGridWithScrollbar(
                columns = GridCells.Fixed(1), // Single column for list-like appearance
                modifier = Modifier.fillMaxSize(),
                scrollbarSettings = providePrimaryScrollbar(false),
                contentPadding = PaddingValues(8.dp)
            ) {
                items()
            }
        }
    }
}