/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.runtime.Composable
import us.blindmint.codex.domain.bookmark.Bookmark
import us.blindmint.codex.domain.reader.ReaderText.Chapter
import us.blindmint.codex.domain.util.Drawer
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.reader.ReaderScreen

@Composable
fun ReaderDrawer(
    drawer: Drawer?,
    chapters: List<Chapter>,
    bookmarks: List<Bookmark>,
    currentChapter: Chapter?,
    currentChapterProgress: Float,
    scrollToChapter: (ReaderEvent.OnScrollToChapter) -> Unit,
    scrollToBookmark: (ReaderEvent.OnScrollToBookmark) -> Unit,
    dismissDrawer: (ReaderEvent.OnDismissDrawer) -> Unit,
    deleteBookmark: (Bookmark) -> Unit,
    clearAllBookmarks: () -> Unit,
    onQuickBookmark: (customName: String) -> Unit
) {
    ReaderChaptersBookmarksDrawer(
        show = drawer == ReaderScreen.CHAPTERS_DRAWER || drawer == ReaderScreen.BOOKMARKS_DRAWER,
        chapters = chapters,
        currentChapter = currentChapter,
        currentChapterProgress = currentChapterProgress,
        bookmarks = bookmarks,
        scrollToChapter = scrollToChapter,
        scrollToBookmark = scrollToBookmark,
        dismissDrawer = dismissDrawer,
        deleteBookmark = deleteBookmark,
        clearAllBookmarks = clearAllBookmarks,
        onQuickBookmark = onQuickBookmark
    )
}