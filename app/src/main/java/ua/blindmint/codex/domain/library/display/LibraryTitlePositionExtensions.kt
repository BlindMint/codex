/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.domain.library.display

fun String.toLibraryTitlePosition(): LibraryTitlePosition {
    return LibraryTitlePosition.entries.find { it.name == this } ?: LibraryTitlePosition.BELOW
}