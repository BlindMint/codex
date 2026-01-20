/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.reader.ReaderText

/**
 * Data class representing a word's position in the text.
 */
data class WordPosition(
    val word: String,
    val textIndex: Int,          // Index in List<ReaderText>
    val wordIndexInText: Int,    // Word position within that text
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
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class, ExperimentalLayoutApi::class)
@Composable
fun SpeedReadingWordPickerSheet(
    text: List<ReaderText>,
    currentProgress: Float,
    backgroundColor: Color,
    fontColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (progress: Float, wordIndexInText: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Extract all words from text
    val allWords = remember(text) {
        extractWords(text)
    }

    // Group words by paragraph (textIndex)
    val paragraphs = remember(allWords) {
        allWords.groupBy { it.textIndex }
            .map { (textIndex, words) -> WordParagraph(textIndex, words) }
            .sortedBy { it.textIndex }
    }

    // Calculate current word index based on progress
    val currentTextIndex = remember(currentProgress, text) {
        (currentProgress * text.lastIndex).toInt().coerceIn(0, text.lastIndex)
    }

    // Find the current word position (first word in current text)
    val currentWordPosition = remember(currentTextIndex, allWords) {
        allWords.firstOrNull { it.textIndex == currentTextIndex }
    }

    // State for selected word
    var selectedWord by remember { mutableStateOf(currentWordPosition) }

    // Sentence start checkbox state (when true, starts from beginning of sentence)
    var sentenceStart by remember { mutableStateOf(false) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }

    // Debounce search input
    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery }
            .debounce(300)
            .collect { debouncedSearchQuery = it }
    }

    // Find search matches
    val searchMatches by remember(debouncedSearchQuery, allWords) {
        derivedStateOf {
            if (debouncedSearchQuery.isBlank()) {
                emptyList()
            } else {
                allWords.filter {
                    it.word.contains(debouncedSearchQuery, ignoreCase = true)
                }
            }
        }
    }

    // Current search result index
    var currentSearchIndex by remember { mutableIntStateOf(0) }

    // Reset search index when matches change
    LaunchedEffect(searchMatches) {
        currentSearchIndex = 0
    }

    // Auto-scroll to current word on open
    LaunchedEffect(currentWordPosition, paragraphs) {
        currentWordPosition?.let { position ->
            val paragraphIndex = paragraphs.indexOfFirst { it.textIndex == position.textIndex }
            if (paragraphIndex >= 0) {
                listState.scrollToItem(paragraphIndex)
            }
        }
    }

    // Scroll to search result
    fun scrollToSearchResult(index: Int) {
        if (searchMatches.isNotEmpty() && index in searchMatches.indices) {
            val match = searchMatches[index]
            val paragraphIndex = paragraphs.indexOfFirst { it.textIndex == match.textIndex }
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
                            val isCurrentWord = wordPosition.textIndex == currentWordPosition?.textIndex &&
                                    wordPosition.wordIndexInText == currentWordPosition?.wordIndexInText
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

                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = fontColor.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Row with Sentence start checkbox, Cancel, and Confirm buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sentence start checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Checkbox(
                        checked = sentenceStart,
                        onCheckedChange = { sentenceStart = it }
                    )
                    Text(
                        text = "Sentence start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = fontColor
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = fontColor)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        selectedWord?.let { word ->
                            // Calculate progress from textIndex
                            val newProgress = if (text.lastIndex > 0) {
                                word.textIndex.toFloat() / text.lastIndex.toFloat()
                            } else {
                                0f
                            }
                            // Pass word index: 0 if sentence start enabled, otherwise exact word position
                            val wordIndex = if (sentenceStart) 0 else word.wordIndexInText
                            onConfirm(newProgress, wordIndex)
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
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

/**
 * Extract all words from the text list.
 */
private fun extractWords(text: List<ReaderText>): List<WordPosition> {
    val words = mutableListOf<WordPosition>()
    var globalIndex = 0

    text.forEachIndexed { textIndex, readerText ->
        if (readerText is ReaderText.Text) {
            val lineWords = readerText.line.text.split("\\s+".toRegex()).filter { it.isNotBlank() }
            lineWords.forEachIndexed { wordIndex, word ->
                words.add(
                    WordPosition(
                        word = word,
                        textIndex = textIndex,
                        wordIndexInText = wordIndex,
                        globalWordIndex = globalIndex
                    )
                )
                globalIndex++
            }
        }
    }

    return words
}
