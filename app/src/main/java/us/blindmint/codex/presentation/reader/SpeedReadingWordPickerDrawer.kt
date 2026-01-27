/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.presentation.core.components.modal_drawer.DrawerSide
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.presentation.settings.reader.speed_reading.SpeedReadingSubcategory
import us.blindmint.codex.presentation.settings.reader.speed_reading.SpeedReadingTab

@OptIn(ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun SpeedReadingWordPickerDrawer(
    show: Boolean = true,
    words: List<SpeedReaderWord>,
    currentWordIndex: Int,
    totalWords: Int,
    backgroundColor: Color,
    fontColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (progress: Float, wordIndexInText: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Convert SpeedReaderWord to WordPosition for display
    val allWords: List<WordPosition> = remember(words) {
        words.mapIndexed { index: Int, word: SpeedReaderWord ->
            WordPosition(
                word = word.text,
                textIndex = word.paragraphIndex,
                wordIndexInText = index,
                globalWordIndex = word.globalIndex
            )
        }
    }

    // Current word index is passed directly
    val currentProgress = remember(currentWordIndex, totalWords) {
        if (totalWords > 0) currentWordIndex.toFloat() / totalWords else 0f
    }

    // Find current word position
    val currentWordPosition: WordPosition? = remember(currentWordIndex, allWords) {
        allWords.getOrNull(currentWordIndex)
    }

    // State for selected word
    var selectedWord: WordPosition? by remember { mutableStateOf(currentWordPosition) }

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

    // Auto-scroll to current word on open
    LaunchedEffect(show, allWords, currentWordIndex) {
        if (show) {
            currentWordPosition?.let { position ->
                // Scroll near to current word position (find by globalWordIndex)
                // Since we're displaying all words in one list, find the index
                // of the word in allWords and scroll to approximately that position
                // Estimate: assume ~10 words per row, scroll to 1/10th position
                val scrollPosition = (position.globalWordIndex / 10).coerceIn(0, maxOf(0, (allWords.size / 10) - 1))
                listState.animateScrollToItem(scrollPosition)
            }
        }
    }

    val panelAlpha = animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.EaseInOutCubic),
        label = "wordPickerAlpha"
    )

    val panelOffset = animateDpAsState(
        targetValue = if (show) 0.dp else 400.dp,
        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.EaseInOutCubic),
        label = "wordPickerOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * panelAlpha.value))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                )
                .offset { IntOffset(x = panelOffset.value.roundToPx(), y = 0) }
                .alpha(panelAlpha.value)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .systemBarsPadding()
            ) {
                // Header
                Column {
                    Box(Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Text(
                            text = "Select Starting Word",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Search Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search...", color = fontColor.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = fontColor.copy(alpha = 0.7f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = fontColor,
                                focusedTextColor = fontColor
                            )
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
                                color = fontColor.copy(alpha = 0.7f)
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
                                    }
                                },
                                enabled = searchMatches.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Previous result",
                                    tint = fontColor.copy(alpha = if (searchMatches.isNotEmpty()) 0.7f else 0.3f)
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
                                    }
                                },
                                enabled = searchMatches.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Next result",
                                    tint = fontColor.copy(alpha = if (searchMatches.isNotEmpty()) 0.7f else 0.3f)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                }

                // Word content in LazyColumn
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 9.dp)
                    ) {
                        // Word content - display all words in continuous flow
                        // Use single item to prevent paragraph breaks, FlowRow for wrapping
                        item(key = "all_words") {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                allWords.forEach { wordPosition ->
                                    val isCurrentWord = wordPosition.globalWordIndex == currentWordPosition?.globalWordIndex
                                    val isSelectedWord = wordPosition == selectedWord
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
                                        fontColor = fontColor,
                                        onClick = { selectedWord = wordPosition }
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer - Progress Bar for quick navigation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = fontColor)
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
                                }
                            },
                            enabled = selectedWord != null
                        ) {
                            Text("Confirm")
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = fontColor.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = fontColor.copy(alpha = 0.7f),
                            modifier = Modifier.width(60.dp)
                        )
                        var sliderValue by remember { mutableFloatStateOf(currentProgress) }

                         LaunchedEffect(selectedWord?.globalWordIndex, allWords.size) {
                             val currentSelectedWord = selectedWord
                             sliderValue = if (currentSelectedWord != null) {
                                 (currentSelectedWord.globalWordIndex.toFloat() / allWords.size).coerceIn(0f, 1f)
                             } else {
                                 currentProgress
                             }
                         }

                         Slider(
                             value = sliderValue,
                             onValueChange = { newProgress ->
                                 sliderValue = newProgress
                                 val newIndex = (newProgress * allWords.size).toInt().coerceIn(0, allWords.size - 1)
                                 val word = allWords.getOrNull(newIndex)
                                 if (word != null) {
                                     selectedWord = word
                                     scope.launch {
                                         listState.animateScrollToItem(0)
                                     }
                                 }
                             },
                             valueRange = 0f..1f,
                             modifier = Modifier.weight(1f)
                         )
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
    fontColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor: Color = when {
        isSelectedWord -> MaterialTheme.colorScheme.tertiaryContainer
        isCurrentWord -> MaterialTheme.colorScheme.primaryContainer
        isCurrentSearchResult -> MaterialTheme.colorScheme.secondaryContainer
        isSearchMatch -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor: Color = when {
        isSelectedWord -> MaterialTheme.colorScheme.onTertiaryContainer
        isCurrentWord -> MaterialTheme.colorScheme.onPrimaryContainer
        isCurrentSearchResult || isSearchMatch -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> fontColor
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
