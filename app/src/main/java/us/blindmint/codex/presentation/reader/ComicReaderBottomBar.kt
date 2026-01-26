/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.util.HorizontalAlignment
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.theme.readerBarsColor

@Composable
fun ComicReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    progressBarPadding: Dp,
    progressBarAlignment: HorizontalAlignment,
    progressBarFontSize: TextUnit,
    fontColor: androidx.compose.ui.graphics.Color,
    sidePadding: Dp,
    comicReadingDirection: String = "LTR",
    onPageSelected: (Int) -> Unit
) {
    if (totalPages <= 0) return

    var sliderValue by remember { mutableFloatStateOf((currentPage + 1).toFloat()) }

    LaunchedEffect(currentPage) {
        sliderValue = (currentPage + 1).toFloat()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .noRippleClickable(onClick = {})
            .navigationBarsPadding()
            .padding(horizontal = sidePadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        val displayPage = sliderValue.toInt().coerceIn(1, totalPages)
        val percentage = ((displayPage).toFloat() / totalPages * 100).toInt()
        StyledText(
            text = "$percentage% ($displayPage/$totalPages)",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(Modifier.height(12.dp))

        val layoutDirection = if (comicReadingDirection == "RTL") LayoutDirection.Rtl else LayoutDirection.Ltr

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = sliderValue,
                    valueRange = 1f..totalPages.toFloat(),
                    onValueChange = {},
                    onValueChangeFinished = {
                        val newPage = sliderValue.toInt().coerceIn(1, totalPages) - 1
                        if (newPage != currentPage) {
                            onPageSelected(newPage)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp + progressBarPadding))
    }
}
