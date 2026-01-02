/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.mapper.bookmark

import us.blindmint.codex.data.local.dto.BookmarkEntity
import us.blindmint.codex.domain.bookmark.Bookmark
import javax.inject.Inject

class BookmarkMapperImpl @Inject constructor() : BookmarkMapper {
    override suspend fun toBookmarkEntity(bookmark: Bookmark): BookmarkEntity {
        return BookmarkEntity(
            id = bookmark.id,
            bookId = bookmark.bookId,
            scrollIndex = bookmark.scrollIndex,
            scrollOffset = bookmark.scrollOffset,
            timestamp = bookmark.timestamp
        )
    }

    override suspend fun toBookmark(bookmarkEntity: BookmarkEntity): Bookmark {
        return Bookmark(
            id = bookmarkEntity.id,
            bookId = bookmarkEntity.bookId,
            scrollIndex = bookmarkEntity.scrollIndex,
            scrollOffset = bookmarkEntity.scrollOffset,
            timestamp = bookmarkEntity.timestamp
        )
    }
}
