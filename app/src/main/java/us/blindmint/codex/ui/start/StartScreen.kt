/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.start

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.domain.navigator.StackEvent
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.start.StartContent
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.help.HelpScreen
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Parcelize
object StartScreen : Screen, Parcelable {

    @IgnoredOnParcel
    const val SETTINGS = "settings"

    @IgnoredOnParcel
    const val GENERAL_SETTINGS = "general_settings"

    @IgnoredOnParcel
    const val APPEARANCE_SETTINGS = "appearance_settings"

    @IgnoredOnParcel
    const val SCAN_SETTINGS = "scan_settings"

    @IgnoredOnParcel
    const val DONE = "done"

    @IgnoredOnParcel
    const val FINAL_DONE = "final_done"

    @SuppressLint("InlinedApi")
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val mainModel = hiltViewModel<MainModel>()

        val mainState = mainModel.state.collectAsStateWithLifecycle()

        val activity = LocalActivity.current

        val currentPage = remember { mutableIntStateOf(1) }
        val stackEvent = remember { mutableStateOf(StackEvent.Default) }

        StartContent(
            currentPage = currentPage.intValue,
            stackEvent = stackEvent.value,
            navigateForward = {
                if (currentPage.intValue >= 3) {
                    return@StartContent
                }

                stackEvent.value = StackEvent.Default
                currentPage.intValue += 1
            },
            navigateBack = {
                if (currentPage.intValue <= 1) {
                    activity.finish()
                } else {
                    stackEvent.value = StackEvent.Pop
                    currentPage.intValue -= 1
                }
            },
            navigateToBrowse = {
                navigator.push(
                    BrowseScreen,
                    saveInBackStack = false
                )
                BrowseScreen.refreshListChannel.trySend(Unit)
                mainModel.onEvent(MainEvent.OnChangeShowStartScreen(false))
            },
            navigateToHelp = {
                navigator.push(
                    HelpScreen(fromStart = true),
                    saveInBackStack = false
                )
            }
        )
    }
}