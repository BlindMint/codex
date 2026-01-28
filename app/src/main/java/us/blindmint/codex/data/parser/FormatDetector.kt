/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

/**
 * Centralized format detection utility.
 *
 * Eliminates duplicate format detection logic across FileParserImpl and TextParserImpl.
 */
object FormatDetector {

    /**
     * Supported file formats with their associated extensions.
     */
    enum class Format(val extensions: List<String>) {
        PDF(listOf("pdf")),
        EPUB(listOf("epub")),
        FB2(listOf("fb2")),
        HTML(listOf("html", "htm")),
        TXT(listOf("txt", "md")),  // Markdown treated as text
        FODT(listOf("fodt")),
        COMIC(listOf("cbr", "cbz", "cb7")),
        UNKNOWN(emptyList())
    }

    /**
     * Detect the format of a file based on its extension.
     *
     * @param fileName The name of the file (with extension)
     * @return The detected Format, or UNKNOWN if not recognized
     */
    fun detect(fileName: String): Format {
        val extension = fileName.substringAfterLast('.').lowercase()
        return Format.entries.find { extension in it.extensions }
            ?: Format.UNKNOWN
    }

    /**
     * Check if a file name matches a specific format.
     *
     * @param fileName The name of the file (with extension)
     * @param format The format to check against
     * @return true if the file matches the format, false otherwise
     */
    fun isFormat(fileName: String, format: Format): Boolean {
        return detect(fileName) == format
    }
}
