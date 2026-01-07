/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.opds.OpdsEntry
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.browse.OpdsCatalogScreen
import us.blindmint.codex.ui.browse.opds.model.OpdsCatalogModel
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsCatalogContent(
    source: OpdsSourceEntity,
    url: String?,
    title: String?,
    listState: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    navigateBack: () -> Unit
) {
    val navigator = LocalNavigator.current
    val model = hiltViewModel<OpdsCatalogModel>()
    val state by model.state.collectAsStateWithLifecycle()

    model.loadFeed(source, url ?: source.url)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: source.name) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).fillMaxSize().wrapContentSize(Alignment.Center))
        } else if (state.error != null) {
            Text("Error: ${state.error}", modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            LazyColumnWithScrollbar(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                state = listState
            ) {
                state.feed?.entries?.forEach { entry ->
                    item {
                        Text(
                            text = entry.title ?: "No title",
                            modifier = Modifier
                                .clickable {
                                    val link = entry.links.firstOrNull()
                                    if (link != null) {
                                        if (link.type?.startsWith("application/atom+xml") == true) {
                                            // Navigate to sub-feed
                                            val fullUrl = URI(source.url).resolve(link.href).toString()
                                            navigator.push(OpdsCatalogScreen(source, fullUrl, entry.title))
                                        } else {
                                            // Handle book download/import
                                            // TODO: implement import
                                        }
                                    }
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}