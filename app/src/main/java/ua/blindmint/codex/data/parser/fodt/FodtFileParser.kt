/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.data.parser.fodt

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import ua.blindmint.codex.R
import ua.blindmint.codex.data.parser.FileParser
import ua.blindmint.codex.domain.file.CachedFile
import ua.blindmint.codex.domain.library.book.Book
import ua.blindmint.codex.domain.library.book.BookWithCover
import ua.blindmint.codex.domain.library.category.Category
import ua.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

/**
 * Parser for FODT (Flat OpenDocument Text) files.
 * FODT is an uncompressed XML format for OpenDocument text files.
 */
class FodtFileParser @Inject constructor() : FileParser {

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return try {
            val content = cachedFile.openInputStream()?.bufferedReader()?.use { it.readText() }
                ?: return null

            val doc = Jsoup.parse(content, "", Parser.xmlParser())

            // Extract title from meta or use filename
            val metaTitle = doc.select("office|meta dc|title, dc\\:title").firstOrNull()?.text()
            val title = if (!metaTitle.isNullOrBlank()) {
                metaTitle
            } else {
                cachedFile.name.substringBeforeLast(".").trim()
            }

            // Extract author from meta
            val metaAuthor = doc.select("office|meta dc|creator, dc\\:creator").firstOrNull()?.text()
            val author = if (!metaAuthor.isNullOrBlank()) {
                UIText.StringValue(metaAuthor)
            } else {
                UIText.StringResource(R.string.unknown_author)
            }

            // Extract description/subject from meta
            val description = doc.select("office|meta dc|description, dc\\:description, dc|subject, dc\\:subject")
                .firstOrNull()?.text()

            BookWithCover(
                book = Book(
                    title = title,
                    author = author,
                    description = description,
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
