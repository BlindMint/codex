/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.blindmint.codex.domain.reader.ReaderProgressCount
import us.blindmint.codex.domain.util.HorizontalAlignment
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.material.Slider

private const val SLIDER_TAG = "CodexComicSlider"
private const val BUG_TAG = "ComicBug"

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReaderBottomBarComicSlider(
    currentPage: Int,
    totalPages: Int,
    lockMenu: Boolean,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showProgressBar: Boolean = true,
    progressCount: ReaderProgressCount = ReaderProgressCount.PAGE,
    progressBarPadding: Dp = 4.dp,
    progressBarAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    progressBarFontSize: TextUnit = 8.sp,
    comicReadingDirection: String = "LTR"
) {

    if (totalPages <= 0) return

    var sliderDragValue by remember { mutableIntStateOf(currentPage) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    val sliderValue = if (isDragged) sliderDragValue else currentPage

    Log.d(BUG_TAG, "[S1] Slider COMPOSABLE: currentPage=$currentPage, sliderValue=$sliderValue, isDragged=$isDragged, lockMenu=$lockMenu")

    Column(modifier = modifier) {
        if (showProgressBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                contentAlignment = progressBarAlignment.alignment
            ) {
                DisableSelection {
                    val displayPage = (sliderValue + 1).coerceIn(1, totalPages)
                    val progressText = when (progressCount) {
                        ReaderProgressCount.PERCENTAGE -> {
                            val percentage = (displayPage.toFloat() / totalPages * 100).toInt()
                            "$percentage%"
                        }
                        ReaderProgressCount.PAGE -> "$displayPage/$totalPages"
                        ReaderProgressCount.QUANTITY -> "$displayPage/$totalPages"
                    }

                    StyledText(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = progressBarFontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(progressBarPadding))
        }

        // Slider
        val layoutDirection = remember(comicReadingDirection) {
            if (comicReadingDirection == "RTL") LayoutDirection.Rtl else LayoutDirection.Ltr
        }

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Slider(
                value = sliderValue,
                valueRange = 0..totalPages - 1,
                enabled = !lockMenu,
                interactionSource = interactionSource,
                onValueChange = { newValue ->
                    sliderDragValue = newValue
                },
                onValueChangeFinished = {
                    val newPage = sliderDragValue
                    Log.d(BUG_TAG, "[S3] Slider onValueChangeFinished: newPage=$newPage, currentPage=$currentPage, sliderValue=$sliderDragValue")
                    if (newPage != currentPage) {
                        Log.d(BUG_TAG, "[S3] >>> Slider calling onPageSelected($newPage)")
                        onPageSelected(newPage)
                    } else {
                        Log.d(BUG_TAG, "[S3] Slider skipping (same page)")
                    }
                },
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                    disabledThumbColor = MaterialTheme.colorScheme.primary,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                )
            )
        }
    }
}
