/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import us.blindmint.codex.domain.library.book.BookSource

class TypeConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
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