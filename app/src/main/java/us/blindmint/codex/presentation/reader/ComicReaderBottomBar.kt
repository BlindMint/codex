/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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

        // Progress text at top
        val percentage = ((currentPage + 1).toFloat() / totalPages * 100).toInt()
        StyledText(
            text = "$percentage% (${currentPage + 1}/$totalPages)",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(Modifier.height(12.dp))

        // Interactive slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRTL = comicReadingDirection == "RTL"
            val sliderValue = if (isRTL) {
                1f - (currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1))
            } else {
                (currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1))
            }

            Slider(
                value = if (isRTL) 1f - sliderValue else sliderValue,
                onValueChange = { newValue ->
                    val adjustedValue = if (isRTL) newValue else 1f - newValue
                    val newPage = (adjustedValue * (totalPages - 1)).toInt().coerceIn(0, totalPages - 1)
                    onPageSelected(newPage)
                },
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        if (isRTL) scaleX = -1f
                    },
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(Modifier.height(8.dp + progressBarPadding))
    }
}
