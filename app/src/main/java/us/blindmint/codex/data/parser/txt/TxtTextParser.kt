/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.txt

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import us.blindmint.codex.data.parser.BaseTextParser
import us.blindmint.codex.data.parser.MarkdownParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.presentation.core.util.clearAllMarkdown
import javax.inject.Inject

private const val TXT_TAG = "TXT Parser"

class TxtTextParser @Inject constructor(
    private val markdownParser: MarkdownParser
) : BaseTextParser() {

    override val tag = TXT_TAG

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        Log.i(tag, "Started TXT parsing: ${cachedFile.name}.")

        return safeParse {
            val readerText = mutableListOf<ReaderText>()
            var chapterAdded = false

            withContext(Dispatchers.IO) {
                cachedFile.openInputStream()?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        if (line.isNotBlank()) {
                            when (line) {
                                "***", "---" -> readerText.add(
                                    ReaderText.Separator
                                )

                                else -> {
                                    if (!chapterAdded && line.clearAllMarkdown().isNotBlank()) {
                                        readerText.add(
                                            0, ReaderText.Chapter(
                                                title = line.clearAllMarkdown(),
                                                nested = false
                                            )
                                        )
                                        chapterAdded = true
                                    } else readerText.add(
                                        ReaderText.Text(
                                            line = markdownParser.parse(line)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            yield()

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                Log.e(tag, "Could not extract text from TXT.")
                return emptyList()
            }

            Log.i(tag, "Successfully finished TXT parsing.")
            readerText
        }
    }
}