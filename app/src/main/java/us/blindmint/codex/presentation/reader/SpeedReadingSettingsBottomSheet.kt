/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheetTabRow
import us.blindmint.codex.presentation.settings.reader.speed_reading.SpeedReadingSubcategory
import us.blindmint.codex.presentation.settings.reader.speed_reading.SpeedReadingTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingSettingsBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    wpm: Int = 300,
    onWpmChange: (Int) -> Unit = {},
    manualSentencePauseEnabled: Boolean = false,
    onManualSentencePauseEnabledChange: (Boolean) -> Unit = {},
    sentencePauseDuration: Int = 350,
    onSentencePauseDurationChange: (Int) -> Unit = {},
    osdEnabled: Boolean = true,
    onOsdEnabledChange: (Boolean) -> Unit = {},
    wordSize: Int = 48,
    onWordSizeChange: (Int) -> Unit = {},
    accentCharacterEnabled: Boolean = true,
    onAccentCharacterEnabledChange: (Boolean) -> Unit = {},
    accentColor: Color = Color.Red,
    onAccentColorChange: (Color) -> Unit = {},
    accentOpacity: Float = 1.0f,
    onAccentOpacityChange: (Float) -> Unit = {},
    showVerticalIndicators: Boolean = true,
    onShowVerticalIndicatorsChange: (Boolean) -> Unit = {},
    verticalIndicatorsSize: Int = 8,
    onVerticalIndicatorsSizeChange: (Int) -> Unit = {},
    showHorizontalBars: Boolean = true,
    onShowHorizontalBarsChange: (Boolean) -> Unit = {},
    horizontalBarsThickness: Int = 2,
    onHorizontalBarsThicknessChange: (Int) -> Unit = {},
    horizontalBarsLength: Float = 0.9f,
    onHorizontalBarsLengthChange: (Float) -> Unit = {},
    horizontalBarsDistance: Int = 8,
    onHorizontalBarsDistanceChange: (Int) -> Unit = {},
    horizontalBarsColor: Color = Color.Gray,
    onHorizontalBarsColorChange: (Color) -> Unit = {},
    horizontalBarsOpacity: Float = 1.0f,
    onHorizontalBarsOpacityChange: (Float) -> Unit = {},
    focalPointPosition: Float = 0.38f,
    onFocalPointPositionChange: (Float) -> Unit = {},
    verticalIndicatorType: us.blindmint.codex.ui.reader.SpeedReadingVerticalIndicatorType = us.blindmint.codex.ui.reader.SpeedReadingVerticalIndicatorType.LINE,
    onVerticalIndicatorTypeChange: (us.blindmint.codex.ui.reader.SpeedReadingVerticalIndicatorType) -> Unit = {},
    customFontEnabled: Boolean = false,
    onCustomFontEnabledChange: (Boolean) -> Unit = {},
    selectedFontFamily: String = "default",
    onFontFamilyChange: (String) -> Unit = {}
) {
    if (show) {
        val scrollState = rememberLazyListState()
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        ModalBottomSheet(
            hasFixedHeight = true,
            scrimColor = BottomSheetDefaults.ScrimColor,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            onDismissRequest = onDismiss,
            sheetGesturesEnabled = true,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            // Tab row with General and Focus tabs
            ModalBottomSheetTabRow(
                selectedTabIndex = selectedTabIndex,
                tabs = listOf(
                    stringResource(id = R.string.speed_reading_general_tab),
                    stringResource(id = R.string.speed_reading_focus_tab)
                )
            ) { newIndex ->
                selectedTabIndex = newIndex
            }

            LazyColumnWithScrollbar(
                modifier = Modifier.fillMaxSize(),
                state = scrollState
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (selectedTabIndex) {
                    0 -> { // General tab
                        SpeedReadingSubcategory(
                            tab = SpeedReadingTab.GENERAL,
                            titleColor = { MaterialTheme.colorScheme.onSurface },
                            showTitle = false,
                            showDivider = false,
                            wpm = wpm,
                            onWpmChange = onWpmChange,
                            manualSentencePauseEnabled = manualSentencePauseEnabled,
                            onManualSentencePauseEnabledChange = onManualSentencePauseEnabledChange,
                            sentencePauseDuration = sentencePauseDuration,
                            onSentencePauseDurationChange = onSentencePauseDurationChange,
                            wordSize = wordSize,
                            onWordSizeChange = onWordSizeChange,
                            accentCharacterEnabled = accentCharacterEnabled,
                            onAccentCharacterEnabledChange = onAccentCharacterEnabledChange,
                            accentColor = accentColor,
                            onAccentColorChange = onAccentColorChange,
                            accentOpacity = accentOpacity,
                            onAccentOpacityChange = onAccentOpacityChange,
                            showVerticalIndicators = showVerticalIndicators,
                            onShowVerticalIndicatorsChange = onShowVerticalIndicatorsChange,
                            verticalIndicatorsSize = verticalIndicatorsSize,
                            onVerticalIndicatorsSizeChange = onVerticalIndicatorsSizeChange,
                            showHorizontalBars = showHorizontalBars,
                            onShowHorizontalBarsChange = onShowHorizontalBarsChange,
                            horizontalBarsThickness = horizontalBarsThickness,
                            onHorizontalBarsThicknessChange = onHorizontalBarsThicknessChange,
                            horizontalBarsLength = horizontalBarsLength,
                            onHorizontalBarsLengthChange = onHorizontalBarsLengthChange,
                            horizontalBarsDistance = horizontalBarsDistance,
                            onHorizontalBarsDistanceChange = onHorizontalBarsDistanceChange,
                            horizontalBarsColor = horizontalBarsColor,
                            onHorizontalBarsColorChange = onHorizontalBarsColorChange,
                            horizontalBarsOpacity = horizontalBarsOpacity,
                            onHorizontalBarsOpacityChange = onHorizontalBarsOpacityChange,
                            focalPointPosition = focalPointPosition,
                            onFocalPointPositionChange = onFocalPointPositionChange,
                            verticalIndicatorType = verticalIndicatorType,
                            onVerticalIndicatorTypeChange = onVerticalIndicatorTypeChange,
                            osdEnabled = osdEnabled,
                            onOsdEnabledChange = onOsdEnabledChange,
                            customFontEnabled = customFontEnabled,
                            onCustomFontChanged = onCustomFontEnabledChange,
                            selectedFontFamily = selectedFontFamily,
                            onFontFamilyChanged = onFontFamilyChange
                        )
                    }
                    1 -> { // Focus tab
                        SpeedReadingSubcategory(
                            tab = SpeedReadingTab.FOCUS,
                            titleColor = { MaterialTheme.colorScheme.onSurface },
                            showTitle = false,
                            showDivider = false,
                            wpm = wpm,
                            onWpmChange = onWpmChange,
                            manualSentencePauseEnabled = manualSentencePauseEnabled,
                            onManualSentencePauseEnabledChange = onManualSentencePauseEnabledChange,
                            sentencePauseDuration = sentencePauseDuration,
                            onSentencePauseDurationChange = onSentencePauseDurationChange,
                            wordSize = wordSize,
                            onWordSizeChange = onWordSizeChange,
                            accentCharacterEnabled = accentCharacterEnabled,
                            onAccentCharacterEnabledChange = onAccentCharacterEnabledChange,
                            accentColor = accentColor,
                            onAccentColorChange = onAccentColorChange,
                            accentOpacity = accentOpacity,
                            onAccentOpacityChange = onAccentOpacityChange,
                            showVerticalIndicators = showVerticalIndicators,
                            onShowVerticalIndicatorsChange = onShowVerticalIndicatorsChange,
                            verticalIndicatorsSize = verticalIndicatorsSize,
                            onVerticalIndicatorsSizeChange = onVerticalIndicatorsSizeChange,
                            showHorizontalBars = showHorizontalBars,
                            onShowHorizontalBarsChange = onShowHorizontalBarsChange,
                            horizontalBarsThickness = horizontalBarsThickness,
                            onHorizontalBarsThicknessChange = onHorizontalBarsThicknessChange,
                            horizontalBarsLength = horizontalBarsLength,
                            onHorizontalBarsLengthChange = onHorizontalBarsLengthChange,
                            horizontalBarsDistance = horizontalBarsDistance,
                            onHorizontalBarsDistanceChange = onHorizontalBarsDistanceChange,
                            horizontalBarsColor = horizontalBarsColor,
                            onHorizontalBarsColorChange = onHorizontalBarsColorChange,
                            horizontalBarsOpacity = horizontalBarsOpacity,
                            onHorizontalBarsOpacityChange = onHorizontalBarsOpacityChange,
                            focalPointPosition = focalPointPosition,
                            onFocalPointPositionChange = onFocalPointPositionChange,
                            verticalIndicatorType = verticalIndicatorType,
                            onVerticalIndicatorTypeChange = onVerticalIndicatorTypeChange,
                            osdEnabled = osdEnabled,
                            onOsdEnabledChange = onOsdEnabledChange,
                            customFontEnabled = customFontEnabled,
                            onCustomFontChanged = onCustomFontEnabledChange,
                            selectedFontFamily = selectedFontFamily,
                            onFontFamilyChanged = onFontFamilyChange
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}