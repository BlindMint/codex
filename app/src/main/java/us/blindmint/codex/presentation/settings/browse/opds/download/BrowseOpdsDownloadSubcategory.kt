/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse.opds.download

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.ui.settings.opds.download.OpdsDownloadModel

fun LazyListScope.BrowseOpdsDownloadSubcategory() {
    item {
        val model = hiltViewModel<OpdsDownloadModel>()
        val downloadUri = model.downloadUri.collectAsStateWithLifecycle().value

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri ->
                model.onFolderSelected(uri)
            }
        }

        Text(
            text = if (downloadUri != null) "Download folder: Selected" else "Download folder: Not set",
            modifier = Modifier
                .clickable {
                    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        addFlags(
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                    }
                    launcher.launch(intent)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}