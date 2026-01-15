/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.dto

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class OpdsSourceEntity(
    @PrimaryKey(true) val id: Int = 0,
    val name: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val enabled: Boolean = true,
    val lastSync: Long = 0
) : Parcelable