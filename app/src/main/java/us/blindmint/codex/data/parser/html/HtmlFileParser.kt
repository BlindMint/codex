/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.html

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

class HtmlFileParser @Inject constructor() : FileParser {

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return try {
            val document = cachedFile.openInputStream()?.use {
                Jsoup.parse(it, null, "", Parser.htmlParser())
            }

            val title = document?.select("head > title")?.text()?.trim().run {
                if (isNullOrBlank()) {
                    return@run cachedFile.name.substringBeforeLast(".").trim()
                }
                return@run this
            }

            BookWithCover(
                book = Book(
                    title = title,
                    authors = emptyList(),
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