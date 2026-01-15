/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.mapper.opds

import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookSource
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

class OpdsMetadataMapper @Inject constructor() {

    fun applyOpdsMetadataToBook(book: Book, opdsEntry: OpdsEntry): Book {
        return book.copy(
            title = opdsEntry.title,
            author = opdsEntry.author?.let { UIText.StringValue(it) } ?: book.author,
            description = opdsEntry.summary ?: book.description,
            tags = opdsEntry.categories.takeIf { it.isNotEmpty() } ?: book.tags,
            seriesName = opdsEntry.series ?: book.seriesName,
            seriesIndex = opdsEntry.seriesIndex ?: book.seriesIndex,
            publicationDate = opdsEntry.published?.let { parseDate(it) } ?: book.publicationDate,
            language = opdsEntry.language ?: book.language,
            publisher = opdsEntry.publisher ?: book.publisher,
            summary = opdsEntry.summary ?: book.summary,
            uuid = opdsEntry.identifiers.firstOrNull { it.startsWith("urn:uuid:") }?.substringAfter("urn:uuid:") ?: book.uuid,
            isbn = opdsEntry.identifiers.firstOrNull { it.startsWith("urn:isbn:") }?.substringAfter("urn:isbn:") ?: book.isbn,
            source = BookSource.OPDS,
            remoteUrl = opdsEntry.links.firstOrNull { it.rel == "http://opds-spec.org/acquisition" }?.href ?: book.remoteUrl
        )
    }

    private fun parseDate(dateString: String): Long? {
        // Simple ISO date parser, e.g., 2023-01-01T00:00:00Z
        return try {
            // For simplicity, assume format and parse
            // Use SimpleDateFormat or java.time
            java.time.Instant.parse(dateString).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}