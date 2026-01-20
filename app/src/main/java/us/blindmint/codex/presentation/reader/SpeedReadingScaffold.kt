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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.components.progress_indicator.SkullProgressIndicator
import us.blindmint.codex.presentation.core.util.noRippleClickable
import androidx.compose.foundation.layout.size

@Composable
fun SpeedReadingScaffold(
    text: List<ReaderText>,
    book: us.blindmint.codex.domain.library.book.Book,
    bookTitle: String,
    chapterTitle: String?,
    currentProgress: Float,
    totalProgress: Float,
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
    verticalIndicatorType: us.blindmint.codex.ui.reader.SpeedReadingVerticalIndicatorType,
    showHorizontalBars: Boolean,
    horizontalBarsThickness: Int,
    horizontalBarsLength: Float,
    horizontalBarsDistance: Int,
    horizontalBarsColor: Color,
    horizontalBarsOpacity: Float,
    focalPointPosition: Float,
    progress: String,
    bottomBarPadding: Dp,
    showWpmIndicator: Boolean,
    wpm: Int,
    isLoading: Boolean = false,
    osdHeight: Float = 0.2f,
    osdSeparation: Float = 0.5f,
    centerWord: Boolean = false,
    onWpmChange: (Int) -> Unit,
    osdEnabled: Boolean,
    onExitSpeedReading: () -> Unit,
    onShowSpeedReadingSettings: () -> Unit,
    onMenuVisibilityChanged: (Boolean) -> Unit = {},
    onNavigateWord: (Int) -> Unit,
    onToggleMenu: () -> Unit = {},
    navigateWord: (Int) -> Unit = {},
    onChangeProgress: (Float) -> Unit = {}
) {
    var alwaysShowPlayPause by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) } // Start with menu hidden
    var isPlaying by remember { mutableStateOf(false) }
    var navigateWordCallback: ((Int) -> Unit)? by remember { mutableStateOf(null) }
    var showWordPicker by remember { mutableStateOf(false) }
    var selectedProgress by remember { mutableFloatStateOf(currentProgress) }
    var selectedWordIndex by remember { mutableIntStateOf(0) }
    var realTimeProgress by remember { mutableFloatStateOf(currentProgress) } // Live progress updates

    // Notify parent of menu visibility changes
    LaunchedEffect(showMenu) {
        onMenuVisibilityChanged(showMenu)
    }

    // Update selectedProgress when currentProgress changes (to start from current position in word picker)
    LaunchedEffect(currentProgress) {
        selectedProgress = currentProgress
        realTimeProgress = currentProgress
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
                    progressValue = realTimeProgress, // Use real-time progress for live updates
                    book = book, // Need to add book parameter
                    lockMenu = false, // For speed reading, allow seeking
                    onChangeProgress = onChangeProgress,
                    wpm = wpm,
                    onWpmChange = onWpmChange,
                    isPlaying = isPlaying,
                    onPlayPause = { isPlaying = !isPlaying },
                    onNavigateWord = onNavigateWord,
                    navigateWord = { direction ->
                        navigateWordCallback?.invoke(direction)
                    },
                    onCloseMenu = { showMenu = false },
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
        ) {
            if (isLoading || text.isEmpty()) {
                // Show loading indicator when text is not ready
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SkullProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        size = 56.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        progressColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.5.dp
                    )
                }
            } else {
                SpeedReadingContent(
                text = text,
                currentProgress = currentProgress,
                totalProgress = totalProgress,
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
                verticalIndicatorType = verticalIndicatorType,
                showHorizontalBars = showHorizontalBars,
                horizontalBarsThickness = horizontalBarsThickness,
                horizontalBarsLength = horizontalBarsLength,
                horizontalBarsDistance = horizontalBarsDistance,
                horizontalBarsColor = horizontalBarsColor,
                horizontalBarsOpacity = horizontalBarsOpacity,
                focalPointPosition = focalPointPosition,
                wpm = wpm,
                isPlaying = isPlaying,
                onWpmChange = onWpmChange,
                onPlayPause = { isPlaying = !isPlaying },
                onNavigateWord = onNavigateWord,
                onToggleMenu = { showMenu = !showMenu },
                navigateWord = navigateWordCallback ?: {},
                onRegisterNavigationCallback = { callback ->
                    navigateWordCallback = callback
                },
                alwaysShowPlayPause = alwaysShowPlayPause,
                showWpmIndicator = showWpmIndicator,
                osdEnabled = osdEnabled,
                osdHeight = osdHeight,
                osdSeparation = osdSeparation,
                centerWord = centerWord,
                initialWordIndex = selectedWordIndex,
                onShowWordPicker = { showWordPicker = true },
                onProgressUpdate = { progress ->
                    // Update real-time progress for UI display
                    realTimeProgress = progress
                    // Also update the underlying book progress periodically
                    onChangeProgress(progress)
                }
            )
            }
        }

        // Word Picker Sheet - only show when not loading
        if (showWordPicker && !isLoading && text.isNotEmpty()) {
            SpeedReadingWordPickerSheet(
                text = text,
                currentProgress = selectedProgress,
                backgroundColor = backgroundColor,
                fontColor = fontColor,
                onDismiss = { showWordPicker = false },
                onConfirm = { progress, wordIndexInText ->
                    selectedProgress = progress
                    selectedWordIndex = wordIndexInText
                    onChangeProgress(progress)
                    showWordPicker = false
                }
            )
        }
    }
}