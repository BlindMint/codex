/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.reader

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * Represents a background image for the reader.
 * Can be a bundled default image or a user-added custom image.
 */
@Immutable
@Parcelize
data class BackgroundImage(
    val name: String,
    val filePath: String,
    val isDefault: Boolean = false
) : Parcelable {
    companion object {
        /**
         * Special value indicating no background image is selected.
         */
        val NONE = BackgroundImage("none", "", isDefault = true)

        fun fromString(value: String): BackgroundImage? {
            val parts = value.split("|", limit = 3)
            return when {
                parts.size == 3 -> BackgroundImage(parts[0], parts[1], parts[2].toBoolean())
                parts.size == 2 -> BackgroundImage(parts[0], parts[1], false)
                else -> null
            }
        }

        fun toString(image: BackgroundImage): String {
            return "${image.name}|${image.filePath}|${image.isDefault}"
        }
    }
}
