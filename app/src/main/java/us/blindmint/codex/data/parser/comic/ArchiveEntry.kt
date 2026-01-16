/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.comic

interface ArchiveEntry {
    fun getPath(): String
    fun getSize(): Long
    fun isDirectory(): Boolean
    fun getMtime(): Long
}