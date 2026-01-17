/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.os.Build
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.FontWithName
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.domain.reader.ReaderHorizontalGesture
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.domain.util.HorizontalAlignment
import androidx.compose.ui.graphics.Color as ComposeColor
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.common.SelectionContainerWithBottomSheet
import us.blindmint.codex.presentation.core.components.common.SpacedItem
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.reader.ReaderModel

@Composable
fun ReaderLayout(
    book: Book,
    text: List<ReaderText>,
    listState: LazyListState,
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
    showPageIndicator: Boolean = true,
    isLoading: Boolean,
    showMenu: Boolean,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    onTextSelected: (ReaderEvent.OnTextSelected) -> Unit,
    onReaderEvent: (ReaderEvent) -> Unit,
    searchQuery: String,
    searchHighlightColor: ComposeColor
) {
    val activity = LocalActivity.current
    val mainModel = hiltViewModel<MainModel>()
    val mainState = mainModel.state.collectAsStateWithLifecycle()

    // Conditional rendering based on whether it's a comic or text book
    if (book.isComic) {
        // Comic reader layout
        ComicReaderLayout(
            book = book,
            currentPage = 0, // TODO: Implement page tracking
            onPageChanged = { /* TODO: Handle page changes */ },
            contentPadding = contentPadding,
            backgroundColor = backgroundColor,
            comicReadingDirection = mainState.value.comicReadingDirection,
            comicTapZone = mainState.value.comicTapZone,
            showPageIndicator = !fullscreenMode,
            onLoadingComplete = {
                // Signal that comic loading is complete
                onReaderEvent(ReaderEvent.OnComicLoadingComplete)
            },
            onMenuToggle = {
                menuVisibility(
                    ReaderEvent.OnMenuVisibility(
                        show = !showMenu,
                        fullscreenMode = fullscreenMode,
                        saveCheckpoint = true,
                        activity = activity
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    SelectionContainerWithBottomSheet(
        onTextSelected = { selectedText ->
            // Build paragraph context from the text list
            val paragraphContext = text.joinToString(" ") { item ->
                when (item) {
                    is ReaderText.Text -> item.line.text
                    is ReaderText.Chapter -> item.title
                    else -> ""
                }
            }

            onTextSelected(
                ReaderEvent.OnTextSelected(
                    selectedText = selectedText,
                    paragraphText = paragraphContext
                )
            )
        }
    ) { toolbarHidden ->
        ReaderBackground(
            backgroundImage = mainState.value.backgroundImage,
            backgroundColor = backgroundColor,
            backgroundImageOpacity = mainState.value.backgroundImageOpacity,
            backgroundScaleMode = mainState.value.backgroundScaleMode
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (!isLoading && toolbarHidden) {
                            Modifier.noRippleClickable(
                                onClick = {
                                    menuVisibility(
                                        ReaderEvent.OnMenuVisibility(
                                            show = !showMenu,
                                            fullscreenMode = fullscreenMode,
                                            saveCheckpoint = true,
                                            activity = activity
                                        )
                                    )
                                }
                            )
                        } else Modifier
                    )
                    .padding(contentPadding)
                    .padding(vertical = verticalPadding)
                    .readerHorizontalGesture(
                        listState = listState,
                        horizontalGesture = horizontalGesture,
                        horizontalGestureScroll = horizontalGestureScroll,
                        horizontalGestureSensitivity = horizontalGestureSensitivity,
                        horizontalGestureAlphaAnim = horizontalGestureAlphaAnim,
                        horizontalGesturePullAnim = horizontalGesturePullAnim,
                        isLoading = isLoading
                    )
            ) {
            LazyColumnWithScrollbar(
                state = listState,
                enableScrollbar = false,
                parentModifier = Modifier.weight(1f),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = (WindowInsets.displayCutout.asPaddingValues()
                        .calculateTopPadding() + paragraphHeight)
                        .coerceAtLeast(18.dp),
                    bottom = (WindowInsets.displayCutout.asPaddingValues()
                        .calculateBottomPadding() + paragraphHeight)
                        .coerceAtLeast(18.dp),
                )
            ) {
                itemsIndexed(
                    text,
                    key = { index, _ -> index }
                ) { index, entry ->
                    when {
                        !images && entry is ReaderText.Image -> return@itemsIndexed
                        else -> {
                            SpacedItem(
                                index = index,
                                spacing = paragraphHeight
                            ) {
                                ReaderLayoutText(
                                    activity = activity,
                                    showMenu = showMenu,
                                    entry = entry,
                                    imagesCornersRoundness = imagesCornersRoundness,
                                    imagesAlignment = imagesAlignment,
                                    imagesWidth = imagesWidth,
                                    imagesColorEffects = imagesColorEffects,
                                    fontFamily = fontFamily,
                                    fontColor = fontColor,
                                    lineHeight = lineHeight,
                                    fontThickness = fontThickness,
                                    fontStyle = fontStyle,
                                    chapterTitleAlignment = chapterTitleAlignment,
                                    textAlignment = textAlignment,
                                    horizontalAlignment = horizontalAlignment,
                                    fontSize = fontSize,
                                    letterSpacing = letterSpacing,
                                    sidePadding = sidePadding,
                                    paragraphIndentation = paragraphIndentation,
                                    fullscreenMode = fullscreenMode,
                                    doubleClickTranslation = doubleClickTranslation,
                                    highlightedReading = highlightedReading,
                                    highlightedReadingThickness = highlightedReadingThickness,
                                    toolbarHidden = toolbarHidden,
                                    openTranslator = openTranslator,
                                    menuVisibility = menuVisibility,
                                    searchQuery = searchQuery,
                                    searchHighlightColor = searchHighlightColor
                                )
                            }
                        }
                    }
                }
            }

                AnimatedVisibility(
                    visible = !showMenu && progressBar,
                    enter = slideInVertically { it } + expandVertically(),
                    exit = slideOutVertically { it } + shrinkVertically()
                ) {
                    ReaderProgressBar(
                        progress = progress,
                        progressBarPadding = progressBarPadding,
                        progressBarAlignment = progressBarAlignment,
                        progressBarFontSize = progressBarFontSize,
                        fontColor = fontColor,
                        sidePadding = sidePadding
                    )
                }
            }
        }
    }
}