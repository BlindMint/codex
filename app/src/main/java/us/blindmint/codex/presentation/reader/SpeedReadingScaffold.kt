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
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.R
import androidx.compose.material3.Scaffold
import androidx.compose.ui.res.painterResource
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
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.Spring.StiffnessMedium
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.domain.reader.FocusIndicatorsType
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.components.placeholder.ErrorPlaceholder
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import us.blindmint.codex.presentation.core.util.LocalActivity


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
    autoHideOsd: Boolean = true,
    centerWord: Boolean = false,
    focusIndicators: String = "OFF",
    errorMessage: UIText? = null,
    onWpmChange: (Int) -> Unit,
    osdEnabled: Boolean,
    onExitSpeedReading: () -> Unit,
    onShowSpeedReadingSettings: () -> Unit,
    onWordPicked: (Int) -> Unit,
    onNavigateWord: (Int) -> Unit,
    onChangeProgress: (Float, Int) -> Unit,
    onSaveProgress: (Float, Int) -> Unit,
    onPlayPause: () -> Unit,
    keepScreenOn: Boolean = true
) {
    var alwaysShowPlayPause by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var navigateWordCallback: ((Int) -> Unit)? by remember { mutableStateOf(null) }
    var showWordPicker by remember { mutableStateOf(false) }
    var wordPickerRefreshKey by remember { mutableIntStateOf(0) }
    // Calculate current progress from word index and total words
    val currentProgress = remember(currentWordIndex, totalWords) {
        if (totalWords > 0) currentWordIndex.toFloat() / totalWords else 0f
    }

    val activity = LocalActivity.current

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
 
    // Update realTimeProgress when currentProgress changes (for UI display)
    LaunchedEffect(currentProgress) {
        realTimeProgress = currentProgress
    }

    // Control system bar visibility and screen timeout based on play state and keep screen on setting
    LaunchedEffect(isPlaying, keepScreenOn) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (isPlaying && keepScreenOn) {
            // Hide system bars when playing for immersive reading
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            // Keep screen on during playback if setting is enabled
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            // Show system bars when paused
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            // Allow screen timeout when paused or setting is disabled
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
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
              if (!isLoading && selectedWordIndex >= 0) {
                  // Show minimal top bar when PAUSED (system bars visible)
                  // Hide when PLAYING for immersive reading
                  if (!isPlaying) {
                      androidx.compose.material3.TopAppBar(
                          title = {},
                            navigationIcon = {
                                androidx.compose.material3.IconButton(onClick = {
                                    if (words.isNotEmpty()) {
                                        val totalWords = words.size
                                        val currentWordIndex = (realTimeProgress * totalWords).toInt().coerceIn(0, totalWords - 1)
                                        Log.d("SPEED_READER", "Exit: saving progress=$realTimeProgress, wordIndex=$currentWordIndex")
                                        onSaveProgress(realTimeProgress, currentWordIndex)
                                    }
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
                              androidx.compose.material3.IconButton(onClick = {
                                  if (isPlaying) onPlayPause()
                                  onShowSpeedReadingSettings()
                              }) {
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
                  }
            }
        },
        bottomBar = {
            if (!isLoading && !isPlaying && selectedWordIndex >= 0) {
                // Minimal bottom bar when PAUSED (just placeholder for Android nav bar area)
                // This ensures Android nav bar has space and doesn't overlay content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor.copy(alpha = 0.9f))
                ) {
                    // Empty box - just reserves space for Android nav bar
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
            if (errorMessage != null) {
                // Show error message when file cannot be loaded
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorPlaceholder(
                        errorMessage = errorMessage.asString(),
                        icon = painterResource(id = R.drawable.skull_large),
                        actionTitle = "",
                        action = {}
                    )
                }
            } else if (isLoading || words.isEmpty() || selectedWordIndex < 0) {
                // Show loading indicator until text is ready AND word index is set
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
                horizontalBarsColor = horizontalBarsColor,
                horizontalBarsOpacity = horizontalBarsOpacity,
                focalPointPosition = focalPointPosition,
                wpm = localWpm,
                isPlaying = isPlaying,
                onWpmChange = localOnWpmChange,
                onPlayPause = { isPlaying = !isPlaying },
                onNavigateWord = onNavigateWord,
                navigateWord = navigateWordCallback ?: {},
                onRegisterNavigationCallback = { callback ->
                    navigateWordCallback = callback
                },
                  playbackControlsEnabled = osdEnabled,
                 focusIndicators = FocusIndicatorsType.valueOf(focusIndicators),
                 centerWord = centerWord,
                 initialWordIndex = selectedWordIndex,
                 onShowWordPicker = {
                     wordPickerRefreshKey++
                     showWordPicker = true
                 },
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
                   showBottomBar = !isPlaying
              )
            }
        }

        // Word Picker Sheet - only show when not loading
        SpeedReadingWordPickerSheet(
            show = showWordPicker && !isLoading && words.isNotEmpty(),
            words = words,
            currentWordIndex = currentWordIndex,
            totalWords = totalWords,
            refreshKey = wordPickerRefreshKey,
            onDismiss = { showWordPicker = false },
            onConfirm = { progress, wordIndexInText ->
                if (isPlaying) onPlayPause()
                selectedWordIndex = wordIndexInText
                realTimeProgress = progress
                onSaveProgress(progress, wordIndexInText)
                // Notify parent that user picked a word - use global index directly
                onWordPicked(wordIndexInText)
                // Also notify parent to ensure proper state update
                parentOnChangeProgress(progress, wordIndexInText)
            }
        )
    }
}