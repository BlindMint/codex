/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import us.blindmint.codex.data.repository.FileSystemRepositoryImpl
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.repository.FileSystemRepository
import us.blindmint.codex.presentation.core.constants.provideExtensions
import us.blindmint.codex.domain.util.ContentHasher
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
    private val insertBook: InsertBook,
    @ApplicationContext private val context: Context
) {

    suspend fun execute(
        folderUri: Uri,
        onProgress: (BulkImportProgress) -> Unit
    ): Int {
        Log.i(BULK_IMPORT, "Starting bulk import from folder: $folderUri")

        val supportedExtensions = provideExtensions()
        val existingPaths = bookRepository.getBooks("").map { it.filePath }
        var importedCount = 0

        val allFiles = getAllFilesFromFolder(folderUri)
        Log.i(BULK_IMPORT, "Found ${allFiles.size} total files in folder")

        val supportedFiles = allFiles.filter { cachedFile ->
            val isSupported = supportedExtensions.any { ext ->
                cachedFile.name.endsWith(ext, ignoreCase = true)
            }
            val alreadyExists = existingPaths.any { existingPath ->
                existingPath.equals(cachedFile.uri.toString(), ignoreCase = true)
            }
            val canAccess = cachedFile.canAccess()

            isSupported && !alreadyExists && canAccess
        }

        Log.i(BULK_IMPORT, "Found ${supportedFiles.size} supported files to import")

        supportedFiles.forEachIndexed { index, cachedFile ->
            try {
                val contentHash = ContentHasher.computeHash(context, cachedFile.uri)
                
                val existingByHash = bookRepository.getBookByContentHash(contentHash)
                if (existingByHash != null) {
                    Log.i(BULK_IMPORT, "Skipping duplicate (by hash): ${cachedFile.name}")
                    onProgress(BulkImportProgress(index + 1, supportedFiles.size, cachedFile.name))
                    return@forEachIndexed
                }

                val nullableBook = fileSystemRepository.getBookFromFile(cachedFile)
                when (nullableBook) {
                    is us.blindmint.codex.domain.library.book.NullableBook.NotNull -> {
                        val bookWithHash = nullableBook.bookWithCover!!.copy(
                            book = nullableBook.bookWithCover.book.copy(
                                contentHash = contentHash,
                                fileSize = cachedFile.size
                            )
                        )
                        insertBook.execute(bookWithHash)
                        importedCount++
                        Log.i(BULK_IMPORT, "Imported: ${cachedFile.name}")
                        onProgress(BulkImportProgress(importedCount, supportedFiles.size, cachedFile.name))
                    }
                    is us.blindmint.codex.domain.library.book.NullableBook.Null -> {
                        Log.w(BULK_IMPORT, "Failed to parse: ${cachedFile.name} - ${nullableBook.message}")
                        onProgress(BulkImportProgress(index + 1, supportedFiles.size, cachedFile.name))
                    }
                }
            } catch (e: Exception) {
                Log.e(BULK_IMPORT, "Error importing ${cachedFile.name}: ${e.message}")
                onProgress(BulkImportProgress(index + 1, supportedFiles.size, cachedFile.name))
            }
        }

        Log.i(BULK_IMPORT, "Bulk import completed. Imported $importedCount books.")
        return importedCount
    }

    private suspend fun getAllFilesFromFolder(folderUri: Uri): List<us.blindmint.codex.domain.file.CachedFile> {
        return fileSystemRepository.getAllFilesFromFolder(folderUri)
    }
}