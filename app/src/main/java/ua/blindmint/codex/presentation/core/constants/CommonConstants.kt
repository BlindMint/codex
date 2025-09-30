/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("SameReturnValue")

package ua.blindmint.codex.presentation.core.constants

import androidx.compose.ui.graphics.Color
import ua.blindmint.codex.domain.library.book.Book
import ua.blindmint.codex.domain.library.category.Category
import ua.blindmint.codex.domain.reader.ColorPreset
import ua.blindmint.codex.domain.ui.UIText

// Main State
fun provideMainState() = "main_state"

// Empty Book
fun provideEmptyBook() = Book(
    id = -1,
    title = "",
    author = UIText.StringValue(""),
    description = null,
    filePath = "",
    coverImage = null,
    scrollIndex = 0,
    scrollOffset = 0,
    progress = 0f,
    lastOpened = null,
    category = Category.READING
)

// Default Color Presets - Following Material Design guidelines
fun provideDefaultColorPresets() = listOf(
    ColorPreset(
        id = 1,
        name = "Light",
        backgroundColor = Color(0xFFFFFFFF), // Material Design light surface (#FFFFFF)
        fontColor = Color(0xFF1C1B1F), // Material Design light onSurface (#1C1B1F)
        isSelected = false
    ),
    ColorPreset(
        id = 2,
        name = "Dark",
        backgroundColor = Color(0xFF0F1419), // Material Design dark surface (#0F1419)
        fontColor = Color(0xFFE6E1E5), // Material Design dark onSurface (#E6E1E5)
        isSelected = false
    )
)

// Legacy function for backward compatibility
fun provideDefaultColorPreset() = provideDefaultColorPresets().first()

// Characters per page for progress estimation
const val CHARACTERS_PER_PAGE = 2000