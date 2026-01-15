/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import us.blindmint.codex.data.local.room.TypeConverters as CodexTypeConverters
import us.blindmint.codex.domain.library.book.BookSource
import us.blindmint.codex.domain.library.category.Category

@Entity
@TypeConverters(CodexTypeConverters::class)
data class BookEntity(
    @PrimaryKey(true) val id: Int = 0,
    val title: String,
    val author: String?,
    val description: String?,
    val filePath: String,
    val scrollIndex: Int,
    val scrollOffset: Int,
    val progress: Float,
    val image: String? = null,
    val category: Category = Category.PLANNING, // TODO: remove when UI updated
    val tags: List<String> = emptyList(),
    val seriesName: String? = null,
    val seriesIndex: Int? = null,
    val publicationDate: Long? = null,
    val language: String? = null,
    val publisher: String? = null,
    val summary: String? = null,
    val uuid: String? = null,
    val isbn: String? = null,
    val source: BookSource = BookSource.LOCAL,
    val remoteUrl: String? = null,
    // Comic fields
    val isComic: Boolean = false,
    val pageCount: Int? = null,
    val currentPage: Int = 0,
    val lastPageRead: Int = 0,
    val readingDirection: String = "LTR",
    val comicReaderMode: String = "PAGED",
    val archiveFormat: String? = null
)