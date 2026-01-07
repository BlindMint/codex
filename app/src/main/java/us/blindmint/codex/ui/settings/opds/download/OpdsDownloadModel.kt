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
import javax.inject.Inject

@HiltViewModel
class OpdsDownloadModel @Inject constructor(
    private val application: Application,
    private val dataStore: DataStore
) : ViewModel() {

    private val _downloadUri = MutableStateFlow<Uri?>(null)
    val downloadUri = _downloadUri.asStateFlow()

    init {
        loadDownloadUri()
    }

    private fun loadDownloadUri() {
        viewModelScope.launch {
            val uriString = dataStore.getNullableData(DataStoreConstants.OPDS_DOWNLOAD_URI)
            _downloadUri.value = uriString?.let { Uri.parse(it) }
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
}