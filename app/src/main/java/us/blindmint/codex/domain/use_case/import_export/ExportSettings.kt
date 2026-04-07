/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.import_export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import us.blindmint.codex.domain.repository.DataStoreRepository
import us.blindmint.codex.domain.use_case.color_preset.GetColorPresets
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import us.blindmint.codex.ui.main.MainState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportSettings @Inject constructor(
    private val repository: DataStoreRepository,
    private val getColorPresets: GetColorPresets,
    @ApplicationContext private val context: Context
) {

    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonPrimitive(null)
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is List<*> -> buildJsonArray {
                value.forEach { add(anyToJsonElement(it)) }
            }
            is Map<*, *> -> buildJsonObject {
                value.forEach { (k, v) ->
                    put(k.toString(), anyToJsonElement(v))
                }
            }
            else -> JsonPrimitive(value.toString()) // Fallback
        }
    }

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

            // Speed Reading Settings
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_WPM.name, currentState.speedReadingWpm)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_MANUAL_SENTENCE_PAUSE_ENABLED.name, currentState.speedReadingManualSentencePauseEnabled)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_SENTENCE_PAUSE_DURATION.name, currentState.speedReadingSentencePauseDuration)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_OSD_ENABLED.name, currentState.speedReadingOsdEnabled)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_WORD_SIZE.name, currentState.speedReadingWordSize)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_ACCENT_CHARACTER_ENABLED.name, currentState.speedReadingAccentCharacterEnabled)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_ACCENT_COLOR.name, currentState.speedReadingAccentColor.toString(16))
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_ACCENT_OPACITY.name, currentState.speedReadingAccentOpacity)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_SHOW_VERTICAL_INDICATORS.name, currentState.speedReadingShowVerticalIndicators)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_VERTICAL_INDICATORS_SIZE.name, currentState.speedReadingVerticalIndicatorsSize)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_VERTICAL_INDICATOR_TYPE.name, currentState.speedReadingVerticalIndicatorType)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_SHOW_HORIZONTAL_BARS.name, currentState.speedReadingShowHorizontalBars)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_THICKNESS.name, currentState.speedReadingHorizontalBarsThickness)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_LENGTH.name, currentState.speedReadingHorizontalBarsLength)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_DISTANCE.name, currentState.speedReadingHorizontalBarsDistance)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_COLOR.name, currentState.speedReadingHorizontalBarsColor.toString(16))
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_OPACITY.name, currentState.speedReadingHorizontalBarsOpacity)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_FOCAL_POINT_POSITION.name, currentState.speedReadingFocalPointPosition)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_OSD_HEIGHT.name, currentState.speedReadingOsdHeight)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_OSD_SEPARATION.name, currentState.speedReadingOsdSeparation)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_AUTO_HIDE_OSD.name, currentState.speedReadingAutoHideOsd)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_CENTER_WORD.name, currentState.speedReadingCenterWord)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_FOCUS_INDICATORS.name, currentState.speedReadingFocusIndicators)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_CUSTOM_FONT_ENABLED.name, currentState.speedReadingCustomFontEnabled)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_SELECTED_FONT_FAMILY.name, currentState.speedReadingSelectedFontFamily)
            addReadingSetting(readingSettings, DataStoreConstants.SPEED_READING_KEEP_SCREEN_ON.name, currentState.speedReadingKeepScreenOn)

            val json = Json { prettyPrint = true }.encodeToString(buildJsonObject {
                readingSettings.forEach { (key, value) ->
                    put(key, anyToJsonElement(value))
                }
            })

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


}