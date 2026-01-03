/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.reader

/**
 * Defines how background images are scaled to fit the screen.
 */
enum class BackgroundScaleMode {
    /**
     * Image fills the entire screen, maintaining aspect ratio.
     * May crop edges if aspect ratios don't match.
     */
    COVER,

    /**
     * Entire image is visible, maintaining aspect ratio.
     * May show background color as letterboxing/pillarboxing.
     */
    FIT,

    /**
     * Image repeats in a tile pattern to fill the screen.
     */
    TILE
}

fun String.toBackgroundScaleMode(): BackgroundScaleMode {
    return BackgroundScaleMode.entries.find { it.name == this } ?: BackgroundScaleMode.COVER
}
