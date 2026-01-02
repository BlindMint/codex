/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.lookup.WebDictionary
import us.blindmint.codex.domain.lookup.WebSearchEngine
import us.blindmint.codex.domain.reader.TextSelectionContext
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.Checkpoint
import us.blindmint.codex.domain.reader.FontWithName
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.domain.reader.ReaderHorizontalGesture
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.ReaderText.Chapter
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.domain.reader.SearchResult
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.domain.util.BottomSheet
import us.blindmint.codex.domain.util.Drawer
import us.blindmint.codex.domain.util.HorizontalAlignment
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.settings.SettingsEvent

@Composable
fun ReaderContent(
    book: Book,
    text: List<ReaderText>,
    bottomSheet: BottomSheet?,
    drawer: Drawer?,
    listState: LazyListState,
    currentChapter: Chapter?,
    nestedScrollConnection: NestedScrollConnection,
    fastColorPresetChange: Boolean,
    perceptionExpander: Boolean,
    perceptionExpanderPadding: Dp,
    perceptionExpanderThickness: Dp,
    currentChapterProgress: Float,
    isLoading: Boolean,
    errorMessage: UIText?,
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
    scrollToChapter: (ReaderEvent.OnScrollToChapter) -> Unit,
    showSettingsBottomSheet: (ReaderEvent.OnShowSettingsBottomSheet) -> Unit,
    dismissBottomSheet: (ReaderEvent.OnDismissBottomSheet) -> Unit,
    showChaptersDrawer: (ReaderEvent.OnShowChaptersDrawer) -> Unit,
    showBookmarksDrawer: (ReaderEvent.OnShowBookmarksDrawer) -> Unit,
    dismissDrawer: (ReaderEvent.OnDismissDrawer) -> Unit,
    showSearch: (ReaderEvent.OnShowSearch) -> Unit,
    hideSearch: (ReaderEvent.OnHideSearch) -> Unit,
    searchQuery: String,
    searchResults: List<SearchResult>,
    currentSearchResultIndex: Int,
    searchHighlightColor: Color,
    isSearchVisible: Boolean,
    onSearchQueryChange: (ReaderEvent.OnSearchQueryChange) -> Unit,
    onNextSearchResult: (ReaderEvent.OnNextSearchResult) -> Unit,
    onPrevSearchResult: (ReaderEvent.OnPrevSearchResult) -> Unit,
    textSelectionContext: TextSelectionContext?,
    onDismissTextSelection: (ReaderEvent.OnDismissTextSelection) -> Unit,
    onExpandSelection: (ReaderEvent.OnExpandSelection) -> Unit,
    onCopySelection: (ReaderEvent.OnCopySelection) -> Unit,
    onBookmarkSelection: (ReaderEvent.OnBookmarkSelection) -> Unit,
    onWebSearch: (ReaderEvent.OnWebSearch) -> Unit,
    onDictionaryLookup: (ReaderEvent.OnDictionaryLookup) -> Unit,
    webViewUrl: String?,
    onDismissWebView: (ReaderEvent.OnDismissWebView) -> Unit,
    onOpenExternalBrowser: (String) -> Unit,
    openLookupsInApp: Boolean,
    navigateToBookInfo: (changePath: Boolean) -> Unit,
    navigateBack: () -> Unit
) {
    val activity = LocalActivity.current
    ReaderBottomSheet(
        bottomSheet = bottomSheet,
        fullscreenMode = fullscreenMode,
        menuVisibility = menuVisibility,
        dismissBottomSheet = dismissBottomSheet
    )

    if (isLoading || errorMessage == null) {
        ReaderScaffold(
            book = book,
            text = text,
            listState = listState,
            currentChapter = currentChapter,
            nestedScrollConnection = nestedScrollConnection,
            fastColorPresetChange = fastColorPresetChange,
            perceptionExpander = perceptionExpander,
            perceptionExpanderPadding = perceptionExpanderPadding,
            perceptionExpanderThickness = perceptionExpanderThickness,
            currentChapterProgress = currentChapterProgress,
            isLoading = isLoading,
            checkpoint = checkpoint,
            showMenu = showMenu,
            lockMenu = lockMenu,
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
            bottomBarPadding = bottomBarPadding,
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
            selectPreviousPreset = selectPreviousPreset,
            selectNextPreset = selectNextPreset,
            menuVisibility = menuVisibility,
            leave = leave,
            restoreCheckpoint = restoreCheckpoint,
            scroll = scroll,
            changeProgress = changeProgress,
            openTranslator = openTranslator,
            onTextSelected = onTextSelected,
            showSettingsBottomSheet = showSettingsBottomSheet,
            showChaptersDrawer = showChaptersDrawer,
            showBookmarksDrawer = showBookmarksDrawer,
            showSearch = showSearch,
            hideSearch = hideSearch,
            searchQuery = searchQuery,
            searchResults = searchResults,
            currentSearchResultIndex = currentSearchResultIndex,
            searchHighlightColor = searchHighlightColor,
            isSearchVisible = isSearchVisible,
            onSearchQueryChange = onSearchQueryChange,
            onNextSearchResult = onNextSearchResult,
            onPrevSearchResult = onPrevSearchResult,
            navigateBack = navigateBack,
            navigateToBookInfo = navigateToBookInfo
        )
    } else {
        ReaderErrorPlaceholder(
            errorMessage = errorMessage,
            leave = leave,
            navigateToBookInfo = navigateToBookInfo,
            navigateBack = navigateBack
        )
    }

    ReaderDrawer(
        drawer = drawer,
        chapters = remember(text) { text.filterIsInstance<Chapter>() },
        currentChapter = currentChapter,
        currentChapterProgress = currentChapterProgress,
        scrollToChapter = scrollToChapter,
        dismissDrawer = dismissDrawer
    )

    ReaderBackHandler(
        leave = leave,
        navigateBack = navigateBack
    )

    // Text Selection Bottom Sheet
    if (textSelectionContext != null) {
        TextSelectionBottomSheet(
            selectionContext = textSelectionContext,
            onDismiss = { onDismissTextSelection(ReaderEvent.OnDismissTextSelection) },
            onCopy = {
                // Copy to clipboard
                val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("Selected text", textSelectionContext.selectedText)
                )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    activity.getString(R.string.copied).showToast(context = activity, longToast = false)
                }
                onCopySelection(ReaderEvent.OnCopySelection(textSelectionContext.selectedText))
            },
            onBookmark = {
                onBookmarkSelection(ReaderEvent.OnBookmarkSelection)
            },
            onWebSearch = { engine ->
                onWebSearch(
                    ReaderEvent.OnWebSearch(
                        engine = engine,
                        query = textSelectionContext.selectedText,
                        activity = activity,
                        openInApp = openLookupsInApp
                    )
                )
            },
            onDictionary = { dictionary ->
                onDictionaryLookup(
                    ReaderEvent.OnDictionaryLookup(
                        dictionary = dictionary,
                        word = textSelectionContext.selectedText,
                        activity = activity,
                        openInApp = openLookupsInApp
                    )
                )
            },
            onExpandSelection = { expandLeading ->
                onExpandSelection(ReaderEvent.OnExpandSelection(expandLeading = expandLeading))
            }
        )
    }

    // WebView Bottom Sheet for in-app browser
    if (webViewUrl != null) {
        WebViewBottomSheet(
            url = webViewUrl,
            onDismiss = { onDismissWebView(ReaderEvent.OnDismissWebView) },
            onOpenExternal = { url -> onOpenExternalBrowser(url) }
        )
    }
}