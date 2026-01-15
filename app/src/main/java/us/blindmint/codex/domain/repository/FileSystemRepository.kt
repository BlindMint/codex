/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.repository

import us.blindmint.codex.domain.browse.file.SelectableFile
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.NullableBook

interface FileSystemRepository {

    suspend fun getFiles(
        query: String = ""
    ): List<SelectableFile>

    suspend fun getBookFromFile(
        cachedFile: CachedFile
    ): NullableBook

    suspend fun getAllFilesFromFolder(
        folderUri: android.net.Uri
    ): List<CachedFile>
}