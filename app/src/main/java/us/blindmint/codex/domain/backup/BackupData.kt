/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.backup

import kotlinx.serialization.Serializable

/**
 * Main backup data structure for Codex.
 * Contains all exportable settings and configuration.
 */
@Serializable
data class CodexBackup(
    val version: Int = CURRENT_VERSION,
    val timestamp: Long,
    val appVersion: String? = null,
    val settings: Map<String, String> = emptyMap(),
    val colorPresets: List<ColorPresetData> = emptyList(),
    val opdsSources: List<OpdsSourceData> = emptyList(),
    val watchedFolders: List<WatchedFolderData> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Backup data for a color preset.
 */
@Serializable
data class ColorPresetData(
    val id: Int,
    val name: String,
    val backgroundColor: Int,
    val fontColor: Int,
    val isSelected: Boolean,
    val order: Int
)

/**
 * Backup data for an OPDS source.
 * Note: Credentials are NOT stored for security reasons.
 */
@Serializable
data class OpdsSourceData(
    val name: String,
    val url: String,
    val authRequired: Boolean, // Flag indicating source required authentication
    val enabled: Boolean = true
)

/**
 * Backup data for a watched folder.
 */
@Serializable
data class WatchedFolderData(
    val path: String,
    val recursive: Boolean = true,
    val enabled: Boolean = true
)

/**
 * Information about an existing backup file.
 */
data class BackupInfo(
    val filename: String,
    val uri: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val version: Int? = null
)