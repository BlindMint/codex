/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.opds

data class OpdsLink(
    val href: String,
    val rel: String? = null,
    val type: String? = null,
    val title: String? = null
)