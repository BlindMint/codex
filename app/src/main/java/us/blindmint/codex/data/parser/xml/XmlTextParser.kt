/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.xml

import android.util.Log
import kotlinx.coroutines.yield
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import us.blindmint.codex.data.parser.BaseTextParser
import us.blindmint.codex.data.parser.DocumentParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.reader.ReaderText
import javax.inject.Inject

private const val XML_TAG = "XML Parser"

class XmlTextParser @Inject constructor(
    private val documentParser: DocumentParser
) : BaseTextParser() {

    override val tag = XML_TAG

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        Log.i(tag, "Started XML parsing: ${cachedFile.name}.")

        return safeParse {
            val readerText = cachedFile.openInputStream()?.use { stream ->
                documentParser.parseDocument(Jsoup.parse(stream, null, "", Parser.xmlParser()))
            }

            yield()

            if (
                readerText.isNullOrEmpty() ||
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                Log.e(tag, "Could not extract text from XML.")
                return emptyList()
            }

            Log.i(tag, "Successfully finished XML parsing.")
            readerText
        }
    }
}