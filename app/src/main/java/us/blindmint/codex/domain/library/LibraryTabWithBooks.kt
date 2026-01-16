/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.library

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import us.blindmint.codex.domain.library.book.SelectableBook
import us.blindmint.codex.domain.ui.UIText

@Immutable
data class LibraryTabWithBooks(
    val tab: LibraryTab,
    val title: UIText,
    val books: List<SelectableBook>,
    val emptyIcon: Painter,
    val emptyMessage: UIText
)