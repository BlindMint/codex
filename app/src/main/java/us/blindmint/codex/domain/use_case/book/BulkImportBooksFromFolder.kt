/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import android.net.Uri
import android.util.Log
import us.blindmint.codex.data.repository.FileSystemRepositoryImpl
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.repository.FileSystemRepository
import us.blindmint.codex.presentation.core.constants.provideExtensions
import javax.inject.Inject

private const val BULK_IMPORT = "BULK IMPORT"

data class BulkImportProgress(
    val current: Int,
    val total: Int,
    val currentFile: String
)

class BulkImportBooksFromFolder @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val bookRepository: BookRepository,
    private val insertBook: InsertBook
) {

    suspend fun execute(
        folderUri: Uri,
        onProgress: (BulkImportProgress) -> Unit
    ): Int {
        Log.i(BULK_IMPORT, "Starting bulk import from folder: $folderUri")

        val supportedExtensions = provideExtensions()
        val existingPaths = bookRepository.getBooks("").map { it.filePath }
        var importedCount = 0

        // Get all files recursively from the folder
        val allFiles = getAllFilesFromFolder(folderUri)

        Log.i(BULK_IMPORT, "Found ${allFiles.size} files to process")

        allFiles.forEachIndexed { index, cachedFile ->
            onProgress(BulkImportProgress(index + 1, allFiles.size, cachedFile.name))

            // Check if file is supported and not already imported
            val isSupported = supportedExtensions.any { ext ->
                cachedFile.name.endsWith(ext, ignoreCase = true)
            }

            val alreadyExists = existingPaths.any { existingPath ->
                existingPath.equals(cachedFile.path, ignoreCase = true)
            }

            if (isSupported && !alreadyExists && cachedFile.canAccess()) {
                try {
                    val nullableBook = fileSystemRepository.getBookFromFile(cachedFile)
                    when (nullableBook) {
                        is us.blindmint.codex.domain.library.book.NullableBook.NotNull -> {
                            insertBook.execute(nullableBook.bookWithCover!!)
                            importedCount++
                            Log.i(BULK_IMPORT, "Imported: ${cachedFile.name}")
                        }
                        is us.blindmint.codex.domain.library.book.NullableBook.Null -> {
                            Log.w(BULK_IMPORT, "Failed to parse: ${cachedFile.name} - ${nullableBook.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(BULK_IMPORT, "Error importing ${cachedFile.name}: ${e.message}")
                }
            }
        }

        Log.i(BULK_IMPORT, "Bulk import completed. Imported $importedCount books.")
        return importedCount
    }

    private suspend fun getAllFilesFromFolder(folderUri: Uri): List<us.blindmint.codex.domain.file.CachedFile> {
        return fileSystemRepository.getAllFilesFromFolder(folderUri)
    }
}