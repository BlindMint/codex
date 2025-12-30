/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

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
import us.blindmint.codex.domain.library.book.SelectableBook
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.constants.providePrimaryScrollbar
import us.blindmint.codex.ui.library.LibraryEvent

@Composable
fun LibraryListLayout(
    books: List<us.blindmint.codex.domain.library.book.SelectableBook>,
    hasSelectedItems: Boolean,
    selectBook: (us.blindmint.codex.ui.library.LibraryEvent.OnSelectBook) -> Unit,
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