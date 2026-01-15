/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.repository

import us.blindmint.codex.domain.opds.OpdsFeed

interface OpdsRepository {

    suspend fun fetchFeed(url: String, username: String? = null, password: String? = null): OpdsFeed

    suspend fun loadMore(url: String, username: String? = null, password: String? = null): OpdsFeed

    suspend fun search(feed: OpdsFeed, query: String, username: String? = null, password: String? = null): OpdsFeed

    suspend fun downloadBook(url: String, username: String? = null, password: String? = null, onProgress: ((Float) -> Unit)? = null): Pair<ByteArray, String?>

    suspend fun downloadCover(url: String, username: String? = null, password: String? = null): ByteArray?
}