/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

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
import androidx.compose.foundation.clickable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.reader.SpeedReaderWord
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown

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
    words: List<SpeedReaderWord>,
    currentWordIndex: Int,
    totalWords: Int,
    backgroundColor: Color,
    fontColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (progress: Float, wordIndexInText: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Convert SpeedReaderWord to WordPosition for display
    val allWords: List<WordPosition> = remember(words) {
        words.map { speedReaderWord ->
            WordPosition(
                word = speedReaderWord.text,
                textIndex = speedReaderWord.paragraphIndex,
                wordIndexInText = 0,
                globalWordIndex = speedReaderWord.globalIndex
            )
        }
    }

    // Group words by paragraph (textIndex)
    val paragraphs: List<WordParagraph> = remember(allWords) {
        allWords.groupBy { it: WordPosition -> it.textIndex }
            .map { (textIndex: Int, words: List<WordPosition>) -> WordParagraph(textIndex, words) }
            .sortedBy { it: WordParagraph -> it.textIndex }
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
    LaunchedEffect(currentWordPosition, paragraphs) {
        currentWordPosition?.let { position: WordPosition ->
            val paragraphIndex = paragraphs.indexOfFirst { paragraph: WordParagraph -> paragraph.textIndex == position.textIndex }
            if (paragraphIndex >= 0) {
                listState.scrollToItem(paragraphIndex)
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
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = backgroundColor,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Top Row with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = fontColor
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
                    placeholder = { Text("Search...", color = fontColor.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = fontColor.copy(alpha = 0.7f)
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
                                scrollToSearchResult(currentSearchIndex)
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
                                scrollToSearchResult(currentSearchIndex)
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

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = fontColor.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Word content in LazyColumn
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
                                fontColor = fontColor,
                                onClick = { selectedWord = wordPosition }
                            )

                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Bottom Row with Cancel and Confirm buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
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
    val backgroundColor = when {
        isSelectedWord -> MaterialTheme.colorScheme.tertiaryContainer
        isCurrentWord -> MaterialTheme.colorScheme.primaryContainer
        isCurrentSearchResult -> MaterialTheme.colorScheme.secondaryContainer
        isSearchMatch -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelectedWord -> MaterialTheme.colorScheme.onTertiaryContainer
        isCurrentWord -> MaterialTheme.colorScheme.onPrimaryContainer
        isCurrentSearchResult || isSearchMatch -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> fontColor
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
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

