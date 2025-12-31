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
    val author: UIText,
    val description: String?,

    val filePath: String,
    val coverImage: Uri?,

    val scrollIndex: Int,
    val scrollOffset: Int,
    val progress: Float,

    val lastOpened: Long?,
    val category: Category
) : Parcelable