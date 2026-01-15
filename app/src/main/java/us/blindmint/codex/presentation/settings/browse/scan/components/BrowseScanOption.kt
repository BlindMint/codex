/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.scan.components

import android.content.Context
import android.content.UriPermission
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getBasePath
import com.anggrayudi.storage.file.getRootPath
import kotlinx.coroutines.launch
import us.blindmint.codex.R
import us.blindmint.codex.domain.use_case.book.BulkImportBooksFromFolder
import us.blindmint.codex.domain.use_case.book.BulkImportProgress
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.settings.SettingsEvent
import us.blindmint.codex.ui.settings.SettingsModel
import us.blindmint.codex.ui.theme.dynamicListItemColor

@Composable
fun BrowseScanOption() {
    val settingsModel = hiltViewModel<SettingsModel>()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var importProgress by remember { mutableStateOf<BulkImportProgress?>(null) }
    var importingFolderUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var codexRootUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    // Get the Codex root URI asynchronously and initialize the list
    androidx.compose.runtime.LaunchedEffect(Unit) {
        codexRootUri = runCatching {
            settingsModel.codexDirectoryManager.getCodexRootUri()
        }.getOrNull()
        isInitialized = true
    }

    fun getPersistedUriPermissions(): List<UriPermission> {
        return context.contentResolver?.persistedUriPermissions.let { permissions ->
            if (permissions.isNullOrEmpty()) return@let emptyList()

            val originalCount = permissions.size
            val filteredPermissions = permissions.filter { permission ->
                val shouldInclude = codexRootUri == null || !permission.uri.toString().equals(codexRootUri.toString(), ignoreCase = true)
                if (!shouldInclude) {
                    android.util.Log.d("BrowseScanOption", "Filtering out Codex root directory: ${permission.uri}")
                }
                shouldInclude
            }

            android.util.Log.d("BrowseScanOption", "Filtered permissions: $originalCount -> ${filteredPermissions.size} (Codex root: $codexRootUri)")

            filteredPermissions.sortedBy { it.uri.path?.lowercase() }
        }
    }

    val persistedUriPermissions = remember {
        mutableStateListOf<UriPermission>()
    }

    // Initialize the list only after codexRootUri is loaded
    androidx.compose.runtime.LaunchedEffect(isInitialized) {
        if (isInitialized) {
            persistedUriPermissions.clear()
            persistedUriPermissions.addAll(getPersistedUriPermissions())
        }
    }

    val persistedUriIntent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        settingsModel.onEvent(
            SettingsEvent.OnGrantPersistableUriPermission(
                uri = uri
            )
        )

        persistedUriPermissions.clear()
        persistedUriPermissions.addAll(getPersistedUriPermissions())

        // Start bulk import instead of refreshing Browse
        importingFolderUri = uri
        coroutineScope.launch {
            try {
                val importedCount = settingsModel.bulkImportBooksFromFolder.execute(uri) { progress ->
                    importProgress = progress
                }
                context.getString(R.string.import_completed, importedCount).showToast(context, longToast = false)
                LibraryScreen.refreshListChannel.trySend(0) // Refresh Library
            } catch (e: Exception) {
                e.printStackTrace()
                context.getString(R.string.error_something_went_wrong).showToast(context, longToast = false)
            } finally {
                importProgress = null
                importingFolderUri = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        if (isInitialized) {
            persistedUriPermissions.forEachIndexed { index, permission ->
                BrowseScanFolderItem(
                    index = index,
                    permission = permission,
                    context = context,
                    releasePersistableUriPermission = {
                        settingsModel.onEvent(
                            SettingsEvent.OnReleasePersistableUriPermission(
                                uri = permission.uri
                            )
                        )

                        persistedUriPermissions.clear()
                        persistedUriPermissions.addAll(getPersistedUriPermissions())
                        BrowseScreen.refreshListChannel.trySend(Unit)
                    },
                    importProgress = importProgress,
                    isImportingThisFolder = importingFolderUri == permission.uri
                )
            }
        }
    }

    BrowseScanAction(
        requestPersistableUriPermission = {
            try {
                persistedUriIntent.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()

                context.getString(R.string.error_no_file_manager_app)
                    .showToast(context, longToast = false)
            }
        }
    )
}

@Composable
private fun BrowseScanFolderItem(
    index: Int,
    permission: UriPermission,
    context: Context,
    releasePersistableUriPermission: () -> Unit,
    importProgress: BulkImportProgress?,
    isImportingThisFolder: Boolean
) {
    val permissionFile = DocumentFileCompat.fromUri(context, permission.uri) ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 18.dp,
                vertical = 8.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.dynamicListItemColor(index))
                    .padding(11.dp)
                    .size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                StyledText(
                    text = permissionFile.getBasePath(context),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                StyledText(
                    text = permissionFile.getRootPath(context),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (isImportingThisFolder && importProgress != null) {
                    StyledText(
                        text = stringResource(
                            R.string.importing_progress,
                            importProgress.current,
                            importProgress.total,
                            importProgress.currentFile
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            IconButton(
                modifier = Modifier.size(24.dp),
                icon = Icons.Outlined.Clear,
                contentDescription = R.string.remove_content_desc,
                disableOnClick = false,
                color = MaterialTheme.colorScheme.onSurface
            ) {
                releasePersistableUriPermission()
            }
        }

        if (isImportingThisFolder && importProgress != null) {
            LinearProgressIndicator(
                progress = { importProgress.current.toFloat() / importProgress.total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BrowseScanAction(
    requestPersistableUriPermission: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .noRippleClickable {
                requestPersistableUriPermission()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        StyledText(
            text = stringResource(id = R.string.add_folder),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.secondary
            )
        )
    }
}