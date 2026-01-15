/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.opf

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.ui.UIText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Writer for generating OPF (Open Packaging Format) metadata files.
 * Creates Calibre-compatible OPF files when downloading books from OPDS sources.
 */
class OpfWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "OpfWriter"
        private const val OPF_FILENAME = "metadata.opf"

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    /**
     * Generate OPF content from a Book and optional OPDS entry.
     */
    fun generateOpfContent(book: Book, opdsEntry: OpdsEntry? = null): String {
        val title = escapeXml(book.title)
        val author = escapeXml(getAuthorString(book.author))
        val description = escapeXml(book.description ?: book.summary ?: "")
        val uuid = book.uuid ?: opdsEntry?.identifiers?.find { it.startsWith("urn:uuid:") }
            ?.removePrefix("urn:uuid:") ?: UUID.randomUUID().toString()
        val isbn = book.isbn ?: opdsEntry?.identifiers?.find { it.startsWith("urn:isbn:") }
            ?.removePrefix("urn:isbn:") ?: ""
        val language = escapeXml(book.language ?: opdsEntry?.language ?: "")
        val publisher = escapeXml(book.publisher ?: opdsEntry?.publisher ?: "")
        val series = escapeXml(book.seriesName ?: opdsEntry?.series ?: "")
        val seriesIndex = book.seriesIndex ?: opdsEntry?.seriesIndex ?: 1
        val tags = (book.tags.takeIf { it.isNotEmpty() } ?: opdsEntry?.categories ?: emptyList())
        val publicationDate = formatDate(book.publicationDate) ?: parseOpdsDate(opdsEntry?.published) ?: ""

        return buildString {
            appendLine("""<?xml version='1.0' encoding='utf-8'?>""")
            appendLine("""<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="uuid_id" version="2.0">""")
            appendLine("""    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf" xmlns:calibre="http://calibre.kovidgoyal.net/2009/metadata">""")

            // Title
            appendLine("""        <dc:title>$title</dc:title>""")

            // Author
            if (author.isNotBlank()) {
                appendLine("""        <dc:creator opf:role="aut">$author</dc:creator>""")
            }

            // Description
            if (description.isNotBlank()) {
                appendLine("""        <dc:description>$description</dc:description>""")
            }

            // UUID identifier
            appendLine("""        <dc:identifier id="uuid_id" opf:scheme="uuid">urn:uuid:$uuid</dc:identifier>""")

            // ISBN identifier
            if (isbn.isNotBlank()) {
                appendLine("""        <dc:identifier opf:scheme="isbn">urn:isbn:$isbn</dc:identifier>""")
            }

            // Language
            if (language.isNotBlank()) {
                appendLine("""        <dc:language>$language</dc:language>""")
            }

            // Publisher
            if (publisher.isNotBlank()) {
                appendLine("""        <dc:publisher>$publisher</dc:publisher>""")
            }

            // Publication date
            if (publicationDate.isNotBlank()) {
                appendLine("""        <dc:date>$publicationDate</dc:date>""")
            }

            // Series (Calibre format)
            if (series.isNotBlank()) {
                appendLine("""        <meta name="calibre:series" content="$series"/>""")
                appendLine("""        <meta name="calibre:series_index" content="$seriesIndex"/>""")
            }

            // Tags/subjects
            for (tag in tags) {
                if (tag.isNotBlank()) {
                    appendLine("""        <dc:subject>${escapeXml(tag)}</dc:subject>""")
                }
            }

            // Cover reference (if cover.jpg exists alongside the book)
            appendLine("""        <meta name="cover" content="cover"/>""")

            appendLine("""    </metadata>""")

            // Manifest (cover image reference)
            appendLine("""    <manifest>""")
            appendLine("""        <item id="cover" href="cover.jpg" media-type="image/jpeg"/>""")
            appendLine("""    </manifest>""")

            // Guide (cover reference)
            appendLine("""    <guide>""")
            appendLine("""        <reference type="cover" href="cover.jpg" title="Cover"/>""")
            appendLine("""    </guide>""")

            appendLine("""</package>""")
        }
    }

    /**
     * Write an OPF file to the specified folder.
     */
    suspend fun writeOpfFile(
        parentDir: DocumentFile,
        book: Book,
        opdsEntry: OpdsEntry? = null
    ): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val opfContent = generateOpfContent(book, opdsEntry)

            // Check if OPF file already exists
            var opfFile = parentDir.findFile(OPF_FILENAME)
            if (opfFile != null) {
                // Delete existing file to replace it
                opfFile.delete()
            }

            // Create new OPF file
            opfFile = parentDir.createFile("application/oebps-package+xml", OPF_FILENAME)
            if (opfFile == null) {
                Log.e(TAG, "Failed to create OPF file")
                return@withContext null
            }

            // Write content
            context.contentResolver.openOutputStream(opfFile.uri)?.use { outputStream ->
                outputStream.write(opfContent.toByteArray(Charsets.UTF_8))
            }

            Log.i(TAG, "Created OPF file: ${opfFile.uri}")
            opfFile
        } catch (e: Exception) {
            Log.e(TAG, "Error writing OPF file", e)
            null
        }
    }

    /**
     * Convert author UIText to plain string.
     */
    private fun getAuthorString(author: UIText): String {
        return when (author) {
            is UIText.StringValue -> author.value
            is UIText.StringResource -> "" // Can't resolve resource without context in this context
        }
    }

    /**
     * Format epoch millis to ISO date string.
     */
    private fun formatDate(epochMillis: Long?): String? {
        if (epochMillis == null || epochMillis == 0L) return null
        return try {
            DATE_FORMAT.format(Date(epochMillis))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse OPDS date string (ISO format) to formatted date.
     */
    private fun parseOpdsDate(dateStr: String?): String? {
        if (dateStr.isNullOrBlank()) return null
        // OPDS dates are typically in ISO format, just take the date part
        return dateStr.take(10).takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
    }

    /**
     * Escape special XML characters.
     */
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
