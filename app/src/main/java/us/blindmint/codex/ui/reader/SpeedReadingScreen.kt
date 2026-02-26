/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.util.calculateProgress
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.domain.reader.FontWithName
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import us.blindmint.codex.presentation.reader.SpeedReadingScaffold
import us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType
import us.blindmint.codex.presentation.reader.SpeedReadingSettingsBottomSheet
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.history.HistoryScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

@Parcelize
data class SpeedReadingScreen(
    val bookId: Int,
    val sessionId: Long = System.currentTimeMillis()  // Forces new ViewModel instance
) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val speedReaderModel = hiltViewModel<SpeedReaderModel>()
        val mainModel = hiltViewModel<us.blindmint.codex.ui.main.MainModel>()
        val settingsModel = hiltViewModel<us.blindmint.codex.ui.settings.SettingsModel>()

        val activity = LocalActivity.current
        val view = LocalView.current

        val mainState = mainModel.state.collectAsStateWithLifecycle()
        val settingsState = settingsModel.state.collectAsStateWithLifecycle()

        // Lock status bar appearance to prevent icon color shift when bottom sheets open
        // Use LaunchedEffect with colorPreset as key to ensure it re-runs and overrides Theme.kt
        LaunchedEffect(settingsState.value.selectedColorPreset) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Calculate luminance to determine if theme is dark
            val r = settingsState.value.selectedColorPreset.backgroundColor.red
            val g = settingsState.value.selectedColorPreset.backgroundColor.green
            val b = settingsState.value.selectedColorPreset.backgroundColor.blue
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b
            val isDarkTheme = luminance <= 0.5f
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
        }

        // Speed reading settings state
        val wpm = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(mainState.value.speedReadingWpm) }
        val speedReadingManualSentencePauseEnabled = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingManualSentencePauseEnabled) }
        val speedReadingSentencePauseDuration = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(mainState.value.speedReadingSentencePauseDuration) }
        val speedReadingOsdEnabled = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingOsdEnabled) }
        val speedReadingWordSize = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(mainState.value.speedReadingWordSize) }
        val speedReadingAccentCharacterEnabled = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingAccentCharacterEnabled) }
        val speedReadingAccentColor = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.graphics.Color(mainState.value.speedReadingAccentColor.toInt())) }
        val speedReadingAccentOpacity = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(mainState.value.speedReadingAccentOpacity) }
        val speedReadingShowVerticalIndicators = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingShowVerticalIndicators) }
        val speedReadingVerticalIndicatorsSize = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(mainState.value.speedReadingVerticalIndicatorsSize) }
        val speedReadingVerticalIndicatorType = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingVerticalIndicatorType) }
        val speedReadingShowHorizontalBars = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingShowHorizontalBars) }
        val speedReadingHorizontalBarsThickness = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(mainState.value.speedReadingHorizontalBarsThickness) }
        val speedReadingHorizontalBarsLength = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(mainState.value.speedReadingHorizontalBarsLength) }
        val speedReadingHorizontalBarsDistance = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(mainState.value.speedReadingHorizontalBarsDistance) }
        val speedReadingHorizontalBarsColor = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.graphics.Color(mainState.value.speedReadingHorizontalBarsColor.toInt())) }
        val speedReadingHorizontalBarsOpacity = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(mainState.value.speedReadingHorizontalBarsOpacity) }

        val coroutineScope = rememberCoroutineScope()
        val localFocalPointPosition = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(mainState.value.speedReadingFocalPointPosition) }
        val focalPointDebounceJob = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }

        LaunchedEffect(mainState.value.speedReadingFocalPointPosition) {
            localFocalPointPosition.floatValue = mainState.value.speedReadingFocalPointPosition
        }

        LaunchedEffect(localFocalPointPosition.floatValue) {
            focalPointDebounceJob.value?.cancel()
            focalPointDebounceJob.value = coroutineScope.launch {
                delay(300)
                mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingFocalPointPosition(localFocalPointPosition.floatValue))
            }
        }

        val localOnFocalPointPositionChange = { newValue: Float ->
            localFocalPointPosition.floatValue = newValue
        }
        val speedReadingOsdHeight = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(mainState.value.speedReadingOsdHeight) }
        val speedReadingOsdSeparation = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(mainState.value.speedReadingOsdSeparation) }
        val speedReadingAutoHideOsd = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingAutoHideOsd) }
        val speedReadingCenterWord = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingCenterWord) }
        val speedReadingFocusIndicators = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingFocusIndicators) }
        val speedReadingCustomFontEnabled = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingCustomFontEnabled) }
        val speedReadingSelectedFontFamily = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mainState.value.speedReadingSelectedFontFamily) }

        // Settings visibility state
        val speedReadingSettingsVisible = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
  
        // Handle cleanup when exiting speed reader
        val onExitWithSystemBars = remember {
            {
                // Navigate without manually showing system bars
                // SpeedReadingScaffold will show them when isPlaying becomes false
                navigator.pop()
            }
        }
  
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose {
                speedReaderModel.onLeave()
                LibraryScreen.refreshListChannel.trySend(0)
                HistoryScreen.refreshListChannel.trySend(0)
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingManualSentencePauseEnabled) {
            speedReadingManualSentencePauseEnabled.value = mainState.value.speedReadingManualSentencePauseEnabled
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingSentencePauseDuration) {
            speedReadingSentencePauseDuration.intValue = mainState.value.speedReadingSentencePauseDuration
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingOsdEnabled) {
            speedReadingOsdEnabled.value = mainState.value.speedReadingOsdEnabled
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingWordSize) {
            speedReadingWordSize.intValue = mainState.value.speedReadingWordSize
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingAccentCharacterEnabled) {
            speedReadingAccentCharacterEnabled.value = mainState.value.speedReadingAccentCharacterEnabled
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingAccentColor) {
            speedReadingAccentColor.value = androidx.compose.ui.graphics.Color(mainState.value.speedReadingAccentColor.toInt())
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingAccentOpacity) {
            speedReadingAccentOpacity.floatValue = mainState.value.speedReadingAccentOpacity
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingShowVerticalIndicators) {
            speedReadingShowVerticalIndicators.value = mainState.value.speedReadingShowVerticalIndicators
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingVerticalIndicatorsSize) {
            speedReadingVerticalIndicatorsSize.intValue = mainState.value.speedReadingVerticalIndicatorsSize
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingVerticalIndicatorType) {
            speedReadingVerticalIndicatorType.value = mainState.value.speedReadingVerticalIndicatorType
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingShowHorizontalBars) {
            speedReadingShowHorizontalBars.value = mainState.value.speedReadingShowHorizontalBars
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingHorizontalBarsThickness) {
            speedReadingHorizontalBarsThickness.intValue = mainState.value.speedReadingHorizontalBarsThickness
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingHorizontalBarsLength) {
            speedReadingHorizontalBarsLength.floatValue = mainState.value.speedReadingHorizontalBarsLength
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingHorizontalBarsDistance) {
            speedReadingHorizontalBarsDistance.intValue = mainState.value.speedReadingHorizontalBarsDistance
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingHorizontalBarsColor) {
            val colorValue = mainState.value.speedReadingHorizontalBarsColor.toInt()
            speedReadingHorizontalBarsColor.value = if (colorValue == 0) androidx.compose.ui.graphics.Color(0xFF424242) else androidx.compose.ui.graphics.Color(colorValue)
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingHorizontalBarsOpacity) {
            speedReadingHorizontalBarsOpacity.floatValue = mainState.value.speedReadingHorizontalBarsOpacity
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingOsdHeight) {
            speedReadingOsdHeight.floatValue = mainState.value.speedReadingOsdHeight
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingOsdSeparation) {
            speedReadingOsdSeparation.floatValue = mainState.value.speedReadingOsdSeparation
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingAutoHideOsd) {
            speedReadingAutoHideOsd.value = mainState.value.speedReadingAutoHideOsd
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingFocusIndicators) {
            speedReadingFocusIndicators.value = mainState.value.speedReadingFocusIndicators
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingCenterWord) {
            speedReadingCenterWord.value = mainState.value.speedReadingCenterWord
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingCustomFontEnabled) {
            speedReadingCustomFontEnabled.value = mainState.value.speedReadingCustomFontEnabled
        }
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingSelectedFontFamily) {
            speedReadingSelectedFontFamily.value = mainState.value.speedReadingSelectedFontFamily
        }

        // Persist settings changes
        androidx.compose.runtime.LaunchedEffect(wpm.intValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingWpm(wpm.intValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingManualSentencePauseEnabled.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingManualSentencePauseEnabled(speedReadingManualSentencePauseEnabled.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingSentencePauseDuration.intValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingSentencePauseDuration(speedReadingSentencePauseDuration.intValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingOsdEnabled.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingOsdEnabled(speedReadingOsdEnabled.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingWordSize.intValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingWordSize(speedReadingWordSize.intValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingAccentCharacterEnabled.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingAccentCharacterEnabled(speedReadingAccentCharacterEnabled.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingAccentColor.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingAccentColor(speedReadingAccentColor.value.value.toLong()))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingAccentOpacity.floatValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingAccentOpacity(speedReadingAccentOpacity.floatValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingShowVerticalIndicators.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingShowVerticalIndicators(speedReadingShowVerticalIndicators.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingVerticalIndicatorsSize.intValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingVerticalIndicatorsSize(speedReadingVerticalIndicatorsSize.intValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingVerticalIndicatorType.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingVerticalIndicatorType(speedReadingVerticalIndicatorType.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingShowHorizontalBars.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingShowHorizontalBars(speedReadingShowHorizontalBars.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingHorizontalBarsThickness.intValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingHorizontalBarsThickness(speedReadingHorizontalBarsThickness.intValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingHorizontalBarsLength.floatValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingHorizontalBarsLength(speedReadingHorizontalBarsLength.floatValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingHorizontalBarsDistance.intValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingHorizontalBarsDistance(speedReadingHorizontalBarsDistance.intValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingHorizontalBarsColor.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingHorizontalBarsColor(speedReadingHorizontalBarsColor.value.value.toLong()))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingHorizontalBarsOpacity.floatValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingHorizontalBarsOpacity(speedReadingHorizontalBarsOpacity.floatValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingOsdHeight.floatValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingOsdHeight(speedReadingOsdHeight.floatValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingOsdSeparation.floatValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingOsdSeparation(speedReadingOsdSeparation.floatValue))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingAutoHideOsd.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingAutoHideOsd(speedReadingAutoHideOsd.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingCenterWord.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingCenterWord(speedReadingCenterWord.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingFocusIndicators.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingFocusIndicators(speedReadingFocusIndicators.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingCustomFontEnabled.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingCustomFontEnabled(speedReadingCustomFontEnabled.value))
        }
        androidx.compose.runtime.LaunchedEffect(speedReadingSelectedFontFamily.value) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingSelectedFontFamily(speedReadingSelectedFontFamily.value))
        }

        // Load the book for speed reading
        androidx.compose.runtime.LaunchedEffect(bookId) {
            speedReaderModel.loadBook(bookId, activity) {
                navigator.pop()
            }
        }

        // Calculate progress for display
        val book = speedReaderModel.book.value
        val words = speedReaderModel.words.value
        val totalWords = speedReaderModel.totalWords.intValue
        val isLoading = speedReaderModel.isLoading.value
        val isReadyForDisplay = speedReaderModel.isReadyForDisplay.value
        val errorMessage = speedReaderModel.errorMessage.value

        // Use empty book as fallback when book hasn't loaded yet
        val displayBook = book ?: us.blindmint.codex.presentation.core.constants.provideEmptyBook()

        val bottomBarPadding = androidx.compose.runtime.remember(mainState.value.bottomBarPadding) {
            (mainState.value.bottomBarPadding * 4f).dp
        }

        // Animate color changes for smooth transitions, matching ReaderScreen behavior
        val backgroundColor = animateColorAsState(
            targetValue = settingsState.value.selectedColorPreset.backgroundColor
        )
        val fontColor = animateColorAsState(
            targetValue = settingsState.value.selectedColorPreset.fontColor
        )

        val bookProgress = androidx.compose.runtime.remember(
            speedReaderModel.currentWordIndex.intValue, totalWords
        ) {
            val progress = if (totalWords > 0) speedReaderModel.currentWordIndex.intValue.toFloat() / totalWords else 0f
            "${progress.calculateProgress(2)}%"
        }

        // Screen-size-aware focal point position defaults
        val configuration = LocalConfiguration.current
        val isTablet = configuration.smallestScreenWidthDp >= 600
        val screenAwareFocalPointPosition = remember(localFocalPointPosition.floatValue, isTablet) {
            if (localFocalPointPosition.floatValue == 0.38f) {
                if (isTablet) 0.45f else 0.38f
            } else {
                localFocalPointPosition.floatValue
            }
        }

        val speedReaderFontFamily = remember(
            speedReadingCustomFontEnabled.value,
            speedReadingSelectedFontFamily.value,
            mainState.value.customFonts
        ) {
            if (speedReadingCustomFontEnabled.value && speedReadingSelectedFontFamily.value.startsWith("custom_")) {
                val customFontName = speedReadingSelectedFontFamily.value.removePrefix("custom_")
                val customFont = mainState.value.customFonts.find { it.name == customFontName }
                customFont?.let {
                    try {
                        FontWithName(
                            id = "speed_reader_custom_${it.name}",
                            fontName = us.blindmint.codex.domain.ui.UIText.StringValue(it.name),
                            font = FontFamily(Font(java.io.File(it.filePath)))
                        )
                    } catch (_: Exception) {
                        us.blindmint.codex.presentation.core.constants.provideFonts().first()
                    }
                } ?: us.blindmint.codex.presentation.core.constants.provideFonts().first()
            } else {
                us.blindmint.codex.presentation.core.constants.provideFonts().run {
                    find {
                        it.id == speedReadingSelectedFontFamily.value
                    } ?: get(0)
                }
            }
        }

        SpeedReadingScaffold(
            words = words,
            book = displayBook,
            bookTitle = displayBook.title,
            chapterTitle = null, // Speed reader doesn't track chapters
            currentWordIndex = speedReaderModel.currentWordIndex.intValue,
            totalWords = totalWords,
            initialWordIndex = speedReaderModel.initialWordIndex.intValue,
            backgroundColor = backgroundColor.value,
            fontColor = fontColor.value,
            isLoading = !isReadyForDisplay,
            accentCharacterEnabled = speedReadingAccentCharacterEnabled.value,
            accentColor = speedReadingAccentColor.value,
            fontFamily = speedReaderFontFamily.font,
            sentencePauseMs = if (speedReadingManualSentencePauseEnabled.value) {
                speedReadingSentencePauseDuration.intValue
            } else {
                // Automatic pause calculation based on WPM
                val baseWpm = 300f
                val basePause = 350f
                val minPause = 50f
                (basePause * (baseWpm / wpm.intValue) + minPause).toInt().coerceIn(50, 1000)
            },
            wordSize = speedReadingWordSize.intValue,
            accentOpacity = speedReadingAccentOpacity.floatValue,
            horizontalBarsColor = speedReadingHorizontalBarsColor.value,
            horizontalBarsOpacity = speedReadingHorizontalBarsOpacity.floatValue,
             focalPointPosition = screenAwareFocalPointPosition,
             osdHeight = speedReadingOsdHeight.floatValue,
             osdSeparation = speedReadingOsdSeparation.floatValue,
             errorMessage = errorMessage,
             autoHideOsd = speedReadingAutoHideOsd.value,
             centerWord = speedReadingCenterWord.value,
             focusIndicators = speedReadingFocusIndicators.value,
             progress = bookProgress,
            bottomBarPadding = bottomBarPadding,
            showWpmIndicator = true,
            wpm = wpm.intValue,
            onWpmChange = { wpm.intValue = it },
            osdEnabled = speedReadingOsdEnabled.value,
            onExitSpeedReading = onExitWithSystemBars,
            onShowSpeedReadingSettings = {
                speedReadingSettingsVisible.value = true
            },
            onNavigateWord = { direction ->
                // Speed reading handles word navigation internally
            },
            onWordPicked = { newWordIndex ->
                speedReaderModel.updateProgress(0f, newWordIndex)
            },
            onChangeProgress = { progress, wordIndex ->
                speedReaderModel.updateProgress(progress, wordIndex)
            },
             onSaveProgress = { progress, wordIndex ->
                  speedReaderModel.updateProgress(progress, wordIndex)
             },
             onPlayPause = {},
             isReadyForDisplay = isReadyForDisplay
          )

        // Speed reading settings bottom sheet
        SpeedReadingSettingsBottomSheet(
            show = speedReadingSettingsVisible.value,
            onDismiss = {
                speedReadingSettingsVisible.value = false
            },
            wpm = wpm.intValue,
            onWpmChange = { wpm.intValue = it },
            manualSentencePauseEnabled = speedReadingManualSentencePauseEnabled.value,
            onManualSentencePauseEnabledChange = { speedReadingManualSentencePauseEnabled.value = it },
            sentencePauseDuration = speedReadingSentencePauseDuration.intValue,
            onSentencePauseDurationChange = { speedReadingSentencePauseDuration.intValue = it },
            osdEnabled = speedReadingOsdEnabled.value,
            onOsdEnabledChange = { speedReadingOsdEnabled.value = it },
            wordSize = speedReadingWordSize.intValue,
            onWordSizeChange = { speedReadingWordSize.intValue = it },
            accentCharacterEnabled = speedReadingAccentCharacterEnabled.value,
            onAccentCharacterEnabledChange = { speedReadingAccentCharacterEnabled.value = it },
            accentColor = speedReadingAccentColor.value,
            onAccentColorChange = { speedReadingAccentColor.value = it },
            accentOpacity = speedReadingAccentOpacity.floatValue,
            onAccentOpacityChange = { speedReadingAccentOpacity.floatValue = it },
            showVerticalIndicators = speedReadingShowVerticalIndicators.value,
            onShowVerticalIndicatorsChange = { speedReadingShowVerticalIndicators.value = it },
            verticalIndicatorsSize = speedReadingVerticalIndicatorsSize.intValue,
            onVerticalIndicatorsSizeChange = { speedReadingVerticalIndicatorsSize.intValue = it },
            showHorizontalBars = speedReadingShowHorizontalBars.value,
            onShowHorizontalBarsChange = { speedReadingShowHorizontalBars.value = it },
            horizontalBarsThickness = speedReadingHorizontalBarsThickness.intValue,
            onHorizontalBarsThicknessChange = { speedReadingHorizontalBarsThickness.intValue = it },
            horizontalBarsLength = speedReadingHorizontalBarsLength.floatValue,
            onHorizontalBarsLengthChange = { speedReadingHorizontalBarsLength.floatValue = it },
            horizontalBarsDistance = speedReadingHorizontalBarsDistance.intValue,
            onHorizontalBarsDistanceChange = { speedReadingHorizontalBarsDistance.intValue = it },
            horizontalBarsColor = speedReadingHorizontalBarsColor.value,
            onHorizontalBarsColorChange = { speedReadingHorizontalBarsColor.value = it },
            horizontalBarsOpacity = speedReadingHorizontalBarsOpacity.floatValue,
            onHorizontalBarsOpacityChange = { speedReadingHorizontalBarsOpacity.floatValue = it },
            focalPointPosition = screenAwareFocalPointPosition,
            onFocalPointPositionChange = localOnFocalPointPositionChange,
            osdHeight = speedReadingOsdHeight.floatValue,
            onOsdHeightChange = { speedReadingOsdHeight.floatValue = it },
            osdSeparation = speedReadingOsdSeparation.floatValue,
            onOsdSeparationChange = { speedReadingOsdSeparation.floatValue = it },
            autoHideOsd = speedReadingAutoHideOsd.value,
            onAutoHideOsdChange = { speedReadingAutoHideOsd.value = it },
            centerWord = speedReadingCenterWord.value,
            onCenterWordChange = { speedReadingCenterWord.value = it },
            focusIndicators = speedReadingFocusIndicators.value,
            onFocusIndicatorsChange = { speedReadingFocusIndicators.value = it },
            verticalIndicatorType = SpeedReadingVerticalIndicatorType.valueOf(speedReadingVerticalIndicatorType.value),
            onVerticalIndicatorTypeChange = { speedReadingVerticalIndicatorType.value = it.name },
            customFontEnabled = speedReadingCustomFontEnabled.value,
            onCustomFontEnabledChange = { speedReadingCustomFontEnabled.value = it },
            selectedFontFamily = speedReadingSelectedFontFamily.value,
            onFontFamilyChange = { speedReadingSelectedFontFamily.value = it }
        )
    }
}