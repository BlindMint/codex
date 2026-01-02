/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val scrollIndex: Int,
    val scrollOffset: Int,
    val timestamp: Long,
    val selectedText: String = "",
    val customName: String = "",
    val pageNumber: Int = 0
)
