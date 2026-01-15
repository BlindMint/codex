/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.opf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import us.blindmint.codex.domain.opf.OpfMetadata
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Parser for standalone OPF (Open Packaging Format) metadata files.
 * Used for Calibre-style metadata where .opf files exist alongside book files.
 *
 * Supports:
 * - Dublin Core elements (dc:title, dc:creator, dc:description, etc.)
 * - Calibre-specific metadata (calibre:series, calibre:series_index)
 * - Standard OPF identifiers (urn:uuid, urn:isbn)
 * - Cover image path resolution from manifest
 */
class OpfParser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "OpfParser"

        // Date formats commonly used in OPF files
        private val DATE_FORMATS = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US),
            SimpleDateFormat("yyyy", Locale.US)
        )
    }

    /**
     * Parse OPF metadata from a DocumentFile.
     */
    suspend fun parse(opfFile: DocumentFile): OpfMetadata? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(opfFile.uri)
            inputStream?.use { parseFromInputStream(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OPF file: ${opfFile.name}", e)
            null
        }
    }

    /**
     * Parse OPF metadata from an InputStream.
     */
    suspend fun parseFromInputStream(inputStream: InputStream): OpfMetadata? = withContext(Dispatchers.IO) {
        try {
            val content = inputStream.bufferedReader().use { it.readText() }
            parseFromString(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OPF from stream", e)
            null
        }
    }

    /**
     * Parse OPF metadata from a string.
     */
    fun parseFromString(content: String): OpfMetadata? {
        return try {
            val document = Jsoup.parse(content, "", Parser.xmlParser())
            extractMetadata(document)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OPF content", e)
            null
        }
    }

    private fun extractMetadata(document: Document): OpfMetadata {
        // Dublin Core metadata
        val title = document.selectFirst("metadata dc\\:title, metadata title")?.text()?.trim()
        val author = document.selectFirst("metadata dc\\:creator, metadata creator")?.text()?.trim()
        val description = document.selectFirst("metadata dc\\:description, metadata description")?.text()?.let {
            Jsoup.parse(it).text().trim().ifBlank { null }
        }
        val language = document.selectFirst("metadata dc\\:language, metadata language")?.text()?.trim()
        val publisher = document.selectFirst("metadata dc\\:publisher, metadata publisher")?.text()?.trim()

        // Publication date
        val dateStr = document.selectFirst("metadata dc\\:date, metadata date")?.text()?.trim()
        val publicationDate = parseDateToEpoch(dateStr)

        // Calibre-specific series metadata
        val series = document.selectFirst("metadata meta[name=calibre:series]")?.attr("content")?.trim()
            ?: document.selectFirst("metadata calibre\\:series")?.text()?.trim()

        val seriesIndexStr = document.selectFirst("metadata meta[name=calibre:series_index]")?.attr("content")?.trim()
            ?: document.selectFirst("metadata calibre\\:series_index")?.text()?.trim()
        val seriesIndex = seriesIndexStr?.toDoubleOrNull()?.toInt()

        // Identifiers (UUID, ISBN)
        val identifiers = document.select("metadata dc\\:identifier, metadata identifier")
        var uuid: String? = null
        var isbn: String? = null

        for (identifier in identifiers) {
            val text = identifier.text().trim()
            val scheme = identifier.attr("opf:scheme")?.lowercase() ?: ""
            val id = identifier.attr("id")?.lowercase() ?: ""

            when {
                text.startsWith("urn:uuid:", ignoreCase = true) -> {
                    uuid = text.removePrefix("urn:uuid:").removePrefix("URN:UUID:")
                }
                text.startsWith("urn:isbn:", ignoreCase = true) -> {
                    isbn = text.removePrefix("urn:isbn:").removePrefix("URN:ISBN:")
                }
                scheme == "uuid" || id.contains("uuid") -> {
                    uuid = text
                }
                scheme == "isbn" || id.contains("isbn") -> {
                    isbn = text
                }
                // Fallback: Check if it looks like a UUID
                text.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) -> {
                    uuid = uuid ?: text
                }
                // Check if it looks like an ISBN
                text.replace("-", "").matches(Regex("\\d{10}|\\d{13}")) -> {
                    isbn = isbn ?: text.replace("-", "")
                }
            }
        }

        // Tags/subjects
        val tags = document.select("metadata dc\\:subject, metadata subject")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        // Cover path from manifest
        val coverPath = extractCoverPath(document)

        return OpfMetadata(
            title = title,
            author = author,
            description = description,
            series = series,
            seriesIndex = seriesIndex,
            uuid = uuid,
            isbn = isbn,
            language = language,
            publisher = publisher,
            publicationDate = publicationDate,
            coverPath = coverPath,
            tags = tags
        )
    }

    private fun extractCoverPath(document: Document): String? {
        // Method 1: meta[name=cover] pointing to manifest item
        val coverItemId = document.selectFirst("metadata meta[name=cover]")?.attr("content")
        if (!coverItemId.isNullOrBlank()) {
            val coverHref = document.selectFirst("manifest item[id=$coverItemId]")?.attr("href")
            if (!coverHref.isNullOrBlank()) {
                return coverHref
            }
        }

        // Method 2: Look for item with id containing "cover" and image media type
        val coverItem = document.select("manifest item[media-type^=image]")
            .firstOrNull { item ->
                val id = item.attr("id").lowercase()
                val href = item.attr("href").lowercase()
                id.contains("cover") || href.contains("cover")
            }
        if (coverItem != null) {
            return coverItem.attr("href")
        }

        // Method 3: Calibre-style cover reference
        val calibreCover = document.selectFirst("guide reference[type=cover]")?.attr("href")
        if (!calibreCover.isNullOrBlank()) {
            return calibreCover
        }

        return null
    }

    private fun parseDateToEpoch(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null

        for (format in DATE_FORMATS) {
            try {
                return format.parse(dateStr)?.time
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    /**
     * Load cover image bitmap from the folder containing the OPF file.
     */
    suspend fun loadCoverImage(opfFolder: DocumentFile, coverPath: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (coverPath.isNullOrBlank()) return@withContext null

        try {
            // Find the cover file in the folder
            val coverFileName = coverPath.substringAfterLast("/")
            val coverFile = opfFolder.listFiles().find { file ->
                file.name.equals(coverFileName, ignoreCase = true) ||
                file.name.equals(coverPath, ignoreCase = true)
            }

            if (coverFile != null) {
                context.contentResolver.openInputStream(coverFile.uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                // Try common cover names as fallback
                val commonCoverNames = listOf("cover.jpg", "cover.jpeg", "cover.png", "cover.gif")
                for (name in commonCoverNames) {
                    val file = opfFolder.findFile(name)
                    if (file != null) {
                        return@withContext context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cover image: $coverPath", e)
            null
        }
    }
}
