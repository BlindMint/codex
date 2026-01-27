/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.library.book

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.ui.UIText

@Parcelize
@Immutable
data class Book(
    val id: Int = 0,

    val title: String,
    val authors: List<String> = emptyList(),
    val description: String?,

    val filePath: String,
    val coverImage: Uri?,

    val scrollIndex: Int,
    val scrollOffset: Int,
    val progress: Float,

    // Speed reader progress (separate from normal reader)
    val speedReaderWordIndex: Int = 0,
    val speedReaderHasBeenOpened: Boolean = false,
    val speedReaderTotalWords: Int = 0,

    val lastOpened: Long?,
    val category: Category = Category.PLANNING, // TODO: remove when UI updated

    val tags: List<String> = emptyList(),
    val series: List<String> = emptyList(),
    val publicationDate: Long? = null,
    val languages: List<String> = emptyList(),
    val publisher: String? = null,
    val uuid: String? = null,
    val isbn: String? = null,
    val source: BookSource = BookSource.LOCAL,
    val opdsSourceUrl: String? = null,
    val opdsSourceId: Int? = null,
    val opdsCalibreId: String? = null,
    val metadataLastRefreshTime: Long? = null,
    // Comic fields
    val isComic: Boolean = false,
    val pageCount: Int? = null,
    val currentPage: Int = 0,
    val lastPageRead: Int = 0,
    val readingDirection: String = "LTR",
    val comicReaderMode: String = "PAGED",
    val isFavorite: Boolean = false
) : Parcelable