/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.repository

import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SpeedReaderWord

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

    suspend fun getSpeedReaderWords(
        bookId: Int
    ): List<SpeedReaderWord>

    suspend fun insertBook(
        bookWithCover: BookWithCover
    )

    suspend fun updateBook(
        book: Book
    )

    suspend fun updateSpeedReaderProgress(
        bookId: Int,
        wordIndex: Int
    )

    suspend fun updateNormalReaderProgress(
        bookId: Int,
        scrollIndex: Int,
        scrollOffset: Int,
        progress: Float
    )

    suspend fun updateComicPdfProgress(
        bookId: Int,
        lastPageRead: Int,
        progress: Float
    )

    suspend fun markSpeedReaderOpened(
        bookId: Int
    )

    suspend fun updateSpeedReaderTotalWords(
        bookId: Int,
        totalWords: Int
    )

    suspend fun deleteBooks(
        books: List<Book>
    )

    suspend fun deleteProgressHistory(
        book: Book
    )

    suspend fun getAllTags(): List<String>

    suspend fun getAllAuthors(): List<String>

    suspend fun getAllSeries(): List<String>

    suspend fun getAllLanguages(): List<String>

    suspend fun getPublicationYearRange(): Pair<Int, Int>

    suspend fun getBooksByOpdsSourceUrl(
        opdsSourceUrl: String
    ): List<Book>

    suspend fun getBooksByOpdsSourceId(
        opdsSourceId: Int
    ): List<Book>

    suspend fun getBookByCalibreId(
        calibreId: String
    ): Book?

    suspend fun getBookByContentHash(
        contentHash: String
    ): Book?

    suspend fun findExistingBook(
        filePath: String,
        fileName: String? = null,
        fileSize: Long? = null
    ): Book?
}