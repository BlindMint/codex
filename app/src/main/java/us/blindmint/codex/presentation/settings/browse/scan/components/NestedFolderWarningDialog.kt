/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.scan.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.blindmint.codex.presentation.core.util.FolderRelationship

@Composable
fun NestedFolderWarningDialog(
    relationship: FolderRelationship,
    newFolderName: String,
    existingFolderName: String,
    onImportAnyway: () -> Unit,
    onSkipFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, message) = when (relationship) {
        FolderRelationship.EQUAL -> {
            Pair(
                "Folder Already Added",
                "$newFolderName has already been added to your library. " +
                "Would you like to scan it again to check for new files?"
            )
        }
        FolderRelationship.EXISTING_IS_PARENT -> {
            Pair(
                "Nested Folder Detected",
                "$newFolderName is inside $existingFolderName, which you've already added.\n\n" +
                "Books will be imported from both folders. Files may appear in your library twice if they're accessible from both folders."
            )
        }
        FolderRelationship.NEW_IS_PARENT -> {
            Pair(
                "Parent Folder Detected",
                "$existingFolderName is inside $newFolderName, which you're adding now.\n\n" +
                "Books will be imported from both folders. You may want to remove the existing folder to avoid importing from both."
            )
        }
        FolderRelationship.UNRELATED -> return
    }

    AlertDialog(
        onDismissRequest = onSkipFolder,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onImportAnyway) {
                Text("Import anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkipFolder) {
                Text("Skip this folder")
            }
        }
    )
}
