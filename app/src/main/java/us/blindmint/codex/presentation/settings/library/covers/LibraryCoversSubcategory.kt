/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.library.covers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.ui.library.CoverRegenerationModel

fun LazyListScope.LibraryCoversSubcategory(
    showDivider: Boolean = true
) {
    SettingsSubcategory(
        titleColor = { MaterialTheme.colorScheme.primary },
        title = { "Book covers" },
        showTitle = true,
        showDivider = showDivider
    ) {
        item {
            val model = hiltViewModel<CoverRegenerationModel>()
            val state = model.state.collectAsStateWithLifecycle().value

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Generate covers for books whose saved cover is missing or unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.running) {
                    LinearProgressIndicator(
                        progress = {
                            if (state.total == 0) 0f
                            else state.processed.toFloat() / state.total
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (state.total == 0) {
                            "Finding missing covers…"
                        } else {
                            "${state.processed} of ${state.total}: ${state.currentBook}"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                state.resultMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = model::regenerateMissing,
                    enabled = !state.running
                ) {
                    Text(if (state.running) "Generating…" else "Regenerate missing covers")
                }
            }
        }
    }
}
