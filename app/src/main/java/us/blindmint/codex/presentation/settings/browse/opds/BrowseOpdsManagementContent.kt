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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel

@Composable
fun BrowseOpdsManagementContent() {
    val context = LocalContext.current
    val opdsModel = hiltViewModel<OpdsSourcesModel>()
    val sources = opdsModel.state.collectAsStateWithLifecycle().value.sources

    var showDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        sources.forEach { source ->
            Text(
                text = source.name,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Setup OPDS Source")
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add OPDS Source") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username (optional)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                var testUrl = url.trimEnd('/')
                                if (!testUrl.endsWith("/opds")) {
                                    testUrl += "/opds"
                                }
                                opdsModel.testConnection(testUrl, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() })
                                opdsModel.addOpdsSource(name, testUrl, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() })
                                Toast.makeText(context, "Source added successfully", Toast.LENGTH_SHORT).show()
                                showDialog = false
                                name = ""
                                url = ""
                                username = ""
                                password = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to connect: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}