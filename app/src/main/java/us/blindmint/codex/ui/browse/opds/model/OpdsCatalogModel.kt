/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.data.paging.OpdsPagingSource
import us.blindmint.codex.data.security.CredentialEncryptor
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.opds.OpdsFeed
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.repository.OpdsRepository
import us.blindmint.codex.domain.use_case.opds.ImportOpdsBookUseCase
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.paging.PagingData

@HiltViewModel
class OpdsCatalogModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val opdsRepository: OpdsRepository,
    private val importOpdsBookUseCase: ImportOpdsBookUseCase,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OpdsCatalogState())
    val state = _state.asStateFlow()

    fun createPager(source: OpdsSourceEntity, feedUrl: String? = null): Flow<PagingData<OpdsEntry>> {
        val url = feedUrl ?: source.url

        val username = CredentialEncryptor.decryptCredential(application, source.usernameEncrypted)
        val password = CredentialEncryptor.decryptCredential(application, source.passwordEncrypted)

        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
                initialLoadSize = 20,
                maxSize = 100
            ),
            pagingSourceFactory = {
                OpdsPagingSource(
                    opdsRepository = opdsRepository,
                    sourceUrl = url,
                    username = username,
                    password = password
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    fun loadFeed(source: OpdsSourceEntity, url: String, isDownloadDirectoryAccessible: Boolean = true) {
        android.util.Log.d("OPDS_DEBUG", "loadFeed called for URL: $url")

        if (state.value.feed != null && state.value.feedUrl == url) {
            android.util.Log.d("OPDS_DEBUG", "Feed already loaded for this URL, skipping")
            return
        }

        val username = CredentialEncryptor.decryptCredential(application, source.usernameEncrypted)
        val password = CredentialEncryptor.decryptCredential(application, source.passwordEncrypted)

        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            feedUrl = url,
            isDownloadDirectoryAccessible = isDownloadDirectoryAccessible,
            username = username,
            password = password
        )
        viewModelScope.launch {
            try {
                android.util.Log.d("OPDS_DEBUG", "Fetching feed from: $url")
                val feed = opdsRepository.fetchFeed(url, username, password)
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
                val username = CredentialEncryptor.decryptCredential(application, source.usernameEncrypted)
                val password = CredentialEncryptor.decryptCredential(application, source.passwordEncrypted)
                val nextFeed = opdsRepository.loadMore(nextUrl, username, password)
                val currentFeed = state.value.feed
                if (currentFeed != null) {
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
        android.util.Log.d("OPDS_DEBUG", "Starting download for book: ${entry.title}")

        val username = CredentialEncryptor.decryptCredential(application, source.usernameEncrypted)
        val password = CredentialEncryptor.decryptCredential(application, source.passwordEncrypted)

        viewModelScope.launch {
            try {
                android.util.Log.d("OPDS_DEBUG", "Calling importOpdsBookUseCase for: ${entry.title}")
                val bookWithCover = importOpdsBookUseCase(
                    opdsEntry = entry,
                    sourceUrl = source.url,
                    username = username,
                    password = password,
                    onProgress = { progress ->
                        _state.value = _state.value.copy(downloadProgress = progress)
                    }
                )
                if (bookWithCover != null) {
                    android.util.Log.d("OPDS_DEBUG", "Book import successful, saving to database: ${bookWithCover.book.title}")
                    try {
                        val insertedBookId = bookRepository.insertBook(bookWithCover)
                        android.util.Log.d("OPDS_DEBUG", "Book saved to database with ID: $insertedBookId")

                        us.blindmint.codex.ui.library.LibraryScreen.refreshListChannel.trySend(0)
                        android.util.Log.d("OPDS_DEBUG", "Triggered Library refresh")

                        _state.value = _state.value.copy(isDownloading = false, downloadProgress = 0f)
                        android.util.Log.d("OPDS_DEBUG", "Download completed successfully")
                        onSuccess()
                    } catch (e: Exception) {
                        android.util.Log.e("OPDS_DEBUG", "Failed to save book to database", e)
                        _state.value = _state.value.copy(isDownloading = false, downloadProgress = 0f, downloadError = "Failed to save book to database")
                    }
                } else {
                    android.util.Log.e("OPDS_DEBUG", "Book import returned null for: ${entry.title}")
                    _state.value = _state.value.copy(isDownloading = false, downloadProgress = 0f, downloadError = "Failed to download book")
                }
            } catch (e: Exception) {
                android.util.Log.e("OPDS_DEBUG", "Download failed with exception", e)
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
            loadFeed(source, source.url)
            return
        }

        val currentFeed = state.value.feed
        if (currentFeed == null) {
            _state.value = _state.value.copy(error = "No feed loaded to search in")
            return
        }

        val username = CredentialEncryptor.decryptCredential(application, source.usernameEncrypted)
        val password = CredentialEncryptor.decryptCredential(application, source.passwordEncrypted)

        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                println("DEBUG: Calling opdsRepository.search...")
                val searchFeed = opdsRepository.search(currentFeed, query, username, password)
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

        val username = CredentialEncryptor.decryptCredential(application, source.usernameEncrypted)
        val password = CredentialEncryptor.decryptCredential(application, source.passwordEncrypted)

        _state.value = _state.value.copy(isDownloading = true, downloadProgress = 0f)
        viewModelScope.launch {
            var completed = 0
            val total = selectedEntries.size

            for (entry in selectedEntries) {
                try {
                    val bookWithCover = importOpdsBookUseCase(
                        opdsEntry = entry,
                        sourceUrl = source.url,
                        username = username,
                        password = password,
                        onProgress = { progress ->
                            _state.value = _state.value.copy(downloadProgress = progress)
                        }
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
