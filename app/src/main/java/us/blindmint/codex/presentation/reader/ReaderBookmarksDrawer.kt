/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.bookmark.Bookmark
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawerSelectableItem
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawerTitleItem
import us.blindmint.codex.ui.reader.ReaderEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReaderBookmarksDrawer(
    show: Boolean,
    bookmarks: List<Bookmark>,
    scrollToBookmark: (ReaderEvent.OnScrollToBookmark) -> Unit,
    dismissDrawer: (ReaderEvent.OnDismissDrawer) -> Unit,
    deleteBookmark: (Bookmark) -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    ModalDrawer(
        show = show,
        startIndex = 0,
        onDismissRequest = { dismissDrawer(ReaderEvent.OnDismissDrawer) },
        header = {
            ModalDrawerTitleItem(
                title = stringResource(id = R.string.bookmarks)
            )
        }
    ) {
        if (bookmarks.isEmpty()) {
            item {
                StyledText(
                    text = stringResource(id = R.string.no_bookmarks),
                    modifier = Modifier.width(360.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        } else {
            items(bookmarks) { bookmark ->
                ModalDrawerSelectableItem(
                    selected = false,
                    onClick = {
                        scrollToBookmark(
                            ReaderEvent.OnScrollToBookmark(
                                scrollIndex = bookmark.scrollIndex,
                                scrollOffset = bookmark.scrollOffset
                            )
                        )
                        dismissDrawer(ReaderEvent.OnDismissDrawer)
                    }
                ) {
                    StyledText(
                        text = dateFormatter.format(Date(bookmark.timestamp)),
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { deleteBookmark(bookmark) },
                        modifier = Modifier.width(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(id = R.string.delete_bookmark_content_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
