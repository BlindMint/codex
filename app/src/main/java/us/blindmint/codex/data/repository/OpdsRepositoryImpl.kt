/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun loadMore(url: String, username: String?, password: String?): OpdsFeed {
        return fetchFeed(url, username, password)
    }

    override suspend fun search(url: String, query: String, username: String?, password: String?): OpdsFeed {
        val client = createOkHttpClient(username, password)
        val retrofitWithAuth = retrofit.newBuilder().client(client).build()
        val service = retrofitWithAuth.create(OpdsApiService::class.java)
        val dto = service.searchFeed(url, query)
        return mapToDomain(dto)
    }

    override suspend fun downloadBook(url: String, username: String?, password: String?, onProgress: ((Float) -> Unit)?): Pair<ByteArray, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val client = createOkHttpClient(username, password)
                val request = okhttp3.Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("Download failed: HTTP ${response.code} ${response.message}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val buffer = okio.Buffer()

                var bytesRead = 0L
                val source = body.source()

                while (true) {
                    val read = source.read(buffer, 8192)
                    if (read == -1L) break
                    bytesRead += read

                    if (contentLength > 0 && onProgress != null) {
                        val progress = bytesRead.toFloat() / contentLength.toFloat()
                        onProgress(progress)
                    }
                }

                val result = buffer.readByteArray()

                // Extract filename from content-disposition header
                val contentDisposition = response.header("content-disposition")
                val filename = extractFilenameFromContentDisposition(contentDisposition)

                Pair(result, filename)
            } catch (e: Exception) {
                when (e) {
                    is java.net.UnknownHostException -> throw Exception("Unknown host: ${e.message}")
                    is java.net.SocketTimeoutException -> throw Exception("Connection timeout")
                    is javax.net.ssl.SSLException -> throw Exception("SSL error: ${e.message}")
                    is java.io.IOException -> throw Exception("Network error: ${e.message ?: "I/O error"}")
                    else -> throw Exception("Download failed: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    private fun extractFilenameFromContentDisposition(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null

        // Look for filename= or filename*= patterns
        val filenameRegex = Regex("filename[^;=\n]*=((['\"]).*?\\2|[^;\\n]*)")
        val match = filenameRegex.find(contentDisposition)
        return match?.groupValues?.get(1)?.trim()?.removeSurrounding("\"", "\"")?.removeSurrounding("'", "'")
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