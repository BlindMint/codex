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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.utils.FuzzySearchHelper

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
    navigateToHelp: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val appearanceTitle = stringResource(id = R.string.appearance_settings)
    val appearanceDesc = stringResource(id = R.string.appearance_settings_desc)
    val readerTitle = stringResource(id = R.string.reader_settings)
    val readerDesc = stringResource(id = R.string.reader_settings_desc)
    val libraryTitle = stringResource(id = R.string.library_settings)
    val libraryDesc = stringResource(id = R.string.library_settings)
    val browseTitle = stringResource(id = R.string.browse_settings)
    val browseDesc = stringResource(id = R.string.browse_settings_desc)
    val importExportTitle = stringResource(id = R.string.import_export_settings)
    val importExportDesc = stringResource(id = R.string.import_export_settings_desc)
    val aboutTitle = stringResource(id = R.string.about_screen)
    val aboutDesc = stringResource(id = R.string.about_screen)
    val aboutIcon = painterResource(id = R.drawable.skull_small)

    val allSettingsItems = remember(
        appearanceTitle, appearanceDesc,
        readerTitle, readerDesc,
        libraryTitle, libraryDesc,
        browseTitle, browseDesc,
        importExportTitle, importExportDesc,
        aboutTitle, aboutDesc,
        aboutIcon
    ) {
        listOf(
            SettingsItem(
                id = "appearance",
                title = appearanceTitle,
                description = appearanceDesc,
                icon = Icons.Outlined.Palette,
                onClick = navigateToAppearanceSettings
            ),
            SettingsItem(
                id = "reader",
                title = readerTitle,
                description = readerDesc,
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                onClick = navigateToReaderSettings
            ),
            SettingsItem(
                id = "library",
                title = libraryTitle,
                description = libraryDesc,
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                onClick = navigateToLibrarySettings
            ),
            SettingsItem(
                id = "browse",
                title = browseTitle,
                description = browseDesc,
                icon = Icons.Outlined.Explore,
                onClick = navigateToBrowseSettings
            ),
            SettingsItem(
                id = "import_export",
                title = importExportTitle,
                description = importExportDesc,
                icon = Icons.Outlined.ImportExport,
                onClick = navigateToImportExportSettings
            ),
            SettingsItem(
                id = "about",
                title = aboutTitle,
                description = aboutDesc,
                icon = aboutIcon,
                onClick = navigateToAbout
            )
        )
    }

    val filteredItems = remember(searchQuery, allSettingsItems) {
        if (searchQuery.isBlank()) {
            allSettingsItems
        } else {
            FuzzySearchHelper.searchSettings(
                items = allSettingsItems,
                query = searchQuery,
                threshold = 50
            )
        }
    }

    LazyColumnWithScrollbar(
        Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(filteredItems) { item ->
            SettingsLayoutItem(
                icon = item.icon,
                title = item.title,
                description = item.description,
                onClick = item.onClick
            )
        }
    }
}
