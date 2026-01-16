/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.ui.browse.OpdsRootScreen

@Composable
fun BrowseSettingsLayout(
    listState: LazyListState,
    paddingValues: PaddingValues,
    onNavigateToOpdsCatalog: (OpdsRootScreen) -> Unit
) {
    LazyColumnWithScrollbar(
        Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        state = listState
    ) {
        BrowseSettingsCategory(onNavigateToOpdsCatalog = onNavigateToOpdsCatalog)
    }
}