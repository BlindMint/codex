/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import android.app.Application
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.data.parser.opf.OpfParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.BookSource
import us.blindmint.codex.domain.opf.OpfMetadata
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.presentation.core.constants.provideExtensions
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val AUTO_IMPORT = "AUTO IMPORT"

data class AutoImportProgress(
    val current: Int,
    val total: Int,
    val currentFolder: String
)

class AutoImportCodexBooksUseCase @Inject constructor(
    private val application: Application,
    private val codexDirectoryManager: CodexDirectoryManager,
    private val fileParser: FileParser,
    private val opfParser: OpfParser,
    private val bookRepository: BookRepository,
    private val insertBook: InsertBook
) {

    suspend fun execute(
        onProgress: (AutoImportProgress) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        Log.i(AUTO_IMPORT, "Starting automatic import of existing books from Codex downloads folder")

        if (!codexDirectoryManager.isConfigured()) {
            Log.w(AUTO_IMPORT, "Codex directory not configured, skipping auto-import")
            return@withContext 0
        }

        val downloadsDir = codexDirectoryManager.getDownloadsDir()
        if (downloadsDir == null) {
            Log.w(AUTO_IMPORT, "Downloads directory not available")
            return@withContext 0
        }

        val supportedExtensions = provideExtensions()
        val existingPaths = bookRepository.getBooks("").map { it.filePath }
        var importedCount = 0

        // Get all book folders from downloads directory
        val bookFolders = downloadsDir.listFiles()?.filter { it.isDirectory }?.toTypedArray() ?: emptyArray()
        Log.i(AUTO_IMPORT, "Found ${bookFolders.size} book folders to process")

        bookFolders.forEachIndexed { index, folder ->
            onProgress(AutoImportProgress(index + 1, bookFolders.size, folder.name ?: "Unknown"))

            try {
                val importedBook = processBookFolder(folder, supportedExtensions, existingPaths)
                if (importedBook != null) {
                    importedCount++
                    Log.i(AUTO_IMPORT, "Auto-imported: ${folder.name}")
                }
            } catch (e: Exception) {
                Log.e(AUTO_IMPORT, "Error processing folder ${folder.name}: ${e.message}")
            }
        }

        Log.i(AUTO_IMPORT, "Auto-import completed. Imported $importedCount books.")
        importedCount
    }

    private suspend fun processBookFolder(
        folder: DocumentFile,
        supportedExtensions: List<String>,
        existingPaths: List<String>
    ): us.blindmint.codex.domain.library.book.BookWithCover? {
        val folderFiles = folder.listFiles() ?: return null

        // Look for OPF metadata file
        val opfFile = folderFiles.firstOrNull { it.name?.endsWith(".opf", ignoreCase = true) == true }

        // Look for book files
        val bookFiles = folderFiles.filter { file ->
            file.isFile && supportedExtensions.any { ext ->
                file.name?.endsWith(ext, ignoreCase = true) == true
            }
        }

        if (bookFiles.isEmpty()) {
            Log.d(AUTO_IMPORT, "No book files found in folder: ${folder.name}")
            return null
        }

        // Use the first book file found
        val bookFile = bookFiles.first()

        // Check if already imported
        val alreadyExists = existingPaths.any { existingPath ->
            existingPath.equals(bookFile.uri.toString(), ignoreCase = true)
        }

        if (alreadyExists) {
            Log.d(AUTO_IMPORT, "Book already exists: ${bookFile.name}")
            return null
        }

        try {
            // Create CachedFile for parsing
            val cachedFile = CachedFile(
                context = application,
                uri = bookFile.uri,
                builder = CachedFileCompat.build(
                    name = bookFile.name ?: "unknown",
                    path = bookFile.uri.toString(),
                    isDirectory = false
                )
            )

            // Parse the book file
            val parsedBook = fileParser.parse(cachedFile)
            if (parsedBook == null) {
                Log.w(AUTO_IMPORT, "Failed to parse book file: ${bookFile.name}")
                return null
            }

            // If OPF file exists, merge metadata
            var finalBook = parsedBook.book
            if (opfFile != null) {
                try {
                    val opfMetadata = opfParser.parse(opfFile)
                    if (opfMetadata != null) {
                        finalBook = mergeOpfMetadata(finalBook, opfMetadata)
                        Log.d(AUTO_IMPORT, "Merged OPF metadata for: ${bookFile.name}")
                    }
                } catch (e: Exception) {
                    Log.w(AUTO_IMPORT, "Failed to parse OPF file: ${opfFile.name}", e)
                }
            }

            // Mark as OPDS source
            finalBook = finalBook.copy(source = BookSource.OPDS)

            // Create BookWithCover
            val bookWithCover = us.blindmint.codex.domain.library.book.BookWithCover(
                book = finalBook,
                coverImage = parsedBook.coverImage
            )

            // Insert into database
            insertBook.execute(bookWithCover)

            Log.i(AUTO_IMPORT, "Successfully imported: ${bookFile.name}")
            return bookWithCover

        } catch (e: Exception) {
            Log.e(AUTO_IMPORT, "Error processing book file ${bookFile.name}: ${e.message}")
            return null
        }
    }

    private fun mergeOpfMetadata(book: us.blindmint.codex.domain.library.book.Book, opfMetadata: OpfMetadata): us.blindmint.codex.domain.library.book.Book {
        return book.copy(
            title = opfMetadata.title ?: book.title,
            author = UIText.StringValue(opfMetadata.author ?: book.author.getAsString() ?: "Unknown"),
            description = opfMetadata.description ?: book.description,
            tags = opfMetadata.tags.takeIf { it.isNotEmpty() } ?: book.tags,
            seriesName = opfMetadata.series ?: book.seriesName,
            seriesIndex = opfMetadata.seriesIndex ?: book.seriesIndex,
            publicationDate = opfMetadata.publicationDate ?: book.publicationDate,
            language = opfMetadata.language ?: book.language,
            publisher = opfMetadata.publisher ?: book.publisher,
            isbn = opfMetadata.isbn ?: book.isbn
        )
    }
}