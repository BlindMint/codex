/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import us.blindmint.codex.R
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.browse.BrowseTopBar
import us.blindmint.codex.ui.settings.BrowseSettingsScreen
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel

object BrowseScreen : Screen {

    const val ADD_DIALOG = "add_dialog"

    const val FILTER_BOTTOM_SHEET = "filter_bottom_sheet"

    val refreshListChannel: Channel<Unit> = Channel(Channel.CONFLATED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = hiltViewModel<OpdsSourcesModel>()

        val sources = screenModel.state.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.browse_screen)) })
            }
        ) { padding ->
            LazyColumn(Modifier.padding(padding)) {
                item {
                    Text(
                        text = "Local Files",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    Button(onClick = { navigator.push(BrowseSettingsScreen) }) {
                        Text("Manage Local Sources")
                    }
                }
                item {
                    Text(
                        text = "OPDS Catalogs",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(sources.value.sources) { source ->
                    Text(source.name, modifier = Modifier.padding(16.dp))
                }
                item {
                    Button(onClick = { /* TODO: add source */ }) {
                        Text("Add OPDS Source")
                    }
                }
            }
        }
    }
}