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
            authors = book.authors,
            description = book.description,
            filePath = book.filePath,
            scrollIndex = book.scrollIndex,
            scrollOffset = book.scrollOffset,
            progress = book.progress,
            speedReaderWordIndex = book.speedReaderWordIndex,
            speedReaderHasBeenOpened = book.speedReaderHasBeenOpened,
            image = book.coverImage?.toString(),
            category = book.category,
            tags = book.tags,
            series = book.series,
            publicationDate = book.publicationDate,
            languages = book.languages,
            publisher = book.publisher,
            summary = book.summary,
            uuid = book.uuid,
            isbn = book.isbn,
            source = book.source,
            remoteUrl = book.remoteUrl,
            opdsSourceUrl = book.opdsSourceUrl,
            opdsSourceId = book.opdsSourceId,
            metadataLastRefreshTime = book.metadataLastRefreshTime,
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
            authors = bookEntity.authors,
            description = bookEntity.description,
            filePath = bookEntity.filePath,
            coverImage = bookEntity.image?.toUri(),
            scrollIndex = bookEntity.scrollIndex,
            scrollOffset = bookEntity.scrollOffset,
            progress = bookEntity.progress,
            speedReaderWordIndex = bookEntity.speedReaderWordIndex,
            speedReaderHasBeenOpened = bookEntity.speedReaderHasBeenOpened,
            lastOpened = null,
            category = bookEntity.category,
            tags = bookEntity.tags,
            series = bookEntity.series,
            publicationDate = bookEntity.publicationDate,
            languages = bookEntity.languages,
            publisher = bookEntity.publisher,
            summary = bookEntity.summary,
            uuid = bookEntity.uuid,
            isbn = bookEntity.isbn,
            source = bookEntity.source,
            remoteUrl = bookEntity.remoteUrl,
            opdsSourceUrl = bookEntity.opdsSourceUrl,
            opdsSourceId = bookEntity.opdsSourceId,
            metadataLastRefreshTime = bookEntity.metadataLastRefreshTime,
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