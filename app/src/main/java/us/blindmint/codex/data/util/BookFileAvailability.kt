/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import us.blindmint.codex.domain.library.book.Book
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookFileAvailability @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isAvailable(book: Book): Boolean {
        val file = CachedFileFactory.fromBook(context, book) ?: return false
        return !file.isDirectory && file.canAccess()
    }
}
