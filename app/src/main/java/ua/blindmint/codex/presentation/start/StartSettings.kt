/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.start

import androidx.compose.runtime.Composable
import ua.blindmint.codex.domain.navigator.StackEvent
import ua.blindmint.codex.ui.start.StartScreen

@Composable
fun StartSettings(
    currentPage: Int,
    stackEvent: StackEvent,
    navigateForward: () -> Unit
) {
    StartSettingsScaffold(
        currentPage = currentPage,
        navigateForward = navigateForward
    ) {
        StartContentTransition(
            targetValue = when (currentPage) {
                1 -> StartScreen.APPEARANCE_SETTINGS
                else -> StartScreen.SCAN_SETTINGS
            },
            stackEvent = stackEvent
        ) { page ->
            StartSettingsLayout {
                when (page) {
                    StartScreen.APPEARANCE_SETTINGS -> {
                        StartSettingsLayoutAppearance()
                    }

                    StartScreen.SCAN_SETTINGS -> {
                        StartSettingsLayoutScan()
                    }
                }
            }
        }
    }
}