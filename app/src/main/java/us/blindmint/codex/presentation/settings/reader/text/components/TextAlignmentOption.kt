/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.text.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.settings.GenericOption
import us.blindmint.codex.presentation.core.components.settings.OptionConfig
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding
import us.blindmint.codex.ui.main.MainEvent

@Composable
fun TextAlignmentOption() {
    var showDialog by remember { mutableStateOf(false) }

    GenericOption(
        OptionConfig(
            stateSelector = { it.textAlignment.toString() },
            eventCreator = { MainEvent.OnChangeTextAlignment(it) },
            component = { value, onChange ->
                val alignmentText = when (ReaderTextAlignment.entries.find { it.toString() == value }) {
                    ReaderTextAlignment.START -> stringResource(id = R.string.alignment_start)
                    ReaderTextAlignment.JUSTIFY -> stringResource(id = R.string.alignment_justify)
                    ReaderTextAlignment.CENTER -> stringResource(id = R.string.alignment_center)
                    ReaderTextAlignment.END -> stringResource(id = R.string.alignment_end)
                    ReaderTextAlignment.ORIGINAL -> stringResource(id = R.string.alignment_original)
                    else -> ""
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialog = true }
                        .padding(horizontal = SettingsHorizontalPadding, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.text_alignment_option),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.height(36.dp),
                            selected = true,
                            label = {
                                StyledText(
                                    text = alignmentText,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            },
                            onClick = { showDialog = true },
                        )
                    }
                }

                if (showDialog) {
                    TextAlignmentDialog(
                        onDismissRequest = { showDialog = false },
                        onAlignmentSelected = { alignment ->
                            onChange(alignment.toString())
                        }
                    )
                }
            }
        )
    )
}
