/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.BookWithCover


interface FileParser {

    suspend fun parse(cachedFile: CachedFile): BookWithCover?
}