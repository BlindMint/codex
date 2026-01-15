/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings.opds.download

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.blindmint.codex.data.local.data_store.DataStore
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import javax.inject.Inject

@HiltViewModel
class OpdsDownloadModel @Inject constructor(
    private val application: Application,
    private val dataStore: DataStore,
    private val codexDirectoryManager: CodexDirectoryManager
) : ViewModel() {

    private val _downloadUri = MutableStateFlow<Uri?>(null)
    val downloadUri = _downloadUri.asStateFlow()

    private val _isCodexDirectoryConfigured = MutableStateFlow(false)
    val isCodexDirectoryConfigured = _isCodexDirectoryConfigured.asStateFlow()

    init {
        loadDownloadUri()
        loadCodexDirectoryStatus()
    }

    private fun loadDownloadUri() {
        viewModelScope.launch {
            val uriString = dataStore.getNullableData(DataStoreConstants.OPDS_DOWNLOAD_URI)
            _downloadUri.value = uriString?.let { Uri.parse(it) }
        }
    }

    private fun loadCodexDirectoryStatus() {
        viewModelScope.launch {
            val uriString = dataStore.getNullableData(DataStoreConstants.CODEX_ROOT_URI)
            _isCodexDirectoryConfigured.value = uriString?.isNotBlank() == true
        }
    }



    fun onFolderSelected(uri: Uri) {
        try {
            application.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModelScope.launch {
                dataStore.putData(DataStoreConstants.OPDS_DOWNLOAD_URI, uri.toString())
                _downloadUri.value = uri
            }
        } catch (e: Exception) {
            // Handle error, perhaps log or show toast
            e.printStackTrace()
        }
    }

    fun isDownloadDirectoryAccessible(): Boolean {
        // Check if Codex directory is configured
        return _isCodexDirectoryConfigured.value
    }

    fun removeDownloadFolder() {
        viewModelScope.launch {
            // Try to release the persisted permission
            _downloadUri.value?.let { uri ->
                try {
                    application.contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Permission may already be released
                    e.printStackTrace()
                }
            }
            // Clear the stored URI
            dataStore.putData(DataStoreConstants.OPDS_DOWNLOAD_URI, "")
            _downloadUri.value = null
        }
    }

    /**
     * Returns a pair of (folderName, parentPath) for display purposes.
     * Returns null if no download folder is set.
     */
    fun getDisplayPath(): Pair<String, String>? {
        val uri = _downloadUri.value ?: return null
        return try {
            val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(application, uri)
            val name = documentFile?.name ?: uri.lastPathSegment?.substringAfterLast(':') ?: "Unknown"
            val fullPath = uri.path ?: uri.toString()

            // Try to extract a readable path
            val decodedPath = when {
                fullPath.contains("/tree/") -> {
                    // SAF tree URI format: /tree/primary:path/to/folder
                    val treePath = fullPath.substringAfter("/tree/")
                    val storage = when {
                        treePath.startsWith("primary:") -> "/storage/emulated/0/"
                        else -> "/storage/${treePath.substringBefore(':')}/"
                    }
                    storage + treePath.substringAfter(':')
                }
                else -> fullPath
            }

            val folderName = decodedPath.trimEnd('/').substringAfterLast('/')
            val parentPath = decodedPath.trimEnd('/').substringBeforeLast('/') + "/"

            Pair(folderName, parentPath)
        } catch (e: Exception) {
            val lastSegment = uri.lastPathSegment ?: "Unknown"
            Pair(lastSegment, "")
        }
    }
}