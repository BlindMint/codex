/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse

import androidx.compose.runtime.Composable
import us.blindmint.codex.domain.util.BottomSheet
import us.blindmint.codex.ui.browse.BrowseEvent
import us.blindmint.codex.ui.browse.BrowseScreen

@Composable
fun BrowseBottomSheet(
    bottomSheet: BottomSheet?,
    dismissBottomSheet: (BrowseEvent.OnDismissBottomSheet) -> Unit
) {
    when (bottomSheet) {
        BrowseScreen.FILTER_BOTTOM_SHEET -> {
            BrowseFilterBottomSheet(
                dismissBottomSheet = dismissBottomSheet
            )
        }
    }
}