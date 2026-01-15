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

        Log.d(AUTO_IMPORT, "Codex directory is configured, getting downloads directory")
        val downloadsDir = codexDirectoryManager.getDownloadsDir()
        if (downloadsDir == null) {
            Log.w(AUTO_IMPORT, "Downloads directory not available")
            return@withContext 0
        }

        Log.d(AUTO_IMPORT, "Downloads directory available: ${downloadsDir.uri}")
        val supportedExtensions = provideExtensions()
        Log.d(AUTO_IMPORT, "Supported extensions: $supportedExtensions")

        val existingPaths = bookRepository.getBooks("").map { it.filePath }
        Log.d(AUTO_IMPORT, "Found ${existingPaths.size} existing books in library")

        // Double-check that we have access to the directory
        val canRead = downloadsDir.canRead()
        val canWrite = downloadsDir.canWrite()
        Log.d(AUTO_IMPORT, "Downloads directory permissions - canRead: $canRead, canWrite: $canWrite")

        var importedCount = 0

        // Get all book folders from downloads directory
        Log.d(AUTO_IMPORT, "Listing files in downloads directory")
        val allFiles = try {
            downloadsDir.listFiles()
        } catch (e: Exception) {
            Log.e(AUTO_IMPORT, "Failed to list files in downloads directory", e)
            return@withContext 0
        }
        Log.d(AUTO_IMPORT, "Downloads directory contains ${allFiles?.size ?: 0} items")

        if (allFiles != null) {
            allFiles.forEach { file ->
                Log.d(AUTO_IMPORT, "Found item: ${file.name} (isDirectory: ${file.isDirectory}, isFile: ${file.isFile}, canRead: ${file.canRead()})")
            }
        }

        val bookFolders = allFiles?.filter { it.isDirectory }?.toTypedArray() ?: emptyArray()
        Log.i(AUTO_IMPORT, "Found ${bookFolders.size} book folders to process")

        if (bookFolders.isEmpty()) {
            Log.i(AUTO_IMPORT, "No book folders found to process")
            return@withContext 0
        }

        bookFolders.forEachIndexed { index, folder ->
            Log.d(AUTO_IMPORT, "Processing folder ${index + 1}/${bookFolders.size}: ${folder.name} (${folder.uri})")
            onProgress(AutoImportProgress(index + 1, bookFolders.size, folder.name ?: "Unknown"))

            try {
                // Check if folder can be accessed
                if (!folder.canRead()) {
                    Log.w(AUTO_IMPORT, "Cannot read folder: ${folder.name}")
                    return@forEachIndexed
                }

                val importedBook = processBookFolder(folder, supportedExtensions, existingPaths)
                if (importedBook != null) {
                    importedCount++
                    Log.i(AUTO_IMPORT, "Auto-imported: ${folder.name}")
                } else {
                    Log.d(AUTO_IMPORT, "No book imported from folder: ${folder.name}")
                }
            } catch (e: Exception) {
                Log.e(AUTO_IMPORT, "Error processing folder ${folder.name}: ${e.message}", e)
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
        Log.d(AUTO_IMPORT, "Processing folder: ${folder.name} (${folder.uri})")

        val folderFiles = folder.listFiles()
        Log.d(AUTO_IMPORT, "Folder contains ${folderFiles?.size ?: 0} files")

        if (folderFiles == null || folderFiles.isEmpty()) {
            Log.d(AUTO_IMPORT, "No files found in folder: ${folder.name}")
            return null
        }

        // Log all files in the folder
        folderFiles.forEach { file ->
            Log.d(AUTO_IMPORT, "File in folder: ${file.name} (isDirectory: ${file.isDirectory}, isFile: ${file.isFile})")
        }

        // Look for OPF metadata file
        val opfFile = folderFiles.firstOrNull { it.name?.endsWith(".opf", ignoreCase = true) == true }
        Log.d(AUTO_IMPORT, "OPF file found: ${opfFile?.name}")

        // Look for book files
        val bookFiles = folderFiles.filter { file ->
            file.isFile && supportedExtensions.any { ext ->
                file.name?.endsWith(ext, ignoreCase = true) == true
            }
        }

        Log.d(AUTO_IMPORT, "Found ${bookFiles.size} potential book files: ${bookFiles.map { it.name }}")

        if (bookFiles.isEmpty()) {
            Log.d(AUTO_IMPORT, "No supported book files found in folder: ${folder.name}")
            return null
        }

        // Use the first book file found
        val bookFile = bookFiles.first()
        Log.d(AUTO_IMPORT, "Processing book file: ${bookFile.name} (${bookFile.uri})")

        // Check if already imported
        val bookUriString = bookFile.uri.toString()
        val alreadyExists = existingPaths.any { existingPath ->
            existingPath.equals(bookUriString, ignoreCase = true)
        }

        Log.d(AUTO_IMPORT, "Book URI: $bookUriString")
        Log.d(AUTO_IMPORT, "Already exists in library: $alreadyExists")

        if (alreadyExists) {
            Log.d(AUTO_IMPORT, "Book already exists, skipping: ${bookFile.name}")
            return null
        }

        try {
            // Create CachedFile for parsing
            Log.d(AUTO_IMPORT, "Creating CachedFile for parsing")
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
            Log.d(AUTO_IMPORT, "Parsing book file with FileParser")
            val parsedBook = fileParser.parse(cachedFile)
            if (parsedBook == null) {
                Log.w(AUTO_IMPORT, "Failed to parse book file: ${bookFile.name}")
                return null
            }

            Log.d(AUTO_IMPORT, "Successfully parsed book: ${parsedBook.book.title}")

            // If OPF file exists, merge metadata
            var finalBook = parsedBook.book
            if (opfFile != null) {
                try {
                    Log.d(AUTO_IMPORT, "Parsing OPF metadata file")
                    val opfMetadata = opfParser.parse(opfFile)
                    if (opfMetadata != null) {
                        Log.d(AUTO_IMPORT, "Merging OPF metadata: ${opfMetadata.title}")
                        finalBook = mergeOpfMetadata(finalBook, opfMetadata)
                        Log.d(AUTO_IMPORT, "Merged OPF metadata for: ${bookFile.name}")
                    } else {
                        Log.d(AUTO_IMPORT, "OPF parsing returned null")
                    }
                } catch (e: Exception) {
                    Log.w(AUTO_IMPORT, "Failed to parse OPF file: ${opfFile.name}", e)
                }
            }

            // Mark as OPDS source and set category to READING (currently reading)
            finalBook = finalBook.copy(
                source = BookSource.OPDS,
                category = us.blindmint.codex.domain.library.category.Category.READING
            )
            Log.d(AUTO_IMPORT, "Marked book as OPDS source")

            // Create BookWithCover
            val bookWithCover = us.blindmint.codex.domain.library.book.BookWithCover(
                book = finalBook,
                coverImage = parsedBook.coverImage
            )

            // Insert into database
            Log.d(AUTO_IMPORT, "Inserting book into database")
            insertBook.execute(bookWithCover)

            Log.i(AUTO_IMPORT, "Successfully imported: ${bookFile.name}")
            return bookWithCover

        } catch (e: Exception) {
            Log.e(AUTO_IMPORT, "Error processing book file ${bookFile.name}: ${e.message}", e)
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