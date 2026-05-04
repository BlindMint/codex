/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.room

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import us.blindmint.codex.domain.library.book.BookSource

class TypeConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return if (value.trimStart().startsWith("[")) {
            runCatching {
                json.decodeFromString<List<String>>(value)
            }.getOrElse {
                parseLegacyCommaSeparatedList(value)
            }
        } else {
            parseLegacyCommaSeparatedList(value)
        }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return json.encodeToString(list ?: emptyList())
    }

    private fun parseLegacyCommaSeparatedList(value: String): List<String> {
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromBookSource(value: String?): BookSource {
        return value?.let { BookSource.valueOf(it) } ?: BookSource.LOCAL
    }

    @TypeConverter
    fun toBookSource(source: BookSource?): String {
        return source?.name ?: BookSource.LOCAL.name
    }
}
