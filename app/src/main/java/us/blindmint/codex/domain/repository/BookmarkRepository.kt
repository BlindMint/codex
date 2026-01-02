/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.repository

import us.blindmint.codex.domain.bookmark.Bookmark

interface BookmarkRepository {

    suspend fun insertBookmark(bookmark: Bookmark)

    suspend fun getBookmarksByBookId(bookId: Int): List<Bookmark>

    suspend fun getBookmarkById(bookmarkId: Int): Bookmark?

    suspend fun deleteBookmark(bookmark: Bookmark)

    suspend fun deleteBookmarksByBookId(bookId: Int)

    suspend fun deleteAllBookmarks()
}
