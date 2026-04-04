/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.model

import androidx.compose.runtime.Immutable

@Immutable
data class PdfCropBounds(
    val leftRatio: Float = 0f,
    val topRatio: Float = 0f,
    val rightRatio: Float = 1f,
    val bottomRatio: Float = 1f
)
