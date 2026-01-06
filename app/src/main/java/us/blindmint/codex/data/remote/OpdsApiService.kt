/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import us.blindmint.codex.data.remote.dto.OpdsFeedDto

interface OpdsApiService {

    @GET
    suspend fun getFeed(@Url url: String): OpdsFeedDto

    @GET
    suspend fun searchFeed(@Url url: String, @Query("q") query: String): OpdsFeedDto
}