/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.opds

import android.app.Application
import android.net.Uri as AndroidUri
import us.blindmint.codex.data.local.data_store.DataStore
import us.blindmint.codex.data.mapper.opds.OpdsMetadataMapper
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.repository.OpdsRepository
import us.blindmint.codex.domain.util.CoverImage
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.opf.OpfWriter
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import us.blindmint.codex.domain.ui.UIText
import java.util.UUID

/**
 * Result of attempting to import an OPDS book.
 */
sealed class ImportOpdsResult {
    data class Success(val bookWithCover: BookWithCover) : ImportOpdsResult()
    data class Error(val message: String) : ImportOpdsResult()
    object CodexFolderNotConfigured : ImportOpdsResult()
}

class ImportOpdsBookUseCase @Inject constructor(
    private val application: Application,
    private val opdsRepository: OpdsRepository,
    private val fileParser: FileParser,
    private val bookRepository: BookRepository,
    private val opdsMetadataMapper: OpdsMetadataMapper,
    private val dataStore: DataStore,
    private val codexDirectoryManager: CodexDirectoryManager,
    private val opfWriter: OpfWriter
) {

    companion object {
        private const val TAG = "ImportOpdsBookUseCase"
    }

    /**
     * Sanitizes a filename by removing or replacing invalid characters for Android filesystem.
     * Also handles UTF-8 characters and limits the length.
     */
    private fun sanitizeFilename(filename: String): String {
        // Replace invalid filesystem characters with underscores
        val sanitized = filename
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("[\\x00-\\x1F]"), "") // Remove control characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()

        // Ensure the filename isn't empty after sanitization
        val result = sanitized.ifBlank { "downloaded_book" }

        // Limit length (Android max is 255, but leave room for path)
        return if (result.length > 200) {
            val extension = result.substringAfterLast(".", "")
            val baseName = result.substringBeforeLast(".")
            val truncatedBase = baseName.take(200 - extension.length - 1)
            if (extension.isNotBlank()) "$truncatedBase.$extension" else truncatedBase
        } else {
            result
        }
    }

    private suspend fun copyFileToDownloadFolder(tempFile: File, downloadUri: AndroidUri, context: android.content.Context, filename: String): String? {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, downloadUri)
            if (documentFile != null && documentFile.canWrite()) {
                val newFile = documentFile.createFile("application/octet-stream", filename)
                if (newFile != null) {
                    context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    // Return the SAF URI as string
                    newFile.uri.toString()
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Determines the file extension from MIME type, URL, or suggested filename.
     */
    private fun determineExtension(mimeType: String?, href: String?, suggestedFilename: String?): String {
        // First, try to get extension from suggested filename
        suggestedFilename?.let {
            val ext = it.substringAfterLast(".", "").lowercase()
            if (ext.isNotBlank() && ext.length <= 5) return ".$ext"
        }

        // Second, try to extract from URL path
        href?.let {
            val urlPath = try { java.net.URI(it).path } catch (e: Exception) { it }
            val ext = urlPath.substringAfterLast(".", "").substringBefore("?").lowercase()
            if (ext.isNotBlank() && ext.length <= 5 && !ext.contains("/")) return ".$ext"
        }

        // Third, determine from MIME type
        return when {
            mimeType == null -> ".epub" // Default fallback
            mimeType.contains("epub") -> ".epub"
            mimeType.contains("pdf") -> ".pdf"
            mimeType.contains("fb2") || mimeType.contains("fictionbook") -> ".fb2"
            mimeType.contains("mobi") || mimeType.contains("x-mobipocket") -> ".mobi"
            mimeType.contains("azw") -> ".azw"
            mimeType.contains("cbz") -> ".cbz"
            mimeType.contains("cbr") -> ".cbr"
            mimeType.contains("cb7") -> ".cb7"
            mimeType.contains("html") -> ".html"
            mimeType.contains("plain") || mimeType.contains("text") -> ".txt"
            mimeType.contains("zip") -> ".epub" // application/zip often used for epub
            else -> ".epub" // Default fallback
        }
    }

    /**
     * Import a book from OPDS, returning a result that indicates success, error, or missing configuration.
     */
    suspend fun invokeWithResult(
        opdsEntry: OpdsEntry,
        sourceUrl: String,
        username: String? = null,
        password: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): ImportOpdsResult = withContext(Dispatchers.IO) {
        // Check if Codex folder is configured
        if (!codexDirectoryManager.isConfigured()) {
            return@withContext ImportOpdsResult.CodexFolderNotConfigured
        }

        try {
            val result = performImport(opdsEntry, sourceUrl, username, password, onProgress)
            if (result != null) {
                ImportOpdsResult.Success(result)
            } else {
                ImportOpdsResult.Error("Failed to import book")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing OPDS book", e)
            ImportOpdsResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Legacy invoke method for backward compatibility.
     * Returns null if Codex folder is not configured.
     */
    suspend operator fun invoke(
        opdsEntry: OpdsEntry,
        sourceUrl: String,
        username: String? = null,
        password: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): BookWithCover? = withContext(Dispatchers.IO) {
        when (val result = invokeWithResult(opdsEntry, sourceUrl, username, password, onProgress)) {
            is ImportOpdsResult.Success -> result.bookWithCover
            else -> null
        }
    }

    /**
     * Parse EPUB file directly without CachedFile complications
     */
    private suspend fun parseEpubDirectly(file: File): BookWithCover? {
        android.util.Log.d("OPDS_DEBUG", "Parsing EPUB directly: ${file.absolutePath}")
        return try {
            withContext(Dispatchers.IO) {
                ZipFile(file).use { zip ->
                    val opfEntry = zip.entries().asSequence().find { entry ->
                        entry.name.endsWith(".opf", ignoreCase = true)
                    } ?: run {
                        android.util.Log.e("OPDS_DEBUG", "No OPF file found in EPUB")
                        return@withContext null
                    }

                    val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                    val document = Jsoup.parse(opfContent)

                    val title = document.select("metadata > dc|title").text().trim().run {
                        ifBlank { file.name.substringBeforeLast(".").trim() }
                    }

                    val author = document.select("metadata > dc|creator").text().trim().run {
                        if (isBlank()) UIText.StringResource(R.string.unknown_author) else UIText.StringValue(this)
                    }

                    val description = Jsoup.parse(document.select("metadata > dc|description").text()).text().run {
                        ifBlank { null }
                    }

                    android.util.Log.d("OPDS_DEBUG", "Parsed EPUB: title='$title', author='$author'")

                    BookWithCover(
                        book = Book(
                            title = title,
                            author = author,
                            description = description,
                            scrollIndex = 0,
                            scrollOffset = 0,
                            progress = 0f,
                            filePath = file.absolutePath,
                            lastOpened = null,
                            category = Category.entries[0],
                            coverImage = null
                        ),
                        coverImage = extractCoverImageFromEpub(file)
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OPDS_DEBUG", "Failed to parse EPUB directly", e)
            null
        }
    }

    private fun extractCoverImageFromEpub(file: File): Bitmap? {
        return try {
            ZipFile(file).use { zip ->
                // Find cover image path from OPF
                val opfEntry = zip.entries().asSequence().find { it.name.endsWith(".opf", ignoreCase = true) } ?: return null
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                val document = Jsoup.parse(opfContent)

                val coverImagePath = document.select("metadata > meta[name=cover]").attr("content").run {
                    if (isNotBlank()) {
                        document.select("manifest > item[id=$this]").attr("href")
                    } else {
                        document.select("manifest > item[media-type*=image]").firstOrNull()?.attr("href")
                    }
                } ?: return null

                // Find and extract the image
                val imageEntry = zip.entries().asSequence().find { it.name.endsWith(coverImagePath) } ?: return null
                val imageBytes = zip.getInputStream(imageEntry).readBytes()
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }
        } catch (e: Exception) {
            android.util.Log.w("OPDS_DEBUG", "Failed to extract cover image", e)
            null
        }
    }

    private suspend fun performImport(
        opdsEntry: OpdsEntry,
        sourceUrl: String,
        username: String?,
        password: String?,
        onProgress: ((Float) -> Unit)?
    ): BookWithCover? {
        // Find acquisition link - try multiple rel patterns
        val acquisitionLink = opdsEntry.links.firstOrNull {
            it.rel == "http://opds-spec.org/acquisition" ||
            it.rel?.startsWith("http://opds-spec.org/acquisition/") == true
        }
        if (acquisitionLink == null) {
            android.util.Log.d("OPDS_DEBUG", "No acquisition link found for book: ${opdsEntry.title}")
            android.util.Log.d("OPDS_DEBUG", "Available links:")
            opdsEntry.links.forEach { link ->
                android.util.Log.d("OPDS_DEBUG", "  Link: rel='${link.rel}', type='${link.type}', href='${link.href}'")
            }
            return null
        }
        android.util.Log.d("OPDS_DEBUG", "Found acquisition link: rel='${acquisitionLink.rel}', type='${acquisitionLink.type}', href='${acquisitionLink.href}'")

        // Resolve relative URL against source URL
        val resolvedUrl = try {
            val resolved = java.net.URI(sourceUrl).resolve(acquisitionLink.href).toString()
            android.util.Log.d("OPDS_DEBUG", "Resolved acquisition URL: $resolved (from href: ${acquisitionLink.href}, source: $sourceUrl)")
            resolved
        } catch (e: Exception) {
            if (acquisitionLink.href.startsWith("http")) {
                android.util.Log.d("OPDS_DEBUG", "Using absolute href URL: ${acquisitionLink.href}")
                acquisitionLink.href
            } else {
                throw Exception("Cannot resolve relative URL: ${acquisitionLink.href}")
            }
        }

        // Download the book
        val (bookBytes, suggestedFilename) = opdsRepository.downloadBook(resolvedUrl, username, password, onProgress)

        // Determine proper file extension
        val extension = determineExtension(acquisitionLink.type, acquisitionLink.href, suggestedFilename)

        // Save to temp file
        val tempFile = File(application.cacheDir, "temp_book_${System.currentTimeMillis()}$extension")
        tempFile.writeBytes(bookBytes)
        android.util.Log.d("OPDS_DEBUG", "Saved ${bookBytes.size} bytes to temp file: ${tempFile.absolutePath}")

        // Debug: Check if file looks like a ZIP/EPUB
        try {
            val firstBytes = bookBytes.take(4).toByteArray()
            val hexString = firstBytes.joinToString("") { "%02x".format(it) }
            android.util.Log.d("OPDS_DEBUG", "First 4 bytes of file: $hexString (should be 504b for ZIP)")
            if (bookBytes.size > 100) {
                val header = String(bookBytes.take(100).toByteArray(), Charsets.UTF_8)
                if (header.contains("<html", ignoreCase = true)) {
                    android.util.Log.e("OPDS_DEBUG", "Downloaded file appears to be HTML, not a book file!")
                    android.util.Log.d("OPDS_DEBUG", "HTML content preview: ${header.take(200)}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OPDS_DEBUG", "Error checking file header", e)
        }

        try {
            // Parse the file using a direct File-based approach
            android.util.Log.d("OPDS_DEBUG", "Attempting to parse file: ${tempFile.name} (extension: $extension)")

            // Create a simple wrapper that provides the raw file
            val simpleCachedFile = object {
                val rawFile = tempFile
                val name = tempFile.name
                val path = tempFile.absolutePath
            }

            // For EPUB files, parse directly
            val parsedBook = if (extension == ".epub") {
                parseEpubDirectly(tempFile)
            } else {
                // For other formats, use the existing parser with a proper CachedFile
                val cachedFile = CachedFile(
                    context = application,
                    uri = android.net.Uri.fromFile(tempFile),
                    builder = CachedFileCompat.build(
                        name = tempFile.name,
                        path = tempFile.absolutePath,
                        isDirectory = false
                    )
                )
                fileParser.parse(cachedFile)
            }
            if (parsedBook == null) {
                android.util.Log.e("OPDS_DEBUG", "File parser returned null for ${tempFile.name}")
                return null
            }
            android.util.Log.d("OPDS_DEBUG", "Successfully parsed book: ${parsedBook.book.title}")

            // Create book folder in Codex downloads directory
            val uuid = extractUuid(opdsEntry) ?: UUID.randomUUID().toString().take(8)
            val title = sanitizeFilename(opdsEntry.title.trim())
            val folderName = "${uuid}_$title"
            android.util.Log.d("OPDS_DEBUG", "Creating book folder: $folderName")

            val bookFolder = codexDirectoryManager.createBookFolder(folderName)
            if (bookFolder == null) {
                android.util.Log.e("OPDS_DEBUG", "Failed to create book folder: $folderName")
                return null
            }
            android.util.Log.d("OPDS_DEBUG", "Successfully created book folder: ${bookFolder.uri}")

            // Generate book filename
            val author = opdsEntry.author?.trim() ?: "Unknown"
            val bookFilename = sanitizeFilename("$title - $author$extension")

            // Save book file to folder
            android.util.Log.d("OPDS_DEBUG", "Creating book file: $bookFilename")
            val bookFile = bookFolder.createFile("application/octet-stream", bookFilename)
            if (bookFile == null) {
                android.util.Log.e("OPDS_DEBUG", "Failed to create book file: $bookFilename")
                return null
            }

            try {
                application.contentResolver.openOutputStream(bookFile.uri)?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.i("OPDS_DEBUG", "Saved book to: ${bookFile.uri}")
            } catch (e: Exception) {
                android.util.Log.e("OPDS_DEBUG", "Failed to copy book file", e)
                return null
            }

            // Download and save cover image
            val coverUrl = opdsEntry.coverUrl ?: opdsEntry.links.firstOrNull {
                it.rel?.contains("image") == true || it.type?.startsWith("image/") == true
            }?.href

            var coverBitmap: Bitmap? = parsedBook.coverImage
            if (coverUrl != null) {
                try {
                    val resolvedCoverUrl = java.net.URI(sourceUrl).resolve(coverUrl).toString()
                    val coverBytes = opdsRepository.downloadCover(resolvedCoverUrl, username, password)
                    if (coverBytes != null) {
                        // Save cover.jpg
                        val coverFile = bookFolder.createFile("image/jpeg", "cover.jpg")
                        if (coverFile != null) {
                            application.contentResolver.openOutputStream(coverFile.uri)?.use { output ->
                                output.write(coverBytes)
                            }
                            Log.i(TAG, "Saved cover to: ${coverFile.uri}")

                            // Decode bitmap for display
                            coverBitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to download cover", e)
                }
            }

            // Apply OPDS metadata
            val bookWithPath = parsedBook.book.copy(filePath = bookFile.uri.toString())
            val bookWithMetadata = opdsMetadataMapper.applyOpdsMetadataToBook(bookWithPath, opdsEntry)

            // Generate and save metadata.opf
            opfWriter.writeOpfFile(bookFolder, bookWithMetadata, opdsEntry)

            return BookWithCover(
                book = bookWithMetadata,
                coverImage = coverBitmap ?: parsedBook.coverImage
            )
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Extract UUID from OPDS entry identifiers.
     */
    private fun extractUuid(opdsEntry: OpdsEntry): String? {
        for (identifier in opdsEntry.identifiers) {
            if (identifier.startsWith("urn:uuid:", ignoreCase = true)) {
                return identifier.removePrefix("urn:uuid:").removePrefix("URN:UUID:").take(8)
            }
        }
        // Fallback: use entry ID if it looks like a UUID
        if (opdsEntry.id.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-.*"))) {
            return opdsEntry.id.take(8)
        }
        return null
    }
}