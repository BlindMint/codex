/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

/**
 * Modal bottom sheet for editing a list of metadata items (tags, series, languages).
 *
 * Changes are committed when the sheet is dismissed — no explicit "Save" button required.
 * Tapping X removes a chip immediately (no confirmation dialog). The keyboard Done action
 * and the "+ Add" button both add the typed item.
 *
 * The chip list grows naturally for small sets. Once it would exceed ~55% of the screen
 * height it becomes internally scrollable, keeping the drag handle and add section always
 * visible.
 */
@Composable
fun MetadataItemEditor(
    title: String,
    items: List<String>,
    onItemsChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentItems by remember { mutableStateOf(items) }
    var newItemText by remember { mutableStateOf("") }

    fun addItem() {
        val trimmed = newItemText.trim()
        if (trimmed.isNotBlank() && trimmed !in currentItems) {
            currentItems = currentItems + trimmed
            newItemText = ""
        }
    }

    ModalBottomSheet(
        modifier = modifier.fillMaxWidth(),
        onDismissRequest = { onItemsChanged(currentItems) },
        sheetGesturesEnabled = true
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        // Chips list grows freely up to ~55% of screen height, then scrolls internally.
        // Header + add section occupy the rest, keeping total sheet well under ~85%.
        val maxChipsHeight = screenHeight * 0.55f

        Column(modifier = Modifier.fillMaxWidth()) {

            // Title header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsSubcategoryTitle(
                    title = title,
                    padding = 0.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Chip list — grows naturally, scrollable once it hits maxChipsHeight
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxChipsHeight)
            ) {
                items(currentItems, key = { it }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick = { currentItems = currentItems - item },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.remove_content_desc),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Add section — always visible below the chip list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    label = { Text(stringResource(id = R.string.add_new)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { addItem() }),
                    singleLine = true
                )

                Button(
                    onClick = { addItem() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newItemText.trim().isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.add))
                }
            }
        }
    }
}
