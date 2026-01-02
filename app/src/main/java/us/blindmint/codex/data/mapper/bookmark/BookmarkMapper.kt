/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.mapper.bookmark

import us.blindmint.codex.data.local.dto.BookmarkEntity
import us.blindmint.codex.domain.bookmark.Bookmark

interface BookmarkMapper {
    suspend fun toBookmarkEntity(bookmark: Bookmark): BookmarkEntity

    suspend fun toBookmark(bookmarkEntity: BookmarkEntity): Bookmark
}
