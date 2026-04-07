/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.model

import androidx.compose.runtime.Immutable

@Immutable
data class PdfPageGeometry(
    val pageIndex: Int,
    val width: Float,
    val height: Float
) {
    val aspectRatio: Float
        get() = if (height == 0f) 1f else width / height
}
