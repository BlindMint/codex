/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.outlined.HelpOutline  // Help disabled
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar

@Composable
fun SettingsLayout(
    listState: LazyListState,
    paddingValues: PaddingValues,
    navigateToAppearanceSettings: () -> Unit,
    navigateToReaderSettings: () -> Unit,
    navigateToLibrarySettings: () -> Unit,
    navigateToBrowseSettings: () -> Unit,
    navigateToImportExportSettings: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToHelp: () -> Unit
) {
    LazyColumnWithScrollbar(
        Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SettingsLayoutItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(id = R.string.appearance_settings),
                description = stringResource(id = R.string.appearance_settings_desc)
            ) {
                navigateToAppearanceSettings()
            }
        }

        item {
            SettingsLayoutItem(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                title = stringResource(id = R.string.reader_settings),
                description = stringResource(id = R.string.reader_settings_desc)
            ) {
                navigateToReaderSettings()
            }
        }

        item {
            SettingsLayoutItem(
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                title = stringResource(id = R.string.library_settings),
                description = stringResource(id = R.string.library_settings)
            ) {
                navigateToLibrarySettings()
            }
        }

        item {
            SettingsLayoutItem(
                icon = Icons.Outlined.Explore,
                title = stringResource(id = R.string.browse_settings),
                description = stringResource(id = R.string.browse_settings_desc)
            ) {
                navigateToBrowseSettings()
            }
        }

        item {
            SettingsLayoutItem(
                icon = Icons.Outlined.ImportExport,
                title = stringResource(id = R.string.import_export_settings),
                description = stringResource(id = R.string.import_export_settings_desc)
            ) {
                navigateToImportExportSettings()
            }
        }

        // Help menu item disabled - see to-do.md
        // item {
        //     SettingsLayoutItem(
        //         icon = Icons.AutoMirrored.Outlined.HelpOutline,
        //         title = stringResource(id = R.string.help_screen),
        //         description = stringResource(id = R.string.help_screen)
        //     ) {
        //         navigateToHelp()
        //     }
        // }

        item {
            SettingsLayoutItem(
                icon = painterResource(id = R.drawable.bottle_tonic_skull_outline),
                title = stringResource(id = R.string.about_screen),
                description = stringResource(id = R.string.about_screen)
            ) {
                navigateToAbout()
            }
        }
    }
}