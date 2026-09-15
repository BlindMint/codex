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
import org.jsoup.parser.Parser
import us.blindmint.codex.R
import us.blindmint.codex.data.parser.BaseFileParser
import us.blindmint.codex.data.parser.BookFactory
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.ui.UIText
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import javax.inject.Inject

class EpubFileParser @Inject constructor() : BaseFileParser() {

    override val tag = "EPUB_PARSER"

    override suspend fun parse(cachedFile: CachedFile): BookWithCover? {
        android.util.Log.d(tag, "Parsing EPUB file: ${cachedFile.name}")
        return safeParse {
            var book: BookWithCover? = null

            // Try to get the raw file first, but fall back to creating from path if needed
            var rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) {
                android.util.Log.d(tag, "CachedFile.rawFile is null/inaccessible, trying path: ${cachedFile.path}")
                // Try to create file from path
                if (cachedFile.path.isNotEmpty()) {
                    val fileFromPath = File(cachedFile.path)
                    if (fileFromPath.exists() && fileFromPath.canRead()) {
                        rawFile = fileFromPath
                        android.util.Log.d(tag, "Successfully created file from path: ${rawFile.absolutePath}")
                    }
                }
            }

            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) {
                android.util.Log.e(tag, "File does not exist or cannot be read: ${cachedFile.name} (path: ${cachedFile.path})")
                return null
            }

            withContext(Dispatchers.IO) {
                ZipFile(rawFile).use { zip ->
                    android.util.Log.d(tag, "Opened ZIP file, looking for OPF entries")
                    val opfFiles = zip.entries().asSequence().filter { entry ->
                        entry.name.endsWith(".opf", ignoreCase = true)
                    }.toList()
                    android.util.Log.d(tag, "Found ${opfFiles.size} OPF files: ${opfFiles.map { it.name }}")

                    val declaredOpfPath = zip.getEntry("META-INF/container.xml")?.let { containerEntry ->
                        zip.getInputStream(containerEntry).use { input ->
                            Jsoup.parse(input, null, "", Parser.xmlParser())
                                .selectFirst("rootfile")
                                ?.attr("full-path")
                        }
                    }?.takeIf { it.isNotBlank() }
                    val opfEntry = opfFiles.firstOrNull {
                        it.name.equals(declaredOpfPath, ignoreCase = true)
                    } ?: opfFiles.firstOrNull() ?: run {
                        android.util.Log.e(tag, "No OPF file found in EPUB")
                        return@withContext
                    }

                    android.util.Log.d(tag, "Using OPF file: ${opfEntry.name}")
                    val opfContent = zip
                        .getInputStream(opfEntry)
                        .bufferedReader()
                        .use { it.readText() }
                    android.util.Log.d(tag, "OPF content length: ${opfContent.length}")
                    val document = Jsoup.parse(opfContent)

                    val title = document.select("metadata > dc|title").text().trim().run {
                        ifBlank {
                            cachedFile.name.substringBeforeLast(".").trim()
                        }
                    }

                    val authors = document.select("metadata > dc|creator").text().trim().run {
                        if (isBlank()) {
                            emptyList()
                        } else {
                            listOf(this)
                        }
                    }

                    val description = Jsoup.parse(
                        document.select("metadata > dc|description").text()
                    ).text().run {
                        ifBlank {
                            null
                        }
                    }

                    val manifestItems = document.select("manifest > item")
                    val coverId = document.selectFirst("metadata > meta[name=cover]")
                        ?.attr("content")
                        ?.trim()
                    val coverImage = manifestItems.firstOrNull { item ->
                        item.attr("properties").split(Regex("\\s+")).any {
                            it.equals("cover-image", ignoreCase = true)
                        }
                    }?.attr("href")?.takeIf { it.isNotBlank() }
                        ?: manifestItems.firstOrNull { item ->
                            coverId != null && item.attr("id") == coverId
                        }?.attr("href")?.takeIf { it.isNotBlank() }
                        ?: manifestItems.firstOrNull { item ->
                            item.attr("media-type").startsWith("image/")
                        }?.attr("href")?.takeIf { it.isNotBlank() }
                        ?: document.selectFirst("guide > reference[type=cover]")
                            ?.attr("href")?.takeIf { it.isNotBlank() }
                        ?: manifestItems.firstOrNull { item ->
                            val id = item.attr("id").lowercase()
                            val href = item.attr("href").lowercase()
                            item.attr("media-type").startsWith("image/") &&
                                (id.contains("cover") || href.contains("cover") || href.contains("front"))
                        }?.attr("href")?.takeIf { it.isNotBlank() }

                    book = BookFactory.createWithDefaults(
                        title = title,
                        authors = authors,
                        description = description,
                        filePath = cachedFile.uri.toString(),
                        category = Category.entries[0],
                        coverImage = extractCoverImageBitmap(rawFile, opfEntry.name, coverImage)
                    )
                }
            }
            book?.let {
                android.util.Log.d(tag, "Successfully parsed EPUB: ${it.book.title}")
            } ?: android.util.Log.e(tag, "EPUB parsing returned null")
            book
        }
    }

    private fun extractCoverImageBitmap(
        file: File,
        opfPath: String,
        coverImagePath: String?
    ): Bitmap? {
        if (coverImagePath.isNullOrBlank()) {
            return null
        }

        val decodedPath = URLDecoder.decode(
            coverImagePath.substringBefore('#').replace("+", "%2B"),
            StandardCharsets.UTF_8.name()
        )
        val opfDirectory = opfPath.substringBeforeLast('/', "")
        val resolvedPath = listOf(opfDirectory, decodedPath)
            .filter { it.isNotBlank() }
            .joinToString("/")
            .split('/')
            .fold(mutableListOf<String>()) { parts, part ->
                when (part) {
                    "", "." -> Unit
                    ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                    else -> parts.add(part)
                }
                parts
            }
            .joinToString("/")

        ZipFile(file).use { zip ->
            val entry = zip.entries().asSequence().firstOrNull {
                it.name.equals(resolvedPath, ignoreCase = true)
            } ?: return null
            val imageBytes = zip.getInputStream(entry).use { it.readBytes() }
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }
}
