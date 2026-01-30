/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.utils

import me.xdrop.fuzzywuzzy.FuzzySearch
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.presentation.settings.SettingsItem

/**
 * Fuzzy search utility for OPDS catalog browsing and settings.
 *
 * Provides fuzzy search capabilities to improve discoverability in OPDS catalogs and settings menus.
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

        val entryScores = entries.map { entry ->
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

            Pair(entry, maxScore)
        }

        return entryScores
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Search through settings items using fuzzy matching.
     *
     * @param items List of settings items to search
     * @param query Search query string
     * @param threshold Minimum similarity score (0-100) to consider a match
     * @return Filtered and sorted list of settings items
     */
    fun searchSettings(
        items: List<SettingsItem>,
        query: String,
        threshold: Int = 60
    ): List<SettingsItem> {
        if (query.isBlank()) return items

        val queryLower = query.lowercase()

        val itemScores = items.map { item ->
            val titleScore = FuzzySearch.partialRatio(queryLower, item.title.lowercase())
            val maxScore = titleScore

            Pair(item, maxScore)
        }

        return itemScores
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
