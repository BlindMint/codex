/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.import_progress

/**
 * Represents a single import operation tracking its state and progress.
 */
data class ImportOperation(
    val id: String,
    val folderName: String,
    val folderPath: String,
    val totalBooks: Int,
    val currentProgress: Int,
    val status: ImportStatus,
    val errorMessage: String? = null,
    val currentFile: String = ""
)
