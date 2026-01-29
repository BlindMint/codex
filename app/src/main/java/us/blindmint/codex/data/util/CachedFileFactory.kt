/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import us.blindmint.codex.data.local.dto.BookEntity
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.Book
import java.io.File

/**
 * Factory for creating CachedFile objects from various sources.
 * Handles both URI and file path conversion logic.
 */
object CachedFileFactory {

    /**
     * Creates a CachedFile from a BookEntity (database entity).
     * Handles both content URIs and file paths.
     *
     * @param context Application context
     * @param bookEntity Book entity from database
     * @return CachedFile or null if creation fails
     */
    fun fromBookEntity(
        context: Context,
        bookEntity: BookEntity
    ): CachedFile? {
        val uri = bookEntity.filePath.toUri()
        return if (!uri.scheme.isNullOrBlank()) {
            val name = if (uri.scheme == "content") {
                uri.lastPathSegment?.let { Uri.decode(it) } ?: "unknown"
            } else {
                uri.lastPathSegment ?: bookEntity.filePath.substringAfterLast(File.separator)
            }
            CachedFileCompat.fromUri(
                context = context,
                uri = uri,
                builder = CachedFileCompat.build(
                    name = name,
                    path = bookEntity.filePath,
                    isDirectory = false
                )
            )
        } else {
            CachedFileCompat.fromFullPath(
                context = context,
                path = bookEntity.filePath,
                builder = CachedFileCompat.build(
                    name = bookEntity.filePath.substringAfterLast(File.separator),
                    path = bookEntity.filePath,
                    isDirectory = false
                )
            )
        }
    }

    /**
     * Creates a CachedFile from a Book (domain model).
     * Handles both content URIs and file paths.
     *
     * @param context Application context
     * @param book Book domain model
     * @return CachedFile or null if creation fails
     */
    fun fromBook(
        context: Context,
        book: Book
    ): CachedFile? {
        val uri = book.filePath.toUri()
        return if (!uri.scheme.isNullOrBlank()) {
            CachedFileCompat.fromUri(
                context = context,
                uri = uri
            )
        } else {
            CachedFileCompat.fromFullPath(
                context = context,
                path = book.filePath
            )
        }
    }
}
