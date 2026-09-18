/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun MissingBookFileDialog(
    bookTitle: String,
    navigateBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = navigateBack,
        title = { Text("Book file unavailable") },
        text = {
            Text(
                "The file for “$bookTitle” is missing or inaccessible. " +
                    "Reconnect its storage location or update the path in File details."
            )
        },
        confirmButton = {
            TextButton(onClick = navigateBack) {
                Text("Go back")
            }
        }
    )
}
