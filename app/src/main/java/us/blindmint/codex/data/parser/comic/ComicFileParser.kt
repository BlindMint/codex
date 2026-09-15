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
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.BaseFileParser
import us.blindmint.codex.data.parser.BookFactory
import us.blindmint.codex.data.parser.NaturalOrderComparator
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
                    val imageEntries = archive.entries
                        .filter { ArchiveReader.isImageFile(it.getPath()) }
                    val pageCount = imageEntries.size
                    val coverImage: CoverImage? = try {
                        val firstImageEntry = findComicInfoCover(archive, imageEntries)
                            ?: imageEntries.minWithOrNull(
                                Comparator { a, b ->
                                    val priority = coverNamePriority(a.getPath())
                                        .compareTo(coverNamePriority(b.getPath()))
                                    if (priority != 0) priority
                                    else NaturalOrderComparator.compare(a.getPath(), b.getPath())
                                }
                            )

                        firstImageEntry?.let { entry ->
                            archive.getInputStream(entry)?.use { input ->
                                val options = BitmapFactory.Options().apply {
                                    inSampleSize = 4
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

    private fun coverNamePriority(path: String): Int {
        val name = path.substringAfterLast('/').substringAfterLast('\\')
            .substringBeforeLast('.')
            .lowercase()
        return when {
            name == "cover" -> 0
            name in setOf("front", "frontcover", "front_cover", "folder", "poster") -> 1
            "frontcover" in name || "front_cover" in name -> 2
            "cover" in name -> 3
            "front" in name || "folder" in name -> 4
            else -> 10
        }
    }

    private fun findComicInfoCover(
        archive: ArchiveReader.ArchiveHandle,
        imageEntries: List<ComicArchiveEntry>
    ): ComicArchiveEntry? {
        val path = archive.allEntryPaths.firstOrNull {
            it.substringAfterLast('/').substringAfterLast('\\')
                .equals("ComicInfo.xml", ignoreCase = true)
        } ?: return null
        val document = archive.getInputStream(path)?.use {
            Jsoup.parse(it, null, "", Parser.xmlParser())
        } ?: return null
        val page = document.select("Page, page").firstOrNull { element ->
            element.attributes().asList().any { attribute ->
                attribute.key.equals("Type", ignoreCase = true) &&
                    attribute.value.replace(" ", "").equals("FrontCover", ignoreCase = true)
            }
        } ?: return null
        val index = page.attributes().asList().firstOrNull {
            it.key.equals("Image", ignoreCase = true)
        }?.value?.toIntOrNull() ?: return null
        return imageEntries.getOrNull(index)
    }
}
