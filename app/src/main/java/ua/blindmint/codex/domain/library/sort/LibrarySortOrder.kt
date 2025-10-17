/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.domain.library.sort

import androidx.compose.runtime.Immutable

@Immutable
enum class LibrarySortOrder {
    NAME,
    LAST_READ,
    PROGRESS,
    AUTHOR
}

fun String.toLibrarySortOrder(): LibrarySortOrder {
    return try {
        LibrarySortOrder.valueOf(this)
    } catch (_: IllegalArgumentException) {
        LibrarySortOrder.NAME
    }
}