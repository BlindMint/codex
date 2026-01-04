/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.pdf

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.buildAnnotatedString
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.yield
import us.blindmint.codex.data.parser.MarkdownParser
import us.blindmint.codex.data.parser.TextParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.presentation.core.util.clearAllMarkdown
import javax.inject.Inject

private const val PDF_TAG = "PDF Parser"

class PdfTextParser @Inject constructor(
    private val markdownParser: MarkdownParser,
    private val application: Application
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        Log.i(PDF_TAG, "Started PDF parsing: ${cachedFile.name}.")

        return try {
            yield()

            PDFBoxResourceLoader.init(application)

            yield()

            val oldText: String

            val pdfStripper = PDFTextStripper()
            pdfStripper.paragraphStart = "</br>"

            PDDocument.load(cachedFile.openInputStream()).use {
                oldText = pdfStripper.getText(it)
                    .replace("\r", "")
            }

            yield()

            val readerText = mutableListOf<ReaderText>()

            // Optimized text processing for PDFs - remove excessive yields and simplify processing
            // For PDFs, we can be more aggressive about line breaks and skip complex joining logic
            val text = oldText.replace("\\s+".toRegex(), " ") // Simple space normalization

            // Split into paragraphs and filter empty lines
            val paragraphs = text.split(pdfStripper.paragraphStart.toRegex())
                .flatMap { it.split("\n") }
                .map { it.trim() }
                .filter { it.isNotBlank() }

            var chapterAdded = false
            paragraphs.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    when (paragraph) {
                        "***", "---" -> readerText.add(ReaderText.Separator)
                        else -> {
                            if (!chapterAdded && paragraph.clearAllMarkdown().isNotBlank()) {
                                readerText.add(
                                    0, ReaderText.Chapter(
                                        title = paragraph.clearAllMarkdown(),
                                        nested = false
                                    )
                                )
                                chapterAdded = true
                            } else {
                                // For PDFs, skip expensive markdown parsing as PDF text rarely contains markdown
                                // Just use the text as-is for better performance
                                readerText.add(
                                    ReaderText.Text(
                                        line = buildAnnotatedString { append(paragraph) }
                                    )
                                )
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
                Log.e(PDF_TAG, "Could not extract text from PDF.")
                return emptyList()
            }

            Log.i(PDF_TAG, "Successfully finished PDF parsing.")
            readerText
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}