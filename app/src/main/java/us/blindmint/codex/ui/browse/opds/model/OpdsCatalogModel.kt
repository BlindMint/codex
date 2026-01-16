/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.opds.OpdsFeed
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.repository.OpdsRepository
import us.blindmint.codex.domain.use_case.opds.ImportOpdsBookUseCase
import javax.inject.Inject

@HiltViewModel
class OpdsCatalogModel @Inject constructor(
    private val opdsRepository: OpdsRepository,
    private val importOpdsBookUseCase: ImportOpdsBookUseCase,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OpdsCatalogState())
    val state = _state.asStateFlow()

    fun loadFeed(source: OpdsSourceEntity, url: String, isDownloadDirectoryAccessible: Boolean = true) {
        android.util.Log.d("OPDS_DEBUG", "loadFeed called for URL: $url")

        if (state.value.feed != null && state.value.feedUrl == url) {
            android.util.Log.d("OPDS_DEBUG", "Feed already loaded for this URL, skipping")
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            feedUrl = url,
            isDownloadDirectoryAccessible = isDownloadDirectoryAccessible,
            username = source.username,
            password = source.password
        )
        viewModelScope.launch {
            try {
                android.util.Log.d("OPDS_DEBUG", "Fetching feed from: $url")
                val feed = opdsRepository.fetchFeed(url, source.username, source.password)
                val nextPageUrl = feed.links.firstOrNull { it.rel == "next" }?.href
                android.util.Log.d("OPDS_DEBUG", "Feed loaded successfully: ${feed.entries?.size ?: 0} entries, title: ${feed.title}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    feed = feed,
                    hasNextPage = nextPageUrl != null,
                    nextPageUrl = nextPageUrl
                )
            } catch (e: Exception) {
                android.util.Log.e("OPDS_DEBUG", "Error loading feed from $url", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadMore(source: OpdsSourceEntity) {
        val nextUrl = state.value.nextPageUrl ?: return
        _state.value = _state.value.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val nextFeed = opdsRepository.loadMore(nextUrl, source.username, source.password)
                val currentFeed = state.value.feed
                if (currentFeed != null && nextFeed != null) {
                    // Combine current entries with new entries
                    val combinedEntries = currentFeed.entries + nextFeed.entries
                    val combinedFeed = currentFeed.copy(entries = combinedEntries, links = nextFeed.links)

                    val nextPageUrl = nextFeed.links.firstOrNull { it.rel == "next" }?.href
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        feed = combinedFeed,
                        hasNextPage = nextPageUrl != null,
                        nextPageUrl = nextPageUrl
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingMore = false, error = e.message)
            }
        }
    }

    fun downloadBook(entry: OpdsEntry, source: OpdsSourceEntity, onSuccess: () -> Unit) {
        _state.value = _state.value.copy(isDownloading = true, downloadProgress = 0f, downloadError = null)
        viewModelScope.launch {
            try {
                val bookWithCover = importOpdsBookUseCase(
                    opdsEntry = entry,
                    sourceUrl = source.url,
                    username = source.username,
                    password = source.password,
                    onProgress = { progress ->
                        _state.value = _state.value.copy(downloadProgress = progress)
                    }
                )
                if (bookWithCover != null) {
                    // Save the downloaded book to the database
                    bookRepository.insertBook(bookWithCover)
                    _state.value = _state.value.copy(isDownloading = false, downloadProgress = 0f)
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(isDownloading = false, downloadProgress = 0f, downloadError = "Failed to download book")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isDownloading = false, downloadProgress = 0f, downloadError = "Download failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun clearDownloadError() {
        _state.value = _state.value.copy(downloadError = null)
    }

    fun search(query: String, source: OpdsSourceEntity) {
        println("DEBUG: OpdsCatalogModel.search called with query: $query")
        if (query.isBlank()) {
            // If query is empty, reload the original feed
            loadFeed(source, source.url)
            return
        }

        val currentFeed = state.value.feed
        if (currentFeed == null) {
            _state.value = _state.value.copy(error = "No feed loaded to search in")
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                println("DEBUG: Calling opdsRepository.search...")
                val searchFeed = opdsRepository.search(currentFeed, query, source.username, source.password)
                println("DEBUG: Search completed, entries: ${searchFeed.entries.size}")
                _state.value = _state.value.copy(isLoading = false, feed = searchFeed)
                println("DEBUG: Search state updated")
            } catch (e: Exception) {
                println("DEBUG: Search failed: ${e.message}")
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleSelectionMode() {
        _state.value = _state.value.copy(
            isSelectionMode = !_state.value.isSelectionMode,
            selectedBooks = if (_state.value.isSelectionMode) emptySet() else _state.value.selectedBooks
        )
    }

    fun toggleBookSelection(bookId: String) {
        val currentSelection = _state.value.selectedBooks
        val newSelection = if (currentSelection.contains(bookId)) {
            currentSelection - bookId
        } else {
            currentSelection + bookId
        }
        _state.value = _state.value.copy(selectedBooks = newSelection)
    }

    fun selectAllBooks() {
        val allBookIds = _state.value.feed?.entries
            ?.filter { it.links.any { link -> link.rel == "http://opds-spec.org/acquisition" } }
            ?.map { it.id }
            ?.toSet() ?: emptySet()
        _state.value = _state.value.copy(selectedBooks = allBookIds)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedBooks = emptySet())
    }

    fun downloadSelectedBooks(source: OpdsSourceEntity, onComplete: () -> Unit) {
        val selectedEntries = _state.value.feed?.entries?.filter { it.id in _state.value.selectedBooks } ?: emptyList()
        if (selectedEntries.isEmpty()) return

        _state.value = _state.value.copy(isDownloading = true, downloadProgress = 0f)
        viewModelScope.launch {
            var completed = 0
            val total = selectedEntries.size

            for (entry in selectedEntries) {
                try {
                    val bookWithCover = importOpdsBookUseCase(
                        opdsEntry = entry,
                        sourceUrl = source.url,
                        username = source.username,
                        password = source.password
                    )
                    if (bookWithCover != null) {
                        bookRepository.insertBook(bookWithCover)
                    }
                    completed++
                    _state.value = _state.value.copy(downloadProgress = completed.toFloat() / total.toFloat())
                } catch (e: Exception) {
                    println("DEBUG: Failed to download book ${entry.title}: ${e.message}")
                    completed++
                    _state.value = _state.value.copy(downloadProgress = completed.toFloat() / total.toFloat())
                }
            }

            _state.value = _state.value.copy(isDownloading = false, downloadProgress = 0f, selectedBooks = emptySet(), isSelectionMode = false)
            onComplete()
        }
    }
}