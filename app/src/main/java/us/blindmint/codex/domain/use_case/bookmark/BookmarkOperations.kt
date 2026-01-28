/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.bookmark

import us.blindmint.codex.domain.bookmark.Bookmark
import us.blindmint.codex.domain.repository.BookmarkRepository
import javax.inject.Inject

/**
 * Consolidated bookmark operations use case.
 * Combines 5 thin wrapper use cases into a single class.
 *
 * Original use cases consolidated:
 * - GetBookmarksByBookId (20 lines)
 * - InsertBookmark (20 lines)
 * - DeleteBookmark (20 lines)
 * - DeleteBookmarksByBookId (19 lines)
 * - DeleteAllBookmarks (19 lines)
 *
 * Total consolidation: 98 lines → ~60 lines (savings: ~38 lines)
 */
class BookmarkOperations @Inject constructor(
    private val repository: BookmarkRepository
) {

    // GET OPERATIONS

    /**
     * Get all bookmarks for a specific book.
     * Consolidates: GetBookmarksByBookId.kt
     */
    suspend fun getBookmarksByBookId(bookId: Int): List<Bookmark> {
        return repository.getBookmarksByBookId(bookId)
    }

    // CREATE OPERATIONS

    /**
     * Insert a new bookmark.
     * Consolidates: InsertBookmark.kt
     */
    suspend fun insertBookmark(bookmark: Bookmark) {
        repository.insertBookmark(bookmark)
    }

    // DELETE OPERATIONS

    /**
     * Delete a specific bookmark.
     * Consolidates: DeleteBookmark.kt
     */
    suspend fun deleteBookmark(bookmark: Bookmark) {
        repository.deleteBookmark(bookmark)
    }

    /**
     * Delete all bookmarks for a specific book.
     * Consolidates: DeleteBookmarksByBookId.kt
     */
    suspend fun deleteBookmarksByBookId(bookId: Int) {
        repository.deleteBookmarksByBookId(bookId)
    }

    /**
     * Delete all bookmarks in the database.
     * Consolidates: DeleteAllBookmarks.kt
     */
    suspend fun deleteAllBookmarks() {
        repository.deleteAllBookmarks()
    }
}
