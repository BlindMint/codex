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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheetTabRow
import us.blindmint.codex.presentation.settings.reader.speed_reading.SpeedReadingSubcategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingSettingsBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    wpm: Int = 300,
    onWpmChange: (Int) -> Unit = {},
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
    verticalIndicatorsSize: Int = 32,
    onVerticalIndicatorsSizeChange: (Int) -> Unit = {},
    showHorizontalBars: Boolean = true,
    onShowHorizontalBarsChange: (Boolean) -> Unit = {},
    horizontalBarsThickness: Int = 2,
    onHorizontalBarsThicknessChange: (Int) -> Unit = {},
    horizontalBarsColor: Color = Color.Gray,
    onHorizontalBarsColorChange: (Color) -> Unit = {},
    customFontEnabled: Boolean = false,
    onCustomFontEnabledChange: (Boolean) -> Unit = {},
    selectedFontFamily: String = "default",
    onFontFamilyChange: (String) -> Unit = {}
) {
    if (show) {
        val scrollState = rememberLazyListState()

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
            // Single tab row for Speed Reading
            ModalBottomSheetTabRow(
                selectedTabIndex = 0,
                tabs = listOf(stringResource(id = R.string.speed_reading_tab))
            ) {
                // Only one tab, no navigation needed
            }

            LazyColumnWithScrollbar(
                modifier = Modifier.fillMaxSize(),
                state = scrollState
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SpeedReadingSubcategory(
                    titleColor = { MaterialTheme.colorScheme.onSurface },
                    showTitle = false,
                    showDivider = false,
                    wpm = wpm,
                    onWpmChange = onWpmChange,
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
                    horizontalBarsColor = horizontalBarsColor,
                    onHorizontalBarsColorChange = onHorizontalBarsColorChange,
                    customFontEnabled = customFontEnabled,
                    onCustomFontChanged = onCustomFontEnabledChange,
                    selectedFontFamily = selectedFontFamily,
                    onFontFamilyChanged = onFontFamilyChange
                )

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}