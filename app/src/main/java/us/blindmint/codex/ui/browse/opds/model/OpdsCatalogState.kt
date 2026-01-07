/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds.model

import androidx.compose.runtime.Immutable
import us.blindmint.codex.domain.opds.OpdsFeed

@Immutable
data class OpdsCatalogState(
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val feed: OpdsFeed? = null,
    val error: String? = null,
    val feedUrl: String? = null
)