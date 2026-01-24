/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.reader.speed_reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingWpmOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingAccentCharacterOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorsCombinedOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingColorsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingSentencePauseOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingCustomFontOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingFontFamilyOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingWordSizeOption
import us.blindmint.codex.presentation.settings.appearance.colors.components.BackgroundImageOption
import us.blindmint.codex.presentation.settings.appearance.colors.components.ColorPresetOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsThicknessOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsColorOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsDistanceOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarLengthOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingFocalPointPositionOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorTypeOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorsLengthOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorsToggleOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingOsdHeightOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingOsdSeparationOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.FocusIndicatorsOption
import us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType

import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle

enum class SpeedReadingTab {
    GENERAL,
    FOCUS
}

fun LazyListScope.SpeedReadingSubcategory(
    tab: SpeedReadingTab? = null,
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.speed_reading_reader_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true,
    wpm: Int = 300,
    onWpmChange: (Int) -> Unit = {},
    manualSentencePauseEnabled: Boolean = false,
    onManualSentencePauseEnabledChange: (Boolean) -> Unit = {},
    sentencePauseDuration: Int = 350,
    onSentencePauseDurationChange: (Int) -> Unit = {},
    osdEnabled: Boolean = true,
    onOsdEnabledChange: (Boolean) -> Unit = {},
    autoHideOsd: Boolean = true,
    onAutoHideOsdChange: (Boolean) -> Unit = {},
    wordSize: Int = 48,
    onWordSizeChange: (Int) -> Unit = {},
    accentCharacterEnabled: Boolean = true,
    onAccentCharacterEnabledChange: (Boolean) -> Unit = {},
    accentCharacterEnabledParam: Boolean = accentCharacterEnabled,
    accentColor: Color = Color.Red,
    onAccentColorChange: (Color) -> Unit = {},
    accentOpacity: Float = 1.0f,
    onAccentOpacityChange: (Float) -> Unit = {},
     showVerticalIndicators: Boolean = true,
     onShowVerticalIndicatorsChange: (Boolean) -> Unit = {},
     verticalIndicatorsSize: Int = 8,
     onVerticalIndicatorsSizeChange: (Int) -> Unit = {},
     verticalIndicatorType: us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType = us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType.LINE,
     onVerticalIndicatorTypeChange: (us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType) -> Unit = {},
     showHorizontalBars: Boolean = true,
    onShowHorizontalBarsChange: (Boolean) -> Unit = {},
     horizontalBarsThickness: Int = 2,
     onHorizontalBarsThicknessChange: (Int) -> Unit = {},
     horizontalBarsLength: Float = 0.9f,
     onHorizontalBarsLengthChange: (Float) -> Unit = {},
     horizontalBarsDistance: Int = 32,
     onHorizontalBarsDistanceChange: (Int) -> Unit = {},
    horizontalBarsColor: Color = Color.Gray,
    onHorizontalBarsColorChange: (Color) -> Unit = {},
    horizontalBarsOpacity: Float = 1.0f,
    onHorizontalBarsOpacityChange: (Float) -> Unit = {},
    focalPointPosition: Float = 0.38f,
    onFocalPointPositionChange: (Float) -> Unit = {},
    centerWord: Boolean = false,
    onCenterWordChange: (Boolean) -> Unit = {},
    focusIndicators: String = "LINES",
    onFocusIndicatorsChange: (String) -> Unit = {},
    customFontEnabled: Boolean = false,
    selectedFontFamily: String = "default",
    onCustomFontChanged: (Boolean) -> Unit = {},
    onFontFamilyChanged: (String) -> Unit = {}
) {
    when (tab) {
        null -> {
            // Show all settings (legacy behavior for general reader settings)
            // Performance
            item {
                SpeedReadingWpmOption(
                    wpm = wpm,
                    onWpmChange = onWpmChange
                )
            }

            item {
                val localManualPauseEnabled = remember { androidx.compose.runtime.mutableStateOf(manualSentencePauseEnabled) }

                androidx.compose.runtime.LaunchedEffect(manualSentencePauseEnabled) {
                    localManualPauseEnabled.value = manualSentencePauseEnabled
                }

                SwitchWithTitle(
                    selected = localManualPauseEnabled.value,
                    title = stringResource(id = R.string.manual_sentence_pause),
                    onClick = {
                        val newValue = !localManualPauseEnabled.value
                        localManualPauseEnabled.value = newValue
                        onManualSentencePauseEnabledChange(newValue)
                    }
                )
            }

            // Animated sentence pause duration slider (only shown when manual mode is enabled)
            item {
                AnimatedVisibility(
                    visible = manualSentencePauseEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingSentencePauseOption(
                        sentencePauseDuration = sentencePauseDuration,
                        onSentencePauseDurationChange = onSentencePauseDurationChange
                    )
                }
            }

             item {
                 SwitchWithTitle(
                     selected = osdEnabled,
                     title = stringResource(id = R.string.speed_reading_osd),
                     onClick = { onOsdEnabledChange(!osdEnabled) }
                 )
             }

             item {
                 SpeedReadingWordSizeOption(
                     wordSize = wordSize,
                     onWordSizeChange = onWordSizeChange
                 )
             }



            item {
                SpeedReadingWordSizeOption(
                    wordSize = wordSize,
                    onWordSizeChange = onWordSizeChange
                )
            }

            // Accent & Indicators
            item {
                HorizontalDivider()
            }

            // Accent & Indicators
            item {
                SpeedReadingAccentCharacterOption(
                    selected = accentCharacterEnabled,
                    onSelectionChange = onAccentCharacterEnabledChange
                )
            }

            // Accent color section (only shown when accent character is enabled)
            item {
                AnimatedVisibility(
                    visible = accentCharacterEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingColorsOption( // Accent color with RGB sliders and opacity
                        color = accentColor,
                        opacity = accentOpacity,
                        onColorChange = onAccentColorChange,
                        onOpacityChange = onAccentOpacityChange
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            // Horizontal Bars
            item {
                SpeedReadingHorizontalBarsOption(
                    selected = showHorizontalBars,
                    onSelectionChange = onShowHorizontalBarsChange
                )
            }

            item {
                AnimatedVisibility(
                    visible = showHorizontalBars,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingHorizontalBarsThicknessOption(
                        thickness = horizontalBarsThickness,
                        onThicknessChange = onHorizontalBarsThicknessChange
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = showHorizontalBars,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingHorizontalBarLengthOption(
                        length = horizontalBarsLength,
                        onLengthChange = onHorizontalBarsLengthChange
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = showHorizontalBars,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingHorizontalBarsDistanceOption(
                        distance = horizontalBarsDistance,
                        onDistanceChange = onHorizontalBarsDistanceChange
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = showHorizontalBars,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingHorizontalBarsColorOption(
                        color = horizontalBarsColor,
                        opacity = horizontalBarsOpacity,
                        onColorChange = onHorizontalBarsColorChange,
                        onOpacityChange = onHorizontalBarsOpacityChange
                    )
                }
            }

            // Focal Point
            item {
                SettingsSubcategoryTitle(title = stringResource(id = R.string.speed_reading_focal_point))
            }

            item {
                SpeedReadingVerticalIndicatorTypeOption(
                    selectedType = verticalIndicatorType,
                    onTypeChange = onVerticalIndicatorTypeChange
                )
            }

            item {
                SpeedReadingVerticalIndicatorsLengthOption(
                    verticalIndicatorsSize = verticalIndicatorsSize,
                    onVerticalIndicatorsSizeChange = onVerticalIndicatorsSizeChange
                )
            }

            item {
                SpeedReadingFocalPointPositionOption(
                    position = focalPointPosition,
                    onPositionChange = onFocalPointPositionChange
                )
            }

            // Font Settings
            item {
                SpeedReadingCustomFontOption(
                    customFontEnabled = customFontEnabled,
                    onCustomFontChanged = onCustomFontChanged
                )
            }

            item {
                AnimatedVisibility(
                    visible = customFontEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingFontFamilyOption(
                        selectedFontId = selectedFontFamily,
                        onFontChanged = onFontFamilyChanged
                    )
                }
            }
        }
        SpeedReadingTab.GENERAL -> {
            // Performance section (no header needed)
            item {
                SpeedReadingWpmOption(
                    wpm = wpm,
                    onWpmChange = onWpmChange
                )
            }

            item {
                val localManualPauseEnabled = remember { androidx.compose.runtime.mutableStateOf(manualSentencePauseEnabled) }

                androidx.compose.runtime.LaunchedEffect(manualSentencePauseEnabled) {
                    localManualPauseEnabled.value = manualSentencePauseEnabled
                }

                SwitchWithTitle(
                    selected = localManualPauseEnabled.value,
                    title = stringResource(id = R.string.manual_sentence_pause),
                    onClick = {
                        val newValue = !localManualPauseEnabled.value
                        localManualPauseEnabled.value = newValue
                        onManualSentencePauseEnabledChange(newValue)
                    }
                )
            }

            // Animated sentence pause duration slider (only shown when manual mode is enabled)
            item {
                AnimatedVisibility(
                    visible = manualSentencePauseEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingSentencePauseOption(
                        sentencePauseDuration = sentencePauseDuration,
                        onSentencePauseDurationChange = onSentencePauseDurationChange
                    )
                }
            }

            item {
                SwitchWithTitle(
                    selected = autoHideOsd,
                    title = stringResource(id = R.string.speed_reading_auto_hide_osd),
                    onClick = { onAutoHideOsdChange(!autoHideOsd) }
                )
            }

            item {
                SwitchWithTitle(
                    selected = osdEnabled,
                    title = stringResource(id = R.string.speed_reading_playback_controls),
                    onClick = { onOsdEnabledChange(!osdEnabled) }
                )
            }

            item {
                SpeedReadingWordSizeOption(
                    wordSize = wordSize,
                    onWordSizeChange = onWordSizeChange
                )
            }

            // Appearance section (with divider)
            item {
                HorizontalDivider()
            }

            // Color presets (shared with normal reading)
            item {
                ColorPresetOption()
            }

            item {
                BackgroundImageOption()
            }

            item {
                SpeedReadingCustomFontOption(
                    customFontEnabled = customFontEnabled,
                    onCustomFontChanged = onCustomFontChanged
                )
            }

            item {
                AnimatedVisibility(
                    visible = customFontEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingFontFamilyOption(
                        selectedFontId = selectedFontFamily,
                        onFontChanged = onFontFamilyChanged
                    )
                }
            }
        }

        SpeedReadingTab.FOCUS -> {
            // Center Word toggle
            item {
                SwitchWithTitle(
                    selected = centerWord,
                    enabled = true,
                    title = stringResource(id = R.string.speed_reading_center_word),
                    onClick = { onCenterWordChange(!centerWord) }
                )
            }

            // Focal Point position slider
            item {
                SpeedReadingFocalPointPositionOption(
                    position = focalPointPosition,
                    onPositionChange = onFocalPointPositionChange,
                    enabled = !centerWord
                )
            }

            // Focus Indicators segmented button
            item {
                FocusIndicatorsOption(
                    selected = focusIndicators,
                    onSelectionChange = onFocusIndicatorsChange
                )
            }

            // Focus Indicators Colors section (only shown when focus indicators are enabled)
            item {
                AnimatedVisibility(
                    visible = focusIndicators != "OFF",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        HorizontalDivider()
                        SpeedReadingHorizontalBarsColorOption(
                            color = horizontalBarsColor,
                            opacity = horizontalBarsOpacity,
                            onColorChange = onHorizontalBarsColorChange,
                            onOpacityChange = onHorizontalBarsOpacityChange
                        )
                    }
                }
            }

            // Accent Character section
            item {
                HorizontalDivider()
            }

            item {
                SpeedReadingAccentCharacterOption(
                    selected = accentCharacterEnabled,
                    onSelectionChange = onAccentCharacterEnabledChange,
                    enabled = !centerWord
                )
            }

            // Accent color section (only shown when accent character is enabled)
            item {
                AnimatedVisibility(
                    visible = accentCharacterEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeedReadingColorsOption(
                        color = accentColor,
                        opacity = accentOpacity,
                        onColorChange = onAccentColorChange,
                        onOpacityChange = onAccentOpacityChange
                    )
                }
            }
        }
    }
}