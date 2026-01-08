/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.opds

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel
import us.blindmint.codex.data.local.dto.OpdsSourceEntity

@Composable
fun BrowseOpdsManagementContent() {
    val context = LocalContext.current
    val opdsModel = hiltViewModel<OpdsSourcesModel>()
    val sources = opdsModel.state.collectAsStateWithLifecycle().value.sources

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf<OpdsSourceEntity?>(null) }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        // OPDS sources list
        sources.forEach { source ->
            ListItem(
                headlineContent = {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = source.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingContent = {
                    Column {
                        IconButton(
                            onClick = {
                                selectedSource = source
                                showEditDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit source",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedSource = source
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete source",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }

        // Add button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Setup OPDS Source")
        }
    }

    // Add/Edit OPDS Source Dialog
    if (showAddDialog || showEditDialog) {
        var name by remember { mutableStateOf(selectedSource?.name ?: "") }
        var url by remember { mutableStateOf(selectedSource?.url ?: "") }
        var username by remember { mutableStateOf(selectedSource?.username ?: "") }
        var password by remember { mutableStateOf(selectedSource?.password ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                showEditDialog = false
                selectedSource = null
                name = ""
                url = ""
                username = ""
                password = ""
            },
            title = { Text(if (showEditDialog) "Edit OPDS Source" else "Add OPDS Source") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username (optional)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val workingUrl = opdsModel.testConnection(url, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() })

                                if (showEditDialog && selectedSource != null) {
                                    // Update existing source
                                    val updated = selectedSource!!.copy(
                                        name = name,
                                        url = workingUrl,
                                        username = username.takeIf { it.isNotBlank() },
                                        password = password.takeIf { it.isNotBlank() }
                                    )
                                    opdsModel.updateOpdsSource(updated)
                                    Toast.makeText(context, "Source updated successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Add new source
                                    opdsModel.addOpdsSource(name, workingUrl, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() })
                                    val successMessage = if (workingUrl != url) {
                                        "Source added successfully (URL: $workingUrl)"
                                    } else {
                                        "Source added successfully"
                                    }
                                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                }

                                showAddDialog = false
                                showEditDialog = false
                                selectedSource = null
                                name = ""
                                url = ""
                                username = ""
                                password = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(if (showEditDialog) "Update" else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        showEditDialog = false
                        selectedSource = null
                        name = ""
                        url = ""
                        username = ""
                        password = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedSource != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedSource = null
            },
            title = { Text("Delete OPDS Source") },
            text = { Text("Delete \"${selectedSource!!.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        opdsModel.deleteOpdsSource(selectedSource!!.id)
                        Toast.makeText(context, "Source deleted", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                        selectedSource = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedSource = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}