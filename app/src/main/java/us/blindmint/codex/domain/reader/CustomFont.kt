/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.reader

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class CustomFont(
    val name: String,
    val filePath: String
) : Parcelable {
    companion object {
        fun fromString(value: String): CustomFont? {
            val parts = value.split("|", limit = 2)
            return if (parts.size == 2) {
                CustomFont(parts[0], parts[1])
            } else null
        }

        fun toString(customFont: CustomFont): String {
            return "${customFont.name}|${customFont.filePath}"
        }
    }
}