/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.common.StyledText

@Composable
fun BookInfoLayoutInfoTitle(
    book: Book
) {
    StyledText(
        text = book.title,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineSmall.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        maxLines = 4
    )
}