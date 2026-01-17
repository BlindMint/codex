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
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.domain.reader.ReaderProgressCount
import us.blindmint.codex.domain.util.HorizontalAlignment
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.main.MainModel

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
    modifier: Modifier = Modifier
) {
    val mainModel = hiltViewModel<MainModel>()
    val mainState = mainModel.state.collectAsStateWithLifecycle()

    if (totalPages <= 0) return

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
                    val progressText = when (progressCount) {
                        ReaderProgressCount.PERCENTAGE -> {
                            val percentage = ((currentPage + 1).toFloat() / totalPages * 100).toInt()
                            "$percentage%"
                        }
                        ReaderProgressCount.PAGE -> "${currentPage + 1}/$totalPages"
                        ReaderProgressCount.QUANTITY -> "${currentPage + 1}/$totalPages"
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
        Slider(
            value = (currentPage + 1).toFloat() / totalPages,
            enabled = !lockMenu,
            onValueChange = { newValue ->
                val newPage = (newValue * totalPages).toInt().coerceIn(0, totalPages - 1)
                onPageSelected(newPage)
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
