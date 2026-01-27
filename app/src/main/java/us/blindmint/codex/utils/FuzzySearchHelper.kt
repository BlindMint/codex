/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.utils

import me.xdrop.fuzzywuzzy.FuzzySearch
import us.blindmint.codex.domain.opds.OpdsEntry

/**
 * Fuzzy search utility for OPDS catalog browsing.
 *
 * Provides fuzzy search capabilities to improve discoverability in OPDS catalogs.
 * Uses the FuzzyWuzzy library for string similarity matching.
 *
 * @property threshold Minimum similarity score (0-100) to consider a match
 */
object FuzzySearchHelper {

    /**
     * Search through OPDS entries using fuzzy matching.
     *
     * @param entries List of OPDS entries to search
     * @param query Search query string
     * @param threshold Minimum similarity score (0-100) to consider a match
     * @return Filtered and sorted list of entries
     */
    fun searchEntries(
        entries: List<OpdsEntry>,
        query: String,
        threshold: Int = 60
    ): List<OpdsEntry> {
        if (query.isBlank()) return entries

        val queryLower = query.lowercase()

        return entries
            .mapNotNull { entry ->
                val titleScore = entry.title?.let { title ->
                    FuzzySearch.partialRatio(queryLower, title.lowercase())
                } ?: 0

                val authorScore = entry.author?.let { author ->
                    FuzzySearch.partialRatio(queryLower, author.lowercase())
                } ?: 0

                val summaryScore = entry.summary?.let { summary ->
                    FuzzySearch.partialRatio(queryLower, summary.lowercase())
                } ?: 0

                val maxScore = maxOf(titleScore, authorScore, summaryScore)

                maxScore >= threshold
            }
            .sortedByDescending { entry ->
                val entryScore = entry.title?.let { title ->
                    FuzzySearch.partialRatio(queryLower, title.lowercase())
                } ?: 0

                val authorScore = entry.author?.let { author ->
                    FuzzySearch.partialRatio(queryLower, author.lowercase())
                } ?: 0

                val summaryScore = entry.summary?.let { summary ->
                    FuzzySearch.partialRatio(queryLower, summary.lowercase())
                } ?: 0

                maxOf(entryScore, authorScore, summaryScore)
            }
    }
}
