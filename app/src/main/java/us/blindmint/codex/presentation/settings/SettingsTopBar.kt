/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.SearchTextField
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBar
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBarData
import us.blindmint.codex.presentation.navigator.NavigatorBackIconButton
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    navigateBack: () -> Unit,
    showSearch: Boolean,
    searchQuery: String,
    focusRequester: FocusRequester,
    onSearchVisibilityChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        isTopBarScrolled = showSearch,
        shownTopBar = if (showSearch) 1 else 0,
        topBars = listOf(
            TopAppBarData(
                contentID = 0,
                contentNavigationIcon = {
                    NavigatorBackIconButton(
                        navigateBack = navigateBack
                    )
                },
                contentTitle = {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StyledText(text = stringResource(id = R.string.settings_screen))
                    }
                },
                contentActions = {
                    IconButton(
                        icon = Icons.Default.Search,
                        contentDescription = R.string.search_content_desc,
                        disableOnClick = true
                    ) {
                        onSearchVisibilityChange(true)
                    }
                }
            ),
            TopAppBarData(
                contentID = 1,
                contentNavigationIcon = {
                    NavigatorBackIconButton(
                        navigateBack = navigateBack
                    )
                },
                contentTitle = {
                    SearchTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester),
                        initialQuery = searchQuery,
                        onQueryChange = {
                            onSearchQueryChange(it)
                        },
                        onSearch = {
                            onSearch()
                        }
                    )
                },
                contentActions = {}
            )
        )
    )
}