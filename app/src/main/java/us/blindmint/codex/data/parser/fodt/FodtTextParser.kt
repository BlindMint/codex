/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.fodt

import androidx.compose.ui.text.AnnotatedString
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import us.blindmint.codex.data.parser.BaseTextParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.reader.ReaderText
import javax.inject.Inject

/**
 * Parser for FODT (Flat OpenDocument Text) file content.
 * Extracts text from office:body > office:text section.
 */
class FodtTextParser @Inject constructor() : BaseTextParser() {

    override val tag = "FODT Parser"

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        return safeParse {
            val content = cachedFile.openInputStream()?.bufferedReader()?.use { it.readText() }
                ?: return emptyList()

            val doc = Jsoup.parse(content, "", Parser.xmlParser())
            val textContent = mutableListOf<ReaderText>()

            // Find the body content: office:body > office:text
            val bodyText = doc.select("office|body office|text, office\\:body office\\:text")
                .firstOrNull() ?: doc.select("body text").firstOrNull()

            if (bodyText == null) {
                // Fallback: try to find any text:p or text:h elements
                parseElements(doc.select("text|p, text|h, text\\:p, text\\:h"), textContent)
            } else {
                parseElements(bodyText.select("text|p, text|h, text\\:p, text\\:h"), textContent)
            }

            textContent
        }
    }

    private fun parseElements(
        elements: org.jsoup.select.Elements,
        textContent: MutableList<ReaderText>
    ) {
        for (element in elements) {
            val tagName = element.tagName().lowercase()
            val text = element.text().trim()

            if (text.isEmpty()) continue

            when {
                tagName.contains("h") -> {
                    // Heading element
                    textContent.add(ReaderText.Chapter(title = text, nested = false))
                }
                else -> {
                    // Paragraph element
                    textContent.add(ReaderText.Text(line = AnnotatedString(text)))
                }
            }
        }
    }
}
