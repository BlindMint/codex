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
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.parser.opf.OpfWriter
import us.blindmint.codex.domain.storage.CodexDirectoryManager
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
        } ?: return null

        // Resolve relative URL against source URL
        val resolvedUrl = try {
            java.net.URI(sourceUrl).resolve(acquisitionLink.href).toString()
        } catch (e: Exception) {
            if (acquisitionLink.href.startsWith("http")) {
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

        try {
            // Parse the file
            val cachedFile = CachedFile(
                context = application,
                uri = android.net.Uri.fromFile(tempFile),
                builder = CachedFileCompat.build(
                    name = tempFile.name,
                    path = tempFile.absolutePath,
                    isDirectory = false
                )
            )

            val parsedBook = fileParser.parse(cachedFile) ?: return null

            // Create book folder in Codex downloads directory
            val uuid = extractUuid(opdsEntry) ?: UUID.randomUUID().toString().take(8)
            val title = sanitizeFilename(opdsEntry.title.trim())
            val folderName = "${uuid}_$title"

            val bookFolder = codexDirectoryManager.createBookFolder(folderName)
            if (bookFolder == null) {
                Log.e(TAG, "Failed to create book folder")
                return null
            }

            // Generate book filename
            val author = opdsEntry.author?.trim() ?: "Unknown"
            val bookFilename = sanitizeFilename("$title - $author$extension")

            // Save book file to folder
            val bookFile = bookFolder.createFile("application/octet-stream", bookFilename)
            if (bookFile == null) {
                Log.e(TAG, "Failed to create book file")
                return null
            }

            application.contentResolver.openOutputStream(bookFile.uri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Saved book to: ${bookFile.uri}")

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