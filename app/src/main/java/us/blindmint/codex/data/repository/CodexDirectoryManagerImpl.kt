/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.local.data_store.DataStore
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CodexDirectoryManager"

@Singleton
class CodexDirectoryManagerImpl @Inject constructor(
    private val application: Application,
    private val dataStore: DataStore
) : CodexDirectoryManager {

    companion object {
        const val DOWNLOADS_DIR = "downloads"
        const val BACKUPS_DIR = "backups"
    }

    override suspend fun getCodexRootUri(): Uri? = withContext(Dispatchers.IO) {
        val uriString = dataStore.getNullableData(DataStoreConstants.CODEX_ROOT_URI)
        uriString?.let { Uri.parse(it) }
    }

    override suspend fun setCodexRootUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Setting Codex root directory to: ${uri.path}")

            // Take persistable permissions
            application.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Store the URI
            dataStore.putData(DataStoreConstants.CODEX_ROOT_URI, uri.toString())

            // Create directory structure
            ensureDirectoryStructure()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Codex root directory", e)
            false
        }
    }

    override suspend fun isConfigured(): Boolean = withContext(Dispatchers.IO) {
        val rootUri = getCodexRootUri() ?: return@withContext false

        try {
            val rootFile = DocumentFile.fromTreeUri(application, rootUri)
            rootFile?.exists() == true && rootFile.canWrite()
        } catch (e: Exception) {
            Log.w(TAG, "Codex directory not accessible", e)
            false
        }
    }

    override suspend fun ensureDirectoryStructure(): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootFile = getRootDocumentFile() ?: return@withContext false

            // Create downloads directory if it doesn't exist
            if (rootFile.findFile(DOWNLOADS_DIR) == null) {
                val created = rootFile.createDirectory(DOWNLOADS_DIR)
                if (created == null) {
                    Log.e(TAG, "Failed to create downloads directory")
                    return@withContext false
                }
                Log.i(TAG, "Created downloads directory")
            }

            // Create backups directory if it doesn't exist
            if (rootFile.findFile(BACKUPS_DIR) == null) {
                val created = rootFile.createDirectory(BACKUPS_DIR)
                if (created == null) {
                    Log.e(TAG, "Failed to create backups directory")
                    return@withContext false
                }
                Log.i(TAG, "Created backups directory")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure directory structure", e)
            false
        }
    }

    override suspend fun getDownloadsDir(): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val rootFile = getRootDocumentFile() ?: return@withContext null

            // Find or create downloads directory
            var downloadsDir = rootFile.findFile(DOWNLOADS_DIR)
            if (downloadsDir == null) {
                downloadsDir = rootFile.createDirectory(DOWNLOADS_DIR)
            }

            downloadsDir
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get downloads directory", e)
            null
        }
    }

    override suspend fun getBackupsDir(): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val rootFile = getRootDocumentFile() ?: return@withContext null

            // Find or create backups directory
            var backupsDir = rootFile.findFile(BACKUPS_DIR)
            if (backupsDir == null) {
                backupsDir = rootFile.createDirectory(BACKUPS_DIR)
            }

            backupsDir
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get backups directory", e)
            null
        }
    }

    override suspend fun getRootDocumentFile(): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val rootUri = getCodexRootUri() ?: return@withContext null
            DocumentFile.fromTreeUri(application, rootUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get root DocumentFile", e)
            null
        }
    }

    override suspend fun clearConfiguration(): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootUri = getCodexRootUri()
            if (rootUri != null) {
                // Release persistable permissions
                try {
                    application.contentResolver.releasePersistableUriPermission(
                        rootUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to release permissions", e)
                }
            }

            // Clear stored URI
            dataStore.putData(DataStoreConstants.CODEX_ROOT_URI, "")
            Log.i(TAG, "Cleared Codex directory configuration")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear configuration", e)
            false
        }
    }

    override suspend fun getDisplayPath(): String? = withContext(Dispatchers.IO) {
        try {
            val rootUri = getCodexRootUri() ?: return@withContext null

            // Extract readable path from SAF URI
            // Format: content://com.android.externalstorage.documents/tree/primary:codex
            val path = rootUri.lastPathSegment
            if (path != null) {
                // Convert "primary:codex" to "/storage/emulated/0/codex"
                if (path.startsWith("primary:")) {
                    return@withContext "/storage/emulated/0/" + path.removePrefix("primary:")
                }
                // For other storage volumes, just show the path part
                return@withContext path.substringAfter(":")
            }

            rootUri.path
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get display path", e)
            null
        }
    }

    override suspend fun createBookFolder(folderName: String): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = getDownloadsDir() ?: return@withContext null

            // Sanitize folder name
            val safeName = sanitizeFolderName(folderName)

            // Check if folder already exists
            val existing = downloadsDir.findFile(safeName)
            if (existing != null) {
                return@withContext existing
            }

            // Create new folder
            val newFolder = downloadsDir.createDirectory(safeName)
            if (newFolder != null) {
                Log.i(TAG, "Created book folder: $safeName")
            }
            newFolder
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create book folder: $folderName", e)
            null
        }
    }

    override suspend fun createAuthorFolder(authorName: String): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = getDownloadsDir() ?: return@withContext null
            val safeName = sanitizeFolderName(authorName)

            val existing = downloadsDir.findFile(safeName)
            if (existing != null && existing.isDirectory) {
                Log.i(TAG, "Author folder already exists: $safeName")
                return@withContext existing
            }

            val newFolder = downloadsDir.createDirectory(safeName)
            if (newFolder != null) {
                Log.i(TAG, "Created author folder: $safeName")
            }
            newFolder
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create author folder: $authorName", e)
            null
        }
    }

    /**
     * Sanitizes a folder name by removing invalid filesystem characters.
     */
    private fun sanitizeFolderName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(200) // Limit length
    }
}
