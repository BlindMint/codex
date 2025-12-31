/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.dictionary

import androidx.compose.runtime.Immutable

/**
 * Dictionary sources for word lookups.
 *
 * @param id Unique identifier for persistence
 * @param urlTemplate URL template with %s placeholder for the word (null for system default)
 */
@Immutable
enum class DictionarySource(
    val id: String,
    val urlTemplate: String?
) {
    SYSTEM_DEFAULT("system_default", null),
    ONELOOK("onelook", "https://www.onelook.com/?w=%s"),
    WIKTIONARY("wiktionary", "https://en.wiktionary.org/wiki/%s"),
    GOOGLE_DEFINE("google_define", "https://www.google.com/search?q=define+%s"),
    MERRIAM_WEBSTER("merriam_webster", "https://www.merriam-webster.com/dictionary/%s"),
    CUSTOM("custom", null);

    companion object {
        fun fromId(id: String): DictionarySource =
            entries.find { it.id == id } ?: SYSTEM_DEFAULT
    }
}

fun String.toDictionarySource(): DictionarySource {
    return DictionarySource.fromId(this)
}
