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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingScaffold(
    words: List<SpeedReaderWord>,
    book: us.blindmint.codex.domain.library.book.Book,
    bookTitle: String,
    chapterTitle: String?,
    currentWordIndex: Int,
    totalWords: Int,
    initialWordIndex: Int = -1,
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
    onCurrentWordIndexChange: (Int) -> Unit = {},
    onNavigateWord: (Int) -> Unit,
    onChangeProgress: (Float, Int) -> Unit = { _, _ -> },
    onSaveProgress: (Float, Int) -> Unit = { _, _ -> },
    showOverlayMenu: Boolean = true,
    onPlayPause: () -> Unit = {}
) {
    var alwaysShowPlayPause by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) } // Start with menu hidden
    var isPlaying by remember { mutableStateOf(false) }
    var navigateWordCallback: ((Int) -> Unit)? by remember { mutableStateOf(null) }
    var showWordPicker by remember { mutableStateOf(false) }
    // Calculate current progress from word index and total words
    val currentProgress = remember(currentWordIndex, totalWords) {
        if (totalWords > 0) currentWordIndex.toFloat() / totalWords else 0f
    }

    var selectedWordIndex by remember { mutableIntStateOf(-1) } // Start invalid, set when ready
    var realTimeProgress by remember { mutableFloatStateOf(currentProgress) } // Live progress updates

    // Debounce WPM updates to prevent excessive screen re-renders
    val coroutineScope = rememberCoroutineScope()
    var localWpm by remember { mutableIntStateOf(wpm) }
    var debounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Update local WPM when parent changes (e.g., loading settings)
    LaunchedEffect(wpm) {
        localWpm = wpm
    }

    // Debounce WPM updates during slider dragging to prevent excessive re-renders
    LaunchedEffect(localWpm) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(300) // 300ms debounce
            onWpmChange(localWpm)
        }
    }

    // Local WPM change handler for immediate UI responsiveness
    val localOnWpmChange = { newWpm: Int ->
        localWpm = newWpm
    }

    // Store parent callbacks to avoid name collision
    val parentOnChangeProgress = onChangeProgress

    // Notify parent of menu visibility changes
    LaunchedEffect(showMenu) {
        onMenuVisibilityChanged(showMenu)
    }

    // Update realTimeProgress when currentProgress changes (for UI display)
    LaunchedEffect(currentProgress) {
        realTimeProgress = currentProgress
    }

    // Initialize selectedWordIndex based on initial word index when words load
    LaunchedEffect(words, initialWordIndex) {
        Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect START] words.size=${words.size}, initialWordIndex=$initialWordIndex, isLoading=$isLoading")
        Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect START] currentWordIndex=$currentWordIndex, totalWords=$totalWords")

        if (words.isNotEmpty() && initialWordIndex >= 0) {
            val wordIndex = initialWordIndex.coerceIn(0, words.size - 1)

            Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect] Setting selectedWordIndex:")
            Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect]   initialWordIndex = $initialWordIndex")
            Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect]   words.size = ${words.size}")
            Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect]   coerced wordIndex = $wordIndex")
            Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect]   BEFORE: selectedWordIndex = $selectedWordIndex")

            selectedWordIndex = wordIndex

            Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect]   AFTER: selectedWordIndex = $selectedWordIndex")
        } else {
            Log.w("SPEED_READER_SCAFFOLD", "[LaunchedEffect] Skipping - words.isEmpty=${words.isEmpty()}, initialWordIndex=$initialWordIndex")
        }

        Log.d("SPEED_READER_SCAFFOLD", "[LaunchedEffect END] completed")
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        topBar = {
            if (!isLoading && !showOverlayMenu && selectedWordIndex >= 0) {
                // For independent speed reader: always show minimal top bar with back and settings icons
                androidx.compose.material3.TopAppBar(
                    title = {},
                      navigationIcon = {
                          androidx.compose.material3.IconButton(onClick = {
                              // Always save current progress on exit
                              val totalWords = words.size
                              // If playing, pause first to ensure progress is saved
                              val wasPlaying = isPlaying
                              if (isPlaying) {
                                  onPlayPause()
                              }
                              val currentWordIndex = (realTimeProgress * totalWords).toInt().coerceIn(0, totalWords - 1)
                              Log.d("SPEED_READER", "Exit: wasPlaying=$wasPlaying, saving progress=$realTimeProgress, wordIndex=$currentWordIndex")
                              onSaveProgress(realTimeProgress, currentWordIndex)
                              onExitSpeedReading()
                          }) {
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
            } else if (!isLoading && showOverlayMenu && selectedWordIndex >= 0) {
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
                        currentWordIndex = currentWordIndex,
                        totalWords = totalWords,
                        onExitSpeedReading = onExitSpeedReading,
                        onShowSettings = onShowSpeedReadingSettings
                    )
                }
            }
        },
        bottomBar = {
            if (!isLoading && showOverlayMenu && selectedWordIndex >= 0) {
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
                        onChangeProgress = { progress -> onChangeProgress(progress, 0) },
                        wpm = localWpm,
                        onWpmChange = localOnWpmChange,
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
            if (isLoading || words.isEmpty() || initialWordIndex < 0 || selectedWordIndex < 0) {
                // Show loading indicator until text is ready AND word index is synchronized
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
                Log.d("SPEED_READER_SCAFFOLD", "[CONTENT CALL] Calling SpeedReadingContent with:")
                Log.d("SPEED_READER_SCAFFOLD", "[CONTENT CALL]   words.size=${words.size}")
                Log.d("SPEED_READER_SCAFFOLD", "[CONTENT CALL]   currentWordIndex=$currentWordIndex")
                Log.d("SPEED_READER_SCAFFOLD", "[CONTENT CALL]   totalWords=$totalWords")
                Log.d("SPEED_READER_SCAFFOLD", "[CONTENT CALL]   initialWordIndex=$initialWordIndex")
                Log.d("SPEED_READER_SCAFFOLD", "[CONTENT CALL]   selectedWordIndex=$selectedWordIndex")

                SpeedReadingContent(
                words = words,
                currentWordIndex = currentWordIndex,
                totalWords = totalWords,
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
                wpm = localWpm,
                isPlaying = isPlaying,
                onWpmChange = localOnWpmChange,
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
                 onProgressUpdate = { progress, wordIndex ->
                     // Update real-time progress for UI display
                     realTimeProgress = progress
                     // Also update the underlying book progress periodically
                     parentOnChangeProgress(progress, wordIndex)
                 },
                 onSaveProgress = { progress, wordIndex ->
                     // Immediate progress save for manual pauses (no throttling)
                     realTimeProgress = progress
                     onSaveProgress(progress, wordIndex)
                 },
                showBottomBar = !showOverlayMenu
            )
            }
        }

        // Word Picker Sheet - only show when not loading
        if (showWordPicker && !isLoading && words.isNotEmpty()) {
              SpeedReadingWordPickerSheet(
                  words = words,
                  currentWordIndex = currentWordIndex,
                  totalWords = totalWords,
                  backgroundColor = backgroundColor,
                  fontColor = fontColor,
                  onDismiss = { showWordPicker = false },
                  onConfirm = { progress, wordIndexInText ->
                      if (isPlaying) onPlayPause()
                      selectedWordIndex = wordIndexInText
                      realTimeProgress = progress
                      onSaveProgress(progress, wordIndexInText)
                      // Notify parent to update current word index for reader
                      onCurrentWordIndexChange(wordIndexInText)
                      // Also notify parent to ensure proper state update
                      parentOnChangeProgress(progress, wordIndexInText)
                  }
              )
        }
    }
}