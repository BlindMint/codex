/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import us.blindmint.codex.domain.reader.SpeedReaderWord
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet

/**
 * Data class representing a word's position in the text.
 */
data class WordPosition(
    val word: String,
    val textIndex: Int,          // Index in List<ReaderText> (now paragraphIndex from SpeedReaderWord)
    val wordIndexInText: Int,    // Word position within that text (always 0 for SpeedReaderWord)
    val globalWordIndex: Int     // Global word index across book
)

/**
 * Data class representing a paragraph of words grouped by textIndex.
 */
data class WordParagraph(
    val textIndex: Int,
    val words: List<WordPosition>
)

/**
 * Bottom sheet for selecting a word as the starting point for speed reading.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun SpeedReadingWordPickerSheet(
    show: Boolean,
    words: List<SpeedReaderWord>,
    currentWordIndex: Int,
    totalWords: Int,
    onDismiss: () -> Unit,
    onConfirm: (progress: Float, wordIndexInText: Int) -> Unit,
    refreshKey: Int = 0 // Increment each time sheet opens to trigger scroll
) {
    var visibleShow by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            visibleShow = true
        } else {
            visibleShow = false
        }
    }

    LaunchedEffect(visibleShow) {
        if (!visibleShow && show) {
            delay(300)
            onDismiss()
        }
    }

    if (visibleShow) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val animatedScrimColor by animateColorAsState(
        targetValue = if (visibleShow) BottomSheetDefaults.ScrimColor else Color.Transparent,
        animationSpec = tween(300)
    )
    val animatedHeight by animateFloatAsState(
        targetValue = if (visibleShow) 1.0f else 0.0f,
        animationSpec = tween(300)
    )

    // Lazy load word conversions only when sheet is actually shown to avoid blocking main thread on initial load
    var allWords by remember { mutableStateOf<List<WordPosition>>(emptyList()) }
    var paragraphs by remember { mutableStateOf<List<WordParagraph>>(emptyList()) }

    LaunchedEffect(show, words) {
        if (show && words.isNotEmpty()) {
            allWords = words.map { speedReaderWord ->
                WordPosition(
                    word = speedReaderWord.text,
                    textIndex = speedReaderWord.paragraphIndex,
                    wordIndexInText = 0,
                    globalWordIndex = speedReaderWord.globalIndex
                )
            }
            paragraphs = allWords
                .groupBy { it: WordPosition -> it.textIndex }
                .map { (textIndex: Int, words: List<WordPosition>) -> WordParagraph(textIndex, words) }
                .sortedBy { it: WordParagraph -> it.textIndex }
        }
    }

    // Current word index is passed directly
    val currentProgress = remember(currentWordIndex, totalWords) {
        if (totalWords > 0) currentWordIndex.toFloat() / totalWords else 0f
    }

    // Find the current word position
    val currentWordPosition: WordPosition? = remember(currentWordIndex, allWords) {
        allWords.getOrNull(currentWordIndex)
    }

    // State for selected word
    var selectedWord: WordPosition? by remember { mutableStateOf(currentWordPosition) }



    // Progress slider state (derived from selected word)
    val sliderProgress: Float by remember(selectedWord, allWords) {
        derivedStateOf {
            if (selectedWord != null && allWords.isNotEmpty()) {
                selectedWord!!.globalWordIndex.toFloat() / allWords.size.toFloat()
            } else if (allWords.isNotEmpty()) {
                currentWordIndex.toFloat() / allWords.size.toFloat()
            } else {
                0f
            }
        }
    }

    // Search state
    var searchQuery: String by remember { mutableStateOf("") }
    var debouncedSearchQuery: String by remember { mutableStateOf("") }

    // Debounce search input
    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery }
            .debounce(300)
            .collect { debouncedSearchQuery = it }
    }

    // Find search matches
    val searchMatches: List<WordPosition> by remember(debouncedSearchQuery, allWords) {
        derivedStateOf {
            if (debouncedSearchQuery.isBlank()) {
                emptyList<WordPosition>()
            } else {
                allWords.filter { wordPosition: WordPosition ->
                    wordPosition.word.contains(debouncedSearchQuery, ignoreCase = true)
                }
            }
        }
    }

    // Current search result index
    var currentSearchIndex: Int by remember { mutableIntStateOf(0) }

    // Reset search index when matches change
    LaunchedEffect(searchMatches) {
        currentSearchIndex = 0
    }

    // Track which refreshKey we've already scrolled for
    var scrolledRefreshKey by remember { mutableIntStateOf(-1) }

    // Track if user is currently dragging slider to prevent circular updates
    var isDraggingSlider by remember { mutableStateOf(false) }

    // Track the scroll position where current word was located
    var currentWordScrollIndex by remember { mutableIntStateOf(-1) }

    // Update current word scroll index when refreshKey changes
    LaunchedEffect(refreshKey, currentWordPosition) {
        currentWordPosition?.let { position ->
            val paragraphIndex = paragraphs.indexOfFirst { paragraph: WordParagraph -> paragraph.textIndex == position.textIndex }
            currentWordScrollIndex = paragraphIndex
        }
    }



    // Scroll to current word when sheet opens (refreshKey changes)
    LaunchedEffect(refreshKey) {
        // Only scroll if this is a fresh sheet open (refreshKey changed)
        // Don't re-scroll when user changes selection
        if (refreshKey > scrolledRefreshKey) {
            // Small delay to ensure sheet is fully rendered
            delay(100)
            currentWordPosition?.let { position: WordPosition ->
                val paragraphIndex = paragraphs.indexOfFirst { paragraph: WordParagraph -> paragraph.textIndex == position.textIndex }
                if (paragraphIndex >= 0) {
                    listState.animateScrollToItem(paragraphIndex)
                    // Wait for scroll to complete, then mark as done
                    scope.launch {
                        delay(300)
                        scrolledRefreshKey = refreshKey
                    }
                } else {
                    scrolledRefreshKey = refreshKey
                }
            }
        }
    }

    // Scroll to search result
    fun scrollToSearchResult(index: Int) {
        if (searchMatches.isNotEmpty() && index in searchMatches.indices) {
            val match = searchMatches[index]
            val paragraphIndex = paragraphs.indexOfFirst { paragraph: WordParagraph -> paragraph.textIndex == match.textIndex }
            if (paragraphIndex >= 0) {
                scope.launch {
                    listState.animateScrollToItem(paragraphIndex)
                }
            }
        }
    }

    ModalBottomSheet(
        hasFixedHeight = true,
        scrimColor = animatedScrimColor,
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = false,
        dragHandle = null,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(animatedHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Status bar padding
            Spacer(
                modifier = Modifier
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            )

            // Top Row with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { visibleShow = false }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Search Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true
                )

                // Result count
                if (debouncedSearchQuery.isNotBlank()) {
                    Text(
                        text = if (searchMatches.isNotEmpty()) {
                            "${currentSearchIndex + 1}/${searchMatches.size}"
                        } else {
                            "0/0"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Previous result
                    IconButton(
                        onClick = {
                            if (searchMatches.isNotEmpty()) {
                                currentSearchIndex = if (currentSearchIndex > 0) {
                                    currentSearchIndex - 1
                                } else {
                                    searchMatches.lastIndex
                                }
                                scrollToSearchResult(currentSearchIndex)
                            }
                        },
                        enabled = searchMatches.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Previous result",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (searchMatches.isNotEmpty()) 0.7f else 0.3f)
                        )
                    }

                    // Next result
                    IconButton(
                        onClick = {
                            if (searchMatches.isNotEmpty()) {
                                currentSearchIndex = if (currentSearchIndex < searchMatches.lastIndex) {
                                    currentSearchIndex + 1
                                } else {
                                    0
                                }
                                scrollToSearchResult(currentSearchIndex)
                            }
                        },
                        enabled = searchMatches.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Next result",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (searchMatches.isNotEmpty()) 0.7f else 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Word content in LazyColumn (no scrollbar - progress slider shows position)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(paragraphs, key = { it.textIndex }) { paragraph ->
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paragraph.words.forEach { wordPosition ->
                            val isCurrentWord = wordPosition.globalWordIndex == currentWordPosition?.globalWordIndex
                            val isSelectedWord = wordPosition.globalWordIndex == selectedWord?.globalWordIndex
                            val isSearchMatch = searchMatches.any {
                                it.globalWordIndex == wordPosition.globalWordIndex
                            }
                            val isCurrentSearchResult = searchMatches.getOrNull(currentSearchIndex)
                                ?.globalWordIndex == wordPosition.globalWordIndex

                            WordChip(
                                word = wordPosition.word,
                                isCurrentWord = isCurrentWord,
                                isSelectedWord = isSelectedWord,
                                isSearchMatch = isSearchMatch,
                                isCurrentSearchResult = isCurrentSearchResult,
                                onClick = { selectedWord = wordPosition }
                            )

                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Progress slider (matches main reader style)
            Slider(
                value = sliderProgress,
                onValueChange = { progress ->
                    isDraggingSlider = true
                    val wordIndex = (progress * allWords.size).toInt().coerceIn(0, allWords.size - 1)
                    val wordPosition = paragraphs.flatMap { it.words }
                        .find { it.globalWordIndex == wordIndex }
                    if (wordPosition != null) {
                        selectedWord = wordPosition
                        val paragraphIndex = paragraphs.indexOfFirst { paragraph: WordParagraph -> paragraph.textIndex == wordPosition.textIndex }
                        if (paragraphIndex >= 0) {
                            scope.launch {
                                listState.animateScrollToItem(paragraphIndex)
                            }
                        }
                    }
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                },
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    thumbColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Row with Cancel and Confirm buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { visibleShow = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        selectedWord?.let { word ->
                            // Calculate progress from global word index
                            val totalWords = allWords.size
                            val newProgress = if (totalWords > 0) {
                                word.globalWordIndex.toFloat() / totalWords.toFloat()
                            } else {
                                0f
                            }
                            onConfirm(newProgress, word.globalWordIndex)
                            visibleShow = false
                        }
                    },
                    enabled = selectedWord != null
                ) {
                    Text("Confirm")
                }
            }
        }
    }
    }
}

/**
 * A tappable word chip with different highlights for current, selected, and search matches.
 */
@Composable
private fun WordChip(
    word: String,
    isCurrentWord: Boolean,
    isSelectedWord: Boolean,
    isSearchMatch: Boolean,
    isCurrentSearchResult: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelectedWord -> MaterialTheme.colorScheme.tertiaryContainer
        isCurrentWord && !isSelectedWord -> MaterialTheme.colorScheme.primaryContainer
        isCurrentSearchResult -> MaterialTheme.colorScheme.secondaryContainer
        isSearchMatch -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelectedWord -> MaterialTheme.colorScheme.onTertiaryContainer
        isCurrentWord -> MaterialTheme.colorScheme.onPrimaryContainer
        isCurrentSearchResult || isSearchMatch -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        isSelectedWord -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
        isCurrentWord -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        isCurrentSearchResult -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        else -> Color.Transparent
    }

    val borderWidth = when {
        isSelectedWord -> 2.dp
        isCurrentWord -> 1.dp
        isCurrentSearchResult -> 1.dp
        else -> 0.dp
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

