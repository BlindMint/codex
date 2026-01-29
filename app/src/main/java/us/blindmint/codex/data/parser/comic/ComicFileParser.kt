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
import us.blindmint.codex.data.parser.BaseFileParser
import us.blindmint.codex.data.parser.BookFactory
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.domain.util.CoverImage
import java.io.File
import javax.inject.Inject

private const val TAG = "ComicFileParser"

class ComicFileParser @Inject constructor(
    private val archiveReader: ArchiveReader
) : BaseFileParser() {

    override val tag = TAG

    private val supportedExtensions = setOf("cbz", "cbr", "cb7")

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        if (!isComicFile(cachedFile)) return null

        return withContext(Dispatchers.IO) {
            safeParse {
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
                        Log.w(tag, "Failed to load cover image for ${cachedFile.name}", e)
                        null
                    }

                    BookFactory.createComic(
                        title = cachedFile.name.substringBeforeLast('.'),
                        filePath = cachedFile.uri.toString(),
                        pageCount = pageCount,
                        coverImage = coverImage
                    )
                }
            }
        }
    }

    private fun isComicFile(cachedFile: CachedFile): Boolean {
        val extension = cachedFile.name.substringAfterLast('.').lowercase()
        return extension in supportedExtensions
    }
}