/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoResetProgressDialog(
    actionResetReadingProgress: (BookInfoEvent.OnActionResetReadingProgress) -> Unit,
    dismissDialog: (BookInfoEvent.OnDismissDialog) -> Unit
) {
    val context = LocalContext.current
    var resetNormalReading by remember { mutableStateOf(true) }
    var resetSpeedReading by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { dismissDialog(BookInfoEvent.OnDismissDialog) },
        title = { Text("Reset Progress") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Which progress do you want to reset?")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = resetNormalReading && !resetSpeedReading,
                        onClick = { resetNormalReading = true; resetSpeedReading = false },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text("Normal reading")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = !resetNormalReading && resetSpeedReading,
                        onClick = { resetNormalReading = false; resetSpeedReading = true },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text("Speed reading")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = resetNormalReading && resetSpeedReading,
                        onClick = { resetNormalReading = true; resetSpeedReading = true },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text("Both")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { dismissDialog(BookInfoEvent.OnDismissDialog) }
            ) {
                Text("Cancel")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    actionResetReadingProgress(
                        BookInfoEvent.OnActionResetReadingProgress(
                            context = context,
                            resetNormalReading = resetNormalReading,
                            resetSpeedReading = resetSpeedReading
                        )
                    )
                }
            ) {
                Text("Reset")
            }
        }
    )
}
