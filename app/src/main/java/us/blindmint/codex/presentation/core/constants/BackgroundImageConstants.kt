/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.constants

import android.content.Context
import us.blindmint.codex.domain.reader.BackgroundImage

/**
 * Provides default background images bundled with the app.
 * Default images should be placed in assets/backgrounds/ folder.
 * Supported formats: PNG, JPG, WEBP
 */
object BackgroundImageConstants {
    private const val BACKGROUNDS_FOLDER = "backgrounds"
    private val SUPPORTED_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp")

    /**
     * Gets all default background images from the assets folder.
     */
    fun getDefaultBackgroundImages(context: Context): List<BackgroundImage> {
        return try {
            val assetManager = context.assets
            val files = assetManager.list(BACKGROUNDS_FOLDER) ?: emptyArray()

            files.filter { fileName ->
                SUPPORTED_EXTENSIONS.any { ext ->
                    fileName.lowercase().endsWith(ext)
                }
            }.map { fileName ->
                val displayName = fileName
                    .substringBeforeLast(".")
                    .replace("_", " ")
                    .replaceFirstChar { it.uppercase() }
                BackgroundImage(
                    name = displayName,
                    filePath = "$BACKGROUNDS_FOLDER/$fileName",
                    isDefault = true
                )
            }.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
