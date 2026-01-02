/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.lookup

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * Represents a web dictionary for word lookups.
 *
 * @param id Unique identifier for persistence
 * @param name Display name
 * @param urlTemplate URL template with %s placeholder for the word
 * @param iconResId Optional drawable resource ID for the icon
 */
@Immutable
data class WebDictionary(
    val id: String,
    val name: String,
    val urlTemplate: String,
    @DrawableRes val iconResId: Int? = null
) {
    /**
     * Builds the dictionary URL with the given word.
     */
    fun buildUrl(word: String): String {
        val encodedWord = java.net.URLEncoder.encode(word.lowercase().trim(), "UTF-8")
        return urlTemplate.replace("%s", encodedWord)
    }
}
