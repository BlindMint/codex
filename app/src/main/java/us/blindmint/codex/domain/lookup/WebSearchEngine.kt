/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.lookup

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * Represents a web search engine for text lookups.
 *
 * @param id Unique identifier for persistence
 * @param name Display name
 * @param urlTemplate URL template with %s placeholder for the search query
 * @param iconResId Optional drawable resource ID for the icon
 */
@Immutable
data class WebSearchEngine(
    val id: String,
    val name: String,
    val urlTemplate: String,
    @DrawableRes val iconResId: Int? = null
) {
    /**
     * Builds the search URL with the given query.
     */
    fun buildUrl(query: String): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        return urlTemplate.replace("%s", encodedQuery)
    }
}
