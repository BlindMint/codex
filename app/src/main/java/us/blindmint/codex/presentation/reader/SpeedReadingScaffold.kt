/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.annotation.SuppressLint
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.util.noRippleClickable

@Composable
fun SpeedReadingScaffold(
    text: List<ReaderText>,
    bookTitle: String,
    chapterTitle: String?,
    currentProgress: Float,
    backgroundColor: Color,
    fontColor: Color,
    accentCharacterEnabled: Boolean,
    accentColor: Color,
    fontFamily: FontFamily,
    sentencePauseMs: Int,
    wordSize: Int,
    accentOpacity: Float,
    showVerticalIndicators: Boolean,
    verticalIndicatorsSize: Int,
    showHorizontalBars: Boolean,
    horizontalBarsThickness: Int,
    horizontalBarsDistance: Int,
    horizontalBarsColor: Color,
    horizontalBarsOpacity: Float,
    focalPointPosition: Float,
    progress: String,
    bottomBarPadding: Dp,
    showWpmIndicator: Boolean,
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    odsEnabled: Boolean,
    onExitSpeedReading: () -> Unit,
    onShowSpeedReadingSettings: () -> Unit,
    onMenuVisibilityChanged: (Boolean) -> Unit = {}
) {
    var alwaysShowPlayPause by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(true) } // Start with menu visible
    var isPlaying by remember { mutableStateOf(false) }

    // Notify parent of menu visibility changes
    LaunchedEffect(showMenu) {
        onMenuVisibilityChanged(showMenu)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        topBar = {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = showMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SpeedReadingTopBar(
                    bookTitle = bookTitle,
                    chapterTitle = chapterTitle,
                    currentProgress = currentProgress,
                    onExitSpeedReading = onExitSpeedReading,
                    onShowSettings = onShowSpeedReadingSettings
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
                    wpm = wpm,
                    onWpmChange = onWpmChange,
                    isPlaying = isPlaying,
                    onPlayPause = { isPlaying = !isPlaying },
                    bottomBarPadding = bottomBarPadding
                )
            }
        },
        containerColor = backgroundColor
    ) {
        // Don't use paddingValues to avoid content shifting when menu shows/hides
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .noRippleClickable { showMenu = !showMenu }
        ) {
            SpeedReadingContent(
                text = text,
                currentProgress = currentProgress,
                backgroundColor = backgroundColor,
                fontColor = fontColor,
                fontFamily = fontFamily,
                sentencePauseMs = sentencePauseMs,
                wordSize = wordSize,
                accentCharacterEnabled = accentCharacterEnabled,
                accentColor = accentColor,
                accentOpacity = accentOpacity,
                showVerticalIndicators = showVerticalIndicators,
                verticalIndicatorsSize = verticalIndicatorsSize,
                showHorizontalBars = showHorizontalBars,
                horizontalBarsThickness = horizontalBarsThickness,
                horizontalBarsDistance = horizontalBarsDistance,
                horizontalBarsColor = horizontalBarsColor,
                horizontalBarsOpacity = horizontalBarsOpacity,
                focalPointPosition = focalPointPosition,
                wpm = wpm,
                isPlaying = isPlaying,
                onWpmChange = onWpmChange,
                onPlayPause = { isPlaying = !isPlaying },
                alwaysShowPlayPause = alwaysShowPlayPause,
                showWpmIndicator = showWpmIndicator,
                odsEnabled = odsEnabled
            )
        }
    }
}