/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.net.toUri
import us.blindmint.codex.data.parser.comic.ArchiveReader
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CoverExtractor"

@Singleton
class CoverExtractor @Inject constructor() {

    data class CoverResult(
        val bitmap: Bitmap,
        val fileName: String
    )

    suspend fun extractPdfPageAsCover(
        cachedFile: CachedFile,
        pageNumber: Int
    ): Bitmap? {
        return try {
            val rawFile = cachedFile.rawFile ?: return null
            val fd = ParcelFileDescriptor.open(rawFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            
            if (pageNumber < 0 || pageNumber >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return null
            }
            
            val page = renderer.openPage(pageNumber)
            val width = page.width * 2
            val height = page.height * 2
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PDF page as cover", e)
            null
        }
    }

    suspend fun extractComicPageAsCover(
        cachedFile: CachedFile,
        pageNumber: Int
    ): Bitmap? {
        return try {
            val archiveReader = us.blindmint.codex.data.parser.comic.ArchiveReader()
            archiveReader.openArchive(cachedFile).use { handle ->
                val entries = handle.entries
                    .filter { us.blindmint.codex.data.parser.comic.ArchiveReader.isImageFile(it.getPath()) }
                    .sortedBy { it.getPath() }
                
                if (pageNumber < 0 || pageNumber >= entries.size) return null
                
                val entry = entries[pageNumber]
                val inputStream = handle.getInputStream(entry)
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract comic page as cover", e)
            null
        }
    }

    fun generateTextCover(
        title: String,
        authors: List<String>,
        includeAuthor: Boolean,
        width: Int = 600,
        height: Int = 900
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.parseColor("#1a1a2e"))
        
        val titlePaint = Paint().apply {
            color = Color.parseColor("#eaeaea")
            isAntiAlias = true
            textSize = 48f
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 2f, 2f, Color.parseColor("#40000000"))
        }
        
        val authorPaint = Paint().apply {
            color = Color.parseColor("#b0b0b0")
            isAntiAlias = true
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        
        val titleBounds = Rect()
        titlePaint.getTextBounds(title, 0, title.length, titleBounds)
        
        val titleY = if (includeAuthor && authors.isNotEmpty()) {
            height / 2f - 30f
        } else {
            height / 2f
        }
        
        val maxWidth = width - 80
        val titleLines = wrapText(title, titlePaint, maxWidth)
        val lineHeight = titleBounds.height() + 20
        
        titleLines.forEachIndexed { index, line ->
            canvas.drawText(line, width / 2f, titleY + index * lineHeight, titlePaint)
        }
        
        if (includeAuthor && authors.isNotEmpty()) {
            val authorText = authors.joinToString(", ")
            val authorBounds = Rect()
            authorPaint.getTextBounds(authorText, 0, authorText.length, authorBounds)
            
            val authorLines = wrapText(authorText, authorPaint, maxWidth)
            val authorLineHeight = authorBounds.height() + 15
            val authorStartY = titleY + titleLines.size * lineHeight + 60
            
            authorLines.forEachIndexed { index, line ->
                canvas.drawText(line, width / 2f, authorStartY + index * authorLineHeight, authorPaint)
            }
        }
        
        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val bounds = Rect()
            paint.getTextBounds(testLine, 0, testLine.length, bounds)
            
            if (bounds.width() > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isEmpty()) {
                    currentLine = StringBuilder(word)
                } else {
                    currentLine.append(" ").append(word)
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines
    }

    suspend fun saveCoverToStorage(
        context: Context,
        bitmap: Bitmap,
        bookId: Int
    ): Uri? {
        return try {
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }
            
            val fileName = "generated_$bookId.jpg"
            val coverFile = File(coversDir, fileName)
            
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            coverFile.toUri()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cover to storage", e)
            null
        }
    }
}
