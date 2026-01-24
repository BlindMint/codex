/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.presentation.core.components.common.LazyVerticalGridWithScrollbar
import us.blindmint.codex.presentation.core.constants.providePrimaryScrollbar
import us.blindmint.codex.ui.main.MainModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun LibraryLayout(
    books: List<us.blindmint.codex.domain.library.book.SelectableBook> = emptyList(),
    hasSelectedItems: Boolean = false,
    selectBook: (us.blindmint.codex.ui.library.LibraryEvent.OnSelectBook) -> Unit = {},
    navigateToBrowse: () -> Unit = {},
    navigateToBookInfo: (id: Int) -> Unit = {},
    navigateToReader: (id: Int) -> Unit = {},
    items: LazyGridScope.() -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()
    val windowInfo = LocalWindowInfo.current

    val gridSize = if (state.value.libraryAutoGridSize) {
        // Auto calculation: base on screen width
        // For high-resolution screens like OnePlus 13 or Pixel 9 Pro XL, aim for 3 columns
        // Screen width in dp divided by minimum item width (120dp) gives approximate columns
        val screenWidthDp = with(LocalDensity.current) { windowInfo.containerSize.width.toDp().value }
        val minItemWidth = 120f
        val calculatedColumns = (screenWidthDp / minItemWidth).toInt()
        max(2, min(calculatedColumns, 5)) // Clamp between 2 and 5 columns
    } else {
        state.value.libraryGridSize
    }

    when (state.value.libraryLayout) {
        LibraryLayout.GRID -> {
            LazyVerticalGridWithScrollbar(
                columns = GridCells.Fixed(gridSize),
                modifier = Modifier.fillMaxSize(),
                scrollbarSettings = providePrimaryScrollbar(false),
                contentPadding = PaddingValues(8.dp)
            ) {
                // Convert LazyListScope to LazyGridScope for grid layout
                // This is a workaround since we need to support both layouts
                // In practice, the items function will be called with the appropriate scope
                items()
            }
        }
        LibraryLayout.LIST -> {
            LibraryListLayout(
                books = books,
                hasSelectedItems = hasSelectedItems,
                selectBook = selectBook,
                navigateToBrowse = navigateToBrowse,
                navigateToBookInfo = navigateToBookInfo,
                navigateToReader = navigateToReader
            ) {
                // The items function is called here, but we need to handle the scope mismatch
                // For now, we'll pass an empty lambda and handle items in LibraryPager
                {}
            }
        }
    }
}