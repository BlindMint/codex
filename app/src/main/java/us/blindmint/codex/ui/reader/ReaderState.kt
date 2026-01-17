/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import us.blindmint.codex.domain.bookmark.Bookmark
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.reader.TextSelectionContext
import us.blindmint.codex.domain.reader.Checkpoint
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.ReaderText.Chapter
import us.blindmint.codex.domain.reader.SearchResult
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.domain.util.BottomSheet
import us.blindmint.codex.domain.util.Drawer
import us.blindmint.codex.presentation.core.constants.provideEmptyBook

@Immutable
data class ReaderState(
    val book: Book = provideEmptyBook(),
    val text: List<ReaderText> = emptyList(),
    val listState: LazyListState = LazyListState(),

    val currentChapter: Chapter? = null,
    val currentChapterProgress: Float = 0f,

    val errorMessage: UIText? = null,
    val isLoading: Boolean = true,

    val showMenu: Boolean = false,
    val checkpoint: Checkpoint = Checkpoint(0, 0),
    val lockMenu: Boolean = false,

    val speedReadingMode: Boolean = false,
    val speedReadingFromBookInfo: Boolean = false,

    val bottomSheet: BottomSheet? = null,
    val drawer: Drawer? = null,
    val speedReadingSettingsVisible: Boolean = false,

    val showSearch: Boolean = false,
    val searchBarPersistent: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val currentSearchResultIndex: Int = -1,

    // Text selection for bottom sheet menu
    val textSelectionContext: TextSelectionContext? = null,
    val webViewUrl: String? = null,

    // Bookmarks
    val bookmarks: List<Bookmark> = emptyList()
)