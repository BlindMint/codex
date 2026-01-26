/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun ReaderBottomBarComicSlider(
    currentPage: Int,
    totalPages: Int,
    lockMenu: Boolean,
    onPageSelected: (Int) -> Unit,
    showProgressBar: Boolean = true,
    progressCount: ReaderProgressCount = ReaderProgressCount.PAGE,
    progressBarPadding: Dp = 4.dp,
    progressBarAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    progressBarFontSize: TextUnit = 8.sp,
    comicReadingDirection: String = "LTR",
    modifier: Modifier = Modifier
) {

    if (totalPages <= 0) return

    var sliderValue by remember { mutableFloatStateOf((currentPage + 1).toFloat()) }

    LaunchedEffect(currentPage) {
        sliderValue = (currentPage + 1).toFloat()
    }

    Column(modifier = modifier) {
        // Progress text display
        if (showProgressBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                contentAlignment = progressBarAlignment.alignment
            ) {
                DisableSelection {
                    val displayPage = sliderValue.toInt().coerceIn(1, totalPages)
                    val progressText = when (progressCount) {
                        ReaderProgressCount.PERCENTAGE -> {
                            val percentage = ((displayPage).toFloat() / totalPages * 100).toInt()
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
                valueRange = 1f..totalPages.toFloat(),
                enabled = !lockMenu,
                onValueChange = { newValue ->
                    sliderValue = newValue
                },
                onValueChangeFinished = {
                    val newPage = sliderValue.toInt().coerceIn(1, totalPages) - 1
                    if (newPage != currentPage) {
                        onPageSelected(newPage)
                    }
                },
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
                    disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                    disabledThumbColor = MaterialTheme.colorScheme.primary,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
                )
            )
        }
    }
}
