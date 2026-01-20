/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.navigator.Screen
import androidx.compose.ui.unit.dp
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.util.calculateProgress
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.reader.SpeedReadingScaffold
import us.blindmint.codex.ui.library.LibraryScreen

@Parcelize
data class SpeedReadingScreen(val bookId: Int) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val speedReaderModel = hiltViewModel<SpeedReaderModel>()
        val mainModel = hiltViewModel<us.blindmint.codex.ui.main.MainModel>()
        val settingsModel = hiltViewModel<us.blindmint.codex.ui.settings.SettingsModel>()

        val activity = LocalActivity.current

        val mainState = mainModel.state.collectAsStateWithLifecycle()
        val settingsState = settingsModel.state.collectAsStateWithLifecycle()

        val wpm = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(mainState.value.speedReadingWpm) }

        // Update WPM when mainState changes
        androidx.compose.runtime.LaunchedEffect(mainState.value.speedReadingWpm) {
            wpm.intValue = mainState.value.speedReadingWpm
        }

        // Persist WPM changes
        androidx.compose.runtime.LaunchedEffect(wpm.intValue) {
            mainModel.onEvent(us.blindmint.codex.ui.main.MainEvent.OnChangeSpeedReadingWpm(wpm.intValue))
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            speedReaderModel.loadBook(bookId, activity) {
                navigator.pop()
            }
        }

        // Calculate progress for display
        val book = speedReaderModel.book.value
        val text = speedReaderModel.text.value
        val isLoading = speedReaderModel.isLoading.value

        if (book == null) return

        val bookProgress = androidx.compose.runtime.remember(
            book.progress
        ) {
            "${book.progress.calculateProgress(2)}%"
        }

        val bottomBarPadding = androidx.compose.runtime.remember(mainState.value.bottomBarPadding) {
            (mainState.value.bottomBarPadding * 4f).dp
        }

        SpeedReadingScaffold(
            text = text,
            book = book,
            bookTitle = book.title,
            chapterTitle = null, // Speed reader doesn't track chapters
            currentProgress = book.progress,
            totalProgress = book.progress,
            backgroundColor = settingsState.value.selectedColorPreset.backgroundColor,
            fontColor = settingsState.value.selectedColorPreset.fontColor,
            isLoading = isLoading,
            accentCharacterEnabled = mainState.value.speedReadingAccentCharacterEnabled,
            accentColor = androidx.compose.ui.graphics.Color(mainState.value.speedReadingAccentColor.toInt()),
            fontFamily = us.blindmint.codex.presentation.core.constants.provideFonts().first().font, // Use default font for now
            sentencePauseMs = if (mainState.value.speedReadingManualSentencePauseEnabled) {
                mainState.value.speedReadingSentencePauseDuration
            } else {
                // Automatic pause calculation based on WPM
                val baseWpm = 300f
                val basePause = 350f
                val minPause = 50f
                (basePause * (baseWpm / mainState.value.speedReadingWpm) + minPause).toInt().coerceIn(50, 1000)
            },
            wordSize = mainState.value.speedReadingWordSize,
            accentOpacity = mainState.value.speedReadingAccentOpacity,
            showVerticalIndicators = mainState.value.speedReadingShowVerticalIndicators,
            verticalIndicatorsSize = mainState.value.speedReadingVerticalIndicatorsSize,
            verticalIndicatorType = SpeedReadingVerticalIndicatorType.valueOf(mainState.value.speedReadingVerticalIndicatorType),
            showHorizontalBars = mainState.value.speedReadingShowHorizontalBars,
            horizontalBarsThickness = mainState.value.speedReadingHorizontalBarsThickness,
            horizontalBarsLength = mainState.value.speedReadingHorizontalBarsLength,
            horizontalBarsDistance = mainState.value.speedReadingHorizontalBarsDistance,
            horizontalBarsColor = androidx.compose.ui.graphics.Color(mainState.value.speedReadingHorizontalBarsColor.toInt()),
            horizontalBarsOpacity = mainState.value.speedReadingHorizontalBarsOpacity,
            focalPointPosition = mainState.value.speedReadingFocalPointPosition,
            osdHeight = mainState.value.speedReadingOsdHeight,
            osdSeparation = mainState.value.speedReadingOsdSeparation,
            centerWord = mainState.value.speedReadingCenterWord,
            progress = bookProgress,
            bottomBarPadding = bottomBarPadding,
            showWpmIndicator = true,
            wpm = wpm.intValue,
            onWpmChange = { wpm.intValue = it },
            osdEnabled = mainState.value.speedReadingOsdEnabled,
            onChangeProgress = { progress ->
                // Speed reading doesn't save progress in the same way
            },
            onExitSpeedReading = {
                navigator.push(
                    LibraryScreen,
                    popping = true,
                    saveInBackStack = false
                )
            },
            onNavigateWord = { direction ->
                // Speed reading handles word navigation internally
            },
            onShowSpeedReadingSettings = {
                // Settings not implemented for independent speed reader
            },
            onMenuVisibilityChanged = { showMenu ->
                // Handle menu visibility for speed reading screen
            }
        )
    }
}