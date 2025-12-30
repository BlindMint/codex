/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.ui.library.LibraryEvent

@Composable
fun LibraryBackHandler(
    hasSelectedItems: Boolean,
    showSearch: Boolean,
    showSortMenu: Boolean,
    pagerState: PagerState,
    doublePressExit: Boolean,
    clearSelectedBooks: (LibraryEvent.OnClearSelectedBooks) -> Unit,
    searchVisibility: (LibraryEvent.OnSearchVisibility) -> Unit,
    sortMenuVisibility: (LibraryEvent) -> Unit
) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var shouldExit = rememberSaveable { false }

    BackHandler {
        if (hasSelectedItems) {
            clearSelectedBooks(LibraryEvent.OnClearSelectedBooks)
            return@BackHandler
        }

        if (showSortMenu) {
            sortMenuVisibility(LibraryEvent.OnDismissSortMenu)
            return@BackHandler
        }

        if (showSearch) {
            searchVisibility(LibraryEvent.OnSearchVisibility(false))
            return@BackHandler
        }

        if (pagerState.currentPage > 0) {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
            return@BackHandler
        }

        if (shouldExit || !doublePressExit) {
            activity.finish()
            return@BackHandler
        }

        activity
            .getString(R.string.press_again_toast)
            .showToast(context = activity, longToast = false)
        shouldExit = true

        scope.launch {
            delay(1500)
            shouldExit = false
        }
    }
}