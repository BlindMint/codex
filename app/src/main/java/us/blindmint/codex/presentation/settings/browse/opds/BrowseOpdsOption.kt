/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.opds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.browse.OpdsAddSourceDialog
import us.blindmint.codex.ui.browse.OpdsRootScreen
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel
import us.blindmint.codex.ui.theme.dynamicListItemColor
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.data.local.dto.OpdsSourceStatus

@Composable
fun BrowseOpdsOption(
    onNavigateToOpdsCatalog: (OpdsRootScreen) -> Unit
) {
    val sourcesModel = hiltViewModel<OpdsSourcesModel>()
    val sourcesState by sourcesModel.state.collectAsStateWithLifecycle()

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<OpdsSourceEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<OpdsSourceEntity?>(null) }

    OpdsAddSourceDialog(
        showDialog = showAddSourceDialog,
        onDismiss = { showAddSourceDialog = false },
        onSourceAdded = { name, url, username, password ->
            sourcesModel.addOpdsSource(name, url, username, password)
            showAddSourceDialog = false
        }
    )

    // Edit dialog
    editingSource?.let { source ->
        OpdsAddSourceDialog(
            showDialog = true,
            initialName = source.name,
            initialUrl = source.url,
            initialUsername = source.username,
            initialPassword = source.password,
            onDismiss = { editingSource = null },
            onSourceAdded = { name, url, username, password ->
                val updatedSource = source.copy(
                    name = name,
                    url = url,
                    username = username,
                    password = password
                )
                sourcesModel.updateOpdsSource(updatedSource)
                editingSource = null
            }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { source ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Remove OPDS Source") },
            text = { Text("Are you sure you want to remove \"${source.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sourcesModel.deleteOpdsSource(source.id)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sources list
        sourcesState.sources.forEachIndexed { index, source ->
            OpdsSourceItem(
                index = index,
                source = source,
                onSourceClick = {
                    onNavigateToOpdsCatalog(OpdsRootScreen(source))
                },
                onEditClick = { editingSource = source },
                onDeleteClick = { showDeleteConfirmation = source },
                modifier = Modifier
            )
        }

        // Add button below the list
        OpdsAddSourceAction(
            onClick = { showAddSourceDialog = true }
        )
    }
}

@Composable
private fun OpdsSourceItem(
    index: Int,
    source: OpdsSourceEntity,
    onSourceClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .noRippleClickable { onSourceClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Public,
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
            Text(
                text = source.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = source.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Status based on connection test
            val statusText = when (source.status) {
                OpdsSourceStatus.CONNECTING -> "Testing connection..."
                OpdsSourceStatus.CONNECTED -> {
                    if (!source.username.isNullOrBlank()) "Authenticated"
                    else "Connected"
                }
                OpdsSourceStatus.AUTH_FAILED -> "Authentication failed"
                OpdsSourceStatus.CONNECTION_FAILED -> "Connection failed"
                OpdsSourceStatus.DISABLED -> "Disabled"
                OpdsSourceStatus.UNKNOWN -> "Not tested"
            }
            val statusColor = when (source.status) {
                OpdsSourceStatus.CONNECTING -> MaterialTheme.colorScheme.onSurfaceVariant
                OpdsSourceStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                OpdsSourceStatus.AUTH_FAILED -> MaterialTheme.colorScheme.error
                OpdsSourceStatus.CONNECTION_FAILED -> MaterialTheme.colorScheme.error
                OpdsSourceStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                OpdsSourceStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit source",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Clear,
                    contentDescription = "Remove source",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun OpdsAddSourceAction(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .noRippleClickable {
                onClick()
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
        Text(
            text = stringResource(id = R.string.add_opds_source),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.secondary
            )
        )
    }
}