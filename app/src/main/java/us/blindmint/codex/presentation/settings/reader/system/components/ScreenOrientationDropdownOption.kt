/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.system.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderScreenOrientation
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenOrientationDropdownOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    val currentOrientation = state.value.screenOrientation

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(title = stringResource(id = R.string.screen_orientation_option), padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                value = when (currentOrientation) {
                    ReaderScreenOrientation.DEFAULT -> stringResource(R.string.screen_orientation_default)
                    ReaderScreenOrientation.FREE -> stringResource(id = R.string.screen_orientation_free)
                    ReaderScreenOrientation.PORTRAIT -> stringResource(R.string.screen_orientation_portrait)
                    ReaderScreenOrientation.LANDSCAPE -> stringResource(R.string.screen_orientation_landscape)
                    ReaderScreenOrientation.REVERSE_PORTRAIT -> stringResource(R.string.screen_orientation_reverse_portrait)
                    ReaderScreenOrientation.REVERSE_LANDSCAPE -> stringResource(R.string.screen_orientation_reverse_landscape)
                    ReaderScreenOrientation.LOCKED_PORTRAIT -> stringResource(R.string.screen_orientation_locked_portrait)
                    ReaderScreenOrientation.LOCKED_LANDSCAPE -> stringResource(R.string.screen_orientation_locked_landscape)
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ReaderScreenOrientation.entries.forEach { orientation ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (orientation) {
                                    ReaderScreenOrientation.DEFAULT -> stringResource(R.string.screen_orientation_default)
                                    ReaderScreenOrientation.FREE -> stringResource(id = R.string.screen_orientation_free)
                                    ReaderScreenOrientation.PORTRAIT -> stringResource(R.string.screen_orientation_portrait)
                                    ReaderScreenOrientation.LANDSCAPE -> stringResource(R.string.screen_orientation_landscape)
                                    ReaderScreenOrientation.REVERSE_PORTRAIT -> stringResource(R.string.screen_orientation_reverse_portrait)
                                    ReaderScreenOrientation.REVERSE_LANDSCAPE -> stringResource(R.string.screen_orientation_reverse_landscape)
                                    ReaderScreenOrientation.LOCKED_PORTRAIT -> stringResource(R.string.screen_orientation_locked_portrait)
                                    ReaderScreenOrientation.LOCKED_LANDSCAPE -> stringResource(R.string.screen_orientation_locked_landscape)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            mainModel.onEvent(MainEvent.OnChangeScreenOrientation(orientation.toString()))
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}