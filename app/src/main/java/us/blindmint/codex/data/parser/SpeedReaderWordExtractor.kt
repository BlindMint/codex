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
                val lineWords = item.line.text.split("\\s+".toRegex()).filter { it.isNotBlank() }

                for (word in lineWords) {
                    words.add(
                        SpeedReaderWord(
                            text = word,
                            globalIndex = globalIndex,
                            paragraphIndex = paragraphIndex
                        )
                    )
                    globalIndex++
                }

                paragraphIndex++
            }
        }

        return words
    }
}
