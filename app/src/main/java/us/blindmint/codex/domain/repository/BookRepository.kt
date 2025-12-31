/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.repository

import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.util.CoverImage

interface BookRepository {

    suspend fun getBooks(
        query: String
    ): List<Book>

    suspend fun getBooksById(
        ids: List<Int>
    ): List<Book>

    suspend fun getBookByFilePath(
        filePath: String
    ): Book?

    suspend fun getBookText(
        bookId: Int
    ): List<ReaderText>

    suspend fun insertBook(
        bookWithCover: BookWithCover
    )

    suspend fun updateBook(
        book: Book
    )

    suspend fun updateCoverImageOfBook(
        bookWithOldCover: Book,
        newCoverImage: CoverImage?
    )

    suspend fun deleteBooks(
        books: List<Book>
    )

    suspend fun canResetCover(
        bookId: Int
    ): Boolean

    suspend fun resetCoverImage(
        bookId: Int
    ): Boolean

    suspend fun deleteProgressHistory(
        book: Book
    )
}