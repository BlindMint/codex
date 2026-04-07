/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.font.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.CustomFont
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun FontFamilyChipsOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentFontFamily = state.value.fontFamily

    var showDeleteDialog by remember { mutableStateOf<CustomFont?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Get the file name from URI
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("_display_name")
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            }

            fileName?.let { name ->
                // Copy the font file to app storage
                val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
                val fontFile = File(fontsDir, name)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(fontFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val customFont = CustomFont(name, fontFile.absolutePath)
                    mainModel.onEvent(MainEvent.OnAddCustomFont(customFont))
                } catch (e: Exception) {
                    // Handle error - could show a toast or log
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(title = stringResource(id = R.string.font_family_option), padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Create combined list of built-in and custom fonts, sorted alphabetically
        val builtInFonts = provideFonts().map { font ->
            Triple(
                font.fontName.asString(),
                ButtonItem(
                    id = font.id,
                    title = font.fontName.asString(),
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = font.font
                    ),
                    selected = font.id == currentFontFamily
                ),
                null // No custom font data for built-in fonts
            )
        }

        val customFonts = state.value.customFonts.map { customFont ->
            Triple(
                customFont.name,
                ButtonItem(
                    id = "custom_${customFont.name}",
                    title = customFont.name,
                    textStyle = MaterialTheme.typography.labelLarge,
                    selected = "custom_${customFont.name}" == currentFontFamily
                ),
                customFont // Include custom font data for deletion
            )
        }

        val allFontsSorted = (builtInFonts + customFonts).sortedBy { it.first }

        // Display fonts and add button together
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                // Display all fonts (built-in and custom)
                allFontsSorted.forEach { (_, buttonItem, customFont) ->
                    if (customFont != null) {
                        // Custom font with delete button
                        FilterChip(
                            modifier = Modifier.height(36.dp),
                            selected = buttonItem.selected,
                            label = {
                                StyledText(
                                    text = buttonItem.title,
                                    style = buttonItem.textStyle
                                )
                            },
                            onClick = {
                                mainModel.onEvent(MainEvent.OnChangeFontFamily(buttonItem.id))
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showDeleteDialog = customFont }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(id = R.string.delete_custom_font)
                                    )
                                }
                            }
                        )
                    } else {
                        // Built-in font
                        FilterChip(
                            modifier = Modifier.height(36.dp),
                            selected = buttonItem.selected,
                            label = {
                                StyledText(
                                    text = buttonItem.title,
                                    style = buttonItem.textStyle
                                )
                            },
                            onClick = {
                                mainModel.onEvent(MainEvent.OnChangeFontFamily(buttonItem.id))
                            }
                        )
                    }
                }

                // Add custom font button at the end
                FilterChip(
                    modifier = Modifier.height(36.dp),
                    selected = false,
                    label = {
                        StyledText(
                            text = stringResource(id = R.string.add_custom_font),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    onClick = {
                        launcher.launch("font/*")
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.add_custom_font)
                        )
                    }
                )
            }
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { customFont ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_custom_font)) },
            text = {
                Text(
                    stringResource(R.string.confirm_delete_custom_font, customFont.name)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainModel.onEvent(MainEvent.OnRemoveCustomFont(customFont))
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}