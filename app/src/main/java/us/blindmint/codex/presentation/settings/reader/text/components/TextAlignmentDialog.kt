/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.text.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding
import us.blindmint.codex.ui.main.MainModel

@Composable
fun TextAlignmentDialog(
    onDismissRequest: () -> Unit,
    onAlignmentSelected: (ReaderTextAlignment) -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(id = R.string.text_alignment_option)) },
        text = {
            Column(
                modifier = Modifier.padding(horizontal = SettingsHorizontalPadding)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ReaderTextAlignment.entries.forEach { alignment ->
                        FilterChip(
                            selected = alignment == state.value.textAlignment,
                            label = {
                                StyledText(
                                    text = when (alignment) {
                                        ReaderTextAlignment.START -> stringResource(id = R.string.alignment_start)
                                        ReaderTextAlignment.JUSTIFY -> stringResource(id = R.string.alignment_justify)
                                        ReaderTextAlignment.CENTER -> stringResource(id = R.string.alignment_center)
                                        ReaderTextAlignment.END -> stringResource(id = R.string.alignment_end)
                                        ReaderTextAlignment.ORIGINAL -> stringResource(id = R.string.alignment_original)
                                    },
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                )
                            },
                            onClick = {
                                onAlignmentSelected(alignment)
                                onDismissRequest()
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.done))
            }
        }
    )
}
