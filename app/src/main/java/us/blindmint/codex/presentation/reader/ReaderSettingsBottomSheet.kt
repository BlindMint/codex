/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.settings.appearance.colors.ColorsSubcategory
import us.blindmint.codex.presentation.settings.reader.chapters.ChaptersSubcategory
import us.blindmint.codex.presentation.settings.reader.font.FontSubcategory
import us.blindmint.codex.presentation.settings.reader.images.ImagesSubcategory
import us.blindmint.codex.presentation.settings.reader.misc.MiscSubcategory
import us.blindmint.codex.presentation.settings.reader.padding.PaddingSubcategory
import us.blindmint.codex.presentation.settings.reader.progress.ProgressSubcategory
import us.blindmint.codex.presentation.settings.reader.reading_mode.ReadingModeSubcategory
import us.blindmint.codex.presentation.settings.reader.reading_speed.ReadingSpeedSubcategory
import us.blindmint.codex.presentation.settings.reader.dictionary.DictionarySubcategory
import us.blindmint.codex.presentation.settings.reader.search.SearchSubcategory
import us.blindmint.codex.presentation.settings.reader.system.SystemSubcategory
import us.blindmint.codex.presentation.settings.reader.text.TextSubcategory
import us.blindmint.codex.ui.reader.ReaderEvent

private var initialPage = 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsBottomSheet(
    fullscreenMode: Boolean,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    dismissBottomSheet: (ReaderEvent.OnDismissBottomSheet) -> Unit
) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage) { 3 }

    // Track scroll states for each tab
    val generalScrollState = rememberLazyListState()
    val readerScrollState = rememberLazyListState()
    val colorsScrollState = rememberLazyListState()

    // Determine if current tab can scroll up (not at top)
    val canScrollUp by remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> generalScrollState.canScrollBackward
                1 -> readerScrollState.canScrollBackward
                else -> colorsScrollState.canScrollBackward
            }
        }
    }

    DisposableEffect(Unit) { onDispose { initialPage = pagerState.currentPage } }

    val animatedScrimColor by animateColorAsState(
        targetValue = if (pagerState.currentPage == 2) Color.Transparent
        else BottomSheetDefaults.ScrimColor,
        animationSpec = tween(300)
    )
    val animatedHeight by animateFloatAsState(
        targetValue = 0.7f,
        animationSpec = tween(300)
    )

    LaunchedEffect(pagerState.currentPage) {
        menuVisibility(
            ReaderEvent.OnMenuVisibility(
                show = pagerState.currentPage != 2,
                fullscreenMode = fullscreenMode,
                saveCheckpoint = false,
                activity = activity
            )
        )
    }

    ModalBottomSheet(
        hasFixedHeight = true,
        scrimColor = animatedScrimColor,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(animatedHeight)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        // Only dismiss if we're at the top of content (can't scroll up)
                        if (!canScrollUp) {
                            dismissBottomSheet(ReaderEvent.OnDismissBottomSheet)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // Allow natural scrolling behavior
                        change.consume()
                    }
                )
            },
        onDismissRequest = {
            dismissBottomSheet(ReaderEvent.OnDismissBottomSheet)
        },
        sheetGesturesEnabled = false
    ) {
        ReaderSettingsBottomSheetTabRow(
            currentPage = pagerState.currentPage,
            scrollToPage = {
                scope.launch {
                    pagerState.animateScrollToPage(it)
                }
            }
        )

        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> {
                    LazyColumnWithScrollbar(Modifier.fillMaxSize(), state = generalScrollState) {
                        ReadingModeSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        PaddingSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        SystemSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        ReadingSpeedSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        SearchSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        DictionarySubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        MiscSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface },
                            showDivider = false
                        )
                    }
                }

                1 -> {
                    LazyColumnWithScrollbar(Modifier.fillMaxSize(), state = readerScrollState) {
                        FontSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        TextSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        ImagesSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        ChaptersSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface }
                        )
                        ProgressSubcategory(
                            titleColor = { MaterialTheme.colorScheme.onSurface },
                            showDivider = false
                        )
                    }
                }

                2 -> {
                    LazyColumnWithScrollbar(Modifier.fillMaxSize(), state = colorsScrollState) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        ColorsSubcategory(
                            showTitle = false,
                            showDivider = false,
                            backgroundColor = { MaterialTheme.colorScheme.surfaceContainer }
                        )
                    }
                }
            }
        }
    }
}