/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.opf

/**
 * Metadata extracted from a standalone OPF (Open Packaging Format) file.
 * Used for Calibre-style metadata where .opf files exist alongside book files.
 */
data class OpfMetadata(
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val series: String? = null,
    val seriesIndex: Int? = null,
    val uuid: String? = null,
    val isbn: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val publicationDate: Long? = null,
    val coverPath: String? = null, // Relative path to cover image (e.g., "cover.jpg")
    val tags: List<String> = emptyList()
)
