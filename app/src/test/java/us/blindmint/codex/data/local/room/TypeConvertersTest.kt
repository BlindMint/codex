/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.room

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeConvertersTest {

    private val converters = TypeConverters()

    @Test
    fun stringListRoundTripPreservesCommasAndDuplicates() {
        val values = listOf("Last, First", "tag", "tag", "series, volume 1")

        val stored = converters.toStringList(values)
        val restored = converters.fromStringList(stored)

        assertEquals(values, restored)
    }

    @Test
    fun legacyCommaSeparatedListsRemainReadable() {
        assertEquals(
            listOf("one", "two", "three"),
            converters.fromStringList("one, two,, three")
        )
    }

    @Test
    fun nullAndEmptyListsUseEmptyJsonArray() {
        assertEquals("[]", converters.toStringList(null))
        assertEquals(emptyList<String>(), converters.fromStringList(null))
        assertEquals(emptyList<String>(), converters.fromStringList(""))
    }
}
