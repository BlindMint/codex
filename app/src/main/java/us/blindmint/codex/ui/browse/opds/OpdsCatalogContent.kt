/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.browse.opds.model.OpdsCatalogModel
import us.blindmint.codex.ui.browse.opds.OpdsCatalogContent
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

    var selectedEntryForDownload by remember { mutableStateOf<OpdsEntry?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(url) {
        model.loadFeed(source, url ?: source.url)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchMode) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                // Local filtering will be applied in the UI
                            },
                            placeholder = { Text("Search books...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (state.isSelectionMode) {
                        Text("${state.selectedBooks.size} selected")
                    } else {
                        Text(title ?: source.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isSelectionMode) {
                            model.toggleSelectionMode() // Exit selection mode first
                        } else {
                            navigateBack() // Go back to Catalogs screen
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.isSelectionMode) {
                        IconButton(
                            onClick = { model.clearSelection() },
                            enabled = state.selectedBooks.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear selection"
                            )
                        }
                        IconButton(
                            onClick = {
                                model.downloadSelectedBooks(source) {
                                     navigator.push(LibraryScreen(), saveInBackStack = false)
                                }
                            },
                            enabled = state.selectedBooks.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download selected",
                                tint = if (state.selectedBooks.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            isSearchMode = !isSearchMode
                            if (!isSearchMode) {
                                searchQuery = ""
                                // Local search filter will be cleared
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (isSearchMode) "Exit search" else "Search"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).fillMaxSize().wrapContentSize(Alignment.Center))
        } else if (state.isDownloading) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(32.dp)
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LinearProgressIndicator(
                    progress = { state.downloadProgress },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Text(
                    text = "Downloading book... ${(state.downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
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
                val allCategories = state.feed?.entries?.filter { entry ->
                    // Category if it has subsection links OR specific OPDS navigation links
                    // Exclude navigation breadcrumbs that go back to parent URLs
                    entry.links.any { link ->
                        link.rel == "subsection" ||
                        link.rel == "http://opds-spec.org/subsection" ||
                        (link.type?.startsWith("application/atom+xml") == true)
                    } && !entry.links.any { it.rel == "http://opds-spec.org/acquisition" } &&
                    // Exclude entries that link back to the current or parent URLs (navigation breadcrumbs)
                    entry.links.none { link ->
                        val resolvedUrl = runCatching {
                            java.net.URI(source.url).resolve(link.href).toString()
                        }.getOrNull()
                        resolvedUrl != null && (
                            resolvedUrl == state.feedUrl || // Same as current URL
                            state.feedUrl?.startsWith(resolvedUrl) == true // Current URL under this link
                        )
                    }
                } ?: emptyList()

                // Apply local search filter to categories
                val categories = if (isSearchMode && searchQuery.isNotBlank()) {
                    allCategories.filter { entry ->
                        entry.title?.contains(searchQuery, ignoreCase = true) == true
                    }
                } else {
                    allCategories
                }

                val allBooks = state.feed?.entries?.filter { entry ->
                    entry.links.any { it.rel == "http://opds-spec.org/acquisition" }
                } ?: emptyList()

                // Apply local search filter
                val books = if (isSearchMode && searchQuery.isNotBlank()) {
                    allBooks.filter { entry ->
                        entry.title?.contains(searchQuery, ignoreCase = true) == true ||
                        entry.author?.contains(searchQuery, ignoreCase = true) == true ||
                        entry.summary?.contains(searchQuery, ignoreCase = true) == true
                    }
                } else {
                    allBooks
                }

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
                                     val link = entry.links.firstOrNull { link ->
                                         link.type?.startsWith("application/atom+xml") == true ||
                                         link.rel == "subsection" ||
                                         link.rel == "http://opds-spec.org/subsection" ||
                                         (link.rel != "http://opds-spec.org/acquisition" && link.rel != "http://opds-spec.org/image/thumbnail" && !link.rel.isNullOrBlank())
                                     }
                                      if (link != null) {
                                          println("DEBUG: Found atom link: ${link.href}")
                                          // Navigate to sub-feed - double-encode dots to prevent HTTP client normalization
                                          val encodedHref = link.href.replace(".", "%252E")
                                          val fullUrl = if (encodedHref.startsWith("http")) {
                                              // Already a full URL
                                              encodedHref
                                          } else if (encodedHref.startsWith("/")) {
                                              // Absolute path - construct manually
                                              val baseUri = URI(source.url)
                                              val port = if (baseUri.port != -1 && baseUri.port != 80 && baseUri.port != 443) ":${baseUri.port}" else ""
                                              "${baseUri.scheme}://${baseUri.host}${port}${encodedHref}"
                                          } else {
                                              // Relative path - construct manually
                                              val baseUri = URI(source.url)
                                              val port = if (baseUri.port != -1 && baseUri.port != 80 && baseUri.port != 443) ":${baseUri.port}" else ""
                                              val basePath = baseUri.path.removeSuffix("/")
                                              val relativePath = if (encodedHref.startsWith("/")) encodedHref else "/${encodedHref}"
                                              "${baseUri.scheme}://${baseUri.host}${port}${basePath}${relativePath}"
                                          }
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

                    // Adaptive grid using FlowRow for responsive layout
                    item {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            books.forEach { entry ->
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 110.dp, max = 150.dp)
                                        .weight(1f)
                                ) {
                                    OpdsBookPreview(
                                        entry = entry,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                model.toggleBookSelection(entry.id)
                                            } else {
                                                selectedEntryForDownload = entry
                                                showDownloadDialog = true
                                            }
                                        },
                                        onLongClick = {
                                            if (!state.isSelectionMode) {
                                                model.toggleSelectionMode()
                                                model.toggleBookSelection(entry.id)
                                            }
                                        },
                                        isSelected = state.selectedBooks.contains(entry.id),
                                        isSelectionMode = state.isSelectionMode
                                    )
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
                    navigator.push(LibraryScreen(), saveInBackStack = false)
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