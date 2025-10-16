/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.blindmint.codex.domain.library.book.SelectableBook
import ua.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import ua.blindmint.codex.presentation.core.constants.providePrimaryScrollbar
import ua.blindmint.codex.ui.library.LibraryEvent

@Composable
fun LibraryListLayout(
    books: List<ua.blindmint.codex.domain.library.book.SelectableBook>,
    hasSelectedItems: Boolean,
    selectBook: (ua.blindmint.codex.ui.library.LibraryEvent.OnSelectBook) -> Unit,
    navigateToBrowse: () -> Unit,
    navigateToBookInfo: (id: Int) -> Unit,
    navigateToReader: (id: Int) -> Unit,
    items: LazyListScope.() -> Unit
) {
    LazyColumnWithScrollbar(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        scrollbarSettings = providePrimaryScrollbar(false)
    ) {
        // Call the items function which should provide the book items
        items()
    }
}