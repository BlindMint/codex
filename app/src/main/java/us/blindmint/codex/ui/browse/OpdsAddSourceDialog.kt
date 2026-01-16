/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import us.blindmint.codex.R
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.settings.BrowseSettingsScreen
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel

@Composable
fun OpdsAddSourceDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialUrl: String = "",
    initialUsername: String? = null,
    initialPassword: String? = null,
    onSourceAdded: ((String, String, String?, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val opdsModel = hiltViewModel<OpdsSourcesModel>()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var username by remember { mutableStateOf(initialUsername ?: "") }
    var password by remember { mutableStateOf(initialPassword ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRootDirectoryPrompt by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
                if (onSourceAdded == null) { // Only reset for add dialog, not edit
                    name = ""
                    url = ""
                    username = ""
                    password = ""
                    passwordVisible = false
                }
            },
            title = { Text(if (initialName.isEmpty()) "Add OPDS Source" else "Edit OPDS Source") },
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
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                androidx.compose.material3.Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Check if Codex directory is configured
                            val isCodexConfigured = opdsModel.isCodexDirectoryConfigured()
                            if (!isCodexConfigured) {
                                showRootDirectoryPrompt = true
                                return@launch
                            }

                            if (onSourceAdded != null) {
                                // Edit mode - use callback
                                onSourceAdded(name, url, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() })
                            } else {
                                // Add mode - test connection first
                                try {
                                    val workingUrl = opdsModel.testConnection(url, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() })
                                    opdsModel.addOpdsSource(name, workingUrl, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() })
                                    val successMessage = if (workingUrl != url) {
                                        "Source added successfully (URL: $workingUrl)"
                                    } else {
                                        "Source added successfully"
                                    }
                                    successMessage.showToast(context, longToast = false)

                                    onDismiss()
                                    name = ""
                                    url = ""
                                    username = ""
                                    password = ""
                                    passwordVisible = false
                                } catch (e: Exception) {
                                    "Failed to connect: ${e.message}".showToast(context, longToast = false)
                                }
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        if (onSourceAdded == null) { // Only reset for add dialog, not edit
                            name = ""
                            url = ""
                            username = ""
                            password = ""
                            passwordVisible = false
                        }
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Root directory prompt dialog
    if (showRootDirectoryPrompt) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRootDirectoryPrompt = false },
            title = { androidx.compose.material3.Text("Set Codex Directory") },
            text = {
                androidx.compose.material3.Text(
                    "You need to configure your Codex directory before adding OPDS sources. " +
                    "This is where downloaded books will be stored."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showRootDirectoryPrompt = false
                        onDismiss() // Dismiss the OPDS dialog
                        navigator.push(BrowseSettingsScreen)
                    }
                ) {
                    androidx.compose.material3.Text("Go to Settings")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showRootDirectoryPrompt = false }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}