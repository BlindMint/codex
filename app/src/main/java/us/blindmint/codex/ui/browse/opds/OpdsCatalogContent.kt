/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.browse.OpdsCategoryScreen
import us.blindmint.codex.ui.library.LibraryScreen as LibraryScreenClass
import us.blindmint.codex.ui.browse.opds.model.OpdsCatalogModel
import us.blindmint.codex.ui.library.LibraryScreen
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
    println("DEBUG: OpdsCatalogContent START - url: ${url ?: source.url}, title: $title")

    val navigator = LocalNavigator.current
    val model = hiltViewModel<OpdsCatalogModel>(
        key = url ?: "root" // Use URL as key to ensure different ViewModels for different feeds
    )
    val state by model.state.collectAsStateWithLifecycle()

    println("DEBUG: OpdsCatalogContent rendered with url: ${url ?: source.url}, title: $title")
    println("DEBUG: Current state - isLoading: ${state.isLoading}, error: ${state.error}, feedEntries: ${state.feed?.entries?.size ?: 0}")

    var selectedEntryForDownload by remember { mutableStateOf<OpdsEntry?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    println("DEBUG: OpdsCatalogContent rendered with url: ${url ?: source.url}, title: $title")
    println("DEBUG: Current state - isLoading: ${state.isLoading}, error: ${state.error}, feedEntries: ${state.feed?.entries?.size ?: 0}")

    try {
        model.loadFeed(source, url ?: source.url)
    } catch (e: Exception) {
        println("DEBUG: Exception in OpdsCatalogContent: ${e.message}")
        e.printStackTrace()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: source.name) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (state.isLoading || state.isDownloading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).fillMaxSize().wrapContentSize(Alignment.Center))
        } else if (state.error != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(
                    text = "Error loading catalog:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = state.error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                state = listState
            ) {
                // Separate categories and books
                val categories = state.feed?.entries?.filter { entry ->
                    entry.links.any { it.type?.startsWith("application/atom+xml") == true }
                } ?: emptyList()

                val books = state.feed?.entries?.filter { entry ->
                    entry.links.any { it.rel == "http://opds-spec.org/acquisition" }
                } ?: emptyList()

                // Categories section
                if (categories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    categories.forEach { entry ->
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                onClick = {
                                    println("DEBUG: Category clicked: ${entry.title}")
                                    println("DEBUG: Entry has ${entry.links.size} links")
                                    entry.links.forEachIndexed { index, link ->
                                        println("DEBUG: Link $index: href=${link.href}, type=${link.type}, rel=${link.rel}")
                                    }

                                    val link = entry.links.firstOrNull { it.type?.startsWith("application/atom+xml") == true }
                                        if (link != null) {
                                            println("DEBUG: Found atom link: ${link.href}")
                                            // Navigate to sub-feed
                                            val fullUrl = URI(source.url).resolve(link.href).toString()
                                            println("DEBUG: Resolved URL: $fullUrl")
                                            println("DEBUG: About to navigate to OpdsCatalogScreen with title: ${entry.title}")
                                            try {
                                                navigator.push(OpdsCategoryScreen(source, fullUrl, entry.title))
                                                println("DEBUG: Navigation call completed successfully")
                                            } catch (e: Exception) {
                                                println("DEBUG: Navigation failed with exception: ${e.message}")
                                                e.printStackTrace()
                                            }
                                        } else {
                                            println("DEBUG: No atom+xml link found")
                                        }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Category",
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = entry.title ?: "No title",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Tap to browse",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Books section
                if (books.isNotEmpty()) {
                    item {
                        Text(
                            text = "Books",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Limit books displayed to prevent performance issues with large feeds
                    val maxBooksToShow = 50
                    val displayedBooks = books.take(maxBooksToShow)

                    if (books.size > maxBooksToShow) {
                        item {
                            androidx.compose.material3.Text(
                                text = "Showing first $maxBooksToShow of ${books.size} books. Large catalogs may be limited for performance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Group books into rows for grid-like display
                    val bookRows = displayedBooks.chunked(3) // 3 books per row
                    bookRows.forEach { row ->
                        item {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { entry ->
                                    OpdsBookPreview(
                                        entry = entry,
                                        onClick = {
                                            selectedEntryForDownload = entry
                                            showDownloadDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill remaining space if row is not full
                                repeat(3 - row.size) {
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Download dialog
    if (showDownloadDialog && selectedEntryForDownload != null) {
        OpdsBookDownloadDialog(
            entry = selectedEntryForDownload!!,
            onConfirm = {
                // Start download process
                model.downloadBook(selectedEntryForDownload!!, source, {
                    // On success, navigate to library
                    navigator.push(LibraryScreenClass, saveInBackStack = false)
                })
                showDownloadDialog = false
                selectedEntryForDownload = null
            },
            onDismiss = {
                showDownloadDialog = false
                selectedEntryForDownload = null
            }
        )
    }
}