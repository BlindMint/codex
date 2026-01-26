/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.util

import android.content.UriPermission
import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath

/**
 * Relationship between two folder URIs.
 */
enum class FolderRelationship {
    /** The two URIs point to the same folder */
    EQUAL,
    /** The existing folder is a parent of the new folder */
    EXISTING_IS_PARENT,
    /** The new folder is a parent of the existing folder */
    NEW_IS_PARENT,
    /** The folders are not related (different trees) */
    UNRELATED
}

/**
 * Normalize a URI for comparison.
 * Removes trailing slashes and ensures consistent formatting.
 */
fun Uri.normalize(): Uri {
    val uriString = this.toString()
    val normalized = uriString.removeSuffix("/")
    return Uri.parse(normalized)
}

/**
 * Check if two folder URIs are equal after normalization.
 */
fun Uri.isEqual(other: Uri): Boolean {
    return this.normalize().toString().equals(other.normalize().toString(), ignoreCase = true)
}

/**
 * Get the absolute path from a UriPermission.
 * Returns null if the path cannot be determined.
 */
fun UriPermission.getAbsolutePath(context: Context): String? {
    val documentFile = DocumentFileCompat.fromUri(context, this.uri)
    return documentFile?.getAbsolutePath(context)
}

/**
 * Determine the relationship between a new folder URI and existing folder permissions.
 *
 * @param context Android context
 * @param newFolderUri The new folder URI being added
 * @param existingPermissions List of existing folder permissions
 * @return The relationship type, or null if no relationship is found
 */
fun getFolderRelationship(
    context: Context,
    newFolderUri: Uri,
    existingPermissions: List<UriPermission>
): FolderRelationship {
    for (permission in existingPermissions) {
        val existingUri = permission.uri

        if (newFolderUri.isEqual(existingUri)) {
            return FolderRelationship.EQUAL
        }

        val newPath = newFolderUri.getAbsoluteFilePath(context)
        val existingPath = existingUri.getAbsoluteFilePath(context)

        if (newPath != null && existingPath != null) {
            if (existingPath != newPath && newPath.startsWith("$existingPath/")) {
                return FolderRelationship.EXISTING_IS_PARENT
            }

            if (existingPath != newPath && existingPath.startsWith("$newPath/")) {
                return FolderRelationship.NEW_IS_PARENT
            }
        }
    }

    return FolderRelationship.UNRELATED
}

/**
 * Get the absolute file path from a URI.
 * This uses DocumentFileCompat's getAbsolutePath method which handles both
 * file:// and content:// URIs.
 */
fun Uri.getAbsoluteFilePath(context: Context): String? {
    val documentFile = DocumentFileCompat.fromUri(context, this) ?: return null
    return documentFile.getAbsolutePath(context)
}

/**
 * Get a human-readable relationship description string.
 */
fun FolderRelationship.getDescription(newFolderName: String, existingFolderName: String): String {
    return when (this) {
        FolderRelationship.EQUAL ->
            "You've already added this folder: $newFolderName"

        FolderRelationship.EXISTING_IS_PARENT ->
            "$newFolderName is inside $existingFolderName, which you've already added"

        FolderRelationship.NEW_IS_PARENT ->
            "$existingFolderName is inside $newFolderName, which you're adding now"

        FolderRelationship.UNRELATED ->
            ""
    }
}
