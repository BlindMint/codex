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
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportOpdsBookUseCase @Inject constructor(
    private val application: Application,
    private val opdsRepository: OpdsRepository,
    private val fileParser: FileParser,
    private val bookRepository: BookRepository,
    private val opdsMetadataMapper: OpdsMetadataMapper,
    private val dataStore: DataStore
) {

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

    suspend operator fun invoke(
        opdsEntry: OpdsEntry,
        sourceUrl: String,
        username: String? = null,
        password: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): BookWithCover? = withContext(Dispatchers.IO) {
        // Find acquisition link
        val acquisitionLink = opdsEntry.links.firstOrNull { it.rel == "http://opds-spec.org/acquisition" }
            ?: return@withContext null

        // Resolve relative URL against source URL
        val resolvedUrl = try {
            java.net.URI(sourceUrl).resolve(acquisitionLink.href).toString()
        } catch (e: Exception) {
            // Fallback: if resolution fails, try to use the href directly if it's absolute
            if (acquisitionLink.href.startsWith("http")) {
                acquisitionLink.href
            } else {
                throw Exception("Cannot resolve relative URL: ${acquisitionLink.href}")
            }
        }

        // Download the book
        val (bookBytes, suggestedFilename) = opdsRepository.downloadBook(resolvedUrl, username, password, onProgress)

        // Save to temp file
        val tempFile = File(application.cacheDir, "temp_book_${System.currentTimeMillis()}")
        tempFile.writeBytes(bookBytes)

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

        val parsedBook = fileParser.parse(cachedFile) ?: return@withContext null

        // Move to permanent location
        val downloadUriString = dataStore.getNullableData(DataStoreConstants.OPDS_DOWNLOAD_URI)
        val safeFilename = suggestedFilename ?: "downloaded_book.epub"
        val filePath = if (downloadUriString != null) {
            val downloadUri = AndroidUri.parse(downloadUriString)
            val safPath = copyFileToDownloadFolder(tempFile, downloadUri, application, safeFilename)
            if (safPath != null) {
                safPath
            } else {
                // Fallback to external files dir if copy fails
                val booksDir = File(application.getExternalFilesDir(null), "books")
                booksDir.mkdirs()
                val permanentFile = File(booksDir, safeFilename)
                tempFile.copyTo(permanentFile, overwrite = true)
                permanentFile.absolutePath
            }
        } else {
            // No download folder set, use external files dir
            val booksDir = File(application.getExternalFilesDir(null), "books")
            booksDir.mkdirs()
            val permanentFile = File(booksDir, safeFilename)
            tempFile.copyTo(permanentFile, overwrite = true)
            permanentFile.absolutePath
        }

        tempFile.delete()

        // Update file path in book
        val bookWithPath = parsedBook.book.copy(filePath = filePath)

        // Apply OPDS metadata
        val bookWithMetadata = opdsMetadataMapper.applyOpdsMetadataToBook(bookWithPath, opdsEntry)

        BookWithCover(
            book = bookWithMetadata,
            coverImage = parsedBook.coverImage
        )
    }
}