/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader.viewer.webtoon

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WebtoonLayoutManager(
    context: Context,
    private val extraLayoutSpace: Int
) : LinearLayoutManager(context) {

    init {
        isItemPrefetchEnabled = false
    }

    @Deprecated("Deprecated in Java")
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        return extraLayoutSpace
    }
}
