/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import us.blindmint.codex.presentation.core.components.progress_indicator.CircularProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight

// Find the nearest sentence or paragraph start before the given word index
private fun findNearestSentenceStart(words: List<String>, targetIndex: Int): Int {
    if (targetIndex <= 0) return targetIndex

    // Look backwards up to 50 words to find a sentence boundary
    val searchStart = maxOf(0, targetIndex - 50)

    for (i in targetIndex downTo searchStart) {
        val word = words.getOrNull(i) ?: continue

        // Check for sentence endings (period, exclamation, question mark)
        if (word.endsWith('.') || word.endsWith('!') || word.endsWith('?') ||
            word.endsWith(".\"") || word.endsWith("!\"") || word.endsWith("?\"")) {
            // Return the index after this sentence-ending word
            return (i + 1).coerceAtMost(words.size - 1)
        }
    }

    // If no sentence boundary found within 50 words, don't adjust the position
    return targetIndex
}

@OptIn(ExperimentalMaterial3Api::class)
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
    verticalIndicatorType: us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType,
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
    onChangeProgress: (Float) -> Unit = {},
    showOverlayMenu: Boolean = true,
    onPlayPause: () -> Unit = {},
    onShowWordPicker: () -> Unit = {}
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

    // Initialize selectedWordIndex based on current progress when text loads
    LaunchedEffect(text, currentProgress) {
        if (text.isNotEmpty()) {
            // Extract all words from text to determine starting position
            val allWords = text
                .filterIsInstance<us.blindmint.codex.domain.reader.ReaderText.Text>()
                .flatMap { it.line.text.split("\\s+".toRegex()) }
                .filter { it.isNotBlank() }

            val rawWordIndex = (currentProgress * allWords.size).toInt().coerceIn(0, (allWords.size - 1).coerceAtLeast(0))

            // For better UX when switching from normal reader, snap to nearest sentence/paragraph boundary
            val adjustedWordIndex = findNearestSentenceStart(allWords, rawWordIndex)

            Log.d("SPEED_READER", "Loading with progress=$currentProgress, raw wordIndex=$rawWordIndex, adjusted wordIndex=$adjustedWordIndex, totalWords=${allWords.size}")
            selectedWordIndex = adjustedWordIndex
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        topBar = {
            if (!isLoading && !showOverlayMenu) {
                // For independent speed reader: always show minimal top bar with back and settings icons
                androidx.compose.material3.TopAppBar(
                    title = {},
                    navigationIcon = {
                        androidx.compose.material3.IconButton(onClick = onExitSpeedReading) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = fontColor
                            )
                        }
                    },
                    actions = {
                        androidx.compose.material3.IconButton(onClick = { if (isPlaying) onPlayPause(); onShowSpeedReadingSettings() }) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = fontColor
                            )
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor.copy(alpha = 0.9f)
                    )
                )
            } else if (!isLoading && showOverlayMenu) {
                // For integrated speed reader: show overlay menu when tapped
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
            }
        },
        bottomBar = {
            if (!isLoading && showOverlayMenu) {
                // For integrated speed reader: show overlay menu when tapped
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.5.dp
                    )
                }
            } else {
                SpeedReadingContent(
                text = text,
                currentProgress = currentProgress,
                totalProgress = realTimeProgress,
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
                },
                showBottomBar = !showOverlayMenu
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