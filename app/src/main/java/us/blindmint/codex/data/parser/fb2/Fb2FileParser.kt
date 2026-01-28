/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.fb2

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.BaseFileParser
import us.blindmint.codex.data.parser.BookFactory
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

class Fb2FileParser @Inject constructor() : BaseFileParser() {

    override val tag = "FB2 Parser"

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return safeParse {
            val document = cachedFile.openInputStream()?.use {
                Jsoup.parse(it, null, "", Parser.xmlParser())
            }

            val title = document?.selectFirst("book-title")?.text()?.trim().run {
                if (isNullOrBlank()) {
                    return@run cachedFile.name.substringBeforeLast(".").trim()
                }
                this
            }

            val authors = document?.selectFirst("author")?.text()?.trim().run {
                if (isNullOrBlank()) {
                    return@run emptyList()
                }
                listOf(this.trim())
            }

            val description = document?.selectFirst("annotation")?.text()?.trim().run {
                if (isNullOrBlank()) {
                    return@run null
                }
                this
            }

            BookFactory.createWithDefaults(
                title = title,
                authors = authors,
                description = description,
                filePath = cachedFile.uri.toString()
            )
        }
    }
}