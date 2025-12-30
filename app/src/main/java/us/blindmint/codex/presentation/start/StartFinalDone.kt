/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.start

import androidx.compose.runtime.Composable

@Composable
fun StartFinalDone(
    navigateToBrowse: () -> Unit,
    navigateToHelp: () -> Unit
) {
    StartFinalDoneScaffold(
        navigateToBrowse = navigateToBrowse,
        navigateToHelp = navigateToHelp
    )
}