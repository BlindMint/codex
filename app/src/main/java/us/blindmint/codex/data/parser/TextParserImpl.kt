/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.parser.epub.EpubTextParser
import us.blindmint.codex.data.parser.fodt.FodtTextParser
import us.blindmint.codex.data.parser.html.HtmlTextParser
import us.blindmint.codex.data.parser.pdf.PdfTextParser
import us.blindmint.codex.data.parser.txt.TxtTextParser
import us.blindmint.codex.data.parser.xml.XmlTextParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.reader.ReaderText
import javax.inject.Inject

private const val TEXT_PARSER = "Text Parser"

class TextParserImpl @Inject constructor(
    // Markdown parser (Markdown)
    private val txtTextParser: TxtTextParser,
    private val pdfTextParser: PdfTextParser,

    // Document parser (HTML+Markdown)
    private val epubTextParser: EpubTextParser,
    private val htmlTextParser: HtmlTextParser,
    private val xmlTextParser: XmlTextParser,
    private val fodtTextParser: FodtTextParser
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        if (!cachedFile.canAccess()) {
            Log.e(TEXT_PARSER, "File does not exist or no read access is granted.")
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            when (FormatDetector.detect(cachedFile.name)) {
                FormatDetector.Format.PDF -> pdfTextParser.parse(cachedFile)
                FormatDetector.Format.EPUB -> epubTextParser.parse(cachedFile)
                FormatDetector.Format.TXT -> txtTextParser.parse(cachedFile)
                FormatDetector.Format.FB2 -> xmlTextParser.parse(cachedFile)
                FormatDetector.Format.HTML -> htmlTextParser.parse(cachedFile)
                FormatDetector.Format.FODT -> fodtTextParser.parse(cachedFile)
                FormatDetector.Format.COMIC, FormatDetector.Format.UNKNOWN -> {
                    Log.e(TEXT_PARSER, "Wrong file format, could not find supported extension.")
                    emptyList()
                }
            }
        }
    }
}