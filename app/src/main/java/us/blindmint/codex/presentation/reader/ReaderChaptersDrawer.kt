/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.bookmark.Bookmark
import us.blindmint.codex.domain.reader.ExpandableChapter
import us.blindmint.codex.domain.reader.ReaderText.Chapter
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawerSelectableItem
import us.blindmint.codex.presentation.core.util.calculateProgress
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.theme.ExpandingTransition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReaderChaptersBookmarksDrawer(
    show: Boolean,
    chapters: List<Chapter>,
    currentChapter: Chapter?,
    currentChapterProgress: Float,
    bookmarks: List<Bookmark>,
    scrollToChapter: (ReaderEvent.OnScrollToChapter) -> Unit,
    scrollToBookmark: (ReaderEvent.OnScrollToBookmark) -> Unit,
    dismissDrawer: (ReaderEvent.OnDismissDrawer) -> Unit,
    deleteBookmark: (Bookmark) -> Unit,
    clearAllBookmarks: () -> Unit,
    onQuickBookmark: (customName: String) -> Unit,
    isComic: Boolean = false
) {
    // For comics, start on bookmarks tab (index 1 if both tabs, or 0 if only bookmarks)
    // For books, start on chapters tab (index 0)
    var selectedTabIndex by remember { mutableIntStateOf(if (isComic) 1 else 0) }
    val tabs = if (isComic) {
        listOf(stringResource(R.string.bookmarks))
    } else {
        listOf(stringResource(R.string.chapters), stringResource(R.string.bookmarks))
    }

    val expandableChapters = remember(show, chapters, currentChapter) {
        mutableStateListOf<ExpandableChapter>().apply {
            var index = 0
            while (index < chapters.size) {
                val chapter = chapters.getOrNull(index) ?: continue
                when (chapter.nested) {
                    false -> {
                        val children = chapters.drop(index + 1).takeWhile { it.nested }
                        add(
                            ExpandableChapter(
                                parent = chapter,
                                expanded = chapter.id == currentChapter?.id ||
                                        children.any { it.id == currentChapter?.id },
                                chapters = children.takeIf { it.isNotEmpty() }
                            )
                        )
                        index += children.size + 1
                    }

                    true -> {
                        add(
                            ExpandableChapter(
                                parent = chapter.copy(nested = false),
                                expanded = false,
                                chapters = null
                            )
                        )
                        index++
                    }
                }
            }
        }
    }

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
        startIndex = if (selectedTabIndex == 0) chapters.indexOf(currentChapter).takeIf { it != -1 } ?: 0 else 0,
        onDismissRequest = { dismissDrawer(ReaderEvent.OnDismissDrawer) },
        header = {
            Column {
                // Only show tab row if there are multiple tabs (for books, not comics)
                if (!isComic) {
                    Box(Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 0.5.dp),
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = { tabPositions ->
                                if (selectedTabIndex < tabPositions.size) {
                                    val width by animateDpAsState(
                                        targetValue = tabPositions[selectedTabIndex].contentWidth,
                                        label = ""
                                    )

                                    TabRowDefaults.PrimaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                        width = width
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }
                } else {
                    // For comics, show centered "Bookmarks" tab header without tab row
                    Box(Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Text(
                            text = stringResource(R.string.bookmarks),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        footer = {
            // Bookmarks footer: shown if on bookmarks tab (index 1 for books, 0 for comics)
            val isBookmarksTab = if (isComic) selectedTabIndex == 0 else selectedTabIndex == 1
            if (isBookmarksTab) {
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
        }
    ) {
        // Adjust tab index for comics (they only have bookmarks, so always show bookmarks)
        val displayTabIndex = if (isComic) 1 else selectedTabIndex

        when (displayTabIndex) {
            0 -> { // Chapters tab (only for books)
                expandableChapters.forEach { expandableChapter ->
                    item {
                        ModalDrawerSelectableItem(
                            selected = expandableChapter.parent.id == currentChapter?.id,
                            onClick = {
                                scrollToChapter(
                                    ReaderEvent.OnScrollToChapter(
                                        chapter = expandableChapter.parent
                                    )
                                )
                                dismissDrawer(ReaderEvent.OnDismissDrawer)
                            }
                        ) {
                            StyledText(
                                text = expandableChapter.parent.title,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            if (expandableChapter.parent == currentChapter) {
                                Spacer(modifier = Modifier.width(18.dp))
                                StyledText(text = "${currentChapterProgress.calculateProgress(0)}%")
                            }

                            if (!expandableChapter.chapters.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(18.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropUp,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .noRippleClickable {
                                            expandableChapters.indexOf(expandableChapter)
                                                .also { chapterIndex ->
                                                    if (chapterIndex == -1) return@noRippleClickable
                                            expandableChapters[chapterIndex] =
                                                expandableChapter.copy(
                                                    expanded = !expandableChapter.expanded
                                                )
                                        }
                                }
                                .rotate(
                                    animateFloatAsState(
                                        targetValue = if (expandableChapter.expanded) 0f else -180f
                                    ).value
                                ),
                            contentDescription = stringResource(
                                id = if (expandableChapter.expanded) R.string.collapse_content_desc
                                else R.string.expand_content_desc
                            )
                        )
                    }
                }
            }

            if (!expandableChapter.chapters.isNullOrEmpty()) {
                items(expandableChapter.chapters) { chapter ->
                    ExpandingTransition(visible = expandableChapter.expanded) {
                        ModalDrawerSelectableItem(
                            selected = chapter.id == currentChapter?.id,
                            onClick = {
                                scrollToChapter(
                                    ReaderEvent.OnScrollToChapter(
                                        chapter = chapter
                                    )
                                )
                                dismissDrawer(ReaderEvent.OnDismissDrawer)
                            }
                        ) {
                            Spacer(modifier = Modifier.width(18.dp))

                            StyledText(
                                text = chapter.title,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            if (chapter == currentChapter) {
                                Spacer(modifier = Modifier.width(18.dp))
                                StyledText(text = "${currentChapterProgress.calculateProgress(0)}%")
                            }
                        }
                    }
                }
            }
        }
    }
            1 -> { // Bookmarks tab
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