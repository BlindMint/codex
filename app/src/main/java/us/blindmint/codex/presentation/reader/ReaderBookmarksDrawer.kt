/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.bookmark.Bookmark
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.modal_drawer.DrawerSide
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
    deleteBookmark: (Bookmark) -> Unit,
    clearAllBookmarks: () -> Unit,
    onQuickBookmark: (customName: String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val showClearConfirmation = remember { mutableStateOf(false) }
    val bookmarkToDelete = remember { mutableStateOf<Bookmark?>(null) }
    val showQuickBookmarkDialog = remember { mutableStateOf(false) }

    // Deletion confirmation dialog
    if (bookmarkToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { bookmarkToDelete.value = null },
            title = { Text(stringResource(id = R.string.delete_bookmark)) },
            text = { Text(stringResource(id = R.string.delete_bookmark_content_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteBookmark(bookmarkToDelete.value!!)
                        bookmarkToDelete.value = null
                    }
                ) {
                    Text(stringResource(id = R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { bookmarkToDelete.value = null }
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    // Clear all bookmarks confirmation dialog
    if (showClearConfirmation.value) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation.value = false },
            title = { Text(stringResource(id = R.string.clear_all_bookmarks)) },
            text = { Text(stringResource(id = R.string.clear_all_bookmarks_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        clearAllBookmarks()
                        showClearConfirmation.value = false
                    }
                ) {
                    Text(stringResource(id = R.string.clear_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmation.value = false }
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    // Quick bookmark dialog
    if (showQuickBookmarkDialog.value) {
        QuickBookmarkDialog(
            onDismiss = { showQuickBookmarkDialog.value = false },
            onConfirm = { customName ->
                onQuickBookmark(customName)
                showQuickBookmarkDialog.value = false
            }
        )
    }

    ModalDrawer(
        show = show,
        startIndex = 0,
        side = DrawerSide.RIGHT,
        onDismissRequest = { dismissDrawer(ReaderEvent.OnDismissDrawer) },
        header = {
            ModalDrawerTitleItem(
                title = stringResource(id = R.string.bookmarks)
            )
        },
        footer = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { showQuickBookmarkDialog.value = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.quick_bookmark))
                }

                if (bookmarks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showClearConfirmation.value = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.clear_all_bookmarks))
                    }
                }
            }
        }
    ) {
        if (bookmarks.isEmpty()) {
            item {
                StyledText(
                    text = stringResource(id = R.string.no_bookmarks),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
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
                    val bookmarkLabel = if (bookmark.customName.isNotEmpty()) {
                        bookmark.customName
                    } else if (bookmark.selectedText.isNotEmpty()) {
                        bookmark.selectedText
                    } else {
                        dateFormatter.format(Date(bookmark.timestamp))
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bookmark.pageNumber > 0) {
                            Text(
                                text = "${bookmark.pageNumber}. ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        StyledText(
                            text = bookmarkLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { bookmarkToDelete.value = bookmark },
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

@Composable
private fun QuickBookmarkDialog(
    onDismiss: () -> Unit,
    onConfirm: (customName: String) -> Unit
) {
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.bookmark)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text(stringResource(id = R.string.bookmark_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(customName) }
            ) {
                Text(stringResource(id = R.string.confirm_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(id = android.R.string.cancel))
            }
        }
    )
}
