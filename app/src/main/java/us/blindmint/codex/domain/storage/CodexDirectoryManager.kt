/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.storage

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Manages the unified Codex directory structure for storing downloads, backups, and settings.
 *
 * Directory structure:
 * /codex/  <- User-selected root (SAF persistent)
 * ├── downloads/     <- OPDS books (per-book folders with book, metadata.opf, cover.jpg)
 * └── backups/       <- Timestamped backup files
 */
interface CodexDirectoryManager {

    /**
     * Get the URI of the Codex root directory.
     * Returns null if not configured.
     */
    suspend fun getCodexRootUri(): Uri?

    /**
     * Set the Codex root directory URI.
     * Takes persistable permissions and creates the directory structure.
     */
    suspend fun setCodexRootUri(uri: Uri): Boolean

    /**
     * Check if the Codex directory is configured and accessible.
     */
    suspend fun isConfigured(): Boolean

    /**
     * Ensure the directory structure exists (downloads/, backups/).
     * Creates missing directories.
     */
    suspend fun ensureDirectoryStructure(): Boolean

    /**
     * Get the downloads directory (for OPDS book downloads).
     * Creates it if it doesn't exist.
     */
    suspend fun getDownloadsDir(): DocumentFile?

    /**
     * Get the backups directory.
     * Creates it if it doesn't exist.
     */
    suspend fun getBackupsDir(): DocumentFile?

    /**
     * Get the root DocumentFile for the Codex directory.
     */
    suspend fun getRootDocumentFile(): DocumentFile?

    /**
     * Clear the Codex directory configuration.
     * Releases persistable permissions.
     */
    suspend fun clearConfiguration(): Boolean

    /**
     * Get a human-readable display path for the Codex directory.
     */
    suspend fun getDisplayPath(): String?

    /**
     * Create a book folder under downloads/ with the given name.
     * @param folderName The folder name (e.g., "uuid_BookTitle")
     * @return The created DocumentFile or null if failed
     */
    suspend fun createBookFolder(folderName: String): DocumentFile?
}
