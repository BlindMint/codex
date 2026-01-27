/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.repository.OpdsRepository
import javax.inject.Inject

/**
 * PagingSource for OPDS catalog entries.
 *
 * This class handles paginated loading of OPDS feed entries, following RFC 5005 Feed Paging specification.
 * The key parameter is the next page URL (String), which is null for the first page.
 *
 * @property opdsRepository Repository for fetching OPDS feeds
 * @property sourceUrl Base URL of the OPDS source
 * @property username Optional username for authentication
 * @property password Optional password for authentication
 */
class OpdsPagingSource @Inject constructor(
    private val opdsRepository: OpdsRepository,
    private val sourceUrl: String,
    private val username: String?,
    private val password: String?
) : PagingSource<String, OpdsEntry>() {

    /**
     * Loads a page of OPDS entries.
     *
     * @param params LoadParams containing the key (next page URL) and load size
     * @return LoadResult.Page with loaded entries or LoadResult.Error on failure
     */
    override suspend fun load(
        params: LoadParams<String>
    ): LoadResult<String, OpdsEntry> {
        return try {
            val url = params.key ?: sourceUrl

            android.util.Log.d("OPDS_PAGING", "Loading page from URL: $url")

            val feed = opdsRepository.fetchFeed(url, username, password)

            android.util.Log.d("OPDS_PAGING", "Loaded ${feed.entries.size} total entries")

            val nextKey = feed.links.firstOrNull { it.rel == "next" }?.href

            if (nextKey != null) {
                android.util.Log.d("OPDS_PAGING", "Found next page URL: $nextKey")
            } else {
                android.util.Log.d("OPDS_PAGING", "No next page found - reached end of feed")
            }

            val downloadableItems = feed.entries.filter { entry ->
                entry.links.any { link ->
                    link.rel == "http://opds-spec.org/acquisition"
                }
            }

            android.util.Log.d("OPDS_PAGING", "Filtered to ${downloadableItems.size} downloadable items (excluding categories)")

            LoadResult.Page(
                data = downloadableItems,
                prevKey = null, // OPDS doesn't typically have previous page navigation
                nextKey = nextKey
            )
        } catch (e: Exception) {
            android.util.Log.e("OPDS_PAGING", "Error loading page", e)
            LoadResult.Error(e)
        }
    }

    /**
     * Returns the refresh key for the given PagingState.
     *
     * The refresh key is used when the data needs to be refreshed (e.g., when the user swipes to refresh).
     * For OPDS, we always refresh from the root source URL.
     *
     * @param state Current PagingState
     * @return The key to use for refresh (always sourceUrl)
     */
    override fun getRefreshKey(state: PagingState<String, OpdsEntry>): String? {
        return sourceUrl
    }
}
