/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.comic

import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookSource
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.domain.util.CoverImage
import java.io.File
import javax.inject.Inject

private const val TAG = "ComicFileParser"

class ComicFileParser @Inject constructor(
    private val archiveReader: ArchiveReader
) : FileParser {

    private val supportedExtensions = setOf("cbz", "cbr", "cb7")

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        if (!isComicFile(cachedFile)) return null

        return withContext(Dispatchers.IO) {
            try {
                archiveReader.openArchive(cachedFile).use { archive ->
                    val pageCount = archive.entries.size
                    val coverImage: CoverImage? = try {
                        val firstImageEntry = archive.entries
                            .filter { !it.isDirectory() }
                            .sortedBy { it.getPath() }
                            .firstOrNull()

                        firstImageEntry?.let { entry ->
                            archive.getInputStream(entry).use { input ->
                                val options = BitmapFactory.Options().apply {
                                    inSampleSize = 4 // Downsample for cover thumbnail
                                }
                                BitmapFactory.decodeStream(input, null, options)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load cover image for ${cachedFile.name}", e)
                        null
                    }

                    val book = Book(
                        title = cachedFile.name.substringBeforeLast('.'),
                        authors = emptyList(),
                        description = null,
                        filePath = cachedFile.uri.toString(),
                        coverImage = null,
                        scrollIndex = 0,
                        scrollOffset = 0,
                        progress = 0f,
                        lastOpened = null,
                        // Comic fields
                        isComic = true,
                        pageCount = pageCount
                    )

                    BookWithCover(book, coverImage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse comic file: ${cachedFile.name}", e)
                null
            }
        }
    }

    private fun isComicFile(cachedFile: CachedFile): Boolean {
        val extension = cachedFile.name.substringAfterLast('.').lowercase()
        return extension in supportedExtensions
    }
}