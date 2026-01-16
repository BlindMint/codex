/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import us.blindmint.codex.data.remote.OpdsApiService
import us.blindmint.codex.data.remote.dto.OpdsFeedDto
import us.blindmint.codex.data.remote.dto.opds2.Opds2FeedDto
import us.blindmint.codex.data.remote.dto.opds2.Opds2PublicationDto
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

    // JSON parser configured to be lenient for various OPDS v2 implementations
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Fetches an OPDS feed, automatically detecting whether it's v1 (XML) or v2 (JSON).
     */
    override suspend fun fetchFeed(url: String, username: String?, password: String?): OpdsFeed {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("OPDS_DEBUG", "Fetching OPDS feed from URL: $url")

            val client = createOkHttpClient(username, password)
            val request = okhttp3.Request.Builder()
                .url(encodeUrl(url))
                .build()

            val response = client.newCall(request).execute()
            android.util.Log.d("OPDS_DEBUG", "HTTP Response: ${response.code} ${response.message}")
            android.util.Log.d("OPDS_DEBUG", "Content-Type: ${response.header("Content-Type")}")

            if (!response.isSuccessful) {
                throw Exception("Failed to fetch feed: HTTP ${response.code} ${response.message}")
            }

            val contentType = response.header("Content-Type") ?: ""
            val body = response.body?.string() ?: throw Exception("Empty response body")

            // Log first 500 chars of response for debugging
            val preview = body.take(500).replace("\n", " ").replace("\r", " ")
            android.util.Log.d("OPDS_DEBUG", "Response preview: $preview${if (body.length > 500) "..." else ""}")

            // Detect feed format based on content-type or content inspection
            when {
                isOpdsV2ContentType(contentType) || isOpdsV2Content(body) -> {
                    android.util.Log.d("OPDS_DEBUG", "Detected OPDS v2 format")
                    parseOpdsV2(body)
                }
                else -> {
                    android.util.Log.d("OPDS_DEBUG", "Detected OPDS v1 format (default)")
                    // Default to OPDS v1 (XML/Atom)
                    parseOpdsV1(url, username, password)
                }
            }
        }
    }

    /**
     * Checks if the content-type indicates OPDS v2 (JSON).
     */
    private fun isOpdsV2ContentType(contentType: String): Boolean {
        return contentType.contains("application/opds+json") ||
               contentType.contains("application/json")
    }

    /**
     * Checks if the content appears to be JSON (starts with { or [).
     */
    private fun isOpdsV2Content(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    /**
     * Parses an OPDS v1 (XML/Atom) feed using Retrofit with SimpleXml.
     */
    private suspend fun parseOpdsV1(url: String, username: String?, password: String?): OpdsFeed {
        android.util.Log.d("OPDS_DEBUG", "Parsing OPDS v1 XML feed")
        try {
            val client = createOkHttpClient(username, password)
            val retrofitWithAuth = retrofit.newBuilder().client(client).build()
            val service = retrofitWithAuth.create(OpdsApiService::class.java)
            val dto = service.getFeed(url)
            android.util.Log.d("OPDS_DEBUG", "Successfully parsed OPDS v1 feed with ${dto.entries?.size ?: 0} entries")
            return mapV1ToDomain(dto)
        } catch (e: Exception) {
            android.util.Log.e("OPDS_DEBUG", "Failed to parse OPDS v1 feed", e)
            throw e
        }
    }

    /**
     * Parses an OPDS v2 (JSON) feed.
     */
    private fun parseOpdsV2(jsonContent: String): OpdsFeed {
        val feedDto = json.decodeFromString<Opds2FeedDto>(jsonContent)
        return mapV2ToDomain(feedDto)
    }

    override suspend fun loadMore(url: String, username: String?, password: String?): OpdsFeed {
        return fetchFeed(url, username, password)
    }

    override suspend fun search(feed: OpdsFeed, query: String, username: String?, password: String?): OpdsFeed {
        // Find OpenSearch link
        val openSearchLink = feed.links.firstOrNull { link ->
            link.rel == "search" || link.rel == "opensearchdescription"
        }

        if (openSearchLink != null) {
            // Use OpenSearch URL template
            val searchUrl = openSearchLink.href.replace("{searchTerms}", query)
            val client = createOkHttpClient(username, password)
            val retrofitWithAuth = retrofit.newBuilder().client(client).build()
            val service = retrofitWithAuth.create(OpdsApiService::class.java)
            val dto = service.getFeed(searchUrl)
            return mapV1ToDomain(dto)
        } else {
            // Fallback to basic search if no OpenSearch link
            val baseUrl = feed.links.firstOrNull { it.rel == "self" }?.href ?: feed.links.firstOrNull()?.href
            if (baseUrl != null) {
                val client = createOkHttpClient(username, password)
                val retrofitWithAuth = retrofit.newBuilder().client(client).build()
                val service = retrofitWithAuth.create(OpdsApiService::class.java)
                val dto = service.searchFeed(baseUrl, query)
                return mapV1ToDomain(dto)
            } else {
                throw Exception("No search URL available")
            }
        }
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

    /**
     * Encodes a URL properly, handling special characters while preserving the URL structure.
     * This is important for URLs with non-ASCII characters or special characters in paths.
     */
    private fun encodeUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            java.net.URI(
                uri.scheme,
                uri.userInfo,
                uri.host,
                uri.port,
                uri.path,
                uri.query,
                uri.fragment
            ).toASCIIString()
        } catch (e: Exception) {
            // If encoding fails, return original URL
            url
        }
    }

    private fun createOkHttpClient(username: String? = null, password: String? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            // Add UTF-8 Accept-Charset header for proper character handling
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept-Charset", "utf-8")
                    .header("Accept", "application/atom+xml, application/opds+json, application/xml, text/xml, */*")
                    .build()
                chain.proceed(request)
            }

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

    // ============ OPDS v1 (XML/Atom) Mapping ============

    private fun mapV1ToDomain(dto: OpdsFeedDto): OpdsFeed {
        return OpdsFeed(
            title = dto.title,
            entries = dto.entries.map { mapV1EntryToDomain(it) },
            links = dto.links.map { OpdsLink(it.href, it.rel, it.type, it.title) }
        )
    }

    private fun mapV1EntryToDomain(dto: us.blindmint.codex.data.remote.dto.OpdsEntryDto): OpdsEntry {
        // Join multiple authors with ", " separator
        val authorString = dto.authors
            .mapNotNull { it.name?.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { null }

        // Use content as fallback for summary (some feeds use content instead of summary)
        val description = dto.summary ?: dto.content

        return OpdsEntry(
            id = dto.id,
            title = dto.title,
            author = authorString,
            summary = description,
            published = dto.published ?: dto.updated,
            language = dto.language,
            publisher = dto.publisher,
            rights = dto.rights,
            identifiers = dto.identifiers,
            categories = dto.categories.map { it.term },
            links = dto.links.map { OpdsLink(it.href, it.rel, it.type, it.title) }
        )
    }

    // ============ OPDS v2 (JSON) Mapping ============

    private fun mapV2ToDomain(dto: Opds2FeedDto): OpdsFeed {
        // Combine entries from publications, navigation, and groups
        val entries = mutableListOf<OpdsEntry>()

        // Map publications (books)
        entries.addAll(dto.publications.map { mapV2PublicationToDomain(it) })

        // Map navigation items as category entries
        entries.addAll(dto.navigation.map { nav ->
            OpdsEntry(
                id = nav.href,
                title = nav.title,
                author = null,
                summary = null,
                published = null,
                language = null,
                publisher = null,
                rights = null,
                identifiers = emptyList(),
                categories = emptyList(),
                links = listOf(OpdsLink(nav.href, nav.rel ?: "subsection", nav.type, nav.title))
            )
        })

        // Map groups (nested collections of publications)
        dto.groups.forEach { group ->
            entries.addAll(group.publications.map { mapV2PublicationToDomain(it) })
            entries.addAll(group.navigation.map { nav ->
                OpdsEntry(
                    id = nav.href,
                    title = nav.title,
                    author = null,
                    summary = null,
                    published = null,
                    language = null,
                    publisher = null,
                    rights = null,
                    identifiers = emptyList(),
                    categories = emptyList(),
                    links = listOf(OpdsLink(nav.href, nav.rel ?: "subsection", nav.type, nav.title))
                )
            })
        }

        // Map feed-level links
        val links = dto.links.map { link ->
            OpdsLink(link.href, link.rel, link.type, link.title)
        }

        return OpdsFeed(
            title = dto.metadata?.title ?: "",
            entries = entries,
            links = links
        )
    }

    private fun mapV2PublicationToDomain(dto: Opds2PublicationDto): OpdsEntry {
        val metadata = dto.metadata

        // Join multiple authors
        val authorString = metadata.author
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { null }

        // Join multiple languages
        val language = metadata.language.firstOrNull()

        // Get publisher name
        val publisher = metadata.publisher.firstOrNull()?.name

        // Extract subjects/categories
        val categories = metadata.subject.map { it.name }

        // Extract series information
        val series = metadata.belongsTo?.series?.firstOrNull()
        val seriesName = series?.name
        val seriesIndex = series?.position?.toInt()

        // Build identifier list
        val identifiers = mutableListOf<String>()
        metadata.identifier?.let {
            if (it.startsWith("urn:")) identifiers.add(it)
            else identifiers.add("urn:uuid:$it")
        }

        // Map links (acquisition, etc.)
        val links = dto.links.map { link ->
            OpdsLink(
                href = link.href,
                rel = link.rel,
                type = link.type,
                title = link.title
            )
        }

        // Map image links (cover images)
        val imageLinks = dto.images.map { image ->
            OpdsLink(
                href = image.href,
                rel = if (image.width != null && image.width < 200) {
                    "http://opds-spec.org/image/thumbnail"
                } else {
                    "http://opds-spec.org/image"
                },
                type = image.type,
                title = null
            )
        }

        return OpdsEntry(
            id = metadata.identifier ?: dto.links.firstOrNull()?.href ?: "",
            title = metadata.title,
            author = authorString,
            summary = metadata.description,
            published = metadata.published ?: metadata.modified,
            language = language,
            publisher = publisher,
            rights = metadata.rights,
            identifiers = identifiers,
            categories = categories,
            series = seriesName,
            seriesIndex = seriesIndex,
            links = links + imageLinks
        )
    }

    /**
     * Downloads a cover image from the given URL.
     */
    override suspend fun downloadCover(url: String, username: String?, password: String?): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val client = createOkHttpClient(username, password)
                val request = okhttp3.Request.Builder()
                    .url(encodeUrl(url))
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext null
                }

                response.body?.bytes()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}