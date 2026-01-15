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
    val isLoadingMore: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val selectedBooks: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val feed: OpdsFeed? = null,
    val error: String? = null,
    val downloadError: String? = null, // Separate error for downloads (shown as snackbar, not replacing catalog)
    val feedUrl: String? = null,
    val hasNextPage: Boolean = false,
    val nextPageUrl: String? = null,
    val isDownloadDirectoryAccessible: Boolean = true,
    val username: String? = null,
    val password: String? = null
)