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
            filePath = book.filePath,
            scrollIndex = book.scrollIndex,
            scrollOffset = book.scrollOffset,
            progress = book.progress,
            author = book.author.getAsString(),
            description = book.description,
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
            remoteUrl = book.remoteUrl
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
            scrollIndex = bookEntity.scrollIndex,
            scrollOffset = bookEntity.scrollOffset,
            progress = bookEntity.progress,
            filePath = bookEntity.filePath,
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
            coverImage = bookEntity.image?.toUri()
        )
    }
}