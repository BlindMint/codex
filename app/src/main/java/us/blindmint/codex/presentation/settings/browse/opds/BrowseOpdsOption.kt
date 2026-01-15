/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.opds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.browse.OpdsAddSourceDialog
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel
import us.blindmint.codex.ui.settings.opds.OpdsSourcesState

@Composable
fun BrowseOpdsOption() {
    val sourcesModel = hiltViewModel<OpdsSourcesModel>()
    val sourcesState by sourcesModel.state.collectAsStateWithLifecycle()

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var sourceToEdit by remember { mutableStateOf<OpdsSourceEntity?>(null) }
    var sourceToDelete by remember { mutableStateOf<OpdsSourceEntity?>(null) }

    OpdsAddSourceDialog(
        showDialog = showAddSourceDialog,
        onDismiss = { showAddSourceDialog = false },
        onSourceAdded = { name, url, username, password ->
            sourcesModel.addOpdsSource(name, url, username, password)
            showAddSourceDialog = false
        }
    )

    // Edit dialog (reuse add dialog with pre-filled values)
    if (sourceToEdit != null) {
        OpdsAddSourceDialog(
            showDialog = true,
            initialName = sourceToEdit!!.name,
            initialUrl = sourceToEdit!!.url,
            initialUsername = sourceToEdit!!.username,
            initialPassword = sourceToEdit!!.password,
            onDismiss = { sourceToEdit = null },
            onSourceAdded = { name, url, username, password ->
                val updatedSource = sourceToEdit!!.copy(
                    name = name,
                    url = url,
                    username = username,
                    password = password
                )
                sourcesModel.updateOpdsSource(updatedSource)
                sourceToEdit = null
            }
        )
    }

    // Delete confirmation dialog
    if (sourceToDelete != null) {
        DeleteConfirmationDialog(
            sourceName = sourceToDelete!!.name,
            onConfirm = {
                sourcesModel.deleteOpdsSource(sourceToDelete!!.id)
                sourceToDelete = null
            },
            onDismiss = { sourceToDelete = null }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with Add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OPDS Sources",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(
                onClick = { showAddSourceDialog = true }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(id = R.string.add_opds_source),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // List of sources
        if (sourcesState.sources.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No OPDS sources configured",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add an OPDS source to browse and download books from online catalogs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            // Sources list
            sourcesState.sources.forEach { source ->
                OpdsSourceItem(
                    source = source,
                    onEdit = { sourceToEdit = source },
                    onDelete = { sourceToDelete = source }
                )
            }
        }
    }
}

@Composable
private fun OpdsSourceItem(
    source: OpdsSourceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!source.username.isNullOrBlank()) {
                    Text(
                        text = "Authenticated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit source",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete source",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    sourceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Remove OPDS Source",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Are you sure you want to remove \"$sourceName\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = onConfirm,
                    ) {
                        Text(
                            "Remove",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}