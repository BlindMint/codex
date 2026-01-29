/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import android.util.Log
import us.blindmint.codex.data.parser.comic.ComicFileParser
import us.blindmint.codex.data.parser.epub.EpubFileParser
import us.blindmint.codex.data.parser.fb2.Fb2FileParser
import us.blindmint.codex.data.parser.fodt.FodtFileParser
import us.blindmint.codex.data.parser.html.HtmlFileParser
import us.blindmint.codex.data.parser.pdf.PdfFileParser
import us.blindmint.codex.data.parser.txt.TxtFileParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.BookWithCover
import javax.inject.Inject

private const val FILE_PARSER = "File Parser"

class FileParserImpl @Inject constructor(
    private val txtFileParser: TxtFileParser,
    private val pdfFileParser: PdfFileParser,
    private val epubFileParser: EpubFileParser,
    private val fb2FileParser: Fb2FileParser,
    private val htmlFileParser: HtmlFileParser,
    private val fodtFileParser: FodtFileParser,
    private val comicFileParser: ComicFileParser,
) : FileParser {

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        if (!cachedFile.canAccess()) {
            Log.e(FILE_PARSER, "File does not exist or no read access is granted.")
            return null
        }

        return when (FormatDetector.detect(cachedFile.name)) {
            FormatDetector.Format.PDF -> pdfFileParser.parse(cachedFile)
            FormatDetector.Format.EPUB -> epubFileParser.parse(cachedFile)
            FormatDetector.Format.TXT -> txtFileParser.parse(cachedFile)
            FormatDetector.Format.FB2 -> fb2FileParser.parse(cachedFile)
            FormatDetector.Format.HTML -> htmlFileParser.parse(cachedFile)
            FormatDetector.Format.FODT -> fodtFileParser.parse(cachedFile)
            FormatDetector.Format.COMIC -> comicFileParser.parse(cachedFile)
            FormatDetector.Format.UNKNOWN -> {
                Log.e(FILE_PARSER, "Wrong file format, could not find supported extension.")
                null
            }
        }
    }
}