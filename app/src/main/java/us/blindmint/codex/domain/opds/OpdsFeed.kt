/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.opds

data class OpdsFeed(
    val title: String,
    val entries: List<OpdsEntry> = emptyList(),
    val links: List<OpdsLink> = emptyList()
)