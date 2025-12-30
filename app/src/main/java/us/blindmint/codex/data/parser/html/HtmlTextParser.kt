/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.html

import android.util.Log
import kotlinx.coroutines.yield
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import us.blindmint.codex.data.parser.DocumentParser
import us.blindmint.codex.data.parser.TextParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.reader.ReaderText
import javax.inject.Inject

private const val HTML_TAG = "HTML Parser"

class HtmlTextParser @Inject constructor(
    private val documentParser: DocumentParser
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        Log.i(HTML_TAG, "Started HTML parsing: ${cachedFile.name}.")

        return try {
            val readerText = cachedFile.openInputStream()?.use { stream ->
                documentParser.parseDocument(Jsoup.parse(stream, null, "", Parser.htmlParser()))
            }

            yield()

            if (
                readerText.isNullOrEmpty() ||
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                Log.e(HTML_TAG, "Could not extract text from HTML.")
                return emptyList()
            }

            Log.i(HTML_TAG, "Successfully finished HTML parsing.")
            readerText
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}