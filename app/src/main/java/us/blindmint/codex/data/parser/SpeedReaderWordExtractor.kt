/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SpeedReaderWord

object SpeedReaderWordExtractor {

    private val CONTROL_CHARS_REGEX = Regex("[\\n\\r\\t]")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val DASH_BETWEEN_LETTERS_REGEX = Regex("([\\p{L}])([—–])([\\p{L}])")

    fun extract(readerText: List<ReaderText>): List<SpeedReaderWord> {
        val words = mutableListOf<SpeedReaderWord>()
        var globalIndex = 0
        var paragraphIndex = 0

        for (item in readerText) {
            if (item is ReaderText.Text) {
                val cleanedLine = item.line.text
                    .replace(CONTROL_CHARS_REGEX, " ")
                    .replace(WHITESPACE_REGEX, " ")
                    .trim()

                val lineWords = cleanedLine.split(" ").filter { it.isNotBlank() }

                for (rawWord in lineWords) {
                    val cleanWord = cleanWordForSpeedReader(rawWord)
                    if (cleanWord.isNotBlank()) {
                        words.add(
                            SpeedReaderWord(
                                text = cleanWord,
                                globalIndex = globalIndex,
                                paragraphIndex = paragraphIndex
                            )
                        )
                        globalIndex++
                    }
                }

                paragraphIndex++
            }
        }

        return words
    }

    fun extractWithPreprocessing(readerText: List<ReaderText>): List<SpeedReaderWord> {
        val words = mutableListOf<SpeedReaderWord>()
        var globalIndex = 0
        var paragraphIndex = 0
        
        val fullText = preprocessText(readerText)
        val wordsList = splitIntoWords(fullText)
        
        for (word in wordsList) {
            if (word.isNotBlank()) {
                val cleanWord = cleanWordForSpeedReader(word)
                if (cleanWord.isNotBlank()) {
                    words.add(
                        SpeedReaderWord(
                            text = cleanWord,
                            globalIndex = globalIndex,
                            paragraphIndex = paragraphIndex
                        )
                    )
                    globalIndex++
                    
                    if (isSentenceEnding(word)) {
                        paragraphIndex++
                    }
                }
            }
        }

        return words
    }

    private fun preprocessText(readerText: List<ReaderText>): String {
        val estimatedCapacity = readerText.sumOf { if (it is ReaderText.Text) it.line.text.length else 0 } + 1000
        // Single pass: convert control chars to spaces, collapse whitespace, join text items
        val builder = StringBuilder(estimatedCapacity)
        var inWhitespace = true // Start true to skip leading whitespace

        for (item in readerText) {
            if (item is ReaderText.Text) {
                val text = item.line.text
                // Add separator space between text items
                if (builder.isNotEmpty() && !inWhitespace) {
                    builder.append(' ')
                    inWhitespace = true
                }
                for (char in text) {
                    if (char == '\n' || char == '\r' || char == '\t' || char.isWhitespace()) {
                        if (!inWhitespace) {
                            builder.append(' ')
                            inWhitespace = true
                        }
                    } else {
                        builder.append(char)
                        inWhitespace = false
                    }
                }
            }
        }

        // Trim trailing whitespace
        while (builder.isNotEmpty() && builder[builder.lastIndex].isWhitespace()) {
            builder.deleteCharAt(builder.lastIndex)
        }

        return DASH_BETWEEN_LETTERS_REGEX.replace(builder, "$1 $2 $3")
    }

    private fun splitIntoWords(text: String): List<String> {
        return text.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    }

    private fun isSentenceEnding(word: String): Boolean {
        val trimmed = word.trim()
        return trimmed.endsWith('.') || trimmed.endsWith('!') || trimmed.endsWith('?') ||
               trimmed.endsWith(';') || trimmed.endsWith(':')
    }

    private fun cleanWordForSpeedReader(word: String): String {
        val trimmed = word.trim()

        if (trimmed.isEmpty()) return ""

        val result = buildString(trimmed.length) {
            for (char in trimmed) {
                val isLetterOrDigit = char.isLetterOrDigit()

                when {
                    isLetterOrDigit -> append(char)
                    char.isWhitespace() -> Unit
                    else -> {
                        val isPunctuation = char == '.' ||
                            char == ',' ||
                            char == ';' ||
                            char == ':' ||
                            char == '!' ||
                            char == '?' ||
                            char == '"' ||
                            char == '\'' ||
                            char == '-' ||
                            char == '—' ||
                            char == '…' ||
                            // Smart quotes (curly quotes used in books)
                            char == '\u201C' ||
                            char == '\u201D' ||
                            char == '\u2018' ||
                            char == '\u2019'

                        if (isPunctuation) append(char)
                    }
                }
            }
        }

        return result
    }
}
