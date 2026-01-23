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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import us.blindmint.codex.domain.import_progress.ImportOperation
import us.blindmint.codex.domain.use_case.book.BulkImportBooksFromFolder
import us.blindmint.codex.domain.use_case.book.BulkImportProgress
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.import_progress.ImportProgressViewModel
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.settings.SettingsEvent
import us.blindmint.codex.ui.settings.SettingsModel
import us.blindmint.codex.ui.theme.dynamicListItemColor
import androidx.compose.material3.AlertDialog

@Composable
fun BrowseScanOption() {
    val settingsModel = hiltViewModel<SettingsModel>()
    val importProgressViewModel = hiltViewModel<ImportProgressViewModel>()
    val state by settingsModel.state.collectAsStateWithLifecycle()
    val importOperations by importProgressViewModel.importOperations.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showLocalFolderInfoDialog by remember { mutableStateOf(false) }
    var folderToRemove: android.content.UriPermission? by remember { mutableStateOf(null) }

    suspend fun getPersistedUriPermissions(): List<UriPermission> {
        return context.contentResolver?.persistedUriPermissions.let { permissions ->
            if (permissions.isNullOrEmpty()) return@let emptyList()

            // Always get the most current Codex root URI directly from storage
            // This prevents timing issues where the root folder might appear in local folders
            val storedCodexRootUri = settingsModel.codexDirectoryManager.getCodexRootUri()

            val originalCount = permissions.size
            val filteredPermissions = permissions.filter { permission ->
                val shouldInclude = storedCodexRootUri == null || !permission.uri.toString().equals(storedCodexRootUri.toString(), ignoreCase = true)
                if (!shouldInclude) {
                    android.util.Log.d("BrowseScanOption", "Filtering out Codex root directory: ${permission.uri}")
                }
                shouldInclude
            }

            android.util.Log.d("BrowseScanOption", "Filtered permissions: $originalCount -> ${filteredPermissions.size} (Codex root: $storedCodexRootUri)")

            filteredPermissions.sortedBy { it.uri.path?.lowercase() }
        }
    }

    var persistedUriPermissions by remember {
        mutableStateOf<List<android.content.UriPermission>>(emptyList())
    }

    // Initialize the list when component loads
    androidx.compose.runtime.LaunchedEffect(Unit) {
        persistedUriPermissions = getPersistedUriPermissions()
    }

    // Refresh the list when codexRootUri changes (e.g., when root folder is set/changed)
    androidx.compose.runtime.LaunchedEffect(state.codexRootUri) {
        persistedUriPermissions = getPersistedUriPermissions()
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

                        coroutineScope.launch {
                            persistedUriPermissions = getPersistedUriPermissions()
                        }

        // Start background import using ViewModel
        val permissionFile = DocumentFileCompat.fromUri(context, uri)
        val folderName = permissionFile?.getBasePath(context) ?: "Folder"
        val folderPath = permissionFile?.getRootPath(context) ?: uri.toString()

        importProgressViewModel.startImport(
            folderUri = uri,
            folderName = folderName,
            folderPath = folderPath
        )

        // Schedule library refresh after import completes
        coroutineScope.launch {
            LibraryScreen.refreshListChannel.trySend(0) // Refresh Library
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        persistedUriPermissions.forEachIndexed { index, permission ->
            BrowseScanFolderItem(
                index = index,
                permission = permission,
                context = context,
                onRefreshClick = {
                    val permissionFile = DocumentFileCompat.fromUri(context, permission.uri)
                    val folderName = permissionFile?.getBasePath(context) ?: "Folder"
                    val folderPath = permissionFile?.getRootPath(context) ?: permission.uri.toString()

                    importProgressViewModel.startImport(
                        folderUri = permission.uri,
                        folderName = folderName,
                        folderPath = folderPath
                    )
                    coroutineScope.launch {
                        LibraryScreen.refreshListChannel.trySend(0) // Refresh Library
                    }
                },
                onRemoveClick = {
                    folderToRemove = permission
                },
                importOperations = importOperations,
                folderUri = permission.uri
            )
        }
    }

    BrowseScanAction(
        requestPersistableUriPermission = {
            if (persistedUriPermissions.isEmpty()) {
                showLocalFolderInfoDialog = true
            } else {
                try {
                    persistedUriIntent.launch(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    context.getString(R.string.error_no_file_manager_app)
                        .showToast(context, longToast = false)
                }
            }
        }
    )

    // Informational dialog about Local folders
    if (showLocalFolderInfoDialog) {
        AlertDialog(
            onDismissRequest = { showLocalFolderInfoDialog = false },
            title = { androidx.compose.material3.Text("Local Folders") },
            text = {
                androidx.compose.material3.Text(
                    "Local folders allow you to add books from folders on your device. " +
                    "These folders will be scanned for eBook files and added to your library. " +
                    "Unlike Codex Directory, local folders are for one-time imports and " +
                    "don't automatically sync with the folder contents.\n\n" +
                    "You can add multiple local folders and refresh them individually."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showLocalFolderInfoDialog = false
                    // Now launch the folder picker
                    try {
                        persistedUriIntent.launch(null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        context.getString(R.string.error_no_file_manager_app)
                            .showToast(context, longToast = false)
                    }
                }) {
                    androidx.compose.material3.Text("Continue")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showLocalFolderInfoDialog = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for removing a folder
    if (folderToRemove != null) {
        AlertDialog(
            onDismissRequest = { folderToRemove = null },
            title = { androidx.compose.material3.Text("Remove Folder") },
            text = {
                androidx.compose.material3.Text(
                    "Are you sure you want to remove this folder? " +
                    "This will remove those books and comics from the library."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    settingsModel.onEvent(
                        SettingsEvent.OnReleasePersistableUriPermission(
                            uri = folderToRemove!!.uri
                        )
                    )

                    coroutineScope.launch {
                        persistedUriPermissions = getPersistedUriPermissions()
                    }
                    BrowseScreen.refreshListChannel.trySend(Unit)
                    folderToRemove = null
                }) {
                    androidx.compose.material3.Text("Remove")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { folderToRemove = null }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BrowseScanFolderItem(
    index: Int,
    permission: UriPermission,
    context: Context,
    onRefreshClick: () -> Unit,
    onRemoveClick: () -> Unit,
    importOperations: List<us.blindmint.codex.domain.import_progress.ImportOperation>,
    folderUri: android.net.Uri
) {
    val permissionFile = DocumentFileCompat.fromUri(context, folderUri) ?: return

    // Find import operation for this folder if one exists
    val currentOperation = importOperations.find { op ->
        // Match by folder path
        op.folderPath == permissionFile.getRootPath(context)
    }

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
                if (currentOperation != null && currentOperation.totalBooks > 0) {
                    StyledText(
                        text = stringResource(
                            R.string.importing_progress,
                            currentOperation.currentProgress,
                            currentOperation.totalBooks,
                            currentOperation.currentFile
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh folder",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "Remove folder",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (currentOperation != null && currentOperation.totalBooks > 0) {
            LinearProgressIndicator(
                progress = { currentOperation.currentProgress.toFloat() / currentOperation.totalBooks.toFloat() },
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