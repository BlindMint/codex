/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.mapper.book

import androidx.core.net.toUri
import us.blindmint.codex.R
import us.blindmint.codex.data.local.dto.BookEntity
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.ui.UIText
import javax.inject.Inject

class BookMapperImpl @Inject constructor() : BookMapper {
    override suspend fun toBookEntity(book: Book): BookEntity {
        return BookEntity(
            id = book.id,
            title = book.title,
            author = book.author.getAsString(),
            description = book.description,
            filePath = book.filePath,
            scrollIndex = book.scrollIndex,
            scrollOffset = book.scrollOffset,
            progress = book.progress,
            image = book.coverImage?.toString(),
            category = book.category,
            tags = book.tags,
            seriesName = book.seriesName,
            seriesIndex = book.seriesIndex,
            publicationDate = book.publicationDate,
            language = book.language,
            publisher = book.publisher,
            summary = book.summary,
            uuid = book.uuid,
            isbn = book.isbn,
            source = book.source,
            remoteUrl = book.remoteUrl,
            isComic = book.isComic,
            pageCount = book.pageCount,
            currentPage = book.currentPage,
            lastPageRead = book.lastPageRead,
            readingDirection = book.readingDirection,
            comicReaderMode = book.comicReaderMode,
            archiveFormat = book.archiveFormat,
            isFavorite = book.isFavorite
        )
    }

    override suspend fun toBook(bookEntity: BookEntity): Book {
        return Book(
            id = bookEntity.id,
            title = bookEntity.title,
            author = bookEntity.author?.let { UIText.StringValue(it) } ?: UIText.StringResource(
                R.string.unknown_author
            ),
            description = bookEntity.description,
            filePath = bookEntity.filePath,
            coverImage = bookEntity.image?.toUri(),
            scrollIndex = bookEntity.scrollIndex,
            scrollOffset = bookEntity.scrollOffset,
            progress = bookEntity.progress,
            lastOpened = null,
            category = bookEntity.category,
            tags = bookEntity.tags,
            seriesName = bookEntity.seriesName,
            seriesIndex = bookEntity.seriesIndex,
            publicationDate = bookEntity.publicationDate,
            language = bookEntity.language,
            publisher = bookEntity.publisher,
            summary = bookEntity.summary,
            uuid = bookEntity.uuid,
            isbn = bookEntity.isbn,
            source = bookEntity.source,
            remoteUrl = bookEntity.remoteUrl,
            isComic = bookEntity.isComic,
            pageCount = bookEntity.pageCount,
            currentPage = bookEntity.currentPage,
            lastPageRead = bookEntity.lastPageRead,
            readingDirection = bookEntity.readingDirection,
            comicReaderMode = bookEntity.comicReaderMode,
            archiveFormat = bookEntity.archiveFormat,
            isFavorite = bookEntity.isFavorite
        )
    }
}