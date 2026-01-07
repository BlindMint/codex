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

    fun loadFeed(source: OpdsSourceEntity, url: String) {
        println("DEBUG: OpdsCatalogModel.loadFeed called with URL: $url")
        if (state.value.feed != null && state.value.feedUrl == url) {
            println("DEBUG: Feed already loaded for this URL, skipping")
            return
        }

        println("DEBUG: Starting to load feed from: $url")
        _state.value = _state.value.copy(isLoading = true, error = null, feedUrl = url)
        viewModelScope.launch {
            try {
                println("DEBUG: Calling opdsRepository.fetchFeed...")
                val feed = opdsRepository.fetchFeed(url, source.username, source.password)
                println("DEBUG: Feed loaded successfully, entries: ${feed.entries.size}")
                _state.value = _state.value.copy(isLoading = false, feed = feed)
                println("DEBUG: State updated with feed")
            } catch (e: Exception) {
                println("DEBUG: Feed loading failed: ${e.message}")
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun downloadBook(entry: OpdsEntry, source: OpdsSourceEntity, onSuccess: () -> Unit) {
        _state.value = _state.value.copy(isDownloading = true)
        viewModelScope.launch {
            try {
                val bookWithCover = importOpdsBookUseCase(
                    opdsEntry = entry,
                    username = source.username,
                    password = source.password
                )
                if (bookWithCover != null) {
                    // Save the downloaded book to the database
                    bookRepository.insertBook(bookWithCover)
                    _state.value = _state.value.copy(isDownloading = false)
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(isDownloading = false, error = "Failed to download book")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isDownloading = false, error = "Download failed: ${e.message}")
            }
        }
    }
}