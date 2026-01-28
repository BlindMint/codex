/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.BaseFileParser
import us.blindmint.codex.data.parser.BookFactory
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

class PdfFileParser @Inject constructor() : BaseFileParser() {

    override val tag = "PDF Parser"

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return safeParse {
            val document = PDDocument.load(cachedFile.openInputStream())

            val title = document.documentInformation.title
                ?: cachedFile.name.substringBeforeLast(".").trim()
            val authors = document.documentInformation.author.run {
                if (isNullOrBlank()) emptyList()
                else listOf(this)
            }
            val description = document.documentInformation.subject

            document.close()

            BookFactory.createWithDefaults(
                title = title,
                authors = authors,
                description = description,
                filePath = cachedFile.uri.toString()
            )
        }
    }
}