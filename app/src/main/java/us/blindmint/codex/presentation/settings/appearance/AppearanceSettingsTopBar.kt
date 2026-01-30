/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.appearance

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.navigator.NavigatorBackIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    navigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            StyledText(stringResource(id = R.string.appearance_settings))
        },
        navigationIcon = {
            NavigatorBackIconButton(
                navigateBack = navigateBack
            )
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}