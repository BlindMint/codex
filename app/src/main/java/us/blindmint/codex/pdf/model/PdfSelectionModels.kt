/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.model

import androidx.compose.runtime.Immutable

@Immutable
data class PdfSelectionBounds(
    val pageIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Immutable
data class PdfSelectionState(
    val selectedText: String = "",
    val paragraphText: String = "",
    val bounds: PdfSelectionBounds? = null
)
