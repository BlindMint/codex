/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.opds

import android.app.Application
import us.blindmint.codex.data.mapper.opds.OpdsMetadataMapper
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.repository.OpdsRepository
import us.blindmint.codex.domain.util.CoverImage
import java.io.File
import javax.inject.Inject

class ImportOpdsBookUseCase @Inject constructor(
    private val application: Application,
    private val opdsRepository: OpdsRepository,
    private val fileParser: FileParser,
    private val bookRepository: BookRepository,
    private val opdsMetadataMapper: OpdsMetadataMapper
) {

    suspend operator fun invoke(
        opdsEntry: OpdsEntry,
        username: String? = null,
        password: String? = null
    ): BookWithCover? {
        // Find acquisition link
        val acquisitionLink = opdsEntry.links.firstOrNull { it.rel == "http://opds-spec.org/acquisition" }
            ?: return null

        // Download the book
        val bookBytes = opdsRepository.downloadBook(acquisitionLink.href, username, password)

        // Save to temp file
        val tempFile = File(application.cacheDir, "temp_book_${System.currentTimeMillis()}")
        tempFile.writeBytes(bookBytes)

        // Parse the file
        val cachedFile = CachedFileCompat.fromFullPath(
            context = application,
            path = tempFile.absolutePath,
            builder = CachedFileCompat.build(
                name = tempFile.name,
                path = tempFile.absolutePath,
                isDirectory = false
            )
        ) ?: return null

        val parsedBook = fileParser.parse(cachedFile) ?: return null

        // Move to permanent location (e.g., downloads or books dir)
        val booksDir = File(application.getExternalFilesDir(null), "books")
        booksDir.mkdirs()
        val permanentFile = File(booksDir, tempFile.name)
        tempFile.copyTo(permanentFile, overwrite = true)
        tempFile.delete()

        // Update file path in book
        val bookWithPath = parsedBook.book.copy(filePath = permanentFile.absolutePath)

        // Apply OPDS metadata
        val bookWithMetadata = opdsMetadataMapper.applyOpdsMetadataToBook(bookWithPath, opdsEntry)

        return BookWithCover(
            book = bookWithMetadata,
            coverImage = parsedBook.coverImage
        )
    }
}