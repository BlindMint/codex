/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings.opds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.data.local.room.OpdsSourceDao
import javax.inject.Inject

@HiltViewModel
class OpdsSourcesModel @Inject constructor(
    private val opdsSourceDao: OpdsSourceDao
) : ViewModel() {

    private val _state = MutableStateFlow(OpdsSourcesState())
    val state = _state.asStateFlow()

    init {
        loadOpdsSources()
    }

    private fun loadOpdsSources() {
        viewModelScope.launch {
            val sources = opdsSourceDao.getAllOpdsSources()
            _state.value = _state.value.copy(sources = sources)
        }
    }

    fun addOpdsSource(name: String, url: String, username: String?, password: String?) {
        viewModelScope.launch {
            val entity = OpdsSourceEntity(
                name = name,
                url = url,
                username = username,
                password = password
            )
            opdsSourceDao.insertOpdsSource(entity)
            loadOpdsSources()
        }
    }

    fun updateOpdsSource(entity: OpdsSourceEntity) {
        viewModelScope.launch {
            opdsSourceDao.updateOpdsSource(entity)
            loadOpdsSources()
        }
    }

    fun deleteOpdsSource(id: Int) {
        viewModelScope.launch {
            opdsSourceDao.deleteOpdsSourceById(id)
            loadOpdsSources()
        }
    }
}