/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.display.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.browse.display.BrowseLayout
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun BrowseLayoutOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SegmentedButtonWithTitle(
        title = stringResource(id = R.string.layout_option),
        buttons = BrowseLayout.entries.map {
            ButtonItem(
                it.toString(),
                when (it) {
                    BrowseLayout.LIST -> stringResource(id = R.string.layout_list)
                    BrowseLayout.GRID -> stringResource(id = R.string.layout_grid)
                },
                MaterialTheme.typography.labelLarge,
                it == state.value.browseLayout
            )
        }
    ) {
        mainModel.onEvent(
            MainEvent.OnChangeBrowseLayout(
                it.id
            )
        )
    }
}