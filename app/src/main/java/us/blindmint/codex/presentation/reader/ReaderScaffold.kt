/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.annotation.SuppressLint
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.reader.Checkpoint
import us.blindmint.codex.domain.reader.FontWithName
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.domain.reader.ReaderHorizontalGesture
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.ReaderText.Chapter
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.domain.util.HorizontalAlignment
import us.blindmint.codex.domain.reader.SearchResult
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.settings.SettingsEvent

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ReaderScaffold(
    book: Book,
    text: List<ReaderText>,
    listState: LazyListState,
    currentChapter: Chapter?,
    nestedScrollConnection: NestedScrollConnection,
    fastColorPresetChange: Boolean,
    perceptionExpander: Boolean,
    perceptionExpanderPadding: Dp,
    perceptionExpanderThickness: Dp,
    currentChapterProgress: Float,
    isLoading: Boolean,
    checkpoint: Checkpoint,
    showMenu: Boolean,
    lockMenu: Boolean,
    contentPadding: PaddingValues,
    verticalPadding: Dp,
    horizontalGesture: ReaderHorizontalGesture,
    horizontalGestureScroll: Float,
    horizontalGestureSensitivity: Dp,
    horizontalGestureAlphaAnim: Boolean,
    horizontalGesturePullAnim: Boolean,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    progress: String,
    progressBar: Boolean,
    progressBarPadding: Dp,
    progressBarAlignment: HorizontalAlignment,
    progressBarFontSize: TextUnit,
    paragraphHeight: Dp,
    sidePadding: Dp,
    bottomBarPadding: Dp,
    backgroundColor: Color,
    fontColor: Color,
    images: Boolean,
    imagesCornersRoundness: Dp,
    imagesAlignment: HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    fontFamily: FontWithName,
    lineHeight: TextUnit,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    chapterTitleAlignment: ReaderTextAlignment,
    textAlignment: ReaderTextAlignment,
    horizontalAlignment: Alignment.Horizontal,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    paragraphIndentation: TextUnit,
    doubleClickTranslation: Boolean,
    fullscreenMode: Boolean,
    selectPreviousPreset: (SettingsEvent.OnSelectPreviousPreset) -> Unit,
    selectNextPreset: (SettingsEvent.OnSelectNextPreset) -> Unit,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    leave: (ReaderEvent.OnLeave) -> Unit,
    restoreCheckpoint: (ReaderEvent.OnRestoreCheckpoint) -> Unit,
    scroll: (ReaderEvent.OnScroll) -> Unit,
    changeProgress: (ReaderEvent.OnChangeProgress) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    onTextSelected: (ReaderEvent.OnTextSelected) -> Unit,
    showSettingsBottomSheet: (ReaderEvent.OnShowSettingsBottomSheet) -> Unit,
    showChaptersDrawer: (ReaderEvent.OnShowChaptersDrawer) -> Unit,
    showBookmarksDrawer: (ReaderEvent.OnShowBookmarksDrawer) -> Unit,
    showSearch: (ReaderEvent.OnShowSearchPersistent) -> Unit,
    hideSearch: (ReaderEvent.OnHideSearch) -> Unit,
    searchQuery: String,
    searchResults: List<SearchResult>,
    currentSearchResultIndex: Int,
    searchHighlightColor: Color,
    searchScrollbarOpacity: Double,
    isSearchVisible: Boolean,
    searchBarPersistent: Boolean,
    onSearchQueryChange: (ReaderEvent.OnSearchQueryChange) -> Unit,
    onNextSearchResult: (ReaderEvent.OnNextSearchResult) -> Unit,
    onPrevSearchResult: (ReaderEvent.OnPrevSearchResult) -> Unit,
    onScrollToSearchResult: (ReaderEvent.OnScrollToSearchResult) -> Unit,
    navigateToBookInfo: (changePath: Boolean) -> Unit,
    navigateBack: () -> Unit
) {
    Scaffold(
        Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                AnimatedVisibility(
                    visible = showMenu,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ReaderTopBar(
                        book = book,
                        currentChapter = currentChapter,
                        fastColorPresetChange = fastColorPresetChange,
                        currentChapterProgress = currentChapterProgress,
                        isLoading = isLoading,
                        lockMenu = lockMenu,
                        leave = leave,
                        selectPreviousPreset = selectPreviousPreset,
                        selectNextPreset = selectNextPreset,
                        showSettingsBottomSheet = showSettingsBottomSheet,
                        showChaptersDrawer = showChaptersDrawer,
                        showBookmarksDrawer = showBookmarksDrawer,
                        isSearchVisible = isSearchVisible || (showMenu && searchBarPersistent),
                        showSearch = showSearch,
                        hideSearch = hideSearch,
                        navigateBack = navigateBack,
                        navigateToBookInfo = navigateToBookInfo
                    )
                }

                AnimatedVisibility(
                    visible = isSearchVisible || (showMenu && searchBarPersistent),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ReaderSearchBar(
                        searchQuery = searchQuery,
                        searchResultsCount = searchResults.size,
                        currentResultIndex = currentSearchResultIndex,
                        onQueryChange = onSearchQueryChange,
                        onNextResult = onNextSearchResult,
                        onPrevResult = onPrevSearchResult,
                        onHideSearch = hideSearch
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = showMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ReaderBottomBar(
                    book = book,
                    progress = progress,
                    text = text,
                    listState = listState,
                    lockMenu = lockMenu,
                    checkpoint = checkpoint,
                    bottomBarPadding = bottomBarPadding,
                    restoreCheckpoint = restoreCheckpoint,
                    scroll = scroll,
                    changeProgress = changeProgress
                )
            }
        }
    ) {
        ReaderLayout(
            text = text,
            listState = listState,
            contentPadding = contentPadding,
            verticalPadding = verticalPadding,
            horizontalGesture = horizontalGesture,
            horizontalGestureScroll = horizontalGestureScroll,
            horizontalGestureSensitivity = horizontalGestureSensitivity,
            horizontalGestureAlphaAnim = horizontalGestureAlphaAnim,
            horizontalGesturePullAnim = horizontalGesturePullAnim,
            highlightedReading = highlightedReading,
            highlightedReadingThickness = highlightedReadingThickness,
            progress = progress,
            progressBar = progressBar,
            progressBarPadding = progressBarPadding,
            progressBarAlignment = progressBarAlignment,
            progressBarFontSize = progressBarFontSize,
            paragraphHeight = paragraphHeight,
            sidePadding = sidePadding,
            backgroundColor = backgroundColor,
            fontColor = fontColor,
            images = images,
            imagesCornersRoundness = imagesCornersRoundness,
            imagesAlignment = imagesAlignment,
            imagesWidth = imagesWidth,
            imagesColorEffects = imagesColorEffects,
            fontFamily = fontFamily,
            lineHeight = lineHeight,
            fontThickness = fontThickness,
            fontStyle = fontStyle,
            chapterTitleAlignment = chapterTitleAlignment,
            textAlignment = textAlignment,
            horizontalAlignment = horizontalAlignment,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            paragraphIndentation = paragraphIndentation,
            doubleClickTranslation = doubleClickTranslation,
            fullscreenMode = fullscreenMode,
            isLoading = isLoading,
            showMenu = showMenu,
            menuVisibility = menuVisibility,
            openTranslator = openTranslator,
            onTextSelected = onTextSelected,
            searchQuery = searchQuery,
            searchHighlightColor = searchHighlightColor
        )

        // Search scrollbar - visible when search bar is active
        if (isSearchVisible) {
            ReaderSearchScrollbar(
                text = text,
                listState = listState,
                searchResults = searchResults,
                currentSearchResultIndex = currentSearchResultIndex,
                searchScrollbarOpacity = searchScrollbarOpacity.toFloat(),
                searchHighlightColor = searchHighlightColor,
                showMenu = showMenu,
                onScrollToPosition = { position ->
                    // Scroll to the specific text position
                    listState.requestScrollToItem(position)
                    // Note: Chapter updates happen automatically when scrolling
                },
                onScrollToSearchResult = { index ->
                    onScrollToSearchResult(ReaderEvent.OnScrollToSearchResult(index))
                }
            )
        }

        ReaderPerceptionExpander(
            perceptionExpander = perceptionExpander,
            perceptionExpanderPadding = perceptionExpanderPadding,
            perceptionExpanderThickness = perceptionExpanderThickness,
            perceptionExpanderColor = fontColor
        )

        if (isLoading) {
            ReaderLoadingPlaceholder()
        }
    }
}