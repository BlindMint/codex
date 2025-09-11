/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.reader.font.components

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.reader.CustomFont
import ua.blindmint.codex.domain.ui.ButtonItem
import ua.blindmint.codex.presentation.core.components.common.StyledText
import ua.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import ua.blindmint.codex.ui.main.MainEvent
import ua.blindmint.codex.ui.main.MainModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun CustomFontsOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
        SettingsSubcategoryTitle(title = stringResource(id = R.string.custom_fonts_option), padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Display custom fonts and add button together
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                // Add custom font button
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

                // Display custom fonts
                state.value.customFonts.forEach { customFont ->
                    FilterChip(
                        modifier = Modifier.height(36.dp),
                        selected = state.value.fontFamily == "custom_${customFont.name}",
                        label = {
                            StyledText(
                                text = customFont.name,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        onClick = {
                            mainModel.onEvent(MainEvent.OnChangeFontFamily("custom_${customFont.name}"))
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    mainModel.onEvent(MainEvent.OnRemoveCustomFont(customFont))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(id = R.string.delete_custom_font)
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}