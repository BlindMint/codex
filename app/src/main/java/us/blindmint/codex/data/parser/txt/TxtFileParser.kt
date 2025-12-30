/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.txt

import us.blindmint.codex.R
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

class TxtFileParser @Inject constructor() : FileParser {

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return try {
            val title = cachedFile.name.substringBeforeLast(".").trim()
            val author = UIText.StringResource(R.string.unknown_author)

            BookWithCover(
                book = Book(
                    title = title,
                    author = author,
                    description = null,
                    scrollIndex = 0,
                    scrollOffset = 0,
                    progress = 0f,
                    filePath = cachedFile.path,
                    lastOpened = null,
                    category = Category.entries[0],
                    coverImage = null
                ),
                coverImage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}