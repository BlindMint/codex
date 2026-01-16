/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.settings.reader.speed_reading.SpeedReadingSubcategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingSettingsBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (show) {
        ModalBottomSheet(
            hasFixedHeight = true,
            scrimColor = BottomSheetDefaults.ScrimColor,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            onDismissRequest = onDismiss,
            sheetGesturesEnabled = true,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    androidx.compose.material3.Text(
                        text = stringResource(id = R.string.speed_reading_reader_settings),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), // Use weight to take remaining space
                        horizontalAlignment = Alignment.Start
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        SpeedReadingSubcategory(
                            titleColor = { MaterialTheme.colorScheme.primary },
                            showTitle = false,
                            showDivider = false
                        )

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        )
    }
}