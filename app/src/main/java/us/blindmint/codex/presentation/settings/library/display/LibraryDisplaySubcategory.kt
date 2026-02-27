/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.library.display

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.presentation.settings.library.display.components.LibraryGridSizeOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryLayoutOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryListSizeOption
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.presentation.settings.library.display.components.LibraryShowSpeedProgressOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryShowNormalProgressOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryTitlePositionOption

fun LazyListScope.LibraryDisplaySubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.display_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        item {
            LibraryLayoutOption()
        }

        item {
            // Size slider - content changes based on layout
            val mainModel = androidx.hilt.navigation.compose.hiltViewModel<us.blindmint.codex.ui.main.MainModel>()
            val state = mainModel.state.collectAsStateWithLifecycle()
            if (state.value.libraryLayout == us.blindmint.codex.domain.library.display.LibraryLayout.GRID) {
                LibraryGridSizeOption()
            } else {
                LibraryListSizeOption()
            }
        }

        // Title Position (only visible in Grid mode)
        item {
            val mainModel = androidx.hilt.navigation.compose.hiltViewModel<us.blindmint.codex.ui.main.MainModel>()
            val state = mainModel.state.collectAsStateWithLifecycle()
            if (state.value.libraryLayout == us.blindmint.codex.domain.library.display.LibraryLayout.GRID) {
                LibraryTitlePositionOption()
            }
        }

        item {
            LibraryShowNormalProgressOption()
        }

        item {
            LibraryShowSpeedProgressOption()
        }
    }
}