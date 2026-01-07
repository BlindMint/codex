/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import us.blindmint.codex.data.remote.OpdsApiService
import us.blindmint.codex.data.remote.dto.OpdsFeedDto
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.opds.OpdsFeed
import us.blindmint.codex.domain.opds.OpdsLink
import us.blindmint.codex.domain.repository.OpdsRepository
import java.util.Base64
import javax.inject.Inject

class OpdsRepositoryImpl @Inject constructor() : OpdsRepository {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://placeholder.com/") // Not used since @Url
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .client(createOkHttpClient())
        .build()

    private val apiService: OpdsApiService = retrofit.create(OpdsApiService::class.java)

    override suspend fun fetchFeed(url: String, username: String?, password: String?): OpdsFeed {
        val client = createOkHttpClient(username, password)
        val retrofitWithAuth = retrofit.newBuilder().client(client).build()
        val service = retrofitWithAuth.create(OpdsApiService::class.java)
        val dto = service.getFeed(url)
        return mapToDomain(dto)
    }

    override suspend fun search(url: String, query: String, username: String?, password: String?): OpdsFeed {
        val client = createOkHttpClient(username, password)
        val retrofitWithAuth = retrofit.newBuilder().client(client).build()
        val service = retrofitWithAuth.create(OpdsApiService::class.java)
        val dto = service.searchFeed(url, query)
        return mapToDomain(dto)
    }

    override suspend fun downloadBook(url: String, username: String?, password: String?): ByteArray {
        val client = createOkHttpClient(username, password)
        val request = okhttp3.Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Download failed: ${response.code}")
        }
        return response.body?.bytes() ?: throw Exception("Empty response")
    }

    private fun createOkHttpClient(username: String? = null, password: String? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })

        if (username != null && password != null) {
            val credentials = Credentials.basic(username, password)
            builder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", credentials)
                    .build()
                chain.proceed(request)
            }
        }

        return builder.build()
    }

    private fun mapToDomain(dto: OpdsFeedDto): OpdsFeed {
        return OpdsFeed(
            title = dto.title,
            entries = dto.entries.map { mapEntryToDomain(it) },
            links = dto.links.map { OpdsLink(it.href, it.rel, it.type, it.title) }
        )
    }

    private fun mapEntryToDomain(dto: us.blindmint.codex.data.remote.dto.OpdsEntryDto): OpdsEntry {
        return OpdsEntry(
            id = dto.id,
            title = dto.title,
            author = dto.author, // Now properly parsed from nested author/name element
            summary = dto.summary,
            published = dto.published,
            language = dto.language,
            publisher = dto.publisher,
            rights = dto.rights,
            identifiers = dto.identifiers,
            categories = dto.categories.map { it.term },
            links = dto.links.map { OpdsLink(it.href, it.rel, it.type, it.title) }
        )
    }
}