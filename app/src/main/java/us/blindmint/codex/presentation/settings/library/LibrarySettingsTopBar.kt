/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.library

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettingsTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    navigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.library_settings),
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            androidx.compose.material3.IconButton(
                onClick = navigateBack
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.go_back_content_desc)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}