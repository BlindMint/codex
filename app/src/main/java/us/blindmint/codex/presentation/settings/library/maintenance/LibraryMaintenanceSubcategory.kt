/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.library.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.ui.library.MissingBooksCleanupModel

fun LazyListScope.LibraryMaintenanceSubcategory(
    showDivider: Boolean = true
) {
    SettingsSubcategory(
        titleColor = { MaterialTheme.colorScheme.primary },
        title = { "Library maintenance" },
        showTitle = true,
        showDivider = showDivider
    ) {
        item {
            MissingBooksCleanupOption()
        }
    }
}

@Composable
private fun MissingBooksCleanupOption(
    model: MissingBooksCleanupModel = hiltViewModel()
) {
    val state = model.state.collectAsStateWithLifecycle().value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Find library entries whose book files are missing or inaccessible.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        state.resultMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = model::scan,
            enabled = !state.scanning && !state.removing
        ) {
            if (state.scanning || state.removing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp
                )
            }
            Text(
                when {
                    state.scanning -> "Checking library…"
                    state.removing -> "Removing missing books…"
                    else -> "Remove missing books"
                }
            )
        }
    }

    state.pendingRemovalCount?.let { count ->
        AlertDialog(
            onDismissRequest = model::dismissConfirmation,
            title = { Text("Remove missing books?") },
            text = {
                Text(
                    "This will remove $count ${if (count == 1) "entry" else "entries"} " +
                        "whose files cannot be accessed. The book files themselves will not be deleted."
                )
            },
            dismissButton = {
                TextButton(onClick = model::dismissConfirmation) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = model::confirmRemoval,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove $count")
                }
            }
        )
    }
}
