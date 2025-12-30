/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.blindmint.codex.domain.navigator.StackEvent
import us.blindmint.codex.ui.start.StartScreen

@Composable
fun StartContent(
    currentPage: Int,
    stackEvent: StackEvent,
    navigateForward: () -> Unit,
    navigateBack: () -> Unit,
    navigateToBrowse: () -> Unit,
    navigateToHelp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        StartContentTransition(
            modifier = Modifier,
            targetValue = when {
                currentPage in 1..2 -> StartScreen.SETTINGS
                else -> StartScreen.FINAL_DONE
            },
            stackEvent = stackEvent
        ) { page ->
            when (page) {
                StartScreen.SETTINGS -> {
                    StartSettings(
                        currentPage = currentPage,
                        stackEvent = stackEvent,
                        navigateForward = navigateForward
                    )
                }

                StartScreen.FINAL_DONE -> {
                    StartFinalDone(
                        navigateToBrowse = navigateToBrowse,
                        navigateToHelp = navigateToHelp
                    )
                }
            }
        }
    }

    StartBackHandler(
        navigateBack = navigateBack
    )
}