/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.epub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.ui.UIText
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

class EpubFileParser @Inject constructor() : FileParser {

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        android.util.Log.d("EPUB_PARSER", "Parsing EPUB file: ${cachedFile.name}")
        return try {
            var book: BookWithCover? = null

            // Try to get the raw file first, but fall back to creating from path if needed
            var rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) {
                android.util.Log.d("EPUB_PARSER", "CachedFile.rawFile is null/inaccessible, trying path: ${cachedFile.path}")
                // Try to create file from path
                if (cachedFile.path.isNotEmpty()) {
                    val fileFromPath = java.io.File(cachedFile.path)
                    if (fileFromPath.exists() && fileFromPath.canRead()) {
                        rawFile = fileFromPath
                        android.util.Log.d("EPUB_PARSER", "Successfully created file from path: ${rawFile.absolutePath}")
                    }
                }
            }

            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) {
                android.util.Log.e("EPUB_PARSER", "File does not exist or cannot be read: ${cachedFile.name} (path: ${cachedFile.path})")
                return null
            }

            withContext(Dispatchers.IO) {
                ZipFile(rawFile).use { zip ->
                    android.util.Log.d("EPUB_PARSER", "Opened ZIP file, looking for OPF entries")
                    val opfFiles = zip.entries().asSequence().filter { entry ->
                        entry.name.endsWith(".opf", ignoreCase = true)
                    }.toList()
                    android.util.Log.d("EPUB_PARSER", "Found ${opfFiles.size} OPF files: ${opfFiles.map { it.name }}")

                    val opfEntry = opfFiles.firstOrNull() ?: run {
                        android.util.Log.e("EPUB_PARSER", "No OPF file found in EPUB")
                        return@withContext
                    }

                    android.util.Log.d("EPUB_PARSER", "Using OPF file: ${opfEntry.name}")
                    val opfContent = zip
                        .getInputStream(opfEntry)
                        .bufferedReader()
                        .use { it.readText() }
                    android.util.Log.d("EPUB_PARSER", "OPF content length: ${opfContent.length}")
                    val document = Jsoup.parse(opfContent)

                    val title = document.select("metadata > dc|title").text().trim().run {
                        ifBlank {
                            cachedFile.name.substringBeforeLast(".").trim()
                        }
                    }

                    val author = document.select("metadata > dc|creator").text().trim().run {
                        if (isBlank()) {
                            UIText.StringResource(R.string.unknown_author)
                        } else {
                            UIText.StringValue(this)
                        }
                    }

                    val description = Jsoup.parse(
                        document.select("metadata > dc|description").text()
                    ).text().run {
                        ifBlank {
                            null
                        }
                    }

                    val coverImage = document
                        .select("metadata > meta[name=cover]")
                        .attr("content")
                        .run {
                            if (isNotBlank()) {
                                document
                                    .select("manifest > item[id=$this]")
                                    .attr("href")
                                    .apply { if (isNotBlank()) return@run this }
                            }

                            document
                                .select("manifest > item[media-type*=image]")
                                .firstOrNull()?.attr("href")
                        }

                    book = BookWithCover(
                        book = Book(
                            title = title,
                            author = author,
                            description = description,
                            scrollIndex = 0,
                            scrollOffset = 0,
                            progress = 0f,
                            filePath = cachedFile.path,
                            lastOpened = null,
                            category = Category.entries[0],
                            coverImage = null
                        ),
                        coverImage = extractCoverImageBitmap(rawFile, coverImage)
                    )
                }
            }
            book?.let {
                android.util.Log.d("EPUB_PARSER", "Successfully parsed EPUB: ${it.book.title}")
            } ?: android.util.Log.e("EPUB_PARSER", "EPUB parsing returned null")
            book
        } catch (e: Exception) {
            android.util.Log.e("EPUB_PARSER", "Exception parsing EPUB: ${e.message}", e)
            null
        }
    }

    private fun extractCoverImageBitmap(file: File, coverImagePath: String?): Bitmap? {
        if (coverImagePath.isNullOrBlank()) {
            return null
        }

        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.name.endsWith(coverImagePath)) {
                    val imageBytes = zip.getInputStream(entry).readBytes()
                    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }
            }
        }

        return null
    }
}