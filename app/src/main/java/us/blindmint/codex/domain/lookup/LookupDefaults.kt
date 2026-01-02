/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.lookup

/**
 * Default web search engines and dictionaries for lookups.
 */
object LookupDefaults {

    /**
     * Curated list of web search engines.
     */
    val searchEngines = listOf(
        WebSearchEngine(
            id = "google",
            name = "Google",
            urlTemplate = "https://www.google.com/search?q=%s"
        ),
        WebSearchEngine(
            id = "duckduckgo",
            name = "DuckDuckGo",
            urlTemplate = "https://duckduckgo.com/?q=%s"
        ),
        WebSearchEngine(
            id = "startpage",
            name = "StartPage",
            urlTemplate = "https://www.startpage.com/sp/search?query=%s"
        )
    )

    /**
     * Curated list of web dictionaries.
     */
    val dictionaries = listOf(
        WebDictionary(
            id = "oxford",
            name = "Oxford",
            urlTemplate = "https://www.oxfordlearnersdictionaries.com/definition/english/%s"
        ),
        WebDictionary(
            id = "cambridge",
            name = "Cambridge",
            urlTemplate = "https://dictionary.cambridge.org/dictionary/english/%s"
        ),
        WebDictionary(
            id = "merriam_webster",
            name = "Merriam-Webster",
            urlTemplate = "https://www.merriam-webster.com/dictionary/%s"
        ),
        WebDictionary(
            id = "dictionary_com",
            name = "Dictionary.com",
            urlTemplate = "https://www.dictionary.com/browse/%s"
        ),
        WebDictionary(
            id = "grokipedia",
            name = "Grokipedia",
            urlTemplate = "https://grokipedia.com/search?q=%s"
        ),
        WebDictionary(
            id = "wiktionary",
            name = "Wiktionary",
            urlTemplate = "https://en.wiktionary.org/wiki/%s"
        ),
        WebDictionary(
            id = "google_define",
            name = "Google Define",
            urlTemplate = "https://www.google.com/search?q=define+%s"
        )
    )
}
