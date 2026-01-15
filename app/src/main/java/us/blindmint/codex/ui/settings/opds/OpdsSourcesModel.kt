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
import kotlinx.serialization.json.Json
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.data.local.room.BookDatabase
import us.blindmint.codex.data.local.room.DatabaseHelper
import us.blindmint.codex.domain.repository.OpdsRepository
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import us.blindmint.codex.domain.backup.OpdsSourceData
import us.blindmint.codex.domain.backup.CodexBackup
import javax.inject.Inject

@HiltViewModel
class OpdsSourcesModel @Inject constructor(
    private val application: Application,
    private val opdsRepository: OpdsRepository,
    private val codexDirectoryManager: CodexDirectoryManager
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
            DatabaseHelper.MIGRATION_13_14,
            DatabaseHelper.MIGRATION_14_15
        ).build()
    }

    private val opdsSourceDao = database.opdsSourceDao

    private val _state = MutableStateFlow(OpdsSourcesState())
    val state = _state.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        loadOpdsSources()
    }

    private fun loadOpdsSources() {
        viewModelScope.launch {
            val sources = opdsSourceDao.getAllOpdsSources()
            _state.value = _state.value.copy(sources = sources)
        }
    }

    /**
     * Check for existing OPDS sources in backup files when Codex folder is configured.
     * Should be called when user configures a new Codex directory.
     */
    fun detectExistingOpdsBackups() {
        viewModelScope.launch {
            try {
                val backupsDir = codexDirectoryManager.getBackupsDir()
                if (backupsDir == null) {
                    // No backups directory available
                    return@launch
                }

                // Look for backup files
                val backupFiles = backupsDir.listFiles()
                    .filter { file ->
                        file.isFile && file.name?.endsWith(".json") == true &&
                        file.name?.contains("codex-backup") == true
                    }
                    .sortedByDescending { it.lastModified() } // Most recent first

                // Check each backup file for OPDS sources
                for (backupFile in backupFiles) {
                    try {
                        val backupJson = application.contentResolver.openInputStream(backupFile.uri)?.use { input ->
                            input.bufferedReader().use { it.readText() }
                        } ?: continue

                        val backup = json.decodeFromString<CodexBackup>(backupJson)

                        // Check if this backup has OPDS sources that aren't already imported
                        val existingUrls = _state.value.sources.map { it.url }.toSet()
                        val newSources = backup.opdsSources.filter { source ->
                            source.url !in existingUrls
                        }

                        if (newSources.isNotEmpty()) {
                            // Found sources to import
                            _state.value = _state.value.copy(
                                showBackupImportPrompt = true,
                                backupSourcesToImport = newSources
                            )
                            break // Only show prompt for the most recent backup with sources
                        }
                    } catch (e: Exception) {
                        // Skip malformed backup files
                        continue
                    }
                }
            } catch (e: Exception) {
                // Ignore errors in backup detection
            }
        }
    }

    /**
     * Import OPDS sources from backup.
     */
    fun importOpdsSourcesFromBackup() {
        val sourcesToImport = _state.value.backupSourcesToImport
        if (sourcesToImport.isEmpty()) return

        viewModelScope.launch {
            // Import each source
            sourcesToImport.forEach { sourceData ->
                val entity = OpdsSourceEntity(
                    name = sourceData.name,
                    url = sourceData.url,
                    username = null, // Credentials not stored in backup
                    password = null,
                    enabled = sourceData.enabled
                )
                opdsSourceDao.insertOpdsSource(entity)
            }

            // Clear the prompt
            _state.value = _state.value.copy(
                showBackupImportPrompt = false,
                backupSourcesToImport = emptyList()
            )

            // Reload sources
            loadOpdsSources()
        }
    }

    /**
     * Dismiss the backup import prompt without importing.
     */
    fun dismissBackupImportPrompt() {
        _state.value = _state.value.copy(
            showBackupImportPrompt = false,
            backupSourcesToImport = emptyList()
        )
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

    suspend fun testConnection(url: String, username: String?, password: String?): String {
        // Try different URL variations to find a working one
        val urlVariations = generateUrlVariations(url)

        for (testUrl in urlVariations) {
            try {
                opdsRepository.fetchFeed(testUrl, username, password)
                return testUrl // Return the working URL
            } catch (e: Exception) {
                // Try next variation
                continue
            }
        }

        // If none worked, throw with the original URL
        opdsRepository.fetchFeed(url, username, password)
        return url
    }

    private fun generateUrlVariations(originalUrl: String): List<String> {
        val variations = mutableListOf<String>()

        // Clean the URL
        var cleanUrl = originalUrl.trim()

        // Remove trailing slashes
        cleanUrl = cleanUrl.trimEnd('/')

        // Check if it has a protocol
        val hasProtocol = cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")

        if (hasProtocol) {
            // Has protocol, try original and with /opds
            variations.add(cleanUrl)
            if (!cleanUrl.endsWith("/opds")) {
                variations.add("$cleanUrl/opds")
            }
        } else {
            // No protocol, try both http and https, with and without /opds
            // Prioritize https
            variations.add("https://$cleanUrl")
            variations.add("https://$cleanUrl/opds")
            variations.add("http://$cleanUrl")
            variations.add("http://$cleanUrl/opds")
        }

        return variations.distinct() // Remove duplicates in case original already had /opds
    }
}