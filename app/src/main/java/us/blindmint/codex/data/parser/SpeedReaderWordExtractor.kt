/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SpeedReaderWord

object SpeedReaderWordExtractor {
    
    fun extract(readerText: List<ReaderText>): List<SpeedReaderWord> {
        val words = mutableListOf<SpeedReaderWord>()
        var globalIndex = 0
        var paragraphIndex = 0

        for (item in readerText) {
            if (item is ReaderText.Text) {
                val cleanedLine = item.line.text
                    .replace(Regex("[\\n\\r\\t]"), " ")
                    .replace(Regex("\\s+"), " ")
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
        val builder = StringBuilder(estimatedCapacity)

        var lastWasNonWhitespace = false

        for (item in readerText) {
            if (item is ReaderText.Text) {
                var text = item.line.text

                val processedText = buildString {
                    for (char in text) {
                        when {
                            char == '\n' || char == '\r' || char == '\t' -> append(' ')
                            else -> append(char)
                        }
                    }
                }.trim()

                if (processedText.isNotEmpty()) {
                    if (lastWasNonWhitespace) {
                        builder.append(' ')
                    }
                    builder.append(processedText)
                    lastWasNonWhitespace = true
                }
            }
        }

        val result = builder.toString()

        val collapsed = buildString {
            var inWhitespace = false
            for (char in result) {
                if (char.isWhitespace()) {
                    if (!inWhitespace) {
                        append(' ')
                        inWhitespace = true
                    }
                } else {
                    append(char)
                    inWhitespace = false
                }
            }
        }.trim()

        return collapsed.replace(Regex("([\\p{L}])([—–])([\\p{L}])"), "$1 $2 $3")
    }

    private fun splitIntoWords(text: String): List<String> {
        return text.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotBlank() }
    }

    private fun isSentenceEnding(word: String): Boolean {
        val trimmed = word.trim()
        return trimmed.endsWith('.') || trimmed.endsWith('!') || trimmed.endsWith('?') ||
               trimmed.endsWith(';') || trimmed.endsWith(':')
    }

    private fun cleanWordForSpeedReader(word: String): String {
        val trimmed = word.trim()
        val result = buildString(trimmed.length) {
            for (char in trimmed) {
                val allowed = char.isLetterOrDigit() ||
                            char == '.' ||
                            char == ',' ||
                            char == ';' ||
                            char == ':' ||
                            char == '!' ||
                            char == '?' ||
                            char == '"' ||
                            char == '\'' ||
                            char == '-' ||
                            char == '—' ||
                            char == '…'

                if (allowed) {
                    append(char)
                }
            }
        }

        return result
    }
}
