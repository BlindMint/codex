/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.reader

import androidx.compose.runtime.Immutable

/**
 * Context for text selection in the reader, including selected text
 * and surrounding context for expansion.
 *
 * @param selectedText The full selected text (auto-completed to whole words)
 * @param selectedWords Individual selected words for manipulation
 * @param leadingContext Words before the selection (tappable to expand)
 * @param trailingContext Words after the selection (tappable to expand)
 * @param paragraphText Full paragraph text for reference
 * @param selectionStartIndex Word index where selection starts in paragraph
 * @param selectionEndIndex Word index where selection ends in paragraph (exclusive)
 */
@Immutable
data class TextSelectionContext(
    val selectedText: String,
    val selectedWords: List<String>,
    val leadingContext: List<String>,
    val trailingContext: List<String>,
    val paragraphText: String,
    val selectionStartIndex: Int,
    val selectionEndIndex: Int
) {
    companion object {
        private const val CONTEXT_WORD_COUNT = 3

        /**
         * Creates a TextSelectionContext from raw selected text and paragraph.
         * Auto-completes selection to whole word boundaries.
         *
         * @param selectedText The raw selected text from the user
         * @param paragraphText The full paragraph containing the selection
         * @return TextSelectionContext with expanded selection and context
         */
        fun fromSelection(selectedText: String, paragraphText: String): TextSelectionContext {
            val trimmedSelection = selectedText.trim()
            if (trimmedSelection.isEmpty()) {
                return TextSelectionContext(
                    selectedText = "",
                    selectedWords = emptyList(),
                    leadingContext = emptyList(),
                    trailingContext = emptyList(),
                    paragraphText = paragraphText,
                    selectionStartIndex = 0,
                    selectionEndIndex = 0
                )
            }

            // Split paragraph into words while preserving positions
            val paragraphWords = paragraphText.split(Regex("\\s+")).filter { it.isNotEmpty() }

            // Find the selection in the paragraph
            val selectionStart = paragraphText.indexOf(trimmedSelection)
            if (selectionStart == -1) {
                // Selection not found in paragraph, return just the selection
                val words = trimmedSelection.split(Regex("\\s+")).filter { it.isNotEmpty() }
                return TextSelectionContext(
                    selectedText = trimmedSelection,
                    selectedWords = words,
                    leadingContext = emptyList(),
                    trailingContext = emptyList(),
                    paragraphText = paragraphText,
                    selectionStartIndex = 0,
                    selectionEndIndex = words.size
                )
            }

            // Find word indices for selection boundaries
            var charIndex = 0
            var startWordIndex = 0
            var endWordIndex = paragraphWords.size

            for ((index, word) in paragraphWords.withIndex()) {
                val wordStart = paragraphText.indexOf(word, charIndex)
                val wordEnd = wordStart + word.length

                if (wordStart <= selectionStart && selectionStart < wordEnd) {
                    startWordIndex = index
                }
                if (selectionStart + trimmedSelection.length <= wordEnd) {
                    endWordIndex = index + 1
                    break
                }
                charIndex = wordEnd
            }

            // Extract selected words (auto-completed to whole words)
            val selectedWords = paragraphWords.subList(
                startWordIndex.coerceIn(0, paragraphWords.size),
                endWordIndex.coerceIn(0, paragraphWords.size)
            )

            // Extract context words
            val leadingStart = (startWordIndex - CONTEXT_WORD_COUNT).coerceAtLeast(0)
            val leadingContext = paragraphWords.subList(leadingStart, startWordIndex)

            val trailingEnd = (endWordIndex + CONTEXT_WORD_COUNT).coerceAtMost(paragraphWords.size)
            val trailingContext = paragraphWords.subList(
                endWordIndex.coerceAtMost(paragraphWords.size),
                trailingEnd
            )

            return TextSelectionContext(
                selectedText = selectedWords.joinToString(" "),
                selectedWords = selectedWords,
                leadingContext = leadingContext,
                trailingContext = trailingContext,
                paragraphText = paragraphText,
                selectionStartIndex = startWordIndex,
                selectionEndIndex = endWordIndex
            )
        }
    }

    /**
     * Expands the selection to include the word at the given direction.
     *
     * @param expandLeading If true, adds a word from leading context. If false, from trailing.
     * @return New TextSelectionContext with expanded selection, or this if no expansion possible
     */
    fun expandSelection(expandLeading: Boolean): TextSelectionContext {
        val paragraphWords = paragraphText.split(Regex("\\s+")).filter { it.isNotEmpty() }

        return if (expandLeading && leadingContext.isNotEmpty()) {
            val newStartIndex = selectionStartIndex - 1
            val newSelectedWords = listOf(paragraphWords[newStartIndex]) + selectedWords
            val newLeadingStart = (newStartIndex - CONTEXT_WORD_COUNT).coerceAtLeast(0)

            copy(
                selectedText = newSelectedWords.joinToString(" "),
                selectedWords = newSelectedWords,
                leadingContext = paragraphWords.subList(newLeadingStart, newStartIndex),
                selectionStartIndex = newStartIndex
            )
        } else if (!expandLeading && trailingContext.isNotEmpty()) {
            val newEndIndex = selectionEndIndex + 1
            val newSelectedWords = selectedWords + paragraphWords[selectionEndIndex]
            val newTrailingEnd = (newEndIndex + CONTEXT_WORD_COUNT).coerceAtMost(paragraphWords.size)

            copy(
                selectedText = newSelectedWords.joinToString(" "),
                selectedWords = newSelectedWords,
                trailingContext = paragraphWords.subList(newEndIndex, newTrailingEnd),
                selectionEndIndex = newEndIndex
            )
        } else {
            this
        }
    }
}

/**
 * Direction for expanding text selection.
 */
enum class SelectionDirection {
    LEADING,
    TRAILING
}
