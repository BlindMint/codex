/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.mapper.bookmark.BookmarkMapper
import us.blindmint.codex.domain.bookmark.Bookmark
import us.blindmint.codex.domain.repository.BookmarkRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bookmark repository.
 * Manages all [Bookmark] related work.
 */
@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val database: BookDao,
    private val bookmarkMapper: BookmarkMapper,
) : BookmarkRepository {

    /**
     * Insert bookmark in database.
     */
    override suspend fun insertBookmark(bookmark: Bookmark) {
        database.insertBookmark(
            bookmarkMapper.toBookmarkEntity(bookmark)
        )
    }

    /**
     * Get all bookmarks for a specific book.
     */
    override suspend fun getBookmarksByBookId(bookId: Int): List<Bookmark> {
        return database.getBookmarksByBookId(bookId).map {
            bookmarkMapper.toBookmark(it)
        }
    }

    /**
     * Get a specific bookmark by ID.
     */
    override suspend fun getBookmarkById(bookmarkId: Int): Bookmark? {
        val bookmark = database.getBookmarkById(bookmarkId)
        return bookmark?.let { bookmarkMapper.toBookmark(it) }
    }

    /**
     * Delete a specific bookmark.
     */
    override suspend fun deleteBookmark(bookmark: Bookmark) {
        database.deleteBookmark(
            bookmarkMapper.toBookmarkEntity(bookmark)
        )
    }

    /**
     * Delete all bookmarks for a specific book.
     */
    override suspend fun deleteBookmarksByBookId(bookId: Int) {
        database.deleteBookmarksByBookId(bookId)
    }

    /**
     * Delete all bookmarks.
     */
    override suspend fun deleteAllBookmarks() {
        database.deleteAllBookmarks()
    }
}
