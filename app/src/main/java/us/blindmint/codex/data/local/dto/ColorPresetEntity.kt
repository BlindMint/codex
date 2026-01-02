/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ColorPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String?,
    val backgroundColor: Long,
    val fontColor: Long,
    val isSelected: Boolean,
    val isLocked: Boolean = false,
    val order: Int
)