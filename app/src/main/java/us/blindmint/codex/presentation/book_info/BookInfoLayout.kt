/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.constants.providePrimaryScrollbar
import us.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoLayout(
    book: Book,
    listState: LazyListState,
    paddingValues: PaddingValues,
    showChangeCoverBottomSheet: (BookInfoEvent.OnShowChangeCoverBottomSheet) -> Unit,
    navigateToReader: () -> Unit,
    navigateToSpeedReading: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed top section: Book cover and info
        Box(modifier = Modifier.fillMaxWidth()) {
            if (book.coverImage != null) {
                BookInfoLayoutBackground(
                    height = paddingValues.calculateTopPadding() + 232.dp,
                    image = book.coverImage
                )
            }

            Column(Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding() + 12.dp))
                BookInfoLayoutInfo(
                    book = book,
                    showChangeCoverBottomSheet = showChangeCoverBottomSheet
                )
            }
        }

        // Scrollable middle section: Description (fills remaining space minus button height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 48.dp + 36.dp) // Button height + total padding
        ) {
            LazyColumnWithScrollbar(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                scrollbarSettings = providePrimaryScrollbar(false),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 18.dp,
                    bottom = 18.dp
                )
            ) {
                item {
                    BookInfoLayoutDescription(
                        book = book
                    )
                }
            }
        }

        // Fixed bottom section: Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
        ) {
            BookInfoLayoutButton(
                book = book,
                navigateToReader = navigateToReader,
                navigateToSpeedReading = navigateToSpeedReading
            )
        }
    }
}