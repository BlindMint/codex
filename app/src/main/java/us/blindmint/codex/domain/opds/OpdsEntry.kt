/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.opds

data class OpdsEntry(
    val id: String,
    val title: String,
    val author: String? = null,
    val summary: String? = null,
    val published: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val rights: String? = null,
    val identifiers: List<String> = emptyList(),
    val categories: List<String> = emptyList(), // tags
    val series: String? = null,
    val seriesIndex: Int? = null,
    val coverUrl: String? = null,
    val links: List<OpdsLink> = emptyList()
)