/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.help

import android.os.Parcelable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.components.top_bar.collapsibleTopAppBarScrollBehavior
import us.blindmint.codex.presentation.help.HelpContent
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.start.StartScreen

@Parcelize
data class HelpScreen(val fromStart: Boolean) : Screen, Parcelable {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val mainModel = hiltViewModel<MainModel>()

        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        HelpContent(
            fromStart = fromStart,
            scrollBehavior = scrollBehavior,
            listState = listState,
            changeShowStartScreen = mainModel::onEvent,
            navigateToBrowse = {
                navigator.push(BrowseScreen, saveInBackStack = false)
            },
            navigateToStart = {
                mainModel.onEvent(MainEvent.OnChangeShowStartScreen(true))
                navigator.push(StartScreen, saveInBackStack = false)
            },
            navigateBack = {
                navigator.pop()
            }
        )
    }
}