/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings.opds

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.data.local.room.BookDatabase
import us.blindmint.codex.data.local.room.DatabaseHelper
import us.blindmint.codex.domain.repository.OpdsRepository
import javax.inject.Inject

@HiltViewModel
class OpdsSourcesModel @Inject constructor(
    private val application: Application,
    private val opdsRepository: OpdsRepository
) : ViewModel() {

    private val database: BookDatabase by lazy {
        Room.databaseBuilder(
            application,
            BookDatabase::class.java,
            "book_database"
        ).addMigrations(
            DatabaseHelper.MIGRATION_2_3,
            DatabaseHelper.MIGRATION_4_5,
            DatabaseHelper.MIGRATION_5_6,
            DatabaseHelper.MIGRATION_10_11,
            DatabaseHelper.MIGRATION_11_12,
            DatabaseHelper.MIGRATION_12_13,
            DatabaseHelper.MIGRATION_13_14
        ).build()
    }

    private val opdsSourceDao = database.opdsSourceDao

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

    suspend fun testConnection(url: String, username: String?, password: String?) {
        // This will throw if fails
        opdsRepository.fetchFeed(url, username, password)
    }
}