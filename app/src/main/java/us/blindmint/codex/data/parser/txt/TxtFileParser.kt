/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.txt

import us.blindmint.codex.R
import us.blindmint.codex.data.parser.BaseFileParser
import us.blindmint.codex.data.parser.BookFactory
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

class TxtFileParser @Inject constructor() : BaseFileParser() {

    override val tag = "TXT Parser"

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        return safeParse {
            val title = cachedFile.name.substringBeforeLast(".").trim()

            BookFactory.createWithDefaults(
                title = title,
                filePath = cachedFile.uri.toString()
            )
        }
    }
}