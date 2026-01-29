/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.local.dto.BookmarkEntity
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
    database: BookDao,
    private val bookmarkMapper: BookmarkMapper
) : BaseRepository<Bookmark, BookmarkEntity, BookDao>(), BookmarkRepository {

    override val dao = database

    override suspend fun insertBookmark(bookmark: Bookmark) {
        dao.insertBookmark(bookmarkMapper.toBookmarkEntity(bookmark))
    }

    override suspend fun getBookmarksByBookId(bookId: Int): List<Bookmark> {
        return dao.getBookmarksByBookId(bookId).map { bookmarkMapper.toBookmark(it) }
    }

    override suspend fun getBookmarkById(bookmarkId: Int): Bookmark? {
        return dao.getBookmarkById(bookmarkId)?.let { bookmarkMapper.toBookmark(it) }
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        dao.deleteBookmark(bookmarkMapper.toBookmarkEntity(bookmark))
    }

    override suspend fun deleteBookmarksByBookId(bookId: Int) {
        dao.deleteBookmarksByBookId(bookId)
    }

    override suspend fun deleteAllBookmarks() {
        dao.deleteAllBookmarks()
    }
}
