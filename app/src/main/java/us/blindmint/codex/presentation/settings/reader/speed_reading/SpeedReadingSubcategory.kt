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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingWpmOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingAccentCharacterOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingColorsOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingSentencePauseOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingCustomFontOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingFontFamilyOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingWordSizeOption
import us.blindmint.codex.presentation.settings.appearance.colors.components.BackgroundImageOption
import us.blindmint.codex.presentation.settings.appearance.colors.components.ColorPresetOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingVerticalIndicatorsSizeOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsThicknessOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsColorOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingHorizontalBarsDistanceOption
import us.blindmint.codex.presentation.settings.reader.speed_reading.components.SpeedReadingFocalPointPositionOption

import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle

fun LazyListScope.SpeedReadingSubcategory(
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
    odsEnabled: Boolean = false,
    onOdsEnabledChange: (Boolean) -> Unit = {},
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
    verticalIndicatorsSize: Int = 32,
    onVerticalIndicatorsSizeChange: (Int) -> Unit = {},
    showHorizontalBars: Boolean = true,
    onShowHorizontalBarsChange: (Boolean) -> Unit = {},
    horizontalBarsThickness: Int = 2,
    onHorizontalBarsThicknessChange: (Int) -> Unit = {},
    horizontalBarsDistance: Int = 8,
    onHorizontalBarsDistanceChange: (Int) -> Unit = {},
    horizontalBarsColor: Color = Color.Gray,
    onHorizontalBarsColorChange: (Color) -> Unit = {},
    horizontalBarsOpacity: Float = 1.0f,
    onHorizontalBarsOpacityChange: (Float) -> Unit = {},
    focalPointPosition: Float = 0.38f,
    onFocalPointPositionChange: (Float) -> Unit = {},
    customFontEnabled: Boolean = false,
    selectedFontFamily: String = "default",
    onCustomFontChanged: (Boolean) -> Unit = {},
    onFontFamilyChanged: (String) -> Unit = {}
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
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
                selected = odsEnabled,
                title = stringResource(id = R.string.speed_reading_ods),
                onClick = { onOdsEnabledChange(!odsEnabled) }
            )
        }

        item {
            SpeedReadingWordSizeOption(
                wordSize = wordSize,
                onWordSizeChange = onWordSizeChange
            )
        }

        // Color presets (shared with normal reading)
        item {
            ColorPresetOption(
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        }

        item {
            BackgroundImageOption()
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

        // Vertical Indicators
        item {
            SpeedReadingVerticalIndicatorsOption(
                selected = showVerticalIndicators,
                onSelectionChange = onShowVerticalIndicatorsChange
            )
        }

        item {
            AnimatedVisibility(
                visible = showVerticalIndicators,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SpeedReadingVerticalIndicatorsSizeOption(
                    size = verticalIndicatorsSize,
                    onSizeChange = onVerticalIndicatorsSizeChange
                )
            }
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

        // Focal Point Position
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
}