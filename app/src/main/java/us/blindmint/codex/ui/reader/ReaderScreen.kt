/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import android.content.pm.ActivityInfo
import android.os.Parcelable
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.domain.reader.CustomFont
import us.blindmint.codex.domain.reader.FontWithName
import us.blindmint.codex.domain.reader.ReaderColorEffects
import us.blindmint.codex.domain.reader.ReaderProgressCount
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.presentation.core.constants.CHARACTERS_PER_PAGE
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.util.calculateProgress
import us.blindmint.codex.presentation.core.util.setBrightness
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.reader.ReaderContent
import us.blindmint.codex.presentation.reader.SpeedReadingScaffold
import us.blindmint.codex.presentation.reader.SpeedReadingSettingsBottomSheet
import us.blindmint.codex.ui.book_info.BookInfoScreen
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.settings.SettingsModel
import kotlin.math.roundToInt

@Parcelize
data class ReaderScreen(val bookId: Int, val startInSpeedReading: Boolean = false) : Screen, Parcelable {

    companion object {
        const val CHAPTERS_DRAWER = "chapters_drawer"
        const val BOOKMARKS_DRAWER = "bookmarks_drawer"
        const val SETTINGS_BOTTOM_SHEET = "settings_bottom_sheet"
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = hiltViewModel<ReaderModel>()
        val mainModel = hiltViewModel<MainModel>()
        val settingsModel = hiltViewModel<SettingsModel>()

        val state = screenModel.state.collectAsStateWithLifecycle()
        val mainState = mainModel.state.collectAsStateWithLifecycle()
        val settingsState = settingsModel.state.collectAsStateWithLifecycle()

        val activity = LocalActivity.current
        val density = LocalDensity.current
        val listState = rememberSaveable(
            state.value.listState,
            saver = LazyListState.Saver
        ) {
            state.value.listState
        }
        val nestedScrollConnection = remember {
            derivedStateOf {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        consumed.y.let { velocity ->
                            if (velocity in -70f..70f) return@let
                            if (!state.value.showMenu) return@let
                            if (state.value.lockMenu) return@let
                            if (!mainState.value.hideBarsOnFastScroll) return@let

                            screenModel.onEvent(
                                ReaderEvent.OnMenuVisibility(
                                    show = false,
                                    fullscreenMode = mainState.value.fullscreen,
                                    saveCheckpoint = false,
                                    activity = activity
                                )
                            )
                        }
                        return super.onPostScroll(consumed, available, source)
                    }
                }
            }
        }

        val fontFamily = remember(mainState.value.fontFamily, mainState.value.customFonts) {
            if (mainState.value.fontFamily.startsWith("custom_")) {
                val customFontName = mainState.value.fontFamily.removePrefix("custom_")
                val customFont = mainState.value.customFonts.find { it.name == customFontName }
                customFont?.let {
                    try {
                        FontWithName(
                            id = "custom_${it.name}",
                            fontName = UIText.StringValue(it.name),
                            font = FontFamily(Font(java.io.File(it.filePath)))
                        )
                    } catch (e: Exception) {
                        // Fallback to default if font loading fails
                        provideFonts().first()
                    }
                } ?: provideFonts().first()
            } else {
                provideFonts().run {
                    find {
                        it.id == mainState.value.fontFamily
                    } ?: get(0)
                }
            }
        }
        val backgroundColor = animateColorAsState(
            targetValue = settingsState.value.selectedColorPreset.backgroundColor
        )
        val fontColor = animateColorAsState(
            targetValue = settingsState.value.selectedColorPreset.fontColor
        )

        // Speed reading state
    val speedReadingWpm = remember { mutableStateOf(300) }
    val speedReadingManualSentencePauseEnabled = remember { mutableStateOf(false) }
    val speedReadingSentencePauseDuration = remember { mutableStateOf(350) }
    val speedReadingOdsEnabled = remember { mutableStateOf(false) }

    val speedReadingWordSize = remember { mutableStateOf(48) }
    val speedReadingAccentCharacterEnabled = remember { mutableStateOf(true) }
    val speedReadingAccentColor = remember { mutableStateOf(Color.Red) }
    val speedReadingAccentOpacity = remember { mutableStateOf(1.0f) }
    val speedReadingShowVerticalIndicators = remember { mutableStateOf(true) }
    val speedReadingVerticalIndicatorsSize = remember { mutableStateOf(32) }
    val speedReadingShowHorizontalBars = remember { mutableStateOf(true) }
    val speedReadingHorizontalBarsThickness = remember { mutableStateOf(2) }
    val speedReadingHorizontalBarsDistance = remember { mutableStateOf(8) }
    val speedReadingHorizontalBarsColor = remember { mutableStateOf(Color.Gray) }
    val speedReadingHorizontalBarsOpacity = remember { mutableStateOf(1.0f) }
    val speedReadingFocalPointPosition = remember { mutableStateOf(0.38f) }
    val speedReadingCustomFontEnabled = remember { mutableStateOf(false) }
    val speedReadingSelectedFontFamily = remember { mutableStateOf("default") }

    // Resolve speed reading font based on custom font setting
    val speedReadingFontFamily = remember(
        speedReadingCustomFontEnabled.value,
        speedReadingSelectedFontFamily.value,
        mainState.value.customFonts,
        fontFamily
    ) {
        if (speedReadingCustomFontEnabled.value) {
            val selectedId = speedReadingSelectedFontFamily.value
            if (selectedId.startsWith("custom_")) {
                val customFontName = selectedId.removePrefix("custom_")
                val customFont = mainState.value.customFonts.find { it.name == customFontName }
                customFont?.let {
                    try {
                        FontFamily(Font(java.io.File(it.filePath)))
                    } catch (e: Exception) {
                        fontFamily.font
                    }
                } ?: fontFamily.font
            } else {
                provideFonts().find { it.id == selectedId }?.font ?: fontFamily.font
            }
        } else {
            fontFamily.font
        }
    }
        val lineHeight = remember(
            mainState.value.fontSize,
            mainState.value.lineHeight
        ) {
            (mainState.value.fontSize + mainState.value.lineHeight).sp
        }
        val letterSpacing = remember(mainState.value.letterSpacing) {
            (mainState.value.letterSpacing / 100f).em
        }
        val sidePadding = remember(mainState.value.sidePadding) {
            (mainState.value.sidePadding * 3).dp
        }
        val verticalPadding = remember(mainState.value.verticalPadding) {
            (mainState.value.verticalPadding * 4.5f).dp
        }
        val paragraphHeight = remember(
            mainState.value.paragraphHeight,
            mainState.value.lineHeight
        ) {
            ((mainState.value.paragraphHeight * 3).dp).coerceAtLeast(
                with(density) { mainState.value.lineHeight.sp.toDp().value * 0.5f }.dp
            )
        }
        val fontStyle = remember(mainState.value.isItalic) {
            when (mainState.value.isItalic) {
                true -> FontStyle.Italic
                false -> FontStyle.Normal
            }
        }
        val paragraphIndentation = remember(
            mainState.value.paragraphIndentation,
            mainState.value.textAlignment
        ) {
            if (
                mainState.value.textAlignment == ReaderTextAlignment.CENTER ||
                mainState.value.textAlignment == ReaderTextAlignment.END
            ) {
                return@remember 0.sp
            }
            (mainState.value.paragraphIndentation * 6).sp
        }
        val perceptionExpanderPadding = remember(
            sidePadding,
            mainState.value.perceptionExpanderPadding
        ) {
            sidePadding + (mainState.value.perceptionExpanderPadding * 8).dp
        }
        val perceptionExpanderThickness = remember(
            mainState.value.perceptionExpanderThickness
        ) {
            (mainState.value.perceptionExpanderThickness * 0.25f).dp
        }
        val horizontalGestureSensitivity = remember(mainState.value.horizontalGestureSensitivity) {
            (36f + mainState.value.horizontalGestureSensitivity * (4f - 36f)).dp
        }
        val highlightedReadingThickness = remember(mainState.value.highlightedReadingThickness) {
            when (mainState.value.highlightedReadingThickness) {
                2 -> FontWeight.SemiBold
                3 -> FontWeight.Bold
                else -> FontWeight.Medium
            }
        }
        val horizontalAlignment = remember(mainState.value.textAlignment) {
            when (mainState.value.textAlignment) {
                ReaderTextAlignment.START, ReaderTextAlignment.JUSTIFY, ReaderTextAlignment.ORIGINAL -> Alignment.Start
                ReaderTextAlignment.CENTER -> Alignment.CenterHorizontally
                ReaderTextAlignment.END -> Alignment.End
            }
        }
        val imagesWidth = remember(mainState.value.imagesWidth) {
            mainState.value.imagesWidth.coerceAtLeast(0.01f)
        }
        val imagesCornersRoundness = remember(
            mainState.value.imagesCornersRoundness,
            mainState.value.imagesWidth
        ) {
            (mainState.value.imagesCornersRoundness * 3 * imagesWidth).dp
        }
        val imagesColorEffects = remember(
            mainState.value.imagesColorEffects,
            fontColor.value,
            backgroundColor.value
        ) {
            when (mainState.value.imagesColorEffects) {
                ReaderColorEffects.OFF -> null

                ReaderColorEffects.GRAYSCALE -> ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(0f) }
                )

                ReaderColorEffects.FONT -> ColorFilter.tint(
                    color = fontColor.value,
                    blendMode = BlendMode.Color
                )

                ReaderColorEffects.BACKGROUND -> ColorFilter.tint(
                    color = backgroundColor.value,
                    blendMode = BlendMode.Color
                )
            }
        }
        val progressBarPadding = remember(mainState.value.progressBarPadding) {
            (mainState.value.progressBarPadding * 3).dp
        }
        val progressBarFontSize = remember(mainState.value.progressBarFontSize) {
            (mainState.value.progressBarFontSize * 2).sp
        }
        val searchHighlightColor = remember(mainState.value.searchHighlightColor) {
            Color(mainState.value.searchHighlightColor)
        }

        val layoutDirection = LocalLayoutDirection.current
        val cutoutInsets = WindowInsets.displayCutout
        val systemBarsInsets = WindowInsets.systemBarsIgnoringVisibility

        val cutoutInsetsPadding = remember(mainState.value.cutoutPadding) {
            derivedStateOf {
                cutoutInsets.asPaddingValues(density = density).run {
                    if (mainState.value.cutoutPadding) PaddingValues(
                        top = calculateTopPadding(),
                        start = calculateStartPadding(layoutDirection),
                        end = calculateEndPadding(layoutDirection),
                        bottom = calculateBottomPadding()
                    ) else PaddingValues(0.dp)
                }
            }
        }
        val systemBarsInsetsPadding = remember(mainState.value.fullscreen) {
            derivedStateOf {
                systemBarsInsets.asPaddingValues(density = density).run {
                    if (!mainState.value.fullscreen) PaddingValues(
                        top = calculateTopPadding(),
                        start = calculateStartPadding(layoutDirection),
                        end = calculateEndPadding(layoutDirection),
                        bottom = calculateBottomPadding()
                    ) else PaddingValues(0.dp)
                }
            }
        }
        val contentPadding = remember(
            cutoutInsetsPadding.value,
            systemBarsInsetsPadding.value
        ) {
            PaddingValues(
                top = systemBarsInsetsPadding.value.calculateTopPadding().run {
                    if (equals(0.dp)) return@run cutoutInsetsPadding.value
                        .calculateTopPadding()
                    this
                },
                start = systemBarsInsetsPadding.value.calculateStartPadding(layoutDirection).run {
                    if (equals(0.dp)) return@run cutoutInsetsPadding.value
                        .calculateStartPadding(layoutDirection)
                    this
                },
                end = systemBarsInsetsPadding.value.calculateEndPadding(layoutDirection).run {
                    if (equals(0.dp)) return@run cutoutInsetsPadding.value
                        .calculateEndPadding(layoutDirection)
                    this
                },
                bottom = systemBarsInsetsPadding.value.calculateBottomPadding().run {
                    if (equals(0.dp)) return@run cutoutInsetsPadding.value
                        .calculateBottomPadding()
                    this
                }
            )
        }
        val bottomBarPadding = remember(mainState.value.bottomBarPadding) {
            (mainState.value.bottomBarPadding * 4f).dp
        }

        val bookProgress = remember(
            state.value.book.progress,
            state.value.book.isComic,
            state.value.text,
            mainState.value.progressCount
        ) {
            if (state.value.book.isComic) {
                // For comics, show page-based progress
                when (mainState.value.progressCount) {
                    ReaderProgressCount.PERCENTAGE -> {
                        "${state.value.book.progress.calculateProgress(2)}%"
                    }
                    ReaderProgressCount.QUANTITY -> {
                        // For comics, we don't have item count, so show page estimate
                        val estimatedPage = (state.value.book.progress * 100).roundToInt() + 1
                        "Page $estimatedPage"
                    }
                    ReaderProgressCount.PAGE -> {
                        // For comics, page count is the same as quantity
                        val estimatedPage = (state.value.book.progress * 100).roundToInt() + 1
                        "Page $estimatedPage"
                    }
                }
            } else {
                when (mainState.value.progressCount) {
                    ReaderProgressCount.PERCENTAGE -> {
                        "${state.value.book.progress.calculateProgress(2)}%"
                    }

                    ReaderProgressCount.QUANTITY -> {
                        val index =
                            (state.value.book.progress * state.value.text.lastIndex + 1).roundToInt()
                        "$index / ${state.value.text.size}"
                    }

                    ReaderProgressCount.PAGE -> {
                        val totalChars = state.value.text.sumOf { text ->
                            when (text) {
                                is ReaderText.Text -> text.line.text.length.toInt()
                                else -> 0
                            }
                        }
                        val currentIndex = (state.value.book.progress * state.value.text.lastIndex).roundToInt()
                        val currentChars = state.value.text.take(currentIndex + 1).sumOf { text ->
                            when (text) {
                                is ReaderText.Text -> text.line.text.length.toInt()
                                else -> 0
                            }
                        }
                        val currentPage = (currentChars / CHARACTERS_PER_PAGE) + 1
                        val totalPages = (totalChars / CHARACTERS_PER_PAGE) + 1
                        "$currentPage / $totalPages"
                    }
                }
            }
        }
        val chapterProgress = remember(
            state.value.book.isComic,
            state.value.text,
            state.value.book.progress,
            state.value.currentChapter,
            state.value.currentChapterProgress,
            mainState.value.progressCount
        ) {
            if (state.value.book.isComic) return@remember "" // Comics don't have chapters
            if (state.value.currentChapter == null) return@remember ""
            when (mainState.value.progressCount) {
                ReaderProgressCount.PERCENTAGE -> {
                    " (${state.value.currentChapterProgress.calculateProgress(2)}%)"
                }

                ReaderProgressCount.QUANTITY -> {
                    val (index, length) = screenModel.findChapterIndexAndLength(
                        (state.value.book.progress * state.value.text.lastIndex).roundToInt()
                    ).apply { if (first == -1 && second == -1) return@remember "" }
                    " (${index} / ${length})"
                }

                ReaderProgressCount.PAGE -> {
                    // For chapter progress in PAGE mode, perhaps show chapter page progress
                    // But for simplicity, maybe just return empty or same as book
                    ""
                }
            }
        }
        val progress = remember(bookProgress, chapterProgress) {
            "${bookProgress}${chapterProgress}"
        }

        LaunchedEffect(Unit) {
            screenModel.init(
                bookId = bookId,
                fullscreenMode = mainState.value.fullscreen,
                activity = activity,
                navigateBack = {
                    navigator.pop()
                }
            )
        }

        // Enable speed reading mode if requested
        LaunchedEffect(state.value.isLoading, startInSpeedReading) {
            if (!state.value.isLoading && startInSpeedReading && !state.value.speedReadingMode) {
                screenModel.onEvent(ReaderEvent.OnShowSpeedReading)
                // Ensure menu is visible when entering speed reading from book preview
                screenModel.onEvent(
                    ReaderEvent.OnMenuVisibility(
                        show = true,
                        fullscreenMode = mainState.value.fullscreen,
                        saveCheckpoint = false,
                        activity = activity
                    )
                )
            }
        }
        LaunchedEffect(mainState.value.fullscreen) {
            screenModel.onEvent(
                ReaderEvent.OnMenuVisibility(
                    show = state.value.showMenu,
                    fullscreenMode = mainState.value.fullscreen,
                    saveCheckpoint = false,
                    activity = activity
                )
            )
        }
        LaunchedEffect(listState) {
            screenModel.updateProgress(listState)
        }

        DisposableEffect(mainState.value.screenOrientation) {
            activity.requestedOrientation = mainState.value.screenOrientation.code
            onDispose {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        DisposableEffect(
            mainState.value.screenBrightness,
            mainState.value.customScreenBrightness
        ) {
            when (mainState.value.customScreenBrightness) {
                true -> activity.setBrightness(brightness = mainState.value.screenBrightness)
                false -> activity.setBrightness(brightness = null)
            }

            onDispose {
                activity.setBrightness(brightness = null)
            }
        }
        DisposableEffect(mainState.value.keepScreenOn) {
            when (mainState.value.keepScreenOn) {
                true -> activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                false -> activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                screenModel.resetScreen()
                WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView
                ).show(WindowInsetsCompat.Type.systemBars())
            }
        }

        ReaderContent(
            book = state.value.book,
            text = state.value.text,
            bottomSheet = state.value.bottomSheet,
            drawer = state.value.drawer,
            bookmarks = state.value.bookmarks,
            listState = listState,
            currentChapter = state.value.currentChapter,
            nestedScrollConnection = nestedScrollConnection.value,
            fastColorPresetChange = mainState.value.fastColorPresetChange,
            perceptionExpander = mainState.value.perceptionExpander,
            perceptionExpanderPadding = perceptionExpanderPadding,
            perceptionExpanderThickness = perceptionExpanderThickness,
            currentChapterProgress = state.value.currentChapterProgress,
            isLoading = state.value.isLoading,
            errorMessage = state.value.errorMessage,
            checkpoint = state.value.checkpoint,
            showMenu = state.value.showMenu,
            lockMenu = state.value.lockMenu,
            contentPadding = contentPadding,
            verticalPadding = verticalPadding,
            horizontalGesture = mainState.value.horizontalGesture,
            horizontalGestureScroll = mainState.value.horizontalGestureScroll,
            horizontalGestureSensitivity = horizontalGestureSensitivity,
            horizontalGestureAlphaAnim = mainState.value.horizontalGestureAlphaAnim,
            horizontalGesturePullAnim = mainState.value.horizontalGesturePullAnim,
            highlightedReading = mainState.value.highlightedReading,
            highlightedReadingThickness = highlightedReadingThickness,
            progress = progress,
            progressCount = mainState.value.progressCount,
            progressBar = mainState.value.progressBar,
            progressBarPadding = progressBarPadding,
            progressBarAlignment = mainState.value.progressBarAlignment,
            progressBarFontSize = progressBarFontSize,
            paragraphHeight = paragraphHeight,
            sidePadding = sidePadding,
            bottomBarPadding = bottomBarPadding,
            backgroundColor = backgroundColor.value,
            fontColor = fontColor.value,
            images = mainState.value.images,
            imagesCornersRoundness = imagesCornersRoundness,
            imagesAlignment = mainState.value.imagesAlignment,
            imagesWidth = imagesWidth,
            imagesColorEffects = imagesColorEffects,
            fontFamily = fontFamily,
            lineHeight = lineHeight,
            fontThickness = mainState.value.fontThickness,
            fontStyle = fontStyle,
            chapterTitleAlignment = mainState.value.chapterTitleAlignment,
            textAlignment = mainState.value.textAlignment,
            horizontalAlignment = horizontalAlignment,
            fontSize = mainState.value.fontSize.sp,
            letterSpacing = letterSpacing,
            paragraphIndentation = paragraphIndentation,
            doubleClickTranslation = mainState.value.doubleClickTranslation,
            fullscreenMode = mainState.value.fullscreen,
            selectPreviousPreset = settingsModel::onEvent,
            selectNextPreset = settingsModel::onEvent,
            leave = screenModel::onEvent,
            restoreCheckpoint = screenModel::onEvent,
            scroll = screenModel::onEvent,
            changeProgress = screenModel::onEvent,
            menuVisibility = screenModel::onEvent,
            openTranslator = screenModel::onEvent,
            onTextSelected = screenModel::onEvent,
            scrollToChapter = screenModel::onEvent,
            showSettingsBottomSheet = screenModel::onEvent,
            dismissBottomSheet = screenModel::onEvent,
            showChaptersDrawer = screenModel::onEvent,
            showSpeedReading = screenModel::onEvent,
            scrollToBookmark = screenModel::onEvent,
            dismissDrawer = screenModel::onEvent,
            onDeleteBookmark = { bookmark ->
                screenModel.deleteBookmarkItem(bookmark)
            },
            onClearAllBookmarks = {
                screenModel.clearAllBookmarks()
            },
            showSearch = screenModel::onEvent,
            hideSearch = screenModel::onEvent,
            searchQuery = state.value.searchQuery,
            searchResults = state.value.searchResults,
            currentSearchResultIndex = state.value.currentSearchResultIndex,
            searchHighlightColor = searchHighlightColor,
            searchScrollbarOpacity = mainState.value.searchScrollbarOpacity,
            showSearchScrollbar = mainState.value.showSearchScrollbar,
            isSearchVisible = state.value.showSearch,
            searchBarPersistent = state.value.searchBarPersistent,
            onSearchQueryChange = screenModel::onEvent,
            onNextSearchResult = screenModel::onEvent,
            onPrevSearchResult = screenModel::onEvent,
            onScrollToSearchResult = screenModel::onEvent,
            textSelectionContext = state.value.textSelectionContext,
             onDismissTextSelection = screenModel::onEvent,
             onExpandSelection = screenModel::onEvent,
             onCopySelection = screenModel::onEvent,
             onBookmarkSelection = screenModel::onEvent,
             onWebSearch = screenModel::onEvent,
             onDictionaryLookup = screenModel::onEvent,
             webViewUrl = state.value.webViewUrl,
             onDismissWebView = screenModel::onEvent,
             showPageIndicator = !mainState.value.fullscreen,
            onOpenExternalBrowser = { url ->
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(url)
                }
                activity.startActivity(intent)
            },
            openLookupsInApp = mainState.value.openLookupsInApp,
            navigateBack = {
                navigator.pop()
            },
             navigateToBookInfo = { changePath ->
                 if (changePath) BookInfoScreen.changePathChannel.trySend(true)
                 navigator.push(
                     BookInfoScreen(
                         bookId = bookId,
                     ),
                     popping = true,
                     saveInBackStack = false
                 )
             },
             onReaderEvent = screenModel::onEvent
         )

        // Speed reading settings bottom sheet
        SpeedReadingSettingsBottomSheet(
            show = state.value.speedReadingSettingsVisible,
            onDismiss = {
                screenModel.onEvent(ReaderEvent.OnDismissSpeedReadingSettings)
            },
            wpm = speedReadingWpm.value,
            onWpmChange = { speedReadingWpm.value = it },
            manualSentencePauseEnabled = speedReadingManualSentencePauseEnabled.value,
            onManualSentencePauseEnabledChange = { speedReadingManualSentencePauseEnabled.value = it },
            sentencePauseDuration = speedReadingSentencePauseDuration.value,
            onSentencePauseDurationChange = { speedReadingSentencePauseDuration.value = it },
            odsEnabled = speedReadingOdsEnabled.value,
            onOdsEnabledChange = { speedReadingOdsEnabled.value = it },
            wordSize = speedReadingWordSize.value,
            onWordSizeChange = { speedReadingWordSize.value = it },
            accentCharacterEnabled = speedReadingAccentCharacterEnabled.value,
            onAccentCharacterEnabledChange = { speedReadingAccentCharacterEnabled.value = it },
            accentColor = speedReadingAccentColor.value,
            onAccentColorChange = { speedReadingAccentColor.value = it },
            accentOpacity = speedReadingAccentOpacity.value,
            onAccentOpacityChange = { speedReadingAccentOpacity.value = it },
            showVerticalIndicators = speedReadingShowVerticalIndicators.value,
            onShowVerticalIndicatorsChange = { speedReadingShowVerticalIndicators.value = it },
            verticalIndicatorsSize = speedReadingVerticalIndicatorsSize.value,
            onVerticalIndicatorsSizeChange = { speedReadingVerticalIndicatorsSize.value = it },
            showHorizontalBars = speedReadingShowHorizontalBars.value,
            onShowHorizontalBarsChange = { speedReadingShowHorizontalBars.value = it },
            horizontalBarsThickness = speedReadingHorizontalBarsThickness.value,
            onHorizontalBarsThicknessChange = { speedReadingHorizontalBarsThickness.value = it },
            horizontalBarsDistance = speedReadingHorizontalBarsDistance.value,
            onHorizontalBarsDistanceChange = { speedReadingHorizontalBarsDistance.value = it },
            horizontalBarsColor = speedReadingHorizontalBarsColor.value,
            onHorizontalBarsColorChange = { speedReadingHorizontalBarsColor.value = it },
            horizontalBarsOpacity = speedReadingHorizontalBarsOpacity.value,
            onHorizontalBarsOpacityChange = { speedReadingHorizontalBarsOpacity.value = it },
            focalPointPosition = speedReadingFocalPointPosition.value,
            onFocalPointPositionChange = { speedReadingFocalPointPosition.value = it },
            customFontEnabled = speedReadingCustomFontEnabled.value,
            onCustomFontEnabledChange = { speedReadingCustomFontEnabled.value = it },
            selectedFontFamily = speedReadingSelectedFontFamily.value,
            onFontFamilyChange = { speedReadingSelectedFontFamily.value = it }
        )

        // Speed reading overlay
        if (state.value.speedReadingMode) {
            SpeedReadingScaffold(
                text = state.value.text,
                bookTitle = state.value.book.title,
                chapterTitle = state.value.currentChapter?.title,
                currentProgress = state.value.book.progress,
                backgroundColor = settingsState.value.selectedColorPreset.backgroundColor,
                fontColor = settingsState.value.selectedColorPreset.fontColor,
                accentCharacterEnabled = speedReadingAccentCharacterEnabled.value,
                accentColor = speedReadingAccentColor.value,
                fontFamily = speedReadingFontFamily,
                sentencePauseMs = if (speedReadingManualSentencePauseEnabled.value) {
                    speedReadingSentencePauseDuration.value
                } else {
                    // Automatic pause calculation based on WPM
                    val baseWpm = 300f
                    val basePause = 350f
                    val minPause = 50f
                    (basePause * (baseWpm / speedReadingWpm.value) + minPause).toInt().coerceIn(50, 1000)
                },
                wordSize = speedReadingWordSize.value,
                accentOpacity = speedReadingAccentOpacity.value,
                showVerticalIndicators = speedReadingShowVerticalIndicators.value,
                verticalIndicatorsSize = speedReadingVerticalIndicatorsSize.value,
                showHorizontalBars = speedReadingShowHorizontalBars.value,
                horizontalBarsThickness = speedReadingHorizontalBarsThickness.value,
                horizontalBarsDistance = speedReadingHorizontalBarsDistance.value,
                horizontalBarsColor = speedReadingHorizontalBarsColor.value,
                horizontalBarsOpacity = speedReadingHorizontalBarsOpacity.value,
                focalPointPosition = speedReadingFocalPointPosition.value,
                progress = progress,
                bottomBarPadding = bottomBarPadding,
                showWpmIndicator = true,
                wpm = speedReadingWpm.value,
                onWpmChange = { speedReadingWpm.value = it },
                odsEnabled = speedReadingOdsEnabled.value,
                onExitSpeedReading = {
                    screenModel.onEvent(ReaderEvent.OnDismissSpeedReading)
                    // Reset menu visibility to normal state when exiting speed reading
                    screenModel.onEvent(
                        ReaderEvent.OnMenuVisibility(
                            show = !mainState.value.fullscreen, // Show menu if not in fullscreen
                            fullscreenMode = mainState.value.fullscreen,
                            saveCheckpoint = false,
                            activity = activity
                        )
                    )
                },
                onShowSpeedReadingSettings = {
                    screenModel.onEvent(ReaderEvent.OnShowSpeedReadingSettings)
                },
                onMenuVisibilityChanged = { showMenu ->
                    // Control system UI based on speed reading menu state
                    screenModel.onEvent(
                        ReaderEvent.OnMenuVisibility(
                            show = showMenu,
                            fullscreenMode = mainState.value.fullscreen,
                            saveCheckpoint = false,
                            activity = activity
                        )
                    )
                }
            )
        }
     }
 }