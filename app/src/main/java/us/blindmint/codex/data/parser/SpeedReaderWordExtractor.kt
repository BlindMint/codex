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

    private fun cleanWordForSpeedReader(word: String): String {
        return word
            .trim()
            .replace(Regex("[^\\w.,;:!?\"'\\-]"), "")
            .replace(Regex("\\s+"), "")
    }
}
