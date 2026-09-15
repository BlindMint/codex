/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import us.blindmint.codex.domain.file.CachedFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CoverExtractor"

@Singleton
class CoverExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class CoverResult(
        val bitmap: Bitmap,
        val fileName: String
    )

    suspend fun extractPdfPageAsCover(
        cachedFile: CachedFile,
        pageNumber: Int,
        maxWidth: Int = 800,
        maxHeight: Int = 1200
    ): Bitmap? {
        return try {
            val rawFile = cachedFile.rawFile ?: return null
            ParcelFileDescriptor.open(rawFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (pageNumber < 0 || pageNumber >= renderer.pageCount) return null

                    renderer.openPage(pageNumber).use { page ->
                        val scale = minOf(
                            maxWidth.toFloat() / page.width,
                            maxHeight.toFloat() / page.height,
                            1f
                        )
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(
                            bitmap,
                            null,
                            Matrix().apply { setScale(scale, scale) },
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PDF page as cover", e)
            null
        }
    }

    fun resizeForStorage(
        bitmap: Bitmap,
        maxWidth: Int = 800,
        maxHeight: Int = 1200
    ): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) return bitmap

        val scale = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    fun extractSidecarCover(cachedFile: CachedFile): Bitmap? {
        return try {
            when (cachedFile.uri.scheme) {
                "file" -> cachedFile.uri.path?.let(::File)?.let(::findFileSidecar)
                null, "" -> File(cachedFile.path).let(::findFileSidecar)
                "content" -> findDocumentSidecar(cachedFile)
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to locate sidecar cover for ${cachedFile.name}", e)
            null
        }
    }

    private fun findFileSidecar(bookFile: File): Bitmap? {
        val bookBase = bookFile.nameWithoutExtension.lowercase()
        val candidate = bookFile.parentFile?.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && isImageName(it.name) }
            ?.map { it to sidecarPriority(it.name, bookBase) }
            ?.filter { it.second < Int.MAX_VALUE }
            ?.sortedWith(compareBy<Pair<File, Int>> { it.second }.thenBy { it.first.name.lowercase() })
            ?.firstOrNull()
            ?.first
            ?: return null
        return decodeBounded { candidate.inputStream() }
    }

    private fun findDocumentSidecar(cachedFile: CachedFile): Bitmap? {
        if (!DocumentsContract.isDocumentUri(context, cachedFile.uri)) return null
        val documentId = DocumentsContract.getDocumentId(cachedFile.uri)
        val separator = documentId.lastIndexOf('/')
        if (separator < 0) return null
        val parentId = documentId.substring(0, separator)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(cachedFile.uri, parentId)
        val bookBase = cachedFile.name.substringBeforeLast('.').lowercase()
        val candidates = mutableListOf<Triple<Int, String, String>>()

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn) ?: continue
                val mime = cursor.getString(mimeColumn).orEmpty()
                if (!mime.startsWith("image/") && !isImageName(name)) continue
                val priority = sidecarPriority(name, bookBase)
                if (priority < Int.MAX_VALUE) {
                    candidates += Triple(priority, name.lowercase(), cursor.getString(idColumn))
                }
            }
        }

        val document = candidates.minWithOrNull(
            compareBy<Triple<Int, String, String>> { it.first }.thenBy { it.second }
        ) ?: return null
        val uri = DocumentsContract.buildDocumentUriUsingTree(cachedFile.uri, document.third)
        return decodeBounded { context.contentResolver.openInputStream(uri) }
    }

    private fun sidecarPriority(fileName: String, bookBase: String): Int {
        val name = fileName.substringBeforeLast('.').lowercase()
        return when {
            name == bookBase -> 0
            name in setOf("cover", "folder", "front", "frontcover", "front_cover", "poster") -> 1
            "cover" in name -> 2
            "front" in name || "folder" in name -> 3
            else -> Int.MAX_VALUE
        }
    }

    private fun isImageName(name: String): Boolean {
        return name.substringAfterLast('.', "").lowercase() in
            setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }

    private fun decodeBounded(
        maxWidth: Int = 1600,
        maxHeight: Int = 2400,
        openStream: () -> InputStream?
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxWidth || bounds.outHeight / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return openStream()?.use { BitmapFactory.decodeStream(it, null, options) }
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
