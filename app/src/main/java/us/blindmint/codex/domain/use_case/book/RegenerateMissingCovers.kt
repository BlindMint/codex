/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.repository.BookRepository
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class CoverRegenerationProgress(
    val processed: Int,
    val total: Int,
    val updated: Int,
    val failed: Int,
    val currentBook: String
)

data class CoverRegenerationResult(
    val checked: Int,
    val updated: Int,
    val failed: Int
)

class RegenerateMissingCovers @Inject constructor(
    private val bookRepository: BookRepository,
    private val fileParser: FileParser,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(
        onProgress: (CoverRegenerationProgress) -> Unit = {}
    ): CoverRegenerationResult = withContext(Dispatchers.IO) {
        val candidates = bookRepository.getBooks("").filterNot(::hasUsableCover)
        var updated = 0
        var failed = 0

        candidates.forEachIndexed { index, book ->
            coroutineContext.ensureActive()
            val generated = try {
                val cachedFile = CachedFileFactory.fromBook(context, book)
                val parsed = cachedFile?.let { fileParser.parse(it) }
                val cover = parsed?.coverImage
                if (cover == null) {
                    false
                } else {
                    try {
                        bookRepository.replaceCover(book.id, cover) != null
                    } finally {
                        if (!cover.isRecycled) cover.recycle()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }

            if (generated) updated++ else failed++
            onProgress(
                CoverRegenerationProgress(
                    processed = index + 1,
                    total = candidates.size,
                    updated = updated,
                    failed = failed,
                    currentBook = book.title
                )
            )
        }

        CoverRegenerationResult(
            checked = candidates.size,
            updated = updated,
            failed = failed
        )
    }

    private fun hasUsableCover(book: Book): Boolean {
        val cover = book.coverImage ?: return false
        return when (cover.scheme) {
            "file" -> cover.path?.let(::File)?.let { it.isFile && it.length() > 0 } == true
            "content" -> runCatching {
                context.contentResolver.openAssetFileDescriptor(cover, "r")?.use {
                    it.length != 0L
                } == true
            }.getOrDefault(false)
            null, "" -> File(context.filesDir, "covers/${cover}").let {
                it.isFile && it.length() > 0
            }
            else -> false
        }
    }
}
