/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.util.noRippleClickable

@Composable
fun SpeedReadingScaffold(
    text: List<ReaderText>,
    currentProgress: Float,
    backgroundColor: Color,
    fontFamily: FontFamily,
    sentencePauseMs: Int,
    progress: String,
    bottomBarPadding: Dp,
    onExitSpeedReading: () -> Unit,
    onShowSettings: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var alwaysShowPlayPause by remember { mutableStateOf(false) }
    val wpm = remember { mutableStateOf(300) }
    val isPlaying = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = showMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SpeedReadingTopBar(
                    onExitSpeedReading = onExitSpeedReading,
                    alwaysShowPlayPause = alwaysShowPlayPause,
                    onToggleAlwaysShowPlayPause = { alwaysShowPlayPause = !alwaysShowPlayPause }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = showMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SpeedReadingBottomBar(
                    progress = progress,
                    wpm = wpm.value,
                    onWpmChange = { wpm.value = it },
                    isPlaying = isPlaying.value,
                    onPlayPause = { isPlaying.value = !isPlaying.value },
                    onShowSettings = onShowSettings,
                    bottomBarPadding = bottomBarPadding
                )
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noRippleClickable { showMenu = !showMenu }
                .padding(paddingValues)
        ) {
            SpeedReadingContent(
                text = text,
                currentProgress = currentProgress,
                backgroundColor = backgroundColor,
                fontFamily = fontFamily,
                sentencePauseMs = sentencePauseMs,
                wpm = wpm.value,
                isPlaying = isPlaying.value,
                onWpmChange = { wpm.value = it },
                onPlayPause = { isPlaying.value = !isPlaying.value },
                alwaysShowPlayPause = alwaysShowPlayPause
            )
        }
    }
}