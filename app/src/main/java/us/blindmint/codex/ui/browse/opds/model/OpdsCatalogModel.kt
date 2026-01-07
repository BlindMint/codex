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
import us.blindmint.codex.domain.opds.OpdsFeed
import us.blindmint.codex.domain.repository.OpdsRepository
import javax.inject.Inject

@HiltViewModel
class OpdsCatalogModel @Inject constructor(
    private val opdsRepository: OpdsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OpdsCatalogState())
    val state = _state.asStateFlow()

    fun loadFeed(source: OpdsSourceEntity, url: String) {
        if (state.value.feed != null && state.value.feedUrl == url) return

        _state.value = _state.value.copy(isLoading = true, error = null, feedUrl = url)
        viewModelScope.launch {
            try {
                val feed = opdsRepository.fetchFeed(url, source.username, source.password)
                _state.value = _state.value.copy(isLoading = false, feed = feed)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}