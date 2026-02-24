/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.pdf

import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.reader.SearchResult
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PdfSearchHelper"

@Singleton
class PdfSearchHelper @Inject constructor() {

    data class PageText(
        val pageNumber: Int,
        val text: String
    )

    suspend fun extractAllPagesText(cachedFile: CachedFile): List<PageText> {
        return try {
            val pages = mutableListOf<PageText>()
            PDDocument.load(cachedFile.openInputStream()).use { document ->
                val stripper = PDFTextStripper()
                stripper.paragraphStart = "\n"
                
                val totalPages = document.numberOfPages
                
                for (pageNum in 1..totalPages) {
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    val pageText = stripper.getText(document)
                        .replace("\r", "")
                        .trim()
                    
                    if (pageText.isNotEmpty()) {
                        pages.add(PageText(pageNumber = pageNum, text = pageText))
                    }
                }
            }
            pages
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PDF text for search", e)
            emptyList()
        }
    }

    fun searchInPages(
        pagesText: List<PageText>,
        query: String
    ): List<SearchResult> {
        if (query.isBlank() || pagesText.isEmpty()) return emptyList()
        
        val results = mutableListOf<SearchResult>()
        val queryLower = query.lowercase()
        
        pagesText.forEach { pageText ->
            val textLower = pageText.text.lowercase()
            var searchIndex = 0
            
            while (true) {
                val foundIndex = textLower.indexOf(queryLower, searchIndex)
                if (foundIndex == -1) break
                
                val (matchedText, beforeContext, afterContext) = extractContext(
                    pageText.text, 
                    foundIndex, 
                    query.length
                )
                
                results.add(
                    SearchResult(
                        textIndex = foundIndex,
                        fullText = pageText.text,
                        matchedText = matchedText,
                        beforeContext = beforeContext,
                        afterContext = afterContext,
                        pageNumber = pageText.pageNumber
                    )
                )
                
                searchIndex = foundIndex + 1
            }
        }
        
        return results
    }

    private fun extractContext(
        text: String, 
        queryIndex: Int, 
        queryLength: Int
    ): Triple<String, String, String> {
        val matchedText = text.substring(
            queryIndex,
            (queryIndex + queryLength).coerceAtMost(text.length)
        )

        val beforeStart = (queryIndex - 25).coerceAtLeast(0)
        val beforeContext = text.substring(beforeStart, queryIndex).trimStart()

        val afterEnd = (queryIndex + queryLength + 25).coerceAtMost(text.length)
        val afterContext = text.substring(queryIndex + queryLength, afterEnd).trimEnd()

        return Triple(matchedText, beforeContext, afterContext)
    }
}
