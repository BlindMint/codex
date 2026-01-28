/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import android.graphics.Bitmap
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.library.category.Category

/**
 * Factory for creating Book and BookWithCover objects with consistent default values.
 * Eliminates duplication across FileParsers and provides single source of truth for book construction.
 */
object BookFactory {

    /**
     * Creates a BookWithCover with default values for a standard book.
     *
     * @param title Book title
     * @param authors List of authors (defaults to empty list)
     * @param description Book description (defaults to null)
     * @param filePath File path or URI
     * @param category Reading category (defaults to Category.entries[0])
     * @param coverImage Cover image bitmap (defaults to null)
     * @return BookWithCover with default values applied
     */
    fun createWithDefaults(
        title: String,
        authors: List<String> = emptyList(),
        description: String? = null,
        filePath: String,
        category: Category = Category.entries[0],
        coverImage: Bitmap? = null
    ): BookWithCover {
        return BookWithCover(
            book = Book(
                title = title,
                authors = authors,
                description = description,
                scrollIndex = 0,
                scrollOffset = 0,
                progress = 0f,
                filePath = filePath,
                lastOpened = null,
                category = category,
                coverImage = null
            ),
            coverImage = coverImage
        )
    }

    /**
     * Creates a BookWithCover with default values for a comic book.
     *
     * @param title Book title (typically filename)
     * @param filePath File path or URI
     * @param pageCount Number of pages in the comic
     * @param coverImage Cover image bitmap (defaults to null)
     * @return BookWithCover with comic-specific defaults applied
     */
    fun createComic(
        title: String,
        filePath: String,
        pageCount: Int,
        coverImage: Bitmap? = null
    ): BookWithCover {
        return BookWithCover(
            book = Book(
                title = title,
                authors = emptyList(),
                description = null,
                scrollIndex = 0,
                scrollOffset = 0,
                progress = 0f,
                filePath = filePath,
                lastOpened = null,
                category = Category.entries[0],
                coverImage = null,
                isComic = true,
                pageCount = pageCount
            ),
            coverImage = coverImage
        )
    }
}
