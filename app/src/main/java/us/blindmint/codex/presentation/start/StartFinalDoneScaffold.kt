/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.start

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun StartFinalDoneScaffold(
    navigateToBrowse: () -> Unit,
    navigateToHelp: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            StartFinalDoneBottomBar(
                navigateToBrowse = navigateToBrowse,
                navigateToHelp = navigateToHelp
            )
        },
        containerColor = Color.Transparent
    ) {
        StartFinalDoneLayout(
            paddingValues = it
        )
    }
}