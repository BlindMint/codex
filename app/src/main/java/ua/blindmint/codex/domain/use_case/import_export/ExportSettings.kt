/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.domain.use_case.import_export

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import ua.blindmint.codex.domain.repository.DataStoreRepository
import ua.blindmint.codex.domain.use_case.color_preset.GetColorPresets
import ua.blindmint.codex.presentation.core.constants.DataStoreConstants
import ua.blindmint.codex.ui.main.MainState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportSettings @Inject constructor(
    private val repository: DataStoreRepository,
    private val getColorPresets: GetColorPresets,
    @ApplicationContext private val context: Context
) {

    suspend fun execute(uri: Uri, currentState: MainState): Result<Unit> {
        return try {
            val readingSettings = mutableMapOf<String, Any>()

            // Export all current reading-related settings from MainState
            // This ensures we backup ALL current values, including defaults

            // Export color presets from database
            val colorPresets = getColorPresets.execute()
            val colorPresetsData = colorPresets.map { preset ->
                mapOf(
                    "id" to preset.id,
                    "name" to preset.name,
                    "backgroundColor" to mapOf(
                        "red" to preset.backgroundColor.red,
                        "green" to preset.backgroundColor.green,
                        "blue" to preset.backgroundColor.blue,
                        "alpha" to preset.backgroundColor.alpha
                    ),
                    "fontColor" to mapOf(
                        "red" to preset.fontColor.red,
                        "green" to preset.fontColor.green,
                        "blue" to preset.fontColor.blue,
                        "alpha" to preset.fontColor.alpha
                    ),
                    "isSelected" to preset.isSelected,
                    "order" to colorPresets.indexOf(preset)
                )
            }
            addReadingSetting(readingSettings, "colorPresets", colorPresetsData)
            addReadingSetting(readingSettings, DataStoreConstants.THEME.name, currentState.theme.name)
            addReadingSetting(readingSettings, DataStoreConstants.DARK_THEME.name, currentState.darkTheme.name)
            addReadingSetting(readingSettings, DataStoreConstants.PURE_DARK.name, currentState.pureDark.name)
            addReadingSetting(readingSettings, DataStoreConstants.ABSOLUTE_DARK.name, currentState.absoluteDark)
            addReadingSetting(readingSettings, DataStoreConstants.THEME_CONTRAST.name, currentState.themeContrast.name)
            addReadingSetting(readingSettings, DataStoreConstants.FONT.name, currentState.fontFamily)
            addReadingSetting(readingSettings, DataStoreConstants.CUSTOM_FONTS.name, currentState.customFonts.map { it.toString() })
            addReadingSetting(readingSettings, DataStoreConstants.FONT_THICKNESS.name, currentState.fontThickness.name)
            addReadingSetting(readingSettings, DataStoreConstants.IS_ITALIC.name, currentState.isItalic)
            addReadingSetting(readingSettings, DataStoreConstants.FONT_SIZE.name, currentState.fontSize)
            addReadingSetting(readingSettings, DataStoreConstants.LINE_HEIGHT.name, currentState.lineHeight)
            addReadingSetting(readingSettings, DataStoreConstants.PARAGRAPH_HEIGHT.name, currentState.paragraphHeight)
            addReadingSetting(readingSettings, DataStoreConstants.PARAGRAPH_INDENTATION.name, currentState.paragraphIndentation)
            addReadingSetting(readingSettings, DataStoreConstants.SIDE_PADDING.name, currentState.sidePadding)
            addReadingSetting(readingSettings, DataStoreConstants.VERTICAL_PADDING.name, currentState.verticalPadding)
            addReadingSetting(readingSettings, DataStoreConstants.DOUBLE_CLICK_TRANSLATION.name, currentState.doubleClickTranslation)
            addReadingSetting(readingSettings, DataStoreConstants.FAST_COLOR_PRESET_CHANGE.name, currentState.fastColorPresetChange)
            addReadingSetting(readingSettings, DataStoreConstants.TEXT_ALIGNMENT.name, currentState.textAlignment.name)
            addReadingSetting(readingSettings, DataStoreConstants.LETTER_SPACING.name, currentState.letterSpacing)
            addReadingSetting(readingSettings, DataStoreConstants.CUTOUT_PADDING.name, currentState.cutoutPadding)
            addReadingSetting(readingSettings, DataStoreConstants.FULLSCREEN.name, currentState.fullscreen)
            addReadingSetting(readingSettings, DataStoreConstants.KEEP_SCREEN_ON.name, currentState.keepScreenOn)
            addReadingSetting(readingSettings, DataStoreConstants.HIDE_BARS_ON_FAST_SCROLL.name, currentState.hideBarsOnFastScroll)
            addReadingSetting(readingSettings, DataStoreConstants.PERCEPTION_EXPANDER.name, currentState.perceptionExpander)
            addReadingSetting(readingSettings, DataStoreConstants.PERCEPTION_EXPANDER_PADDING.name, currentState.perceptionExpanderPadding)
            addReadingSetting(readingSettings, DataStoreConstants.PERCEPTION_EXPANDER_THICKNESS.name, currentState.perceptionExpanderThickness)
            addReadingSetting(readingSettings, DataStoreConstants.SCREEN_ORIENTATION.name, currentState.screenOrientation.name)
            addReadingSetting(readingSettings, DataStoreConstants.CUSTOM_SCREEN_BRIGHTNESS.name, currentState.customScreenBrightness)
            addReadingSetting(readingSettings, DataStoreConstants.SCREEN_BRIGHTNESS.name, currentState.screenBrightness)
            addReadingSetting(readingSettings, DataStoreConstants.HORIZONTAL_GESTURE.name, currentState.horizontalGesture.name)
            addReadingSetting(readingSettings, DataStoreConstants.HORIZONTAL_GESTURE_SCROLL.name, currentState.horizontalGestureScroll)
            addReadingSetting(readingSettings, DataStoreConstants.HORIZONTAL_GESTURE_SENSITIVITY.name, currentState.horizontalGestureSensitivity)
            addReadingSetting(readingSettings, DataStoreConstants.HORIZONTAL_GESTURE_ALPHA_ANIM.name, currentState.horizontalGestureAlphaAnim)
            addReadingSetting(readingSettings, DataStoreConstants.HORIZONTAL_GESTURE_PULL_ANIM.name, currentState.horizontalGesturePullAnim)
            addReadingSetting(readingSettings, DataStoreConstants.BOTTOM_BAR_PADDING.name, currentState.bottomBarPadding)
            addReadingSetting(readingSettings, DataStoreConstants.HIGHLIGHTED_READING.name, currentState.highlightedReading)
            addReadingSetting(readingSettings, DataStoreConstants.HIGHLIGHTED_READING_THICKNESS.name, currentState.highlightedReadingThickness)
            addReadingSetting(readingSettings, DataStoreConstants.CHAPTER_TITLE_ALIGNMENT.name, currentState.chapterTitleAlignment.name)
            addReadingSetting(readingSettings, DataStoreConstants.IMAGES.name, currentState.images)
            addReadingSetting(readingSettings, DataStoreConstants.IMAGES_CORNERS_ROUNDNESS.name, currentState.imagesCornersRoundness)
            addReadingSetting(readingSettings, DataStoreConstants.IMAGES_ALIGNMENT.name, currentState.imagesAlignment.name)
            addReadingSetting(readingSettings, DataStoreConstants.IMAGES_WIDTH.name, currentState.imagesWidth)
            addReadingSetting(readingSettings, DataStoreConstants.IMAGES_COLOR_EFFECTS.name, currentState.imagesColorEffects.name)
            addReadingSetting(readingSettings, DataStoreConstants.PROGRESS_BAR.name, currentState.progressBar)
            addReadingSetting(readingSettings, DataStoreConstants.PROGRESS_BAR_PADDING.name, currentState.progressBarPadding)
            addReadingSetting(readingSettings, DataStoreConstants.PROGRESS_BAR_ALIGNMENT.name, currentState.progressBarAlignment.name)
            addReadingSetting(readingSettings, DataStoreConstants.PROGRESS_BAR_FONT_SIZE.name, currentState.progressBarFontSize)
            addReadingSetting(readingSettings, DataStoreConstants.PROGRESS_COUNT.name, currentState.progressCount.name)

            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(readingSettings)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            } ?: return Result.failure(Exception("Cannot write to file"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addReadingSetting(settings: MutableMap<String, Any>, key: String, value: Any) {
        settings[key] = value
    }

    fun getSuggestedFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        return "codex-backup-$timestamp.json"
    }

    private fun isReadingRelatedKey(key: String): Boolean {
        val readingKeys = setOf(
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
            DataStoreConstants.PROGRESS_COUNT.name
            // Note: "colorPresets" is handled separately
        )
        return readingKeys.contains(key)
    }
}