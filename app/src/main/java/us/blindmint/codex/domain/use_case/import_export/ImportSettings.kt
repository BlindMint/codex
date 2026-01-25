/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.import_export

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import us.blindmint.codex.domain.repository.DataStoreRepository
import us.blindmint.codex.domain.use_case.color_preset.ReorderColorPresets
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.presentation.core.constants.provideDefaultColorPresets
import us.blindmint.codex.domain.reader.ColorPreset
import androidx.compose.ui.graphics.Color
import us.blindmint.codex.domain.util.Selected
import us.blindmint.codex.domain.util.Selected.*
import javax.inject.Inject

class ImportSettings @Inject constructor(
    private val repository: DataStoreRepository,
    private val reorderColorPresets: ReorderColorPresets,
    @ApplicationContext private val context: Context
) {

    suspend fun execute(uri: Uri): Result<Unit> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return Result.failure(Exception("Cannot read file"))
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val importedSettings: Map<String, Any> = gson.fromJson(json, type)

            // Separate color presets from other settings for special handling
            val colorPresetsData = importedSettings["colorPresets"]
            val settingsWithoutColorPresets = importedSettings.filterKeys { it != "colorPresets" }

            // Filter and validate other imported settings
            val validSettings = settingsWithoutColorPresets.filter { (key, value) ->
                isValidReadingKey(key) && isValidValue(key, value)
            }

            // Apply settings with proper type conversion
            validSettings.forEach { (key, value) ->
                try {
                    when (key) {
                        DataStoreConstants.THEME.name -> repository.putDataToDataStore(DataStoreConstants.THEME, value as String)
                        DataStoreConstants.DARK_THEME.name -> repository.putDataToDataStore(DataStoreConstants.DARK_THEME, value as String)
                        DataStoreConstants.PURE_DARK.name -> repository.putDataToDataStore(DataStoreConstants.PURE_DARK, value as String)
                        DataStoreConstants.ABSOLUTE_DARK.name -> repository.putDataToDataStore(DataStoreConstants.ABSOLUTE_DARK, value as Boolean)
                        DataStoreConstants.THEME_CONTRAST.name -> repository.putDataToDataStore(DataStoreConstants.THEME_CONTRAST, value as String)
                        DataStoreConstants.FONT.name -> repository.putDataToDataStore(DataStoreConstants.FONT, value as String)
                        DataStoreConstants.CUSTOM_FONTS.name -> repository.putDataToDataStore(DataStoreConstants.CUSTOM_FONTS, (value as List<*>).map { it as String }.toSet())
                        DataStoreConstants.FONT_THICKNESS.name -> repository.putDataToDataStore(DataStoreConstants.FONT_THICKNESS, value as String)
                        DataStoreConstants.IS_ITALIC.name -> repository.putDataToDataStore(DataStoreConstants.IS_ITALIC, value as Boolean)
                        DataStoreConstants.FONT_SIZE.name -> repository.putDataToDataStore(DataStoreConstants.FONT_SIZE, (value as Double).toInt())
                        DataStoreConstants.LINE_HEIGHT.name -> repository.putDataToDataStore(DataStoreConstants.LINE_HEIGHT, (value as Double).toInt())
                        DataStoreConstants.PARAGRAPH_HEIGHT.name -> repository.putDataToDataStore(DataStoreConstants.PARAGRAPH_HEIGHT, (value as Double).toInt())
                        DataStoreConstants.PARAGRAPH_INDENTATION.name -> repository.putDataToDataStore(DataStoreConstants.PARAGRAPH_INDENTATION, (value as Double).toInt())
                        DataStoreConstants.SIDE_PADDING.name -> repository.putDataToDataStore(DataStoreConstants.SIDE_PADDING, (value as Double).toInt())
                        DataStoreConstants.VERTICAL_PADDING.name -> repository.putDataToDataStore(DataStoreConstants.VERTICAL_PADDING, (value as Double).toInt())
                        DataStoreConstants.DOUBLE_CLICK_TRANSLATION.name -> repository.putDataToDataStore(DataStoreConstants.DOUBLE_CLICK_TRANSLATION, value as Boolean)
                        DataStoreConstants.FAST_COLOR_PRESET_CHANGE.name -> repository.putDataToDataStore(DataStoreConstants.FAST_COLOR_PRESET_CHANGE, value as Boolean)
                        DataStoreConstants.TEXT_ALIGNMENT.name -> repository.putDataToDataStore(DataStoreConstants.TEXT_ALIGNMENT, value as String)
                        DataStoreConstants.LETTER_SPACING.name -> repository.putDataToDataStore(DataStoreConstants.LETTER_SPACING, (value as Double).toInt())
                        DataStoreConstants.CUTOUT_PADDING.name -> repository.putDataToDataStore(DataStoreConstants.CUTOUT_PADDING, value as Boolean)
                        DataStoreConstants.FULLSCREEN.name -> repository.putDataToDataStore(DataStoreConstants.FULLSCREEN, value as Boolean)
                        DataStoreConstants.KEEP_SCREEN_ON.name -> repository.putDataToDataStore(DataStoreConstants.KEEP_SCREEN_ON, value as Boolean)
                        DataStoreConstants.HIDE_BARS_ON_FAST_SCROLL.name -> repository.putDataToDataStore(DataStoreConstants.HIDE_BARS_ON_FAST_SCROLL, value as Boolean)
                        DataStoreConstants.PERCEPTION_EXPANDER.name -> repository.putDataToDataStore(DataStoreConstants.PERCEPTION_EXPANDER, value as Boolean)
                        DataStoreConstants.PERCEPTION_EXPANDER_PADDING.name -> repository.putDataToDataStore(DataStoreConstants.PERCEPTION_EXPANDER_PADDING, (value as Double).toInt())
                        DataStoreConstants.PERCEPTION_EXPANDER_THICKNESS.name -> repository.putDataToDataStore(DataStoreConstants.PERCEPTION_EXPANDER_THICKNESS, (value as Double).toInt())
                        DataStoreConstants.SCREEN_ORIENTATION.name -> repository.putDataToDataStore(DataStoreConstants.SCREEN_ORIENTATION, value as String)
                        DataStoreConstants.CUSTOM_SCREEN_BRIGHTNESS.name -> repository.putDataToDataStore(DataStoreConstants.CUSTOM_SCREEN_BRIGHTNESS, value as Boolean)
                        DataStoreConstants.SCREEN_BRIGHTNESS.name -> repository.putDataToDataStore(DataStoreConstants.SCREEN_BRIGHTNESS, (value as Double))
                        DataStoreConstants.HORIZONTAL_GESTURE.name -> repository.putDataToDataStore(DataStoreConstants.HORIZONTAL_GESTURE, value as String)
                        DataStoreConstants.HORIZONTAL_GESTURE_SCROLL.name -> repository.putDataToDataStore(DataStoreConstants.HORIZONTAL_GESTURE_SCROLL, (value as Double))
                        DataStoreConstants.HORIZONTAL_GESTURE_SENSITIVITY.name -> repository.putDataToDataStore(DataStoreConstants.HORIZONTAL_GESTURE_SENSITIVITY, (value as Double))
                        DataStoreConstants.HORIZONTAL_GESTURE_ALPHA_ANIM.name -> repository.putDataToDataStore(DataStoreConstants.HORIZONTAL_GESTURE_ALPHA_ANIM, value as Boolean)
                        DataStoreConstants.HORIZONTAL_GESTURE_PULL_ANIM.name -> repository.putDataToDataStore(DataStoreConstants.HORIZONTAL_GESTURE_PULL_ANIM, value as Boolean)
                        DataStoreConstants.BOTTOM_BAR_PADDING.name -> repository.putDataToDataStore(DataStoreConstants.BOTTOM_BAR_PADDING, (value as Double).toInt())
                        DataStoreConstants.HIGHLIGHTED_READING.name -> repository.putDataToDataStore(DataStoreConstants.HIGHLIGHTED_READING, value as Boolean)
                        DataStoreConstants.HIGHLIGHTED_READING_THICKNESS.name -> repository.putDataToDataStore(DataStoreConstants.HIGHLIGHTED_READING_THICKNESS, (value as Double).toInt())
                        DataStoreConstants.CHAPTER_TITLE_ALIGNMENT.name -> repository.putDataToDataStore(DataStoreConstants.CHAPTER_TITLE_ALIGNMENT, value as String)
                        DataStoreConstants.IMAGES.name -> repository.putDataToDataStore(DataStoreConstants.IMAGES, value as Boolean)
                        DataStoreConstants.IMAGES_CORNERS_ROUNDNESS.name -> repository.putDataToDataStore(DataStoreConstants.IMAGES_CORNERS_ROUNDNESS, (value as Double).toInt())
                        DataStoreConstants.IMAGES_ALIGNMENT.name -> repository.putDataToDataStore(DataStoreConstants.IMAGES_ALIGNMENT, value as String)
                        DataStoreConstants.IMAGES_WIDTH.name -> repository.putDataToDataStore(DataStoreConstants.IMAGES_WIDTH, (value as Double))
                        DataStoreConstants.IMAGES_COLOR_EFFECTS.name -> repository.putDataToDataStore(DataStoreConstants.IMAGES_COLOR_EFFECTS, value as String)
                        DataStoreConstants.PROGRESS_BAR.name -> repository.putDataToDataStore(DataStoreConstants.PROGRESS_BAR, value as Boolean)
                        DataStoreConstants.PROGRESS_BAR_PADDING.name -> repository.putDataToDataStore(DataStoreConstants.PROGRESS_BAR_PADDING, (value as Double).toInt())
                        DataStoreConstants.PROGRESS_BAR_ALIGNMENT.name -> repository.putDataToDataStore(DataStoreConstants.PROGRESS_BAR_ALIGNMENT, value as String)
                        DataStoreConstants.PROGRESS_BAR_FONT_SIZE.name -> repository.putDataToDataStore(DataStoreConstants.PROGRESS_BAR_FONT_SIZE, (value as Double).toInt())
                        DataStoreConstants.PROGRESS_COUNT.name -> repository.putDataToDataStore(DataStoreConstants.PROGRESS_COUNT, value as String)
                    }
                } catch (e: Exception) {
                    // Skip this setting if type conversion fails
                    // This ensures partial imports work and unknown settings are ignored
                }
            }

            // Handle color presets import
            try {
                if (colorPresetsData != null) {
                    // New export format with color presets
                    @Suppress("UNCHECKED_CAST")
                    val colorPresetsList = colorPresetsData as? List<Map<String, Any>>
                    if (colorPresetsList != null) {
                        val colorPresets = colorPresetsList.mapNotNull { presetData ->
                            try {
                                val id = when (val idValue = presetData["id"]) {
                                    is Double -> idValue.toInt()
                                    is Long -> idValue.toInt()
                                    is Int -> idValue
                                    else -> return@mapNotNull null
                                }

                                val name = presetData["name"] as? String

                                // Parse background color components
                                val backgroundColor = when (val bgValue = presetData["backgroundColor"]) {
                                    is Map<*, *> -> {
                                        val red = (bgValue["red"] as? Double)?.toFloat() ?: 0f
                                        val green = (bgValue["green"] as? Double)?.toFloat() ?: 0f
                                        val blue = (bgValue["blue"] as? Double)?.toFloat() ?: 0f
                                        val alpha = (bgValue["alpha"] as? Double)?.toFloat() ?: 1f
                                        Color(red, green, blue, alpha)
                                    }
                                    is Double -> Color(bgValue.toLong()) // Fallback for old format
                                    is Long -> Color(bgValue)
                                    is Int -> Color(bgValue.toLong())
                                    else -> return@mapNotNull null
                                }

                                // Parse font color components
                                val fontColor = when (val fontValue = presetData["fontColor"]) {
                                    is Map<*, *> -> {
                                        val red = (fontValue["red"] as? Double)?.toFloat() ?: 0f
                                        val green = (fontValue["green"] as? Double)?.toFloat() ?: 0f
                                        val blue = (fontValue["blue"] as? Double)?.toFloat() ?: 0f
                                        val alpha = (fontValue["alpha"] as? Double)?.toFloat() ?: 1f
                                        Color(red, green, blue, alpha)
                                    }
                                    is Double -> Color(fontValue.toLong()) // Fallback for old format
                                    is Long -> Color(fontValue)
                                    is Int -> Color(fontValue.toLong())
                                    else -> return@mapNotNull null
                                }

                                val isSelected = presetData["isSelected"] as? Boolean ?: false

                                ColorPreset(
                                    id = id,
                                    name = name,
                                    backgroundColor = backgroundColor,
                                    fontColor = fontColor,
                                    isSelected = isSelected
                                )
                            } catch (e: Exception) {
                                null // Skip malformed preset data
                            }
                        }

                        if (colorPresets.isNotEmpty()) {
                            // Sort presets by their order field to maintain correct ordering
                            val sortedPresets = colorPresets.sortedBy { preset ->
                                preset.id // Use ID as fallback ordering since order field might not be reliable
                            }
                            reorderColorPresets.execute(sortedPresets)
                        }
                    }
                } else {
                    // Old export format without color presets - restore defaults
                    // This ensures backward compatibility with exports created before color preset support
                    val defaultPresets = provideDefaultColorPresets()
                    reorderColorPresets.execute(defaultPresets)
                }
            } catch (e: Exception) {
                // Skip color presets import if there's an error
                // This ensures the rest of the import continues
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidReadingKey(key: String): Boolean {
        val validKeys = setOf(
            DataStoreConstants.THEME.name,
            DataStoreConstants.DARK_THEME.name,
            DataStoreConstants.PURE_DARK.name,
            DataStoreConstants.ABSOLUTE_DARK.name,
            DataStoreConstants.THEME_CONTRAST.name,
            DataStoreConstants.FONT.name,
            DataStoreConstants.CUSTOM_FONTS.name,
            DataStoreConstants.FONT_THICKNESS.name,
            DataStoreConstants.IS_ITALIC.name,
            DataStoreConstants.FONT_SIZE.name,
            DataStoreConstants.LINE_HEIGHT.name,
            DataStoreConstants.PARAGRAPH_HEIGHT.name,
            DataStoreConstants.PARAGRAPH_INDENTATION.name,
            DataStoreConstants.SIDE_PADDING.name,
            DataStoreConstants.VERTICAL_PADDING.name,
            DataStoreConstants.DOUBLE_CLICK_TRANSLATION.name,
            DataStoreConstants.FAST_COLOR_PRESET_CHANGE.name,
            DataStoreConstants.TEXT_ALIGNMENT.name,
            DataStoreConstants.LETTER_SPACING.name,
            DataStoreConstants.CUTOUT_PADDING.name,
            DataStoreConstants.FULLSCREEN.name,
            DataStoreConstants.KEEP_SCREEN_ON.name,
            DataStoreConstants.HIDE_BARS_ON_FAST_SCROLL.name,
            DataStoreConstants.PERCEPTION_EXPANDER.name,
            DataStoreConstants.PERCEPTION_EXPANDER_PADDING.name,
            DataStoreConstants.PERCEPTION_EXPANDER_THICKNESS.name,
            DataStoreConstants.SCREEN_ORIENTATION.name,
            DataStoreConstants.CUSTOM_SCREEN_BRIGHTNESS.name,
            DataStoreConstants.SCREEN_BRIGHTNESS.name,
            DataStoreConstants.HORIZONTAL_GESTURE.name,
            DataStoreConstants.HORIZONTAL_GESTURE_SCROLL.name,
            DataStoreConstants.HORIZONTAL_GESTURE_SENSITIVITY.name,
            DataStoreConstants.HORIZONTAL_GESTURE_ALPHA_ANIM.name,
            DataStoreConstants.HORIZONTAL_GESTURE_PULL_ANIM.name,
            DataStoreConstants.BOTTOM_BAR_PADDING.name,
            DataStoreConstants.HIGHLIGHTED_READING.name,
            DataStoreConstants.HIGHLIGHTED_READING_THICKNESS.name,
            DataStoreConstants.CHAPTER_TITLE_ALIGNMENT.name,
            DataStoreConstants.IMAGES.name,
            DataStoreConstants.IMAGES_CORNERS_ROUNDNESS.name,
            DataStoreConstants.IMAGES_ALIGNMENT.name,
            DataStoreConstants.IMAGES_WIDTH.name,
            DataStoreConstants.IMAGES_COLOR_EFFECTS.name,
            DataStoreConstants.PROGRESS_BAR.name,
            DataStoreConstants.PROGRESS_BAR_PADDING.name,
            DataStoreConstants.PROGRESS_BAR_ALIGNMENT.name,
            DataStoreConstants.PROGRESS_BAR_FONT_SIZE.name,
            DataStoreConstants.PROGRESS_COUNT.name,
            DataStoreConstants.SPEED_READING_KEEP_SCREEN_ON.name
            // Note: "colorPresets" is handled separately
        )
        return validKeys.contains(key)
    }

    private fun isValidValue(key: String, value: Any): Boolean {
        return when (key) {
            DataStoreConstants.FONT.name -> {
                val fontName = value as? String
                fontName != null && (provideFonts().any { it.id == fontName } || fontName == "Default")
            }
            else -> true // For other keys, accept any value and let MainState handle validation
        }
    }

}