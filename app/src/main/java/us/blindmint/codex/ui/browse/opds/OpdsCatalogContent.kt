/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import us.blindmint.codex.presentation.core.components.top_bar.TopAppBar
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBarData
import us.blindmint.codex.presentation.core.components.common.SearchTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.browse.OpdsCategoryScreen
import us.blindmint.codex.ui.browse.opds.model.OpdsCatalogModel
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.settings.BrowseSettingsScreen
import us.blindmint.codex.ui.settings.opds.download.OpdsDownloadModel
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
    val model = hiltViewModel<OpdsCatalogModel>(
        key = url ?: "root"
    )
    val downloadModel = hiltViewModel<OpdsDownloadModel>()
    val state by model.state.collectAsStateWithLifecycle()

    var selectedEntryForDetails by remember { mutableStateOf<OpdsEntry?>(null) }
    var showDetailsBottomSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show download error as snackbar
    LaunchedEffect(state.downloadError) {
        state.downloadError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
                model.clearDownloadError()
            }
        }
    }

    LaunchedEffect(url) {
        android.util.Log.d("OPDS_DEBUG", "LaunchedEffect triggered for URL: $url")
        val isDownloadAccessible = downloadModel.isDownloadDirectoryAccessible()
        model.loadFeed(source, url ?: source.url, isDownloadAccessible)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                isTopBarScrolled = false,
                shownTopBar = if (showSearch) 1 else 0,
                topBars = listOf(
                    TopAppBarData(
                        contentID = 0,
                        contentNavigationIcon = {
                            IconButton(onClick = {
                                if (state.isSelectionMode) {
                                    model.toggleSelectionMode()
                                } else {
                                    navigateBack()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        contentTitle = {
                            when {
                                state.isSelectionMode -> Text("${state.selectedBooks.size} selected")
                                else -> Text(title ?: source.name)
                            }
                        },
                        contentActions = {
                            when {
                                state.isSelectionMode -> {
                                    IconButton(
                                        onClick = { model.clearSelection() },
                                        enabled = state.selectedBooks.isNotEmpty()
                                    ) {
                                        Icon(Icons.Default.Clear, "Clear selection")
                                    }
                                    IconButton(
                                        onClick = {
                                            model.downloadSelectedBooks(source) {
                                                navigator.push(LibraryScreen, saveInBackStack = false)
                                            }
                                        },
                                        enabled = state.selectedBooks.isNotEmpty()
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            "Download selected",
                                            tint = if (state.selectedBooks.isNotEmpty())
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                else -> IconButton(onClick = { showSearch = true }) {
                                    Icon(Icons.Default.Search, "Search")
                                }
                            }
                        }
                    ),
                    TopAppBarData(
                        contentID = 1,
                        contentNavigationIcon = {
                            IconButton(onClick = {
                                showSearch = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit search")
                            }
                        },
                        contentTitle = {
                            SearchTextField(
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onGloballyPositioned {
                                        if (showSearch) {
                                            focusRequester.requestFocus()
                                        }
                                    },
                                initialQuery = searchQuery,
                                onQueryChange = { query ->
                                    searchQuery = query
                                },
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        model.search(searchQuery, source)
                                        showSearch = false
                                    }
                                }
                            )
                        },
                        contentActions = {}
                    )
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Show warning banner if download directory is not accessible
            if (!state.isDownloadDirectoryAccessible) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Codex directory not configured",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Please configure your Codex directory in Settings > Storage to enable book downloads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                // Navigate to Browse settings (which includes storage)
                                navigator.push(BrowseSettingsScreen)
                            }
                        ) {
                            Text("Fix")
                        }
                    }
                }
            }

            when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.padding(padding).fillMaxSize().wrapContentSize(Alignment.Center)
            )
            state.isDownloading -> Column(
                modifier = Modifier.padding(padding).padding(32.dp).fillMaxSize().wrapContentSize(Alignment.Center),
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
                    textAlign = TextAlign.Center
                )
            }
            state.error != null -> Column(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().wrapContentSize(Alignment.Center)
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
            else -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                state = listState
            ) {
                  val allCategories = state.feed?.entries?.filter { entry ->
                      val hasNavigationLinks = entry.links.any { link ->
                          link.rel == "subsection" ||
                          link.rel == "http://opds-spec.org/subsection" ||
                          link.type?.startsWith("application/atom+xml") == true ||
                          (link.rel != null && link.rel != "http://opds-spec.org/acquisition" &&
                           link.rel != "http://opds-spec.org/image/thumbnail" &&
                           link.rel != "self" && link.rel != "alternate")
                      }
                      val hasAcquisitionLinks = entry.links.any { it.rel == "http://opds-spec.org/acquisition" }
                      val hasSelfReferencingLinks = entry.links.any { link ->
                          link.href?.let { href ->
                              val resolvedUrl = runCatching {
                                  URI(source.url).resolve(href).toString()
                              }.getOrNull()
                              resolvedUrl != null && (
                                  resolvedUrl == state.feedUrl ||
                                  state.feedUrl?.startsWith(resolvedUrl) == true
                              )
                          } ?: false
                      }

                      val isCategory = hasNavigationLinks && !hasAcquisitionLinks && !hasSelfReferencingLinks

                      // Debug logging
                      if (entry.title?.contains("sub", ignoreCase = true) == true || isCategory) {
                          android.util.Log.d("OPDS_DEBUG", "Entry '${entry.title}' - Navigation: $hasNavigationLinks, Acquisition: $hasAcquisitionLinks, SelfRef: $hasSelfReferencingLinks, IsCategory: $isCategory")
                          entry.links.forEach { link ->
                              android.util.Log.d("OPDS_DEBUG", "  Link: rel='${link.rel}', type='${link.type}', href='${link.href}'")
                          }
                      }

                      isCategory
                  } ?: emptyList()

                 val categories = if (showSearch && searchQuery.isNotBlank()) {
                     allCategories.filter { entry ->
                         entry.title?.contains(searchQuery, ignoreCase = true) == true
                     }
                 } else allCategories

                 android.util.Log.d("OPDS_DEBUG", "Feed URL: ${state.feedUrl}")
                 android.util.Log.d("OPDS_DEBUG", "Total entries: ${state.feed?.entries?.size ?: 0}")
                 android.util.Log.d("OPDS_DEBUG", "Categories found: ${allCategories.size}")
                 android.util.Log.d("OPDS_DEBUG", "Categories shown: ${categories.size}")

                 val allBooks = state.feed?.entries?.filter { entry ->
                     entry.links.any { it.rel == "http://opds-spec.org/acquisition" } &&
                     !entry.links.any { link ->
                         link.rel == "subsection" ||
                         link.rel == "http://opds-spec.org/subsection" ||
                         link.type?.startsWith("application/atom+xml") == true
                     }
                 } ?: emptyList()

                 android.util.Log.d("OPDS_DEBUG", "Books found: ${allBooks.size}")

                val books = if (showSearch && searchQuery.isNotBlank()) {
                    allBooks.filter { entry ->
                        entry.title?.contains(searchQuery, ignoreCase = true) == true ||
                        entry.author?.contains(searchQuery, ignoreCase = true) == true ||
                        entry.summary?.contains(searchQuery, ignoreCase = true) == true
                    }
                } else allBooks

                if (categories.isNotEmpty()) {
                    item {
                        Text(
                            "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(categories) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = {
                                android.util.Log.d("OPDS_DEBUG", "Category tapped: '${entry.title}'")
                                android.util.Log.d("OPDS_DEBUG", "Source URL: ${source.url}")

                                // Find navigation link - try multiple strategies
                                val link = entry.links.firstOrNull { link ->
                                    link.href?.isNotBlank() == true && (
                                        link.type?.startsWith("application/atom+xml") == true ||
                                        link.rel == "subsection" ||
                                        link.rel == "http://opds-spec.org/subsection" ||
                                        (link.rel != null && link.rel != "http://opds-spec.org/acquisition" &&
                                         link.rel != "http://opds-spec.org/image/thumbnail" &&
                                         link.rel != "self" && link.rel != "alternate")
                                    )
                                }

                                android.util.Log.d("OPDS_DEBUG", "Found navigation link: ${link?.let { "rel='${it.rel}', href='${it.href}'" } ?: "null"}")

                                if (link != null) {
                                    var href = link.href.trim()

                                    // Special handling for href ending with "." - double encode to handle server issues
                                    if (href.endsWith(".")) {
                                        href = href.replace(".", "%252E")
                                    }

                                    val fullUrl = if (href.startsWith("http")) {
                                        href
                                    } else {
                                        // Manual URL construction to avoid URI.resolve() issues with special chars
                                        if (href.startsWith("/")) {
                                            // Absolute path - combine with domain from source URL
                                            val domain = source.url.substringBefore("/opds").trimEnd('/')
                                            "$domain$href"
                                        } else {
                                            // Relative path - append to source URL
                                            val baseUrl = source.url.removeSuffix("/")
                                            "$baseUrl/$href"
                                        }
                                    }
                                    android.util.Log.d("OPDS_DEBUG", "Final navigation URL: $fullUrl")
                                    android.util.Log.d("OPDS_DEBUG", "About to call navigator.push...")
                                    try {
                                        navigator.push(OpdsCategoryScreen(source, fullUrl, entry.title))
                                        android.util.Log.d("OPDS_DEBUG", "navigator.push() completed successfully")
                                    } catch (e: Exception) {
                                        android.util.Log.e("OPDS_DEBUG", "Error calling navigator.push()", e)
                                    }
                                } else {
                                    android.util.Log.d("OPDS_DEBUG", "No navigation link found for '${entry.title}'")
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    "Category",
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Column {
                                    Text(
                                        entry.title ?: "No title",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Tap to browse",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (books.isNotEmpty()) {
                    item {
                        Text(
                            "Books",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }

                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            books.forEach { entry ->
                                Box(
                                    modifier = Modifier.widthIn(min = 110.dp, max = 150.dp).weight(1f)
                                ) {
                                    OpdsBookPreview(
                                        entry = entry,
                                        baseUrl = state.feedUrl ?: source.url,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                model.toggleBookSelection(entry.id)
                                            } else {
                                                selectedEntryForDetails = entry
                                                showDetailsBottomSheet = true
                                            }
                                        },
                                        onLongClick = {
                                            if (!state.isSelectionMode) {
                                                model.toggleSelectionMode()
                                                model.toggleBookSelection(entry.id)
                                            }
                                        },
                                        isSelected = state.selectedBooks.contains(entry.id),
                                        isSelectionMode = state.isSelectionMode,
                                        username = state.username,
                                        password = state.password
                                    )
                                }
                            }
                        }
                    }
                }

                // Load More button
                if (state.hasNextPage && !state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = { model.loadMore(source) },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("Load More")
                            }
                        }
                    }
                }

                // Loading indicator for pagination
                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        }
    }

    if (showDetailsBottomSheet && selectedEntryForDetails != null) {
        OpdsBookDetailsBottomSheet(
            entry = selectedEntryForDetails!!,
            baseUrl = state.feedUrl ?: source.url,
            isDownloadEnabled = state.isDownloadDirectoryAccessible,
            onDownload = {
                model.downloadBook(selectedEntryForDetails!!, source) {
                    navigator.push(LibraryScreen, saveInBackStack = false)
                }
                showDetailsBottomSheet = false
                selectedEntryForDetails = null
            },
            onDismiss = {
                showDetailsBottomSheet = false
                selectedEntryForDetails = null
            },
            username = state.username,
            password = state.password
        )
    }
}