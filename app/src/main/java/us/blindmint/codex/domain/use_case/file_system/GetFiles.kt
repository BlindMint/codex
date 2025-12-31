/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.file_system

import us.blindmint.codex.domain.browse.file.SelectableFile
import us.blindmint.codex.domain.repository.FileSystemRepository
import javax.inject.Inject

class GetFiles @Inject constructor(
    private val repository: FileSystemRepository
) {

    suspend fun execute(query: String): List<SelectableFile> {
        return repository.getFiles(query)
    }
}