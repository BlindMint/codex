/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.navigator.StackEvent
import ua.blindmint.codex.domain.ui.ButtonItem
import ua.blindmint.codex.ui.main.MainEvent
import ua.blindmint.codex.ui.start.StartScreen

@Composable
fun StartContent(
    currentPage: Int,
    stackEvent: StackEvent,
    languages: List<ButtonItem>,
    changeLanguage: (MainEvent.OnChangeLanguage) -> Unit,
    navigateForward: () -> Unit,
    navigateBack: () -> Unit,
    navigateToBrowse: () -> Unit,
    navigateToHelp: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.closed_vault),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        StartContentTransition(
            modifier = Modifier,
            targetValue = when {
                currentPage in 1..3 -> StartScreen.SETTINGS
                else -> StartScreen.FINAL_DONE
            },
            stackEvent = stackEvent
        ) { page ->
            when (page) {
                StartScreen.SETTINGS -> {
                    StartSettings(
                        currentPage = currentPage,
                        stackEvent = stackEvent,
                        languages = languages,
                        changeLanguage = changeLanguage,
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