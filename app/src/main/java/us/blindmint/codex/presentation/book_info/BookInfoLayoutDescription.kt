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
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.common.StyledText

@Composable
fun BookInfoLayoutDescription(
    book: Book
) {
    StyledText(
        text = if (!book.description.isNullOrBlank()) book.description
        else stringResource(id = R.string.error_no_description),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}